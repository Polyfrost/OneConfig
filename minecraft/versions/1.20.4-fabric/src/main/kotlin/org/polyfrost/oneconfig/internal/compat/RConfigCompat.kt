package org.polyfrost.oneconfig.internal.compat

import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigButton
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigEntry
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigObjectEntry
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigValueEntry
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType
import jdk.internal.org.jline.utils.InfoCmp
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import java.util.*
import kotlin.reflect.KClass

internal object RConfigCompat {

    @JvmStatic
    fun enable() {

    }

    @JvmStatic
    fun addConfig(config: ResourcefulConfig) = parseConfig(config)?.let(ConfigManager.active()::register)

    fun parseConfig(config: ResourcefulConfig): Tree? {
        val tree = Tree.tree()
        tree.id = config.id()
        tree.title = config.info().title().toLocalizedString()
        tree.description = config.info().description().toLocalizedString()

        config.categories().values.mapNotNull(::parseConfig).forEach(tree::put)

        parseAny(config.entries().values, tree)
        parseButtons(config.buttons(), tree)

        tree.addMetadata("custom_save", Runnable { config.save() })
        tree.addMetadata("no_cache", true)

        return tree
    }

    fun parseButtons(buttons: List<ResourcefulConfigButton>, tree: Tree) {
        buttons.forEach { button ->
            val property = Properties.dummy(id = UUID.randomUUID().toString())
            property.title = button.title()?.takeUnless { it.isEmpty() }
                ?: "button" //todo find a better way of doing this, rconfig allows empty names
            property.description = button.description()
            property.metadata?.put("visualizer", Visualizer.ButtonVisualizer::class.java)
            property.metadata?.put("runnable", Runnable { button.invoke() })
            tree.put(property)
        }
    }

    fun parseAny(list: Iterable<ResourcefulConfigEntry>, tree: Tree) {
        parseCategories(list.filterIsInstance<ResourcefulConfigObjectEntry>(), tree)
        parseValues(list.filterIsInstance<ResourcefulConfigValueEntry>(), tree)
    }

    fun parseCategories(list: List<ResourcefulConfigObjectEntry>, tree: Tree) {
        list.forEach { entry ->
            val objectEntry = Tree.tree()
            objectEntry.title = entry.options().title.toLocalizedString()
            objectEntry.description = entry.options().comment.toLocalizedString()
            objectEntry.id = UUID.randomUUID().toString()
            objectEntry.addMetadata("category", entry.options().title.toLocalizedString())
            objectEntry.addMetadata("subcategory", entry.options().title.toLocalizedString())
            objectEntry.addMetadata("index", -1)
            objectEntry.addMetadata("icon", "")
            parseAny(entry.entries().values, tree)
            tree.put(objectEntry)
        }
    }

    fun parseValues(list: List<ResourcefulConfigValueEntry>, tree: Tree) {
        list.forEach { buildAndAdd(it, tree) }
    }

    fun buildAndAdd(entry: ResourcefulConfigValueEntry, tree: Tree) {
        val builder = RConfigPropertyBuilder(entry)

        if (entry.get().javaClass.isArray) return
        val options = entry.options()
        val visualizer: KClass<out Visualizer> = when (entry.type()) {
            EntryType.BYTE, EntryType.SHORT, EntryType.INTEGER, EntryType.LONG, EntryType.FLOAT, EntryType.DOUBLE -> {
                if (entry.options().hasRange) {
                    builder["min"] = options.min.toFloat()
                    builder["max"] = options.max.toFloat()
                } else {
                    builder["min"] = Float.MIN_VALUE
                    builder["max"] = Float.MAX_VALUE
                }

                if (entry.options().hasSlider) {
                    Visualizer.SliderVisualizer::class
                } else {
                    Visualizer.NumberVisualizer::class
                }
            }

            EntryType.STRING -> {
                // TODO multiline :pensive:
                builder["validate"] = if (options.hasRegex()) options.regex.pattern() else null
                Visualizer.TextVisualizer::class
            }

            EntryType.BOOLEAN -> Visualizer.SwitchVisualizer::class
            EntryType.ENUM -> Visualizer.DropdownVisualizer::class
            else -> null
        } ?: return

        builder["visualizer"] = visualizer.java
        tree.put(builder.build())
    }

    class RConfigPropertyBuilder internal constructor(option: ResourcefulConfigValueEntry) {
        val name: String? = option.options().title.toLocalizedString()
        val description: String? = option.options().comment().toLocalizedString()

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
                EntryType.OBJECT -> null // cant happen, is handled by ObjectEntry
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