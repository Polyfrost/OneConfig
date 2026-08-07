package org.polyfrost.oneconfig.internal.compat

//? rconfig_compat {
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigButton
//? >= 1.21.8 {
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigCategory
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigElement
import com.teamresourceful.resourcefulconfig.api.types.elements.ResourcefulConfigEntryElement
//? } else {
/*import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigEntry
*///? }
import com.teamresourceful.resourcefulconfig.api.types.info.Translatable
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigObjectEntry
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigValueEntry
import com.teamresourceful.resourcefulconfig.api.types.options.EntryData
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType
import com.teamresourceful.resourcefulconfig.api.types.options.Option
//? < 1.21.8 {
/*import net.minecraft.client.resources.language.I18n
import net.minecraft.util.StringRepresentable
*///? }
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.index
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId

internal object RConfigCompat : Logger by LogManager.getLogger("OneConfig/RconfigCompat") {

    const val RCONFIG_ID = "rconfig_id"

    private val registeredConfigs = LinkedHashMap<String, ResourcefulConfig>()

    @JvmStatic
    fun registeredConfigs(): Collection<ResourcefulConfig> = registeredConfigs.values

    @JvmStatic
    fun enable() {
        info("Detected rconfig, enabling compat layer!")
    }

    @JvmStatic
    fun addConfig(config: ResourcefulConfig) {
        registeredConfigs[config.id()] = config
        val mod = CompatLoader.findFirstMod()
        info("Preparing config wrapper for ${config.id()}!")
        CompatLoader.requireTranslations {
            parseConfig(config, mod).let(CompatSnapshots::register)
        }
    }

    private fun parseConfig(config: ResourcefulConfig, mod: ModInfo?): Tree {
        info("Creating config wrapper for ${config.id()}!")
        val tree = Tree.tree()
        tree.id = config.id()
        //? >= 1.21.8 {
        tree.title = config.info().title().toComponent()
        tree.description = config.info().description().toComponent()
        //? } else {
        /*tree.title = config.info().title().toLocalizedString()
        tree.description = config.info().description().toLocalizedString()
        *///? }
        mod?.extractIconFile()?.let {
            tree.addMetadata("icon_path", it)
        }

        val usedIds = HashSet<String>()
        config.categories().values.forEach {
            parseCategory(it, config.id(), null, tree, usedIds)
        }

        //? >= 1.21.8 {
        parseAny(config.elements(), tree, config.id(), usedIds)
        //? } else {
        /*parseAny(config.entries(), tree, config.id(), usedIds)
        parseButtons(config.buttons(), tree, config.id(), usedIds)
        *///? }

        tree.addMetadata("custom_save", Runnable { config.save() })
        tree.addMetadata("no_cache", true)

        return tree
    }


    // 1st layer gets converted to categories, 2nd+ layer to subcategories
    private fun parseCategory(
        config: ResourcefulConfig,
        id: String,
        category: String?,
        root: Tree,
        usedIds: MutableSet<String>,
    ) {
        val tree = Tree.tree()

        val nestedId = "$id/${config.id()}"
        //? >= 1.21.8 {
        val title = config.info().title().toComponent().string
        //? } else {
        /*val title = config.info().title().toLocalizedString()
        *///? }

        tree.category = category ?: title
        if (category != null) {
            tree.subcategory = title
        }

        for ((_, entry) in config.categories()) {
            parseCategory(entry, nestedId, category ?: title, root, usedIds)
        }
        //? >= 1.21.8 {
        parseAny(config.elements(), tree, nestedId, usedIds)
        //? } else {
        /*parseAny(config.entries(), tree, nestedId, usedIds)
        parseButtons(config.buttons(), tree, nestedId, usedIds)
        *///? }

        tree.map.forEach { (_, node) ->
            node.category = tree.category
            node.subcategory = tree.subcategory
            root.put(node)
        }
    }

    private fun parseButton(button: ResourcefulConfigButton, tree: Tree, path: String, usedIds: MutableSet<String>) {
        val property = Properties.dummy(id = uniqueId(usedIds, "$path/${idPart(button.title(), "button")}"))
        property.title = button.title()?.takeUnless { it.isEmpty() }
            ?: "button" //todo find a better way of doing this, rconfig allows empty names
        property.description = button.description()
        property.visualizer = Visualizer.ButtonVisualizer::class.java
        property.metadata?.put("runnable", Runnable { button.invoke() })
        tree.put(property)
    }

