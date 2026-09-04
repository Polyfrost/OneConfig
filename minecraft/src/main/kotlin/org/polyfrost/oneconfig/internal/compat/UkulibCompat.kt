//? ukulib_compat {
package org.polyfrost.oneconfig.internal.compat

import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.tabs.Tab
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.DoubleConsumer
import java.util.function.Function
import java.util.function.IntConsumer
import java.util.function.UnaryOperator

/**
 * Compat for ukulib (https://github.com/uku3lig/ukulib)
 *
 * ukulib screens describe their options with [WidgetCreator][net.uku3lig.ukulib.config.option.WidgetCreator]
 * arrays returned by `AbstractConfigScreen#getWidgets` or, for tabbed screens, by the
 * `ButtonTab#getWidgets` of every tab of `TabbedConfigScreen#getTabs`
 *
 * A creator only holds the value it was built with so values are read back by asking the screen (or tab) for a
 * fresh creator array every time a property is read; that also keeps the setters bound to the config instance
 * that is live right now instead of one that a reset has already thrown away
 */
object UkulibCompat {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/Ukulib-Compat")

    private const val DEFAULT_CATEGORY = "General"

    /** creators are refetched at most this often because the UI reads properties every frame */
    private const val CACHE_NANOS = 100_000_000L

    private val parsedScreens: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val parsedMods: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Called from the `BaseConfigScreen` constructor
     *
     * The parse itself is deferred to a render frame because building the tabs of a tabbed screen creates
     * widgets, and widget creation bakes font glyphs which corrupts the font atlas when it happens off-frame
     */
    @JvmStatic
    fun onScreenCreated(screen: Any) {
        if (!parsedScreens.add(screen.javaClass.name)) return
        CompatLoader.queueScreenWarmup {
            runCatching { parseScreen(screen) }
                .onFailure { LOGGER.warn("Failed to parse ukulib config screen {}", screen.javaClass.name, it) }
        }
    }

    private fun parseScreen(screen: Any) {
        val mod = CompatLoader.findModByClass(screen.javaClass) ?: return
        if (CompatLoader.nativeLoadedConfigs.contains(mod.id)) return
        // the root screen of a mod wins so sub screens opened later do not overwrite it
        if (!parsedMods.add(mod.id)) return

        val manager = readField(screen, "manager")
        if (manager == null) {
            parsedMods.remove(mod.id)
            return
        }

        val sources = collectSources(screen, manager)
        if (sources.isEmpty()) {
            parsedMods.remove(mod.id)
            return
        }

        val tree = Tree.tree()
        tree.id = mod.id
        tree.title = mod.name.takeIf { it.isNotBlank() }
            ?: screenTitle(screen)
            ?: mod.id
        tree.noCache = true
        mod.extractIconFile()?.let { tree.addMetadata("icon_path", it) }
        CompatLoader.originalScreenOpener(mod.id)?.let { tree.addMetadata("open_original_screen", it) }
        saveFunction(manager)?.let { tree.saveFunction = it }
        resetFunction(manager)?.let { tree.addMetadata(CompatSnapshots.CUSTOM_RESET_METADATA, it) }

        var added = false
        val usedIds = HashSet<String>()
        for (source in sources) {
            if (parseSource(source, tree, usedIds)) added = true
        }

        if (!added) {
            parsedMods.remove(mod.id)
            return
        }

        CompatSnapshots.register(tree)
        CompatLoader.nativeLoadedConfigs.add(mod.id)
    }

    private fun collectSources(screen: Any, manager: Any): List<CreatorSource> {
        val getWidgets = findMethod(screen.javaClass, "getWidgets", 1)
        if (getWidgets != null) {
            return listOf(
                CreatorSource(DEFAULT_CATEGORY, null, idPart(DEFAULT_CATEGORY, "general")) {
                    creators(getWidgets, screen, manager)
                }
            )
        }

        val getTabs = findMethod(screen.javaClass, "getTabs", 1) ?: return emptyList()
        val tabs = (invoke(getTabs, screen, config(manager)) as? Array<*>) ?: return emptyList()

        val sources = ArrayList<CreatorSource>()
        tabs.forEachIndexed { index, tab ->
            if (tab == null) return@forEachIndexed
            val tabWidgets = findMethod(tab.javaClass, "getWidgets", 1) ?: return@forEachIndexed
            val tabManager = readField(tab, "manager") ?: manager
            val titleComponent = (tab as? Tab)?.let { runCatching { it.tabTitle }.getOrNull() }
            val title = titleComponent?.string?.takeIf { it.isNotBlank() } ?: "Tab ${index + 1}"
            sources.add(
                CreatorSource(title, CompatIds.componentKey(titleComponent), idPart(title, "tab_${index + 1}")) {
                    creators(tabWidgets, tab, tabManager)
                }
            )
        }
        return sources
    }

    private fun creators(method: Method, owner: Any, manager: Any): List<Any> {
        val array = invoke(method, owner, config(manager)) as? Array<*> ?: return emptyList()
        return array.filterNotNull().map(::unwrap)
    }

    /** `WideWidgetCreator` only exists to make a widget take up the whole row */
    private fun unwrap(creator: Any): Any {
        var current = creator
        while (kindOf(current) == "WideWidgetCreator") {
            current = readField(current, "creator") ?: return current
        }
        return current
    }

    private fun parseSource(source: CreatorSource, tree: Tree, usedIds: MutableSet<String>): Boolean {
        val creators = source.creators(force = true)
        if (creators.isEmpty()) return false

        var subcategory = source.category
        var subcategoryKey = source.categoryKey
        var subcategoryPath = source.categoryPath
        var added = false

        creators.forEachIndexed { index, creator ->
            val kind = kindOf(creator) ?: return@forEachIndexed
            val key = creatorKey(creator)

            // a lined text option is a section header in every ukulib screen that uses one
            if (kind == "TextOption" && readField(creator, "drawLine") == true) {
                subcategory = key?.let { translate(it) } ?: source.category
                subcategoryKey = key
                subcategoryPath = "${source.categoryPath}/${idPart(key, "section")}"
                return@forEachIndexed
            }

            val ref = CreatorRef(source, index, creator.javaClass.name, key)
            val id = uniqueId(usedIds, "$subcategoryPath/${idPart(key ?: kind, "option")}")
            val property = runCatching { property(kind, creator, ref, id) }
                .onFailure { LOGGER.warn("Failed to parse ukulib option {}", key ?: kind, it) }
                .getOrNull() ?: return@forEachIndexed

            // an info line carries its text as the description so a title would only repeat it
            if (kind != "TextOption") key?.let { property.addMetadata("titleKey", it) }
            describe(creator, kind, key)?.let { property.description = it }
            property.category = source.category
            source.categoryKey?.let { property.addMetadata("categoryKey", it) }
            property.subcategory = subcategory
            subcategoryKey?.let { property.addMetadata("subcategoryKey", it) }

            tree.put(property)
            added = true
        }

        return added
    }

    private fun property(kind: String, creator: Any, ref: CreatorRef, id: String): Property<*>? = when (kind) {
        "CyclingOption" -> cyclingProperty(creator, ref, id)
        "SliderOption" -> sliderProperty(creator, ref, id, int = false)
        "IntSliderOption" -> sliderProperty(creator, ref, id, int = true)
        "InputOption" -> inputProperty(creator, ref, id, typed = false)
        "TypedInputOption" -> inputProperty(creator, ref, id, typed = true)
        "ColorOption" -> colorProperty(creator, ref, id)
        "SimpleButton", "ScreenOpenButton" -> buttonProperty(creator, ref, id, kind)
        "TextOption" -> infoProperty(creator, id)
        else -> null
    }

