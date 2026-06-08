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
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.index
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import java.util.*

internal object RConfigCompat : Logger by LogManager.getLogger("OneConfig/RconfigCompat") {

    @JvmStatic
    fun enable() {
        info("Detected rconfig, enabling compat layer!")
    }

    @JvmStatic
    fun addConfig(config: ResourcefulConfig) {
        val mod = CompatLoader.findFirstMod()
        info("Preparing config wrapper for ${config.id()}!")
        CompatLoader.requireTranslations {
            parseConfig(config, mod).let(ConfigManager.active()::register)
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

        config.categories().values.forEach {
            parseCategory(it, config.id(), null, tree)
        }

        //? >= 1.21.8 {
        parseAny(config.elements(), tree)
        //? } else {
        /*parseAny(config.entries().values, tree)
        parseButtons(config.buttons(), tree)
        *///? }

        tree.addMetadata("custom_save", Runnable { config.save() })
        tree.addMetadata("no_cache", true)

        return tree
    }


    // 1st layer gets converted to categories, 2nd+ layer to subcategories
    private fun parseCategory(config: ResourcefulConfig, id: String, category: String?, root: Tree) {
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
            parseCategory(entry, nestedId, category ?: title, root)
        }
        //? >= 1.21.8 {
        parseAny(config.elements(), tree)
        //? } else {
        /*parseAny(config.entries().values, tree)
        parseButtons(config.buttons(), tree)
        *///? }

        tree.map.forEach { (_, node) ->
            node.category = tree.category
            node.subcategory = tree.subcategory
            root.put(node)
        }
    }

    private fun parseButton(button: ResourcefulConfigButton, tree: Tree) {
        val property = Properties.dummy(id = UUID.randomUUID().toString())
        property.title = button.title()?.takeUnless { it.isEmpty() }
            ?: "button" //todo find a better way of doing this, rconfig allows empty names
        property.description = button.description()
        property.visualizer = Visualizer.ButtonVisualizer::class.java
        property.metadata?.put("runnable", Runnable { button.invoke() })
        tree.put(property)
    }

    //? >= 1.21.8 {
    private fun parseAny(list: Iterable<ResourcefulConfigElement>, tree: Tree) = list.forEach {
        when (it) {
            is ResourcefulConfigCategory -> parseCategory(it, tree)
            is ResourcefulConfigEntryElement -> parseAny(it, tree)
            is ResourcefulConfigButton -> parseButton(it, tree)
        }
    }

    private fun parseAny(entry: ResourcefulConfigEntryElement, tree: Tree) {
        when (val entry = entry.entry()) {
            is ResourcefulConfigObjectEntry -> parseObject(entry, tree)
            is ResourcefulConfigValueEntry -> buildAndAdd(entry, tree)
        }
    }

    private fun parseCategory(entry: ResourcefulConfigCategory, tree: Tree) {
        val category = Tree.tree()
        category.title = entry.info().title().toComponent()
        category.description = entry.info().description().toComponent()
        category.id = UUID.randomUUID().toString()
        category.category = tree.category
        category.subcategory = entry.info().title().toComponent().string
        category.index = -1
        parseAny(entry.elements(), category)
        tree.put(category)
    }

    private fun parseObject(entry: ResourcefulConfigObjectEntry, tree: Tree) {
        val objectEntry = Tree.tree()
        objectEntry.title = entry.options().title.toComponent()
        objectEntry.description = entry.options().comment.toComponent()
        objectEntry.id = UUID.randomUUID().toString()
        objectEntry.category = tree.category
        objectEntry.subcategory = entry.options().title.toComponent().string
        objectEntry.index = -1
        parseAny(entry.elements(), objectEntry)
        tree.put(objectEntry)
    }
    //? } else {
    /*private fun parseAny(list: Iterable<ResourcefulConfigEntry>, tree: Tree) = list.forEach {
        when (it) {
            is ResourcefulConfigObjectEntry -> parseObject(it, tree)
            is ResourcefulConfigValueEntry -> buildAndAdd(it, tree)
        }
    }

    private fun parseButtons(buttons: List<ResourcefulConfigButton>, tree: Tree) {
        buttons.forEach { parseButton(it, tree) }
    }

    private fun parseObject(entry: ResourcefulConfigObjectEntry, tree: Tree) {
        val objectEntry = Tree.tree()
        objectEntry.title = entry.options().title.toLocalizedString()
        objectEntry.description = entry.options().comment.toLocalizedString()
        objectEntry.id = UUID.randomUUID().toString()
        objectEntry.category = tree.category
        objectEntry.subcategory = entry.options().title.toLocalizedString()
        objectEntry.index = -1
        parseAny(entry.entries().values, objectEntry)
        tree.put(objectEntry)
    }
    *///? }

    private fun buildAndAdd(entry: ResourcefulConfigValueEntry, tree: Tree) {
        val builder = RConfigPropertyBuilder(entry)
        val options = entry.options()

        if (entry.isArray) {
            // Only enum arrays are supported: a draggable reorderable list, or a multi-select dropdown.
            if (entry.type() == EntryType.ENUM) buildEnumArray(entry, builder, options, tree)
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
    // name, then the raw enum name. Resolved via Translatable.toComponent so the active language applies.
    private fun enumDisplayName(value: Enum<*>): String = Translatable.toComponent(value).string

    private class RConfigPropertyBuilder constructor(option: ResourcefulConfigValueEntry) {
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
            id = UUID.randomUUID().toString()
        ).apply {
            defaultValue?.let { addMetadata("default", it) }
            this@RConfigPropertyBuilder.metadata.entries.forEach { (key, value) -> addMetadata(key, value) }
        }
    }
}
//? }