    //? >= 1.21.8 {
    private fun parseAny(
        list: Iterable<ResourcefulConfigElement>,
        tree: Tree,
        path: String,
        usedIds: MutableSet<String>,
    ) = list.forEach {
        when (it) {
            is ResourcefulConfigCategory -> parseCategory(it, tree, path, usedIds)
            is ResourcefulConfigEntryElement -> parseAny(it, tree, path, usedIds)
            is ResourcefulConfigButton -> parseButton(it, tree, path, usedIds)
        }
    }

    private fun parseAny(element: ResourcefulConfigEntryElement, tree: Tree, path: String, usedIds: MutableSet<String>) {
        val elementPath = "$path/${idPart(element.id(), "entry")}"
        when (val entry = element.entry()) {
            is ResourcefulConfigObjectEntry -> parseObject(entry, tree, elementPath, usedIds)
            is ResourcefulConfigValueEntry -> buildAndAdd(entry, element.id(), tree, elementPath, usedIds)
        }
    }

    private fun parseCategory(
        entry: ResourcefulConfigCategory,
        tree: Tree,
        path: String,
        usedIds: MutableSet<String>,
    ) {
        val categoryPath = "$path/${idPart(entry.id(), "category")}"
        val category = Tree.tree()
        category.title = entry.info().title().toComponent()
        category.description = entry.info().description().toComponent()
        category.id = uniqueId(usedIds, categoryPath)
        category.category = tree.category
        category.subcategory = entry.info().title().toComponent().string
        category.index = -1
        parseAny(entry.elements(), category, categoryPath, usedIds)
        tree.put(category)
    }

    private fun parseObject(
        entry: ResourcefulConfigObjectEntry,
        tree: Tree,
        path: String,
        usedIds: MutableSet<String>,
    ) {
        val objectEntry = Tree.tree()
        objectEntry.title = entry.options().title.toComponent()
        objectEntry.description = entry.options().comment.toComponent()
        objectEntry.id = uniqueId(usedIds, path)
        objectEntry.category = tree.category
        objectEntry.subcategory = entry.options().title.toComponent().string
        objectEntry.index = -1
        parseAny(entry.elements(), objectEntry, path, usedIds)
        tree.put(objectEntry)
    }
    //? } else {
    /*private fun parseAny(
        entries: Map<String, ResourcefulConfigEntry>,
        tree: Tree,
        path: String,
        usedIds: MutableSet<String>,
    ) = entries.forEach { (id, entry) ->
        val entryPath = "$path/${idPart(id, "entry")}"
        when (entry) {
            is ResourcefulConfigObjectEntry -> parseObject(entry, tree, entryPath, usedIds)
            is ResourcefulConfigValueEntry -> buildAndAdd(entry, id, tree, entryPath, usedIds)
        }
    }

    private fun parseButtons(
        buttons: List<ResourcefulConfigButton>,
        tree: Tree,
        path: String,
        usedIds: MutableSet<String>,
    ) {
        buttons.forEach { parseButton(it, tree, path, usedIds) }
    }

    private fun parseObject(
        entry: ResourcefulConfigObjectEntry,
        tree: Tree,
        path: String,
        usedIds: MutableSet<String>,
    ) {
        val objectEntry = Tree.tree()
        objectEntry.title = entry.options().title.toLocalizedString()
        objectEntry.description = entry.options().comment.toLocalizedString()
        objectEntry.id = uniqueId(usedIds, path)
        objectEntry.category = tree.category
        objectEntry.subcategory = entry.options().title.toLocalizedString()
        objectEntry.index = -1
        parseAny(entry.entries(), objectEntry, path, usedIds)
        tree.put(objectEntry)
    }
    *///? }

    @JvmStatic
    fun buildProperties(entry: ResourcefulConfigObjectEntry): List<Property<*>> {
        val tmp = Tree.tree()
        val usedIds = HashSet<String>()
        //? >= 1.21.8 {
        parseAny(entry.elements(), tmp, "object", usedIds)
        //? } else {
        /*parseAny(entry.entries(), tmp, "object", usedIds)
        *///? }
        val out = ArrayList<Property<*>>()
        collectProperties(tmp, out)
        return out
    }

    private fun collectProperties(tree: Tree, out: MutableList<Property<*>>) {
        tree.map.values.forEach { node ->
            when (node) {
                is Property<*> -> out.add(node)
                is Tree -> collectProperties(node, out)
            }
        }
    }