    private fun name(creator: Any, kind: String): String {
        val key = creatorKey(creator)
        if (kind == "SimpleButton") {
            (readField(creator, "text") as? Component)?.string?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return key?.let { translate(it) } ?: kind
    }

    private fun cyclingProperty(creator: Any, ref: CreatorRef, id: String): Property<*>? {
        val values = (readField(creator, "values") as? Collection<*>)?.toList() ?: return null
        if (values.isEmpty()) return null
        val label = readField(creator, "valueToText")

        if (values.size == 2 && values.all { it is Boolean }) {
            val property = Properties.functional(
                { ref.read("initialValue") as? Boolean ?: false },
                { value -> ref.consume("setter", value) },
                id,
                name(creator, "CyclingOption"),
                null,
                Boolean::class.javaObjectType,
            )
            property.addMetadata("visualizer", Visualizer.SwitchVisualizer::class.java)
            return property
        }

        val labels = values.map { value -> valueLabel(label, value) }
        val property = Properties.functional(
            {
                val current = ref.read("initialValue")
                (ref.readList("values")?.indexOf(current) ?: values.indexOf(current)).coerceAtLeast(0)
            },
            { value ->
                val index = (value as? Number)?.toInt() ?: return@functional
                val options = ref.readList("values") ?: values
                options.getOrNull(index)?.let { ref.consume("setter", it) }
            },
            id,
            name(creator, "CyclingOption"),
            null,
            Int::class.javaObjectType,
        )
        property.addMetadata("visualizer", Visualizer.DropdownVisualizer::class.java)
        property.addMetadata("options", labels)
        return property
    }

    private fun sliderProperty(creator: Any, ref: CreatorRef, id: String, int: Boolean): Property<*>? {
        val min = (readField(creator, "min") as? Number)?.toFloat() ?: return null
        val max = (readField(creator, "max") as? Number)?.toFloat() ?: return null
        val step = (readField(creator, "step") as? Number)?.toFloat()

        val property = if (int) {
            Properties.functional(
                { (ref.read("initialValue") as? Number)?.toInt() ?: 0 },
                { value -> ref.consume("setter", (value as? Number)?.toInt() ?: 0) },
                id,
                name(creator, "IntSliderOption"),
                null,
                Int::class.javaObjectType,
            )
        } else {
            Properties.functional(
                { (ref.read("initialValue") as? Number)?.toFloat() ?: 0f },
                { value -> ref.consume("setter", (value as? Number)?.toFloat() ?: 0f) },
                id,
                name(creator, "SliderOption"),
                null,
                Float::class.javaObjectType,
            )
        }

        property.addMetadata("visualizer", Visualizer.SliderVisualizer::class.java)
        property.addMetadata("min", min)
        property.addMetadata("max", max)
        step?.takeIf { it > 0f }?.let { property.addMetadata("step", it) }
        return property
    }

    private fun inputProperty(creator: Any, ref: CreatorRef, id: String, typed: Boolean): Property<*> {
        val converter = if (typed) readField(creator, "converter") else null

        val property = Properties.functional(
            { ref.read("initialValue") as? String ?: "" },
            { text: String ->
                if (typed) {
                    @Suppress("UNCHECKED_CAST")
                    val converted = (converter as? Function<Any?, *>)
                        ?.let { runCatching { it.apply(text) }.getOrNull() as? java.util.Optional<Any?> }
                    if (converted != null && converted.isPresent) ref.consume("setter", converted.get())
                } else {
                    ref.consume("setter", text)
                }
            },
            id,
            name(creator, if (typed) "TypedInputOption" else "InputOption"),
            null,
            String::class.java,
        )

        property.addMetadata("visualizer", Visualizer.TextVisualizer::class.java)
        creatorKey(creator)?.let { property.addMetadata("placeholderKey", it) }
        (readField(creator, "maxLength") as? Number)?.let { property.addMetadata("maxLength", it.toInt()) }
        return property
    }

    private fun colorProperty(creator: Any, ref: CreatorRef, id: String): Property<*> {
        val allowAlpha = readField(creator, "allowAlpha") == true

        val property = Properties.functional(
            {
                val argb = (ref.read("initialValue") as? Number)?.toInt() ?: 0
                java.awt.Color(argb, true)
            },
            { value -> ref.consume("setter", value.rgb) },
            id,
            name(creator, "ColorOption"),
            null,
            java.awt.Color::class.java,
        )

        property.addMetadata("visualizer", Visualizer.ColorVisualizer::class.java)
        if (!allowAlpha) property.addMetadata("noAlpha", true)
        return property
    }

    private fun buttonProperty(creator: Any, ref: CreatorRef, id: String, kind: String): Property<*> {
        val text = name(creator, kind)
        val property = Properties.dummy(id, text, null)
        property.addMetadata("visualizer", Visualizer.ButtonVisualizer::class.java)
        val textKey = creatorKey(creator)
        if (textKey != null) property.addMetadata("textKey", textKey) else property.addMetadata("text", text)
        property.addMetadata("runnable", Runnable {
            runCatching {
                val current = ref.get() ?: return@runCatching
                if (kind == "ScreenOpenButton") openScreen(current) else press(current)
            }.onFailure { LOGGER.warn("Failed to run ukulib button action", it) }
        })
        return property
    }

    private fun openScreen(creator: Any) {
        @Suppress("UNCHECKED_CAST")
        val opener = readField(creator, "opener") as? UnaryOperator<Screen> ?: return
        val parent: Screen = Platform.screen().current<Screen>() ?: return
        val screen = opener.apply(parent) ?: return
        Platform.screen().display(screen)
    }

    private fun press(creator: Any) {
        val action = readField(creator, "action") as? Button.OnPress ?: return
        // the action is handed the button that owns it, which ukulib options never read, so a throwaway is enough
        action.onPress(Button.builder(Component.empty()) { }.build())
    }

    private fun infoProperty(creator: Any, id: String): Property<*>? {
        val key = creatorKey(creator) ?: return null
        val property = Properties.dummy(id, null, translate(key))
        property.addMetadata("visualizer", Visualizer.InfoVisualizer::class.java)
        property.addMetadata("descriptionKey", key)
        return property
    }

    private fun describe(creator: Any, kind: String, key: String?): String? {
        val tooltip = readField(creator, "tooltipSupplier", "tooltipFactory")
            ?.let { tooltipText(it, readField(creator, "initialValue")) }
        if (tooltip != null) return tooltip
        if (kind == "TextOption") return null
        return key?.let { translateOrNull("$it.tooltip") }
    }

    private fun tooltipText(supplier: Any, value: Any?): String? {
        @Suppress("UNCHECKED_CAST")
        val function = supplier as? Function<Any?, *> ?: return null
        val tooltip = runCatching { function.apply(value) }.getOrNull() ?: return null
        return tooltip.javaClass.declaredFields.asSequence()
            .filter { Component::class.java.isAssignableFrom(it.type) }
            .mapNotNull { field ->
                runCatching {
                    field.isAccessible = true
                    (field.get(tooltip) as? Component)?.string
                }.getOrNull()
            }
            .firstOrNull { it.isNotBlank() }
    }

    private fun valueLabel(label: Any?, value: Any?): String {
        @Suppress("UNCHECKED_CAST")
        val function = label as? Function<Any?, *>
        val component = function?.let { runCatching { it.apply(value) }.getOrNull() }
        (component as? Component)?.string?.takeIf { it.isNotBlank() }?.let { return it }
        return when (value) {
            is Enum<*> -> value.name
            else -> value?.toString() ?: ""
        }
    }

    private fun screenTitle(screen: Any): String? =
        (screen as? Screen)?.title?.string?.takeIf { it.isNotBlank() }

    private fun saveFunction(manager: Any): Runnable? {
        val save = findMethod(manager.javaClass, "saveConfig", 0) ?: return null
        return Runnable {
            runCatching { save.invoke(manager) }
                .onFailure { LOGGER.warn("Failed to save ukulib config", it) }
        }
    }

    private fun resetFunction(manager: Any): Runnable? {
        val reset = findMethod(manager.javaClass, "resetConfig", 0) ?: return null
        val save = findMethod(manager.javaClass, "saveConfig", 0)
        return Runnable {
            runCatching {
                reset.invoke(manager)
                save?.invoke(manager)
            }.onFailure { LOGGER.warn("Failed to reset ukulib config", it) }
        }
    }

    private fun config(manager: Any): Any? =
        findMethod(manager.javaClass, "getConfig", 0)?.let { runCatching { it.invoke(manager) }.getOrNull() }

    private fun translate(key: String): String = translateOrNull(key) ?: cleanKey(key)

    private fun translateOrNull(key: String): String? {
        val translated = runCatching { Component.translatable(key).string }.getOrNull() ?: return null
        return if (translated == key || translated.isBlank()) null else translated
    }

    private fun cleanKey(key: String): String {
        val segment = key.substringAfterLast('.').ifBlank { key }
        return segment.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun creatorKey(creator: Any): String? =
        (readField(creator, "key", "suggestionKey") as? String)?.takeIf { it.isNotBlank() }

    private fun kindOf(creator: Any): String? {
        var current: Class<*>? = creator.javaClass
        while (current != null && current != Any::class.java) {
            when (current.simpleName) {
                "WideWidgetCreator", "CyclingOption", "SliderOption", "IntSliderOption", "InputOption",
                "TypedInputOption", "ColorOption", "SimpleButton", "ScreenOpenButton", "TextOption",
                -> return current.simpleName
            }
            current = current.superclass
        }
        return null
    }

    /**
     * One `getWidgets` implementation, either a plain screen's or one tab's
     *
     * The creator array is cached briefly so a screen full of properties does not refetch it once per property
     * per frame
     */
    private class CreatorSource(
        val category: String,
        val categoryKey: String?,
        val categoryPath: String,
        private val fetch: () -> List<Any>,
    ) {
        private var cache: List<Any> = emptyList()
        private var stamp = 0L

        fun creators(force: Boolean = false): List<Any> {
            val now = System.nanoTime()
            if (force || cache.isEmpty() || now - stamp > CACHE_NANOS) {
                runCatching(fetch).getOrNull()?.takeIf { it.isNotEmpty() }?.let { cache = it }
                stamp = now
            }
            return cache
        }
    }

    /** Points at one creator of a [CreatorSource], resolved again on every read and write */
    private class CreatorRef(
        private val source: CreatorSource,
        private val index: Int,
        private val className: String,
        private val key: String?,
    ) {
        fun get(): Any? {
            val creators = source.creators()
            creators.getOrNull(index)?.takeIf { matches(it) }?.let { return it }
            // an option list can be built conditionally so fall back to finding the same option elsewhere
            return creators.firstOrNull { matches(it) }
        }

        private fun matches(creator: Any): Boolean =
            creator.javaClass.name == className && (key == null || creatorKey(creator) == key)

        fun read(vararg names: String): Any? = get()?.let { readField(it, *names) }

        fun readList(name: String): List<Any?>? = (read(name) as? Collection<*>)?.toList()

        fun consume(name: String, value: Any?) {
            val target = read(name) ?: return
            runCatching {
                when (target) {
                    is IntConsumer -> target.accept((value as? Number)?.toInt() ?: return)
                    is DoubleConsumer -> target.accept((value as? Number)?.toDouble() ?: return)
                    else -> {
                        @Suppress("UNCHECKED_CAST")
                        (target as? Consumer<Any?>)?.accept(value)
                    }
                }
            }.onFailure { LOGGER.warn("Failed to write ukulib option {}", key ?: className, it) }
        }
    }

    private fun readField(target: Any, vararg names: String): Any? {
        val field = findField(target.javaClass, *names) ?: return null
        return runCatching { field.get(target) }.getOrNull()
    }

    private fun findField(cls: Class<*>, vararg names: String): Field? {
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            for (name in names) {
                current.declaredFields.firstOrNull { it.name == name }?.let {
                    it.isAccessible = true
                    return it
                }
            }
            current = current.superclass
        }
        return null
    }

    private fun findMethod(cls: Class<*>, name: String, params: Int): Method? {
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            current.declaredMethods.firstOrNull { it.name == name && it.parameterCount == params }?.let {
                it.isAccessible = true
                return it
            }
            current = current.superclass
        }
        return null
    }

    private fun invoke(method: Method, owner: Any, vararg args: Any?): Any? =
        runCatching { method.invoke(owner, *args) }.getOrNull()
}

//? }
