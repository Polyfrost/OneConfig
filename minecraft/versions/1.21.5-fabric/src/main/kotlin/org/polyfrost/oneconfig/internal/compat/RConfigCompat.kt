package org.polyfrost.oneconfig.internal.compat

import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigButton
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfigElement
import com.teamresourceful.resourcefulconfig.api.types.elements.ResourcefulConfigEntryElement
import com.teamresourceful.resourcefulconfig.api.types.elements.ResourcefulConfigObjectEntryElement
import com.teamresourceful.resourcefulconfig.api.types.elements.ResourcefulConfigSeparatorElement
import com.teamresourceful.resourcefulconfig.api.types.entries.ResourcefulConfigValueEntry
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType
import com.teamresourceful.resourcefulconfig.api.types.options.Option
import dev.deftu.omnicore.common.OmniLoader
import org.polyfrost.oneconfig.api.config.v1.*
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.internal.DynamicPolyImage
import org.polyfrost.oneconfig.utils.v1.dsl.*
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.argb
import org.polyfrost.polyui.color.mutable
import java.util.*
import kotlin.reflect.KClass

internal object RConfigCompat {


    @JvmStatic
    fun enable() = Unit

    @JvmStatic
    fun addConfig(config: ResourcefulConfig) {
        val mod = CompatLoader.findFirstMod()
        CompatLoader.requireTranslations { parseConfig(config, null, null, mod)?.let(ConfigManager.active()::register) }
    }

    private fun parseConfig(config: ResourcefulConfig, category: String?, root: Tree?, mod: OmniLoader.ModInfo?): Tree? {
        val tree = Tree.tree()
        tree.id = config.id()
        tree.title = config.info().title().toLocalizedString()
        tree.description = config.info().description().toLocalizedString()
        tree.category = category ?: config.info().title().toLocalizedString()
        tree.subcategory = config.info().title().toLocalizedString()
        if (category == null) {
            mod?.let {
                val path = it.iconPath ?: return@let
                val stream = it.icon ?: return@let
                tree.icon = DynamicPolyImage(path, stream)
            }
        }

        config.categories().values.mapNotNull {
            parseConfig(
                it,
                category ?: it.info().title().toLocalizedString(),
                root ?: tree,
                mod
            )
        }.forEach((root ?: tree)::put)

        parseAny(config.elements(), tree)

        tree.addMetadata("custom_save", Runnable { config.save() })
        tree.addMetadata("no_cache", true)

        return tree
    }

    private fun parseButton(button: ResourcefulConfigButton, tree: Tree) {
        val property = Properties.dummy(id = UUID.randomUUID().toString())
        property.title = button.title()?.takeUnless { it.isEmpty() }
            ?: "button" //todo find a better way of doing this, rconfig allows empty names
        property.description = button.description()
        property.visualizer = Visualizer.ButtonVisualizer::class.java
        property.metadata?.put("runnable", Runnable { button.invoke() })
        property.addDisplayCondition { if (button.isHidden) Property.Display.HIDDEN else Property.Display.SHOWN }
        tree.put(property)
    }

    private fun parseAny(list: Iterable<ResourcefulConfigElement>, tree: Tree) = list.forEach { parseAny(it, tree) }

    private fun parseAny(element: ResourcefulConfigElement, tree: Tree) = when (element) {
        is ResourcefulConfigButton -> parseButton(element, tree)
        is ResourcefulConfigObjectEntryElement -> parseCategory(element, tree)
        is ResourcefulConfigEntryElement -> buildAndAdd(element, tree)
        is ResourcefulConfigSeparatorElement -> null // Todo, they arent implemented yet :3
        else -> null
    }

    private fun parseCategory(element: ResourcefulConfigObjectEntryElement, tree: Tree) {
        if (element.isHidden) return
        val entry = element.entry()
        val objectEntry = Tree.tree()
        objectEntry.title = entry.options().title.toLocalizedString()
        objectEntry.description = entry.options().comment.toLocalizedString()
        objectEntry.id = UUID.randomUUID().toString()
        objectEntry.category = tree.category
        objectEntry.subcategory = entry.options().title.toLocalizedString()
        objectEntry.index = -1
        parseAny(entry.elements(), objectEntry)
        tree.put(objectEntry)

    }

    private fun buildAndAdd(element: ResourcefulConfigEntryElement, tree: Tree) {
        val entry = element.entry() as? ResourcefulConfigValueEntry ?: return
        val builder = RConfigPropertyBuilder(entry)

        if (entry.get().javaClass.isArray) return
        val options = entry.options() // todo draggable list and multiselects

        val visualizer: KClass<out Visualizer> = when (entry.type()) {
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
                        val polyColor = color as? PolyColor ?: return@setter

                        entry.int = polyColor.argb
                    }

                    builder.getter = {
                        argb(entry.int).mutable()
                    }

                    Visualizer.ColorVisualizer::class
                } else if (entry.options().hasOption(Option.SLIDER)) {
                    Visualizer.SliderVisualizer::class
                } else {
                    Visualizer.NumberVisualizer::class
                }
            }

            EntryType.STRING -> {
                // TODO multiline :pensive:
                builder["validate"] =
                    if (options.hasOption(Option.REGEX)) options.getOption(Option.REGEX).pattern() else null
                Visualizer.TextVisualizer::class
            }

            EntryType.BOOLEAN -> Visualizer.SwitchVisualizer::class
            EntryType.ENUM -> Visualizer.DropdownVisualizer::class
            else -> null
        } ?: return

        builder["visualizer"] = visualizer.java
        val build = builder.build()
        build.addDisplayCondition { if (element.isHidden) Property.Display.HIDDEN else Property.Display.SHOWN }
        tree.put(build)
    }

    private class RConfigPropertyBuilder internal constructor(option: ResourcefulConfigValueEntry) {
        val name: String? = option.options().title.toLocalizedString()
        val description: String? = option.options().comment.toLocalizedString()

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