    private fun buildAndAdd(
        entry: ResourcefulConfigValueEntry,
        id: String,
        tree: Tree,
        path: String,
        usedIds: MutableSet<String>,
    ) {
        val builder = RConfigPropertyBuilder(entry, id, uniqueId(usedIds, path))
        val options = entry.options()

        if (entry.isArray) {
            // Enum arrays are a fixed set: a draggable reorderable list, or a multi-select dropdown.
            // String/number arrays are user-editable, so they become the matching list option.
            if (entry.type() == EntryType.ENUM) buildEnumArray(entry, builder, options, tree)
            else buildValueArray(entry, builder, options, tree)
            return
        }

        val visualizer: Class<out Visualizer> = when (entry.type()) {
            EntryType.BYTE, EntryType.SHORT, EntryType.INTEGER, EntryType.LONG, EntryType.FLOAT, EntryType.DOUBLE -> {
                if (entry.options().getOption(Option.RANGE) != null) {
                    builder["min"] = options.getOption(Option.RANGE).min.toFloat()
                    builder["max"] = options.getOption(Option.RANGE).max.toFloat()
                } else {
                    builder["min"] = Float.MIN_VALUE
                    builder["max"] = Float.MAX_VALUE
                }

                if (entry.options().hasOption(Option.COLOR)) {
                    builder.setter = setter@{ color ->
                        entry.int = (color as? Int) ?: return@setter
                    }
                    builder.getter = { entry.int }
                    Visualizer.ColorVisualizer::class.java
                } else if (entry.options().hasOption(Option.SLIDER)) {
                    Visualizer.SliderVisualizer::class.java
                } else {
                    Visualizer.NumberVisualizer::class.java
                }
            }

            EntryType.STRING -> {
                // TODO multiline :pensive:
                builder["validate"] =
                    if (options.hasOption(Option.REGEX)) options.getOption(Option.REGEX).pattern() else null
                Visualizer.TextVisualizer::class.java
            }

            EntryType.BOOLEAN -> Visualizer.SwitchVisualizer::class.java
            EntryType.ENUM -> {
                // Show the same display names rconfig itself renders (Translatable / StringRepresentable /
                // fallback), positionally aligned to the enum constant order the dropdown iterates.
                entry.objectType().enumConstants?.filterIsInstance<Enum<*>>()?.takeIf { it.isNotEmpty() }?.let {
                    builder["optionLabels"] = it.map(::enumDisplayName).toTypedArray()
                }
                Visualizer.DropdownVisualizer::class.java
            }
            else -> null
        } ?: return

        builder["visualizer"] = visualizer
        tree.put(builder.build())
    }

    private fun buildValueArray(
        entry: ResourcefulConfigValueEntry,
        builder: RConfigPropertyBuilder,
        options: EntryData,
        tree: Tree,
    ) {
        val numeric = when (entry.type()) {
            EntryType.BYTE, EntryType.SHORT, EntryType.INTEGER, EntryType.LONG, EntryType.FLOAT, EntryType.DOUBLE -> true
            else -> false
        }
        val isColor = numeric && options.hasOption(Option.COLOR)

        val visualizer: Class<out Visualizer> = when {
            isColor -> Visualizer.ColorListVisualizer::class.java
            numeric && options.hasOption(Option.SLIDER) -> Visualizer.SliderListVisualizer::class.java
            numeric -> Visualizer.NumberListVisualizer::class.java
            entry.type() == EntryType.STRING -> Visualizer.TextListVisualizer::class.java
            else -> return // boolean arrays and anything else have no list equivalent
        }

        if (numeric) {
            val range = options.getOption(Option.RANGE)
            builder["min"] = range?.min?.toFloat() ?: -Float.MAX_VALUE
            builder["max"] = range?.max?.toFloat() ?: Float.MAX_VALUE
        } else if (options.hasOption(Option.REGEX)) {
            builder["regex"] = options.getOption(Option.REGEX).pattern()
        }

        val element = entry.objectType()
        fun read(value: Any?): Any = if (numeric) (value as? Number ?: 0) else value?.toString() ?: ""
        fun write(value: Any?): Any = if (numeric) coerceNumber(value, element) else value?.toString() ?: ""

        builder.getter = { ArrayList((entry.getArray() ?: emptyArray()).map(::read)) }
        builder.setter = setter@{ value ->
            val values = when {
                value is List<*> -> value
                value is Array<*> -> value.asList()
                else -> return@setter
            }
            entry.setArray(values.map(::write).toTypedArray())
        }
        (entry.defaultValue() as? Array<*>)?.let { builder["default"] = ArrayList(it.map(::read)) }

        builder["visualizer"] = visualizer
        tree.put(builder.build())
    }

    private fun coerceNumber(value: Any?, type: Class<*>): Any {
        val number = value as? Number ?: 0
        return when (type) {
            java.lang.Byte::class.java, java.lang.Byte.TYPE -> number.toByte()
            java.lang.Short::class.java, java.lang.Short.TYPE -> number.toShort()
            Integer::class.java, Integer.TYPE -> number.toInt()
            java.lang.Long::class.java, java.lang.Long.TYPE -> number.toLong()
            java.lang.Float::class.java, java.lang.Float.TYPE -> number.toFloat()
            java.lang.Double::class.java, java.lang.Double.TYPE -> number.toDouble()
            else -> number
        }
    }

