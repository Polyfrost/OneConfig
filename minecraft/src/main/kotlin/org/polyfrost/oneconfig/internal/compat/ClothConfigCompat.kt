//? clothconfig_compat {
package org.polyfrost.oneconfig.internal.compat

import com.mojang.blaze3d.platform.InputConstants
import java.awt.Color
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindCodec
import org.polyfrost.oneconfig.internal.compat.CompatIds.componentKey
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId

/**
 * Compatibility layer for Cloth Config and AutoConfig
 */
object ClothConfigCompat {

    private val LOGGER = LogManager.getLogger("OneConfig/Cloth-Compat")

    private class Slot(var entry: Any, var cached: Any?) {
        var display: Supplier<Property.Display>? = null
    }

    private var slots = HashMap<String, Slot>()

    private class Screen(val slots: Map<String, Slot>, var save: Runnable?)

    private val screens = HashMap<String, Screen>()

    @JvmStatic
    fun parseClothBuilder(builder: Any) {
        runCatching {
            val mod = CompatLoader.findFirstMod()
            val key = mod?.let { "${it.id}|${screenKey(builder)}" }
            if (mod != null && CompatLoader.nativeLoadedConfigs.contains(mod.id)) {
                screens[key]?.let { refresh(builder, it) }
                return
            }
            slots = HashMap()
            val tree = parseBuilder(builder, mod) ?: return
            slots.values.forEach(::captureSaves)
            val screen = Screen(slots, savingRunnable(builder))
            tree.saveFunction = Runnable { screen.save?.run() }
            if (key != null) screens[key] = screen
            CompatSnapshots.register(tree)
            CompatLoader.markFirstModAsSkip()
        }.onFailure {
            LOGGER.warn("Failed to parse Cloth config", it)
        }
    }

