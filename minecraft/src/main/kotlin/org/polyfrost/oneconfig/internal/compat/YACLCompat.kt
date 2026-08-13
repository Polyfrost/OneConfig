//? yacl_compat {
package org.polyfrost.oneconfig.internal.compat

import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.event.v1.EventDelay
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.compat.CompatIds.componentKey
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId

object YACLCompat {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/YACL-Compat")

    private const val FLAG_SETTLE_FRAMES = 20

    private class Ctx(val yacl: Any) {
        val usedIds = HashSet<String>()
        val properties = ArrayList<Property<*>>()

        var cachedScreen: Any? = null

        private val pendingFlags = LinkedHashSet<Any>()

        private var writeStamp = 0L
        private var drainScheduled = false

        fun revaluateDisplays() {
            for (property in properties) property.revaluateDisplay()
        }

        fun pendFlags(flags: Collection<Any>) {
            if (flags.isEmpty()) return
            synchronized(pendingFlags) { pendingFlags.addAll(flags) }
            scheduleDrain()
        }

        private fun scheduleDrain() {
            val client = net.minecraft.client.Minecraft.getInstance()
            if (!client.isSameThread) {
                client.execute { scheduleDrain() }
                return
            }
            writeStamp++
            if (drainScheduled) return
            drainScheduled = true
            awaitSettle()
        }

        private fun awaitSettle() {
            val stamp = writeStamp
            EventDelay.tick(FLAG_SETTLE_FRAMES) {
                if (writeStamp != stamp) {
                    awaitSettle()
                } else {
                    drainScheduled = false
                    runPendingFlags()
                }
            }
        }