    // enum arrays render as either a draggable (reorderable) list or a multi-select dropdown,
    // depending on whether the field is annotated @ConfigOption.Draggable.
    private fun buildEnumArray(
        entry: ResourcefulConfigValueEntry,
        builder: RConfigPropertyBuilder,
        options: EntryData,
        tree: Tree,
    ) {
        val constants = entry.objectType().enumConstants?.filterIsInstance<Enum<*>>()?.takeIf { it.isNotEmpty() } ?: return
        val names = constants.map { it.name }.toTypedArray()
        val byName = constants.associateBy { it.name }
        builder["options"] = names
        // "options" stays the enum names (used for state mapping); show rconfig's display names instead.
        builder["optionLabels"] = constants.map(::enumDisplayName).toTypedArray()

        if (options.hasOption(Option.DRAGGABLE)) {
            // value is the full ordering of enum names
            builder.getter = { (entry.getArray() ?: emptyArray()).mapNotNull { (it as? Enum<*>)?.name }.toTypedArray() }
            builder.setter = setter@{ value ->
                val ordered = (value as? Array<*>)?.mapNotNull { it as? String } ?: return@setter
                entry.setArray(ordered.mapNotNull { byName[it] }.toTypedArray())
            }
            builder["visualizer"] = Visualizer.DraggableListVisualizer::class.java
        } else {
            // value is a boolean[] indexed by option position (selected items)
            builder.getter = {
                val selected = (entry.getArray() ?: emptyArray()).mapNotNull { (it as? Enum<*>)?.name }.toSet()
                BooleanArray(names.size) { names[it] in selected }
            }
            builder.setter = setter@{ value ->
                val flags = value as? BooleanArray ?: return@setter
                entry.setArray(names.filterIndexed { i, _ -> flags.getOrElse(i) { false } }.mapNotNull { byName[it] }.toTypedArray())
            }
            builder["checkable"] = true
            builder["visualizer"] = Visualizer.MultiSelectDropdownVisualizer::class.java
        }
        tree.put(builder.build())
    }

    // Mirror rconfig's own enum label resolution: Translatable key, then StringRepresentable serialized
    // name, then the raw enum name.
    private fun enumDisplayName(value: Enum<*>): String {
        //? >= 1.21.8 {
        return Translatable.toComponent(value).string
        //? } else {
        /*return when (value) {
            is Translatable -> I18n.get(value.translationKey)
            is StringRepresentable -> value.serializedName
            else -> value.toString()
        }
        *///? }
    }

    private class RConfigPropertyBuilder constructor(
        option: ResourcefulConfigValueEntry,
        val sourceId: String,
        val nodeId: String,
    ) {
        //? >= 1.21.8 {
        val name = option.options().title.toComponent()
        val description = option.options().comment.toComponent()
        //? } else {
        /*val name = option.options().title.toLocalizedString()
        val description = option.options().comment.toLocalizedString()
        *///? }

        var setter: (Any) -> Unit = { value ->
            when (option.type()) {
                EntryType.BYTE -> option.byte = (value as Number).toByte()
                EntryType.SHORT -> option.short = (value as Number).toShort()
                EntryType.INTEGER -> option.int = (value as Number).toInt()
                EntryType.LONG -> option.long = (value as Number).toLong()
                EntryType.FLOAT -> option.float = (value as Number).toFloat()
                EntryType.DOUBLE -> option.double = (value as Number).toDouble()

                EntryType.BOOLEAN -> option.boolean = value as Boolean
                EntryType.STRING -> option.string = value as String
                EntryType.ENUM -> option.enum = value as Enum<*>
                else -> null // unknown/handled differently
            }
        }
        var getter: () -> Any = option::get

        // the code-defined default, so the UI can offer "reset to default" (mirrors Config.captureDefaults
        // for native configs). rconfig exposes this directly, so no pre-load snapshot is needed.
        val defaultValue: Any? = option.defaultValue()

        val metadata: MutableMap<String, Any?> = mutableMapOf()

        operator fun set(key: String, value: Any?) = metadata.set(key, value)

        fun build() = Properties.functional(
            getter,
            setter,
            name = name,
            description = description,
            id = nodeId
        ).apply {
            addMetadata(RCONFIG_ID, sourceId)
            defaultValue?.let { addMetadata("default", it) }
            this@RConfigPropertyBuilder.metadata.entries.forEach { (key, value) -> addMetadata(key, value) }
        }
    }
}
//? }