    private fun refresh(builder: Any, screen: Screen) {
        slots = HashMap()
        parseBuilder(builder, null) ?: return
        savingRunnable(builder)?.let { screen.save = it }
        for ((id, fresh) in slots) {
            val slot = screen.slots[id] ?: continue
            if (fresh.entry.javaClass != slot.entry.javaClass) continue
            slot.entry = fresh.entry
            slot.cached = fresh.cached
            slot.display = fresh.display
            captureSaves(slot)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun captureSaves(slot: Slot) {
        val field = findField(slot.entry.javaClass, "saveCallback")?.apply { isAccessible = true } ?: return
        val original = field.get(slot.entry) as? Consumer<Any?> ?: return
        field.set(slot.entry, Consumer<Any?> { v -> slot.cached = v; original.accept(v) })
    }

    private fun savingRunnable(builder: Any): Runnable? = invokeNoArg(builder, "getSavingRunnable") as? Runnable

    private fun categoryMap(builder: Any): Map<*, *>? =
        runCatching { readField(builder, "categoryMap") as? Map<*, *> }.getOrNull()

    private fun screenKey(builder: Any): String {
        val title = invokeNoArg(builder, "getTitle")
        val categories = categoryMap(builder)?.keys?.map { componentKey(it) ?: resolveComponent(it) }?.sortedBy { it.orEmpty() }
        return "${componentKey(title) ?: resolveComponent(title)}|$categories"
    }

    private fun parseBuilder(builder: Any, mod: ModInfo?): Tree? {
        val builderClass = builder::class.java

        val categoryMap = categoryMap(builder) ?: return null
        if (categoryMap.isEmpty()) return null

        val builderTitle = runCatching {
            resolveComponent(builderClass.getMethod("getTitle").invoke(builder))
        }.getOrNull()

        val tree = Tree.tree()
        tree.id = mod?.id ?: builderClass.name
        tree.title = mod?.name?.takeIf { it.isNotBlank() }
            ?: builderTitle?.takeIf { it.isNotBlank() }
            ?: "Cloth Config"
        tree.noCache = true
        // Cloth Config exposes no icon so fall back to the mod icon that Mod Menu uses
        mod?.extractIconFile()?.let {
            tree.addMetadata("icon_path", it)
        }

        var added = false
        val usedIds = HashSet<String>()
        val conditioned = ArrayList<Property<*>>()
        for (category in categoryMap.values) {
            if (category == null) continue
            val categoryKey = runCatching {
                category.javaClass.getMethod("getCategoryKey").invoke(category)
            }.getOrNull()
            val rawCategoryName = resolveComponent(categoryKey)?.takeIf { it.isNotBlank() }
            val categoryName = cleanName(rawCategoryName, "General")
            val categoryId = idPart(componentKey(categoryKey) ?: rawCategoryName, "general")

            @Suppress("UNCHECKED_CAST")
            val entries = runCatching {
                category.javaClass.getMethod("getEntries").invoke(category) as? Collection<*>
            }.getOrNull() ?: continue

            for (entry in entries) {
                if (entry == null) continue
                runCatching {
                    if (parseEntry(entry, categoryName, categoryName, categoryId, tree, tree, usedIds, emptyList(), conditioned)) added = true
                }.onFailure { LOGGER.warn("Failed to parse Cloth entry", it) }
            }
        }

        return if (added) tree else null
    }

    private fun parseEntry(
        entry: Any,
        categoryName: String,
        subcategoryName: String,
        idPath: String,
        dest: Tree,
        root: Tree,
        usedIds: MutableSet<String>,
        gates: List<Any>,
        conditioned: MutableList<Property<*>>,
    ): Boolean {
        val nameComponent = runCatching { entry.javaClass.getMethod("getFieldName").invoke(entry) }.getOrNull()
        val name = resolveComponent(nameComponent) ?: return false
        val entryPath = "$idPath/${idPart(componentKey(nameComponent) ?: name, "entry")}"

        val value = runCatching { invokeGetValue(entry) }.getOrNull()

        if (isSubCategory(entry, value)) {
            val children = value as? Collection<*> ?: return false
            val subNameComponent = runCatching {
                entry.javaClass.getMethod("getCategoryName").invoke(entry)
            }.getOrNull()
            val rawSubName = resolveComponent(subNameComponent)?.takeIf { it.isNotBlank() }
            val subName = cleanName(rawSubName, name)
            val subPath = componentKey(subNameComponent)?.let { "$idPath/${idPart(it, "entry")}" } ?: entryPath

            val accordion = if (dest === root) {
                Tree.tree(uniqueId(usedIds, subPath)).also {
                    it.title = subName
                    it.addMetadata("category", categoryName)
                    if (!isExpanded(entry)) it.addMetadata("collapsed", true)
                }
            } else {
                dest
            }

            var added = false
            for (child in children) {
                if (child == null) continue
                runCatching {
                    if (parseEntry(child, categoryName, subName, subPath, accordion, root, usedIds, gates + entry, conditioned)) added = true
                }.onFailure { LOGGER.warn("Failed to parse Cloth sub-entry", it) }
            }

            if (added && accordion !== dest) root.put(accordion)
            return added
        }

        if (entryClassMatches(entry, "TextListEntry")) {
            val text = runCatching { readField(entry, "text") }.getOrNull()
            val content = resolveComponent(text)?.takeIf { it.isNotBlank() } ?: return false
            val id = uniqueId(usedIds, "$idPath/${idPart((componentKey(text) ?: content).take(48), "text")}")
            val property = Properties.dummy(id, null, content)
            property.addMetadata("visualizer", Visualizer.InfoVisualizer::class.java)
            finish(property, entry, gates, conditioned, categoryName, subcategoryName, dest)
            return true
        }

        val currentValue = value ?: return false

        if (currentValue is List<*>) {
            return parseListEntry(
                entry,
                currentValue,
                name,
                uniqueId(usedIds, entryPath),
                categoryName,
                subcategoryName,
                dest,
                gates,
                conditioned,
            )
        }

        val isColor = isColorEntry(entry)
        val isKeybind = entryClassMatches(entry, "KeyCodeEntry")
        val isItem = !isKeybind && CompatItems.id(currentValue) != null
        val choices = runCatching { choices(entry) }.getOrNull()
        val label = runCatching { choiceLabel(entry) }.getOrNull()

        val isNumber = currentValue is Int || currentValue is Float || currentValue is Double || currentValue is Long
        val isSlider = isNumber && isSliderEntry(entry)

        val visualizer: Class<out Visualizer> = when {
            isColor -> Visualizer.ColorVisualizer::class.java
            isKeybind -> Visualizer.KeybindVisualizer::class.java
            isItem -> Visualizer.ItemListVisualizer::class.java
            currentValue is Boolean -> Visualizer.SwitchVisualizer::class.java
            currentValue is Enum<*> -> Visualizer.DropdownVisualizer::class.java
            choices != null -> Visualizer.DropdownVisualizer::class.java
            isSlider -> Visualizer.SliderVisualizer::class.java
            isNumber -> Visualizer.NumberVisualizer::class.java
            currentValue is String -> Visualizer.TextVisualizer::class.java
            currentValue is Color -> Visualizer.ColorVisualizer::class.java
            else -> return false
        }
        val keyed = visualizer == Visualizer.DropdownVisualizer::class.java && currentValue !is String && currentValue !is Enum<*>
        val ownKeys = if (keyed) distinctKeys(choices!!, ::ownString) else null
        val keyOf: (Any?) -> String? = if (ownKeys == null && label != null) label else ::ownString
        val keys = if (keyed) ownKeys ?: label?.let { distinctKeys(choices!!, it) } else null
        val indexed = keyed && keys == null
        val unstable = keyed && ownKeys == null

        val read: (Any?) -> Any? = when {
            isKeybind -> { v -> keybindOf(v) }
            isItem -> { v -> ArrayList(listOfNotNull(CompatItems.id(v))) }
            keys != null -> { v -> choices!!.indexOf(v).let { if (it >= 0) keys[it] else keyOf(v).orEmpty() } }
            indexed -> { v -> choices!!.indexOf(v) }
            else -> { v -> v }
        }
        val write: (Any?) -> Any? = when {
            isKeybind -> { v -> modifierKeyCodeOf(entry, v) }
            isItem -> { v -> (v as? List<*>)?.firstOrNull()?.let(CompatItems::byId) }
            keys != null -> { v -> keys.indexOf(v as? String).takeIf { it >= 0 }?.let { choices!![it] } }
            indexed -> { v -> (v as? Int)?.let { choices!!.getOrNull(it) } }
            else -> { v -> v }
        }
        val type: Class<*>? = when {
            isKeybind -> OneConfigKeybind::class.java
            isItem -> java.util.List::class.java
            keys != null -> String::class.java
            indexed -> Int::class.javaObjectType
            else -> null
        }

        val property = bindProperty(entry, currentValue, uniqueId(usedIds, entryPath), name, conditioned, type, read, write)

        property.addMetadata("visualizer", visualizer)
        if (unstable) property.addMetadata(CompatSnapshots.NO_SNAPSHOT_META, true)
        defaultOf(entry)?.let(read)?.let { property.addMetadata("default", it) }

        when {
            isColor -> if (readField(entry, "alpha") == false) property.addMetadata("noAlpha", true)
            isKeybind -> if (invokeNoArg(entry, "isAllowModifiers") == false) property.addMetadata("singleKey", true)
            isItem -> property.addMetadata("maxEntries", 1)
            currentValue is Enum<*> -> {
                val values = choices ?: currentValue::class.java.enumConstants?.toList() ?: emptyList()
                property.addMetadata("options", values.map { it.toString() })
                if (choices != null) property.addMetadata("optionValues", choices)
                if (label != null) property.addMetadata("optionLabels", values.map { label(it) ?: it.toString() })
            }
            choices != null -> {
                property.addMetadata("options", choices.map { (label?.invoke(it)) ?: it.toString() })
                (keys ?: choices.takeIf { currentValue is String }?.map { it.toString() })?.let { property.addMetadata("optionValues", it) }
            }
            isSlider -> {
                property.addMetadata("min", readNumberField(entry, "minimum") ?: 0f)
                property.addMetadata("max", readNumberField(entry, "maximum") ?: 100f)
            }
            isNumber -> {
                readNumberField(entry, "minimum")?.let { property.addMetadata("min", it) }
                readNumberField(entry, "maximum")?.let { property.addMetadata("max", it) }
            }
        }

        finish(property, entry, gates, conditioned, categoryName, subcategoryName, dest)
        return true
    }

    private fun parseListEntry(
        entry: Any,
        currentValue: List<*>,
        name: String,
        id: String,
        categoryName: String,
        subcategoryName: String,
        dest: Tree,
        gates: List<Any>,
        conditioned: MutableList<Property<*>>,
    ): Boolean {
        val element = listElementType(entry, currentValue) ?: return false
        val numeric = Number::class.java.isAssignableFrom(element)
        if (!numeric && element != String::class.java) return false

        val read: (Any?) -> Any? = { v ->
            ArrayList((v as? List<*> ?: emptyList<Any?>()).map { if (numeric) it as? Number ?: 0 else it?.toString() ?: "" })
        }
        val property = bindProperty(
            entry, currentValue, id, name, conditioned, java.util.List::class.java, read,
            write = { v -> (v as? List<*>)?.mapTo(ArrayList()) { if (numeric) coerceNumber(it, element) else it?.toString() ?: "" } },
        )

        property.addMetadata(
            "visualizer",
            if (numeric) Visualizer.NumberListVisualizer::class.java else Visualizer.TextListVisualizer::class.java,
        )
        if (numeric) {
            property.addMetadata("min", readNumberField(entry, "minimum") ?: 0f)
            property.addMetadata("max", readNumberField(entry, "maximum") ?: 100f)
        }
        defaultOf(entry)?.let(read)?.let { property.addMetadata("default", it) }

        finish(property, entry, gates, conditioned, categoryName, subcategoryName, dest)
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun bindProperty(
        entry: Any,
        initial: Any?,
        id: String,
        name: String,
        conditioned: List<Property<*>>,
        type: Class<*>?,
        read: (Any?) -> Any?,
        write: (Any?) -> Any?,
    ): Property<Any?> {
        val saveCallbackField = findField(entry.javaClass, "saveCallback")?.apply { isAccessible = true }
        val slot = Slot(entry, initial).also { slots[id] = it }
        return Properties.functional(
            getter = { read(slot.cached) },
            setter = setter@{ v: Any? ->
                val clothValue = write(v) ?: return@setter
                slot.cached = clothValue
                syncWidget(slot.entry, clothValue)
                (saveCallbackField?.get(slot.entry) as? Consumer<Any?>)?.accept(clothValue)
                conditioned.forEach { it.revaluateDisplay() }
            },
            id = id,
            name = name,
            description = resolveTooltip(entry),
            type = type as Class<Any?>?,
        )
    }

    private fun finish(
        property: Property<*>,
        entry: Any,
        gates: List<Any>,
        conditioned: MutableList<Property<*>>,
        categoryName: String,
        subcategoryName: String,
        dest: Tree,
    ) {
        property.category = categoryName
        property.subcategory = subcategoryName
        val conditions = (gates + entry).mapNotNull(::requirementOf)
        if (conditions.isNotEmpty()) {
            val slot = slots.getOrPut(property.id) { Slot(entry, null) }
            slot.display = Supplier {
                val displays = conditions.map { it.get() }
                when {
                    Property.Display.HIDDEN in displays -> Property.Display.HIDDEN
                    Property.Display.DISABLED in displays -> Property.Display.DISABLED
                    else -> Property.Display.SHOWN
                }
            }
            property.addDisplayCondition(Supplier { slot.display?.get() ?: Property.Display.SHOWN })
            conditioned.add(property)
        }
        dest.put(property)
    }

    private fun requirementOf(entry: Any): Supplier<Property.Display>? {
        val enable = invokeNoArg(entry, "getRequirement")
        val display = invokeNoArg(entry, "getDisplayRequirement")
        val editable = runCatching { readField(entry, "editable") as? Boolean }.getOrNull() ?: true
        if (enable == null && display == null && editable) return null
        val check = runCatching {
            Class.forName("me.shedaniel.clothconfig2.api.Requirement", false, entry.javaClass.classLoader).getMethod("check")
        }.getOrNull()
        fun met(requirement: Any?) = requirement == null || runCatching { check?.invoke(requirement) as? Boolean }.getOrNull() != false
        return Supplier {
            when {
                !met(display) -> Property.Display.HIDDEN
                !editable || !met(enable) -> Property.Display.DISABLED
                else -> Property.Display.SHOWN
            }
        }
    }

    private fun syncWidget(entry: Any, value: Any) {
        if (value is List<*>) return
        runCatching {
            when {
                entryClassMatches(entry, "BooleanListEntry") -> (readField(entry, "bool") as? AtomicBoolean)?.set(value as Boolean)
                entryClassMatches(entry, "SelectionListEntry") -> {
                    val index = (readField(entry, "values") as? List<*>)?.indexOf(value) ?: -1
                    if (index >= 0) (readField(entry, "index") as? AtomicInteger)?.set(index)
                }
                entryClassMatches(entry, "DropdownBoxEntry") -> {
                    val top = invokeNoArg(entry, "getSelectionElement")?.let { invokeNoArg(it, "getTopRenderer") } ?: return
                    top.javaClass.methods.firstOrNull { it.name == "setValue" && it.parameterCount == 1 }?.invoke(top, value)
                }
                else -> {
                    val setters = entry.javaClass.methods.filter { it.name == "setValue" && it.parameterCount == 1 }
                    val typed = setters.firstOrNull {
                        it.parameterTypes[0] != String::class.java && it.parameterTypes[0].kotlin.javaObjectType.isInstance(value)
                    }
                    when {
                        typed != null -> typed.invoke(entry, value)
                        value is String || value is Number ->
                            setters.firstOrNull { it.parameterTypes[0] == String::class.java }?.invoke(entry, value.toString())
                    }
                }
            }
        }
    }

    private fun choices(entry: Any): List<Any?>? {
        val values = when {
            entryClassMatches(entry, "SelectionListEntry") -> readField(entry, "values")
            entryClassMatches(entry, "DropdownBoxEntry") ->
                if (invokeNoArg(entry, "isSuggestionMode") != false) null else invokeNoArg(entry, "getSelections")
            else -> null
        }
        return (values as? List<*>)?.takeIf { it.isNotEmpty() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun choiceLabel(entry: Any): ((Any?) -> String?)? {
        val fn = (when {
            entryClassMatches(entry, "SelectionListEntry") -> readField(entry, "nameProvider")
            entryClassMatches(entry, "DropdownBoxEntry") -> invokeNoArg(entry, "getSelectionElement")
                ?.let { invokeNoArg(it, "getTopRenderer") }
                ?.let { runCatching { readField(it, "toTextFunction") }.getOrNull() }
            else -> null
        }) as? java.util.function.Function<Any?, Any?> ?: return null
        return { v -> runCatching { resolveComponent(fn.apply(v)) }.getOrNull()?.takeIf { it.isNotBlank() } }
    }

    private fun ownString(value: Any?): String? {
        if (value == null) return null
        val s = runCatching { value.toString() }.getOrNull() ?: return null
        return s.takeIf { it != value.javaClass.name + "@" + Integer.toHexString(value.hashCode()) }
    }

    private fun distinctKeys(choices: List<Any?>, keyOf: (Any?) -> String?): List<String>? {
        val keys = choices.map { keyOf(it) ?: return null }
        return keys.takeIf { it.toSet().size == it.size }
    }

    private fun keybindOf(code: Any?): OneConfigKeybind {
        val key = invokeNoArg(code, "getKeyCode") as? InputConstants.Key
        val modifier = invokeNoArg(code, "getModifier")
        var mods = 0
        if (invokeNoArg(modifier, "hasShift") == true) mods = mods or KeyModifiers.SHIFT.toInt()
        if (invokeNoArg(modifier, "hasControl") == true) mods = mods or KeyModifiers.CTRL.toInt()
        if (invokeNoArg(modifier, "hasAlt") == true) mods = mods or KeyModifiers.ALT.toInt()
        return when {
            key == null || key == InputConstants.UNKNOWN -> OneConfigKeybind(null, null, KeyModifiers.NONE, 0L) { true }
            key.type == InputConstants.Type.MOUSE -> OneConfigKeybind(null, intArrayOf(key.value), mods.toByte(), 0L) { true }
            else -> OneConfigKeybind(intArrayOf(key.value), null, mods.toByte(), 0L) { true }
        }
    }

    private fun modifierKeyCodeOf(entry: Any, value: Any?): Any? = runCatching {
        val keybind = value as? OneConfigKeybind ?: return null
        val mouse = keybind.mouseBtns?.firstOrNull()
        val keyCode = keybind.keyCodes?.firstOrNull()
        if (mouse != null && invokeNoArg(entry, "isAllowMouse") == false) return null
        if (mouse == null && keyCode != null && invokeNoArg(entry, "isAllowKey") == false) return null
        val key = when {
            mouse != null -> MinecraftKeybindCodec.mouse(mouse)
            keyCode != null -> MinecraftKeybindCodec.keysym(keyCode)
            else -> InputConstants.UNKNOWN
        }
        val mods = if (invokeNoArg(entry, "isAllowModifiers") == false) KeyModifiers.NONE else keybind.mods
        val loader = entry.javaClass.classLoader
        val modifierClass = Class.forName("me.shedaniel.clothconfig2.api.Modifier", false, loader)
        val modifier = modifierClass.getMethod("of", Boolean::class.java, Boolean::class.java, Boolean::class.java).invoke(
            null,
            KeyModifiers.has(mods, KeyModifiers.ALT),
            KeyModifiers.has(mods, KeyModifiers.CTRL),
            KeyModifiers.has(mods, KeyModifiers.SHIFT),
        )
        Class.forName("me.shedaniel.clothconfig2.api.ModifierKeyCode", false, loader)
            .getMethod("of", InputConstants.Key::class.java, modifierClass)
            .invoke(null, key, modifier)
    }.getOrNull()

    private fun defaultOf(entry: Any): Any? = runCatching {
        (entry.javaClass.getMethod("getDefaultValue").invoke(entry) as? Optional<*>)?.orElse(null)
    }.getOrNull()

    private fun invokeNoArg(target: Any?, name: String): Any? =
        target?.let { runCatching { it.javaClass.getMethod(name).invoke(it) }.getOrNull() }

    private fun readField(target: Any, name: String): Any? =
        findField(target.javaClass, name)?.let {
            it.isAccessible = true
            it.get(target)
        }

    /**
     * Falls back to the Cloth entry class so empty lists still render with the right widget
     */
    private fun listElementType(entry: Any, currentValue: List<*>): Class<*>? {
        currentValue.firstOrNull { it != null }?.let { return it.javaClass }
        return when {
            entryClassMatches(entry, "StringListListEntry") -> String::class.java
            entryClassMatches(entry, "IntegerListListEntry") -> Integer::class.java
            entryClassMatches(entry, "LongListListEntry") -> java.lang.Long::class.java
            entryClassMatches(entry, "FloatListListEntry") -> java.lang.Float::class.java
            entryClassMatches(entry, "DoubleListListEntry") -> java.lang.Double::class.java
            else -> null
        }
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

    private fun isExpanded(entry: Any): Boolean {
        val method = entry.javaClass.methods.firstOrNull {
            it.name == "isExpanded" && it.parameterCount == 0 &&
                (it.returnType == Boolean::class.java || it.returnType == Boolean::class.javaPrimitiveType)
        } ?: return false
        return runCatching { method.invoke(entry) as? Boolean }.getOrNull() ?: false
    }

    private fun cleanName(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        if (!looksLikeTranslationKey(raw)) return raw
        val segment = raw.substringAfterLast('.')
        return when {
            segment.isBlank() || segment.equals("default", ignoreCase = true) -> fallback
            else -> segment.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }

    private fun looksLikeTranslationKey(s: String): Boolean =
        !s.any { it.isWhitespace() } && s.count { it == '.' } >= 2

    private fun resolveTooltip(entry: Any): String? {
        val method = entry.javaClass.methods.firstOrNull {
            it.name == "getTooltip" && it.parameterCount == 0
        }?.apply { isAccessible = true } ?: return null
        val optional = runCatching { method.invoke(entry) as? Optional<*> }.getOrNull() ?: return null
        val components = optional.orElse(null) as? Array<*> ?: return null
        val lines = components.mapNotNull { resolveComponent(it)?.takeIf { line -> line.isNotBlank() } }
        return lines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun invokeGetValue(entry: Any): Any? {
        val method = entry.javaClass.methods.firstOrNull {
            it.name == "getValue" && it.parameterCount == 0
        }?.apply { isAccessible = true } ?: return null
        return method.invoke(entry)
    }

    private fun isSubCategory(entry: Any, value: Any?): Boolean {
        if (entry.javaClass.simpleName == "SubCategoryListEntry") return true
        // fallback for entries that are just a collection of things exposing getFieldName
        return value is Collection<*> && value.isNotEmpty() && value.all { it != null && hasGetFieldName(it) }
    }

    private fun hasGetFieldName(o: Any): Boolean =
        o.javaClass.methods.any { it.name == "getFieldName" && it.parameterCount == 0 }

    private fun isColorEntry(entry: Any): Boolean = entryClassMatches(entry, "ColorEntry")

    private fun isSliderEntry(entry: Any): Boolean = entryClassMatches(entry, "Slider")

    private fun entryClassMatches(entry: Any, needle: String): Boolean {
        var cls: Class<*>? = entry.javaClass
        while (cls != null && cls != Any::class.java) {
            if (cls.simpleName.contains(needle)) return true
            cls = cls.superclass
        }
        return false
    }

    private fun readNumberField(entry: Any, name: String): Float? =
        findField(entry.javaClass, name)?.let {
            it.isAccessible = true
            (it.get(entry) as? Number)?.toFloat()
        }

    private fun findField(cls: Class<*>, name: String): Field? {
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            current.declaredFields.firstOrNull { it.name == name }?.let { return it }
            current = current.superclass
        }
        return null
    }

    private fun resolveComponent(value: Any?): String? {
        if (value == null) return null
        if (value is String) return value

        runCatching {
            return value::class.java.getMethod("getString").invoke(value) as? String
        }
        runCatching {
            return value::class.java.getMethod("string").invoke(value) as? String
        }

        return value.toString()
    }
}

//? }
