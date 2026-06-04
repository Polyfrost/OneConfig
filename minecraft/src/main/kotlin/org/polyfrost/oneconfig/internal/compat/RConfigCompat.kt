package org.polyfrost.oneconfig.internal.compat

//? rconfig_compat {
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigButton
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigCategory
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigElement
import com.teamresourceful.resourcefulconfig.api.types.elements.ResourcefulConfigEntryElement
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigObjectEntry
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigValueEntry
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType
import com.teamresourceful.resourcefulconfig.api.types.options.Option
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.icon
import org.polyfrost.oneconfig.api.config.v1.dsl.index
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.internal.DynamicImage
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
        tree.title = config.info().title().toComponent()
        tree.description = config.info().description().toComponent()
        mod?.modIconPath?.let {
            tree.addMetadata("icon_path", it)
        }

        config.categories().values.mapNotNull {
            parseCategory(it, config.id(), null, tree)
        }

        parseAny(config.elements(), tree)

        tree.addMetadata("custom_save", Runnable { config.save() })
        tree.addMetadata("no_cache", true)

        return tree
    }


    // 1st layer gets converted to categories, 2nd+ layer to subcategories
    private fun parseCategory(config: ResourcefulConfig, id: String, category: String?, root: Tree) {
        val tree = Tree.tree()

        val id = "$id/${config.id()}"
        val title = config.info().title().toComponent().string

        tree.category = category ?: title
        if (category != null) {
            tree.subcategory = title
        }

        for ((_, entry) in config.categories()) {
            parseCategory(entry, id, category ?: title, root)
        }
        parseAny(config.elements(), tree)

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

    private fun buildAndAdd(entry: ResourcefulConfigValueEntry, tree: Tree) {
        val builder = RConfigPropertyBuilder(entry)

        if (entry.get().javaClass.isArray) return
        val options = entry.options() // todo draggable list and multiselects

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
            EntryType.ENUM -> Visualizer.DropdownVisualizer::class.java
            else -> null
        } ?: return

        builder["visualizer"] = visualizer
        tree.put(builder.build())
    }

    private class RConfigPropertyBuilder constructor(option: ResourcefulConfigValueEntry) {
        val name = option.options().title.toComponent()
        val description = option.options().comment().toComponent()

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

        val metadata: MutableMap<String, Any?> = mutableMapOf()

        operator fun set(key: String, value: Any?) = metadata.set(key, value)

        fun build() = Properties.functional(
            getter,
            setter,
            name = name,
            description = description,
            id = UUID.randomUUID().toString()
        ).apply {
            this@RConfigPropertyBuilder.metadata.entries.forEach { (key, value) -> addMetadata(key, value) }
        }
    }
}
//? }