        fun runPendingFlags() {
            val client = net.minecraft.client.Minecraft.getInstance()
            if (!client.isSameThread) {
                val pending = synchronized(pendingFlags) { pendingFlags.isNotEmpty() }
                if (pending) client.execute { runPendingFlags() }
                return
            }
            val flags = synchronized(pendingFlags) {
                if (pendingFlags.isEmpty()) return
                pendingFlags.toList().also { pendingFlags.clear() }
            }
            for (flag in flags) {
                runCatching { (flag as java.util.function.Consumer<*>).let { consumeFlag(it, client) } }
                    .onFailure { LOGGER.warn("Failed to run YACL option flag", it) }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun consumeFlag(flag: java.util.function.Consumer<*>, client: Any) {
            (flag as java.util.function.Consumer<Any>).accept(client)
        }
    }

    @JvmStatic
    fun parseYACL(yaclInstance: Any) {
        runCatching {
            val mod = CompatLoader.findFirstMod()
            if (mod != null && CompatLoader.nativeLoadedConfigs.contains(mod.id)) {
                return
            }
            LOGGER.info("Creating config wrapper for " + mod?.id)
            val tree = parseYACLInstance(yaclInstance, mod)
            if (tree != null) {
                CompatSnapshots.register(tree)
                CompatLoader.markFirstModAsSkip()
            }
        }.onFailure {
            LOGGER.warn("Failed to parse YACL config", it)
        }
    }

    private fun parseYACLInstance(yaclInstance: Any, mod: ModInfo?): Tree? {
        val yaclClass = yaclInstance::class.java

        val categoriesMethod = yaclClass.methods.firstOrNull {
            it.name == "categories" && it.parameterCount == 0
        } ?: return null

        @Suppress("UNCHECKED_CAST")
        val categories = categoriesMethod.invoke(yaclInstance) as? Collection<*> ?: return null
        if (categories.isEmpty()) return null

        val saveMethod = yaclClass.methods.firstOrNull {
            it.name == "saveFunction" && it.parameterCount == 0
        }
        val saveRunnable = saveMethod?.let {
            runCatching { it.invoke(yaclInstance) as? Runnable }.getOrNull()
        }

        val ctx = Ctx(yaclInstance)

        val tree = Tree.tree()
        tree.id = mod?.id ?: yaclInstance::class.java.name
        tree.title = mod?.name?.takeIf { it.isNotBlank() } ?: "YACL Config"
        tree.noCache = true
        tree.saveFunction = Runnable {
            runCatching { saveRunnable?.run() }.onFailure { LOGGER.warn("Failed to save YACL config", it) }
            ctx.runPendingFlags()
        }
        // YACL exposes no icon so fall back to the mod icon (same source Mod Menu uses)
        mod?.extractIconFile()?.let {
            tree.addMetadata("icon_path", it)
        }
        mod?.id?.let { id ->
            CompatLoader.originalScreenOpener(id)?.let { tree.addMetadata("open_original_screen", it) }
        }

        for (category in categories) {
            if (category == null) continue
            parseCategory(category, tree, ctx)
        }
        ctx.revaluateDisplays()

        return tree
    }

    private fun parseCategory(category: Any, root: Tree, ctx: Ctx) {
        val categoryClass = category::class.java

        val nameMethod = categoryClass.methods.firstOrNull { it.name == "name" && it.parameterCount == 0 }
        val nameComponent = nameMethod?.let { runCatching { it.invoke(category) }.getOrNull() }
        val categoryName = resolveComponent(nameComponent)?.nonBlankOrNull() ?: "General"
        val categoryPath = idPart(componentKey(nameComponent) ?: categoryName, "general")

        if (inheritsFrom(categoryClass, "PlaceholderCategory")) {
            parsePlaceholderCategory(category, categoryName, categoryPath, root, ctx)
            return
        }

        val groupsMethod = categoryClass.methods.firstOrNull {
            it.name == "groups" && it.parameterCount == 0
        }

        @Suppress("UNCHECKED_CAST")
        val groups = groupsMethod?.invoke(category) as? Collection<*> ?: return

        for (group in groups) {
            if (group == null) continue
            parseGroup(group, categoryName, categoryPath, root, ctx)
        }
    }

    private fun parsePlaceholderCategory(
        category: Any,
        categoryName: String,
        categoryPath: String,
        root: Tree,
        ctx: Ctx,
    ) {
        val screenFactory = category::class.java.methods
            .firstOrNull { it.name == "screen" && it.parameterCount == 0 }
            ?.let { runCatching { it.invoke(category) as? java.util.function.BiFunction<*, *, *> }.getOrNull() }
            ?: return

        val tooltip = category::class.java.methods
            .firstOrNull { it.name == "tooltip" && it.parameterCount == 0 }
            ?.let { runCatching { resolveComponent(it.invoke(category)) }.getOrNull() }
            ?.nonBlankOrNull()

        val property = Properties.dummy(
            id = uniqueId(ctx.usedIds, "$categoryPath/open"),
            name = categoryName,
            description = tooltip,
        )
        property.addMetadata("visualizer", Visualizer.ButtonVisualizer::class.java)
        property.addMetadata("textKey", "Open")
        property.category = categoryName
        property.metadata?.put("runnable", Runnable { openPlaceholderScreen(screenFactory, ctx) })

        root.put(property)
    }

    @Suppress("UNCHECKED_CAST")
    private fun openPlaceholderScreen(screenFactory: java.util.function.BiFunction<*, *, *>, ctx: Ctx) {
        runCatching {
            val client = net.minecraft.client.Minecraft.getInstance()
            val parent = yaclScreen(ctx) ?: Platform.screen().current<Any>()
            val screen = (screenFactory as java.util.function.BiFunction<Any?, Any?, Any?>)
                .apply(client, parent) ?: return@runCatching
            Platform.screen().display(screen)
        }.onFailure { LOGGER.warn("Failed to open YACL placeholder screen", it) }
    }

    private fun yaclScreen(ctx: Ctx): Any? {
        ctx.cachedScreen?.let { return it }
        return runCatching {
            val screenClass = Class.forName(
                "dev.isxander.yacl3.gui.YACLScreen",
                false,
                ctx.yacl::class.java.classLoader,
            )
            val constructor = screenClass.constructors.firstOrNull { it.parameterCount == 2 }
                ?: return@runCatching null
            constructor.newInstance(ctx.yacl, Platform.screen().current<Any>())
        }.getOrNull()?.also { ctx.cachedScreen = it }
    }

    private fun parseGroup(
        group: Any,
        categoryName: String,
        categoryPath: String,
        root: Tree,
        ctx: Ctx,
    ) {
        val groupClass = group::class.java

        val nameMethod = groupClass.methods.firstOrNull { it.name == "name" && it.parameterCount == 0 }
        val nameComponent = nameMethod?.let { runCatching { it.invoke(group) }.getOrNull() }
        val groupName = resolveComponent(nameComponent)?.nonBlankOrNull()
        val isRoot = groupClass.methods.firstOrNull {
            it.name == "isRoot" &&
                it.parameterCount == 0 &&
                (it.returnType == Boolean::class.java || it.returnType == Boolean::class.javaPrimitiveType)
        }?.let { runCatching { it.invoke(group) as? Boolean }.getOrNull() } ?: false
        val subcategoryName = if (isRoot) null else groupName

        if (isListOption(groupClass)) {
            // the group is the option itself so it keeps the category path rather than nesting under itself
            runCatching { parseOption(group, categoryName, null, categoryPath, root, ctx) }
                .onFailure { LOGGER.warn("Failed to parse YACL list option", it) }
            return
        }

        val groupPath = if (isRoot) {
            categoryPath
        } else {
            "$categoryPath/${idPart(componentKey(nameComponent) ?: groupName, "group")}"
        }

        val optionsMethod = groupClass.methods.firstOrNull {
            it.name == "options" && it.parameterCount == 0
        }

        @Suppress("UNCHECKED_CAST")
        val options = optionsMethod?.invoke(group) as? Collection<*> ?: return
        if (options.isEmpty()) return

        for (option in options) {
            if (option == null) continue
            runCatching { parseOption(option, categoryName, subcategoryName, groupPath, root, ctx) }
                .onFailure { LOGGER.warn("Failed to parse YACL option", it) }
        }
    }

    private fun parseOption(
        option: Any,
        categoryName: String,
        subcategoryName: String?,
        groupPath: String,
        root: Tree,
        ctx: Ctx,
    ) {
        val usedIds = ctx.usedIds
        val optionClass = option::class.java

        val nameMethod = optionClass.methods.firstOrNull { it.name == "name" && it.parameterCount == 0 }
        val descMethod = optionClass.methods.firstOrNull { it.name == "description" && it.parameterCount == 0 }
        val nameComponent = nameMethod?.let { runCatching { it.invoke(option) }.getOrNull() }
        val name = resolveComponent(nameComponent)?.nonBlankOrNull() ?: return
        // ids are claimed only once a property is built so skipped options do not shift the suffixes
        val optionPath = "$groupPath/${idPart(componentKey(nameComponent) ?: name, "option")}"
        val desc = descMethod?.let {
            runCatching {
                val descResult = it.invoke(option)
                // description() returns OptionDescription which has text() -> Component
                val textMethod = descResult?.javaClass?.methods?.firstOrNull { m -> m.name == "text" && m.parameterCount == 0 }
                textMethod?.let { tm -> resolveComponent(tm.invoke(descResult)) }?.nonBlankOrNull()
            }.getOrNull()
        }

        // ButtonOption exposes no readable value (its binding throws) so handle it before the binding logic
        if (isButtonOption(optionClass)) {
            parseButtonOption(
                option, name, desc, uniqueId(usedIds, optionPath), categoryName, subcategoryName, root, ctx,
            )
            return
        }

        if (inheritsFrom(optionClass, "LabelOption")) {
            parseLabelOption(option, name, uniqueId(usedIds, optionPath), categoryName, subcategoryName, root)
            return
        }

        val bindingMethod = optionClass.methods.firstOrNull { it.name == "binding" && it.parameterCount == 0 }
        val binding = bindingMethod?.let { runCatching { it.invoke(option) }.getOrNull() }
        val defaultValue = binding?.let {
            it::class.java.methods
                .firstOrNull { m -> m.name == "defaultValue" && m.parameterCount == 0 }
                ?.apply { isAccessible = true }
                ?.let { m -> runCatching { m.invoke(binding) }.getOrNull() }
        }

        val pendingMethod = optionClass.methods
            .firstOrNull { it.name == "pendingValue" && it.parameterCount == 0 }?.apply { isAccessible = true }
        val requestSetMethod = optionClass.methods
            .firstOrNull { it.name == "requestSet" && it.parameterCount == 1 }?.apply { isAccessible = true }
        val applyValueMethod = optionClass.methods
            .firstOrNull { it.name == "applyValue" && it.parameterCount == 0 }?.apply { isAccessible = true }

        val getter: () -> Any?
        /** Writes the value and reports whether it changed which is what gates the flags */
        val baseSetter: (Any?) -> Boolean

        if (pendingMethod != null && requestSetMethod != null) {
            getter = { pendingMethod.invoke(option) }
            baseSetter = { value ->
                requestSetMethod.invoke(option, value)
                applyValueMethod?.let { it.invoke(option) as? Boolean } ?: true
            }
        } else if (binding != null) {
            val bindingClass = binding::class.java
            val getValueMethod = bindingClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }?.apply { isAccessible = true } ?: return
            val setValueMethod = bindingClass.methods.firstOrNull { it.name == "setValue" && it.parameterCount == 1 }?.apply { isAccessible = true } ?: return
            getter = { getValueMethod.invoke(binding) }
            baseSetter = { value ->
                val before = runCatching { getValueMethod.invoke(binding) }.getOrNull()
                setValueMethod.invoke(binding, value)
                before != value
            }
        } else {
            return
        }

        val setter: (Any?) -> Unit = { value ->
            val changed = runCatching { baseSetter(value) }
                .onFailure { LOGGER.warn("Failed to write YACL option '$name'", it) }
                .getOrDefault(false)
            if (changed) ctx.pendFlags(resolveFlags(option))
            ctx.revaluateDisplays()
        }

        // value-less options expose a binding whose getValue() throws UnsupportedOperationException
        // (EmptyBinderImpl) so skip them instead of logging a warning
        val currentValue = runCatching { getter() }.getOrNull() ?: return

        if (currentValue is List<*>) {
            parseListOption(
                option,
                currentValue,
                name,
                desc,
                uniqueId(usedIds, optionPath),
                getter,
                setter,
                defaultValue,
                categoryName,
                subcategoryName,
                root,
                ctx,
            )
            return
        }

        if (currentValue !is Boolean && currentValue !is Enum<*>) {
            val cycling = resolveCyclingValues(option)
            if (cycling != null && cycling.size > 1) {
                parseCyclingOption(
                    option,
                    cycling,
                    name,
                    desc,
                    uniqueId(usedIds, optionPath),
                    getter,
                    setter,
                    defaultValue,
                    categoryName,
                    subcategoryName,
                    root,
                    ctx,
                )
                return
            }
        }

        if (currentValue is String) {
            val allowed = resolveDropdownStringValues(option)
            if (allowed != null && allowed.isNotEmpty()) {
                parseStringDropdown(
                    option,
                    allowed,
                    name,
                    desc,
                    uniqueId(usedIds, optionPath),
                    getter,
                    setter,
                    defaultValue,
                    categoryName,
                    subcategoryName,
                    root,
                    ctx,
                )
                return
            }
        }

        val numberField = currentValue is Number && isNumberFieldController(option)

        val visualizer: Class<out Visualizer> = when {
            currentValue is Boolean -> Visualizer.SwitchVisualizer::class.java
            numberField -> Visualizer.NumberVisualizer::class.java
            currentValue is Int || currentValue is Float || currentValue is Double || currentValue is Long ->
                Visualizer.SliderVisualizer::class.java
            currentValue is String -> Visualizer.TextVisualizer::class.java
            currentValue is Enum<*> -> Visualizer.DropdownVisualizer::class.java
            currentValue is java.awt.Color -> Visualizer.ColorVisualizer::class.java
            else -> return
        }

        val property = Properties.functional(
            getter = { getter() },
            setter = { value -> setter(value) },
            id = uniqueId(usedIds, optionPath),
            name = name,
            description = desc,
        )

        property.addMetadata("visualizer", visualizer)
        defaultValue?.let { property.addMetadata("default", it) }
        property.category = categoryName
        property.subcategory = subcategoryName

        when {
            currentValue is Enum<*> -> {
                val constants = enumClassOf(currentValue)?.enumConstants?.toList() ?: emptyList()
                val values = resolveCyclingValues(option)
                    ?.takeIf { list -> list.isNotEmpty() && list.all { it is Enum<*> } }
                    ?: constants
                property.addMetadata("options", values.map { it.toString() })
                property.addMetadata("optionValues", values)
                resolveEnumLabels(option, values)?.let { property.addMetadata("optionLabels", it) }
            }
            numberField -> {
                val range = resolveSliderRange(option)
                val type = currentValue.javaClass
                property.addMetadata("min", boundedFloat(range?.first, type) ?: -Float.MAX_VALUE)
                property.addMetadata("max", boundedFloat(range?.second, type) ?: Float.MAX_VALUE)
            }
            currentValue is Number -> {
                val range = resolveSliderRange(option)
                property.addMetadata("min", range?.first ?: 0f)
                property.addMetadata("max", range?.second ?: 100f)
                range?.third?.let { property.addMetadata("step", it) }
            }
            currentValue is java.awt.Color -> {
                if (resolveAllowAlpha(option) == false) property.addMetadata("noAlpha", true)
            }
        }

        registerProperty(property, option, ctx)
        root.put(property)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseListOption(
        option: Any,
        currentValue: List<*>,
        name: String,
        desc: String?,
        id: String,
        getter: () -> Any?,
        setter: (Any?) -> Unit,
        defaultValue: Any?,
        categoryName: String,
        subcategoryName: String?,
        root: Tree,
        ctx: Ctx,
    ) {
        val element = currentValue.firstOrNull { it != null }?.javaClass ?: String::class.java

        resolveEnumEntryValues(option, currentValue, defaultValue)?.let { values ->
            parseEnumListOption(
                option, values, name, desc, id, getter, setter, defaultValue,
                categoryName, subcategoryName, root, ctx,
            )
            return
        }

        val numeric = Number::class.java.isAssignableFrom(element)
        val color = element == java.awt.Color::class.java

        val visualizer: Class<out Visualizer> = when {
            color -> Visualizer.ColorListVisualizer::class.java
            numeric -> Visualizer.NumberListVisualizer::class.java
            element == String::class.java -> Visualizer.TextListVisualizer::class.java
            else -> return // record entries have no OneConfig list equivalent
        }

        fun read(value: Any?): Any? = when {
            color -> (value as? java.awt.Color)?.rgb ?: -1
            numeric -> value as? Number ?: 0
            else -> value?.toString() ?: ""
        }

        fun write(value: Any?): Any? = when {
            color -> java.awt.Color((value as? Number)?.toInt() ?: -1, true)
            numeric -> coerceNumber(value, element)
            else -> value?.toString() ?: ""
        }

        val property = Properties.functional(
            getter = {
                val list = runCatching { getter() as? List<*> }.getOrNull() ?: emptyList<Any?>()
                ArrayList(list.map(::read))
            },
            setter = { value: List<Any?> -> setter(value.mapTo(ArrayList(), ::write)) },
            id = id,
            name = name,
            description = desc,
            type = List::class.java,
        )

        property.addMetadata("visualizer", visualizer)
        (defaultValue as? List<*>)?.let { property.addMetadata("default", ArrayList(it.map(::read))) }
        maximumNumberOfEntries(option)?.let { property.addMetadata("maxEntries", it) }
        if (numeric) {
            val range = resolveEntryRange(option)
            property.addMetadata("min", range?.first ?: -Float.MAX_VALUE)
            property.addMetadata("max", range?.second ?: Float.MAX_VALUE)
            range?.third?.let { property.addMetadata("step", it) }
        }
        property.category = categoryName
        property.subcategory = subcategoryName

        registerProperty(property, option, ctx)
        root.put(property)
    }

    private fun parseEnumListOption(
        option: Any,
        values: List<Any>,
        name: String,
        desc: String?,
        id: String,
        getter: () -> Any?,
        setter: (Any?) -> Unit,
        defaultValue: Any?,
        categoryName: String,
        subcategoryName: String?,
        root: Tree,
        ctx: Ctx,
    ) {
        val byName = values.associateBy { (it as Enum<*>).name }

        fun namesOf(value: Any?): Array<String> =
            (value as? List<*>)?.mapNotNull { (it as? Enum<*>)?.name }?.toTypedArray() ?: emptyArray()

        val property = Properties.functional(
            getter = { namesOf(runCatching { getter() }.getOrNull()) },
            setter = { selected: Array<String> ->
                setter(selected.mapNotNullTo(ArrayList()) { byName[it] })
            },
            id = id,
            name = name,
            description = desc,
            type = Array<String>::class.java,
        )

        property.addMetadata("visualizer", Visualizer.DraggableListVisualizer::class.java)
        property.addMetadata("options", values.map { (it as Enum<*>).name }.toTypedArray())
        property.addMetadata("checkable", true)
        property.addMetadata("optionLabels", values.map { it.toString() })
        (defaultValue as? List<*>)?.let { property.addMetadata("default", namesOf(it)) }
        property.category = categoryName
        property.subcategory = subcategoryName

        registerProperty(property, option, ctx)
        root.put(property)
    }

    private fun parseCyclingOption(
        option: Any,
        values: List<Any?>,
        name: String,
        desc: String?,
        id: String,
        getter: () -> Any?,
        setter: (Any?) -> Unit,
        defaultValue: Any?,
        categoryName: String,
        subcategoryName: String?,
        root: Tree,
        ctx: Ctx,
    ) {
        val labels = resolveCyclingLabels(option, values)

        val property = Properties.functional(
            getter = { values.indexOf(runCatching { getter() }.getOrNull()).coerceAtLeast(0) },
            setter = { index: Int -> values.getOrNull(index)?.let { setter(it) } },
            id = id,
            name = name,
            description = desc,
            type = Int::class.javaObjectType,
        )

        property.addMetadata("visualizer", Visualizer.DropdownVisualizer::class.java)
        property.addMetadata("options", labels)
        defaultValue?.let { default ->
            values.indexOf(default).takeIf { it >= 0 }?.let { property.addMetadata("default", it) }
        }
        property.category = categoryName
        property.subcategory = subcategoryName

        registerProperty(property, option, ctx)
        root.put(property)
    }

    private fun parseStringDropdown(
        option: Any,
        values: List<String>,
        name: String,
        desc: String?,
        id: String,
        getter: () -> Any?,
        setter: (Any?) -> Unit,
        defaultValue: Any?,
        categoryName: String,
        subcategoryName: String?,
        root: Tree,
        ctx: Ctx,
    ) {
        val property = Properties.functional(
            getter = { runCatching { getter() as? String }.getOrNull() ?: "" },
            setter = { value: String -> setter(value) },
            id = id,
            name = name,
            description = desc,
            type = String::class.java,
        )

        property.addMetadata("visualizer", Visualizer.DropdownVisualizer::class.java)
        property.addMetadata("options", values)
        property.addMetadata("optionValues", values)
        (defaultValue as? String)?.let { property.addMetadata("default", it) }
        property.category = categoryName
        property.subcategory = subcategoryName

        registerProperty(property, option, ctx)
        root.put(property)
    }

    private fun parseLabelOption(
        option: Any,
        name: String,
        id: String,
        categoryName: String,
        subcategoryName: String?,
        root: Tree,
    ) {
        val label = option::class.java.methods
            .firstOrNull { it.name == "label" && it.parameterCount == 0 }
            ?.let { runCatching { resolveComponent(it.invoke(option)) }.getOrNull() }
            ?.nonBlankOrNull()
            ?: return

        val property = Properties.dummy(id = id, name = name, description = label)
        property.addMetadata("visualizer", Visualizer.InfoVisualizer::class.java)
        property.category = categoryName
        property.subcategory = subcategoryName

        root.put(property)
    }

    private fun registerProperty(property: Property<*>, option: Any?, ctx: Ctx) {
        ctx.properties.add(property)
        val availableMethod = option?.let {
            it::class.java.methods.firstOrNull { m ->
                m.name == "available" && m.parameterCount == 0 &&
                    (m.returnType == Boolean::class.java || m.returnType == Boolean::class.javaPrimitiveType)
            }
        } ?: return
        property.addDisplayCondition {
            val available = runCatching { availableMethod.invoke(option) as? Boolean }.getOrNull() ?: true
            if (available) Property.Display.SHOWN else Property.Display.DISABLED
        }
    }

    private fun resolveFlags(option: Any): Collection<Any> {
        val method = option::class.java.methods.firstOrNull { it.name == "flags" && it.parameterCount == 0 }
            ?: return emptyList()
        return runCatching { (method.invoke(option) as? Collection<*>)?.filterNotNull() }.getOrNull() ?: emptyList()
    }

    private fun isNumberFieldController(option: Any): Boolean {
        val controller = resolveController(option) ?: return false
        val cls = controller::class.java
        return inheritsFrom(cls, "ISliderController") && inheritsFrom(cls, "IStringController")
    }

    private fun boundedFloat(value: Float?, type: Class<*>): Float? {
        if (value == null) return null
        val limit = when (type) {
            Integer::class.java -> Integer.MAX_VALUE.toFloat()
            java.lang.Long::class.java -> Long.MAX_VALUE.toFloat()
            java.lang.Float::class.java -> Float.MAX_VALUE
            else -> Double.MAX_VALUE.toFloat() // infinite so only a genuinely infinite bound matches
        }
        return if (value <= -limit || value >= limit) null else value
    }

    private fun resolveDropdownStringValues(option: Any): List<String>? {
        val controller = resolveController(option) ?: return null
        val cls = controller::class.java
        if (!inheritsFrom(cls, "DropdownStringController")) return null
        if (inheritsFrom(cls, "EnumDropdownController")) return null

        val allowAny = runCatching {
            cls.getField("allowAnyValue").getBoolean(controller)
        }.getOrDefault(false)
        if (allowAny) return null

        var target: Class<*>? = cls
        while (target != null && target != Any::class.java) {
            val field = target.declaredFields.firstOrNull {
                it.name == "allowedValues" && List::class.java.isAssignableFrom(it.type)
            }
            if (field != null) {
                return runCatching {
                    (field.apply { isAccessible = true }.get(controller) as? List<*>)?.mapNotNull { it as? String }
                }.getOrNull()
            }
            target = target.superclass
        }
        return null
    }

    private fun resolveAllowAlpha(option: Any): Boolean? {
        val controller = resolveController(option) ?: return null
        if (!inheritsFrom(controller::class.java, "ColorController")) return null
        val method = controller::class.java.methods.firstOrNull {
            it.name == "allowAlpha" && it.parameterCount == 0
        } ?: return null
        return runCatching { method.invoke(controller) as? Boolean }.getOrNull()
    }

    private fun resolveEnumEntryValues(option: Any, currentValue: List<*>, defaultValue: Any?): List<Any>? {
        val entryController = resolveEntryController(option)

        if (entryController != null) {
            cyclingValuesOf(entryController)
                ?.takeIf { list -> list.isNotEmpty() && list.all { it is Enum<*> } }
                ?.let { return it.filterNotNull() }
        }

        val sample = currentValue.firstOrNull { it != null } as? Enum<*>
            ?: (defaultValue as? List<*>)?.firstOrNull { it != null } as? Enum<*>
            ?: initialEntryValue(option) as? Enum<*>

        val enumClass = sample?.let { enumClassOf(it) }
            ?: entryController?.let { enumClassOfController(it) }
            ?: return null
        return enumClass.enumConstants?.toList()
    }

    private fun initialEntryValue(option: Any): Any? {
        var cls: Class<*>? = option::class.java
        while (cls != null && cls != Any::class.java) {
            val field = cls.declaredFields.firstOrNull {
                it.name == "initialValue" && java.util.function.Supplier::class.java.isAssignableFrom(it.type)
            }
            if (field != null) {
                return runCatching {
                    (field.apply { isAccessible = true }.get(option) as? java.util.function.Supplier<*>)?.get()
                }.getOrNull()
            }
            cls = cls.superclass
        }
        return null
    }

    private fun resolveEntryController(option: Any): Any? {
        val entries = option::class.java.methods
            .firstOrNull { it.name == "options" && it.parameterCount == 0 }
            ?.let { runCatching { it.invoke(option) as? Collection<*> }.getOrNull() }
        val entry = entries?.firstOrNull { it != null } ?: return null
        val controller = resolveController(entry) ?: return null
        if (!inheritsFrom(controller::class.java, "EntryController")) return controller
        return controller::class.java.methods
            .firstOrNull { it.name == "controller" && it.parameterCount == 0 }
            ?.let { runCatching { it.invoke(controller) }.getOrNull() }
            ?: controller
    }

    private fun enumClassOfController(controller: Any): Class<*>? {
        val option = controller::class.java.methods
            .firstOrNull { it.name == "option" && it.parameterCount == 0 }
            ?.let { runCatching { it.invoke(controller) }.getOrNull() } ?: return null
        val value = option::class.java.methods
            .firstOrNull { it.name == "pendingValue" && it.parameterCount == 0 }
            ?.let { runCatching { it.invoke(option) }.getOrNull() } as? Enum<*> ?: return null
        return enumClassOf(value)
    }

    private fun resolveCyclingValues(option: Any): List<Any?>? =
        resolveController(option)?.let { cyclingValuesOf(it) }

    private fun cyclingValuesOf(controller: Any): List<Any?>? {
        if (!inheritsFrom(controller::class.java, "ICyclingController")) return null

        var cls: Class<*>? = controller::class.java
        while (cls != null && cls != Any::class.java) {
            val field = cls.declaredFields.firstOrNull {
                it.name == "values" && Iterable::class.java.isAssignableFrom(it.type)
            } ?: cls.declaredFields.singleOrNull { Iterable::class.java.isAssignableFrom(it.type) }
            if (field != null) {
                val values = runCatching {
                    (field.apply { isAccessible = true }.get(controller) as? Iterable<*>)?.toList()
                }.getOrNull()
                if (values != null) return values
            }
            cls = cls.superclass
        }
        return null
    }

    private fun resolveCyclingLabels(option: Any, values: List<Any?>): List<String> {
        val controller = resolveController(option)
        val formatter = controller?.let { resolveValueFormatter(it) }
        return values.map { value ->
            val formatted = formatter?.let { (target, format) ->
                runCatching { resolveComponent(format.invoke(target, value)) }.getOrNull()
            }
            formatted?.nonBlankOrNull() ?: value?.toString()?.nonBlankOrNull() ?: "—"
        }
    }

    private fun inheritsFrom(cls: Class<*>?, simpleName: String): Boolean {
        if (cls == null || cls == Any::class.java) return false
        if (cls.simpleName == simpleName) return true
        if (cls.interfaces.any { inheritsFrom(it, simpleName) }) return true
        return inheritsFrom(cls.superclass, simpleName)
    }

    private fun isListOption(optionClass: Class<*>): Boolean {
        var cls: Class<*>? = optionClass
        while (cls != null && cls != Any::class.java) {
            if (cls.simpleName == "ListOption") return true
            if (cls.interfaces.any { isListOption(it) }) return true
            cls = cls.superclass
        }
        return false
    }

    private fun maximumNumberOfEntries(option: Any): Int? {
        val method = option::class.java.methods.firstOrNull {
            it.name == "maximumNumberOfEntries" && it.parameterCount == 0
        } ?: return null
        val max = runCatching { (method.invoke(option) as? Number)?.toInt() }.getOrNull() ?: return null
        return if (max <= 0 || max == Int.MAX_VALUE) 0 else max
    }

    private fun resolveEntryRange(option: Any): Triple<Float, Float, Float?>? {
        val optionsMethod = option::class.java.methods.firstOrNull {
            it.name == "options" && it.parameterCount == 0
        } ?: return null
        val entries = runCatching { optionsMethod.invoke(option) as? Collection<*> }.getOrNull() ?: return null
        val entry = entries.firstOrNull { it != null } ?: return null
        return resolveSliderRange(entry)
    }

    private fun coerceNumber(value: Any?, type: Class<*>): Any {
        val number = value as? Number ?: 0
        return when (type) {
            Integer::class.java -> number.toInt()
            java.lang.Long::class.java -> number.toLong()
            java.lang.Float::class.java -> number.toFloat()
            java.lang.Double::class.java -> number.toDouble()
            else -> number
        }
    }

    private fun isButtonOption(optionClass: Class<*>): Boolean {
        var cls: Class<*>? = optionClass
        while (cls != null && cls != Any::class.java) {
            if (cls.simpleName == "ButtonOption") return true
            if (cls.interfaces.any { isButtonOption(it) }) return true
            cls = cls.superclass
        }
        return false
    }

    private fun parseButtonOption(
        option: Any,
        name: String,
        desc: String?,
        id: String,
        categoryName: String,
        subcategoryName: String?,
        root: Tree,
        ctx: Ctx,
    ) {
        val optionClass = option::class.java

        val label = optionClass.methods.firstOrNull { it.name == "text" && it.parameterCount == 0 }
            ?.let { runCatching { resolveComponent(it.invoke(option)) }.getOrNull() }
            ?.nonBlankOrNull() ?: name

        val actionMethod = optionClass.methods.firstOrNull { it.name == "action" && it.parameterCount == 0 }
        val action = actionMethod?.let { runCatching { it.invoke(option) }.getOrNull() }

        val property = Properties.dummy(
            id = id,
            name = name,
            description = desc,
        )
        property.addMetadata("visualizer", Visualizer.ButtonVisualizer::class.java)
        property.addMetadata("textKey", label)
        property.category = categoryName
        property.subcategory = subcategoryName

        if (action != null) {
            val acceptMethod = action::class.java.methods.firstOrNull {
                it.name == "accept" && (it.parameterCount == 1 || it.parameterCount == 2)
            }?.apply { isAccessible = true }
            if (acceptMethod != null) {
                property.metadata?.put("runnable", Runnable {
                    runCatching {
                        val paramTypes = acceptMethod.parameterTypes
                        val screen = yaclScreen(ctx)?.takeIf { paramTypes[0].isInstance(it) }
                        if (paramTypes.size == 2) {
                            acceptMethod.invoke(action, screen, option.takeIf { paramTypes[1].isInstance(it) })
                        } else {
                            acceptMethod.invoke(action, screen)
                        }
                    }.onFailure { LOGGER.warn("Failed to run YACL button action", it) }
                })
            }
        }

        registerProperty(property, option, ctx)
        root.put(property)
    }

    private fun resolveController(option: Any): Any? {
        val controllerMethod = option::class.java.methods.firstOrNull {
            it.name == "controller" && it.parameterCount == 0
        } ?: return null
        // some YACL versions wrap the controller in a Supplier
        var controller = runCatching { controllerMethod.invoke(option) }.getOrNull() ?: return null
        (controller as? java.util.function.Supplier<*>)?.let { controller = it.get() ?: return null }
        return controller
    }

    private fun resolveEnumLabels(option: Any, constants: List<Any?>): List<String>? {
        if (constants.isEmpty()) return null
        val controller = resolveController(option) ?: return null
        val (formatter, formatMethod) = resolveValueFormatter(controller) ?: return null
        return constants.map {
            runCatching { resolveComponent(formatMethod.invoke(formatter, it)) }
                .getOrNull()?.nonBlankOrNull() ?: it.toString()
        }
    }

    private fun enumClassOf(value: Enum<*>): Class<*>? {
        val cls = value.javaClass
        return if (cls.isEnum) cls else cls.superclass?.takeIf { it.isEnum }
    }

    private fun resolveValueFormatter(controller: Any): Pair<Any, java.lang.reflect.Method>? {
        var cls: Class<*>? = controller::class.java
        while (cls != null && cls != Any::class.java) {
            val field = cls.declaredFields.firstOrNull { it.type.name.endsWith("ValueFormatter") }
            if (field != null) {
                val formatter = runCatching { field.apply { isAccessible = true }.get(controller) }.getOrNull()
                val format = formatter?.let {
                    field.type.methods.firstOrNull { m -> m.name == "format" && m.parameterCount == 1 }
                        ?.apply { isAccessible = true }
                }
                if (formatter != null && format != null) return formatter to format
            }
            cls = cls.superclass
        }
        return null
    }

    private fun resolveSliderRange(option: Any): Triple<Float, Float, Float?>? {
        val controller = resolveController(option) ?: return null
        val c = controller
        val min = invokeNumber(c, "min") ?: return null
        val max = invokeNumber(c, "max") ?: return null
        val step = invokeNumber(c, "interval") ?: invokeNumber(c, "step")
        return Triple(min, max, step)
    }

    private fun invokeNumber(target: Any, name: String): Float? {
        val method = target::class.java.methods.firstOrNull {
            it.name == name && it.parameterCount == 0
        } ?: return null
        return runCatching { (method.invoke(target) as? Number)?.toFloat() }.getOrNull()
    }

    private fun resolveComponent(value: Any?): String? {
        if (value == null) return null
        if (value is String) return value

        runCatching {
            val method = value::class.java.getMethod("getString")
            return method.invoke(value) as? String
        }

        runCatching {
            val method = value::class.java.getMethod("string")
            return method.invoke(value) as? String
        }

        return value.toString()
    }

    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

//? }
