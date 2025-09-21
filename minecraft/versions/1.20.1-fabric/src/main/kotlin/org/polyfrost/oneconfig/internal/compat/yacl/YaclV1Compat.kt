package org.polyfrost.oneconfig.internal.compat.yacl

import net.minecraft.text.Text as Text // preprocessor moment, ping me (wyvest) if you want an explanation why this is needed

//#if MC != 1.20.4 || FABRIC
import dev.deftu.omnicore.common.OmniLoader
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Controller
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.gui.controllers.BooleanController
import dev.isxander.yacl3.gui.controllers.ColorController
import dev.isxander.yacl3.gui.controllers.LabelController
import dev.isxander.yacl3.gui.controllers.TickBoxController
import dev.isxander.yacl3.gui.controllers.cycling.EnumController
import dev.isxander.yacl3.gui.controllers.slider.ISliderController
import dev.isxander.yacl3.gui.controllers.string.StringController
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.*
import org.polyfrost.oneconfig.internal.DynamicPolyImage
import org.polyfrost.oneconfig.internal.compat.CompatLoader
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.asMutable
import org.polyfrost.polyui.color.toPolyColor
import java.awt.Color
import java.util.UUID


internal val extraYaclHandlers = mutableListOf<ExtraHandler<out Controller<*>>>()

object YaclV1Compat {

    init {
        ExtraV1Handlers
    }

    internal fun handle(controller: Controller<*>, builder: YaclPropertyBuilder): Class<out Visualizer>? {
        extraYaclHandlers.forEach {
            if (it.canHandle(controller)) return it.handle(controller, builder)
        }
        return null
    }

    @JvmStatic
    fun build(text: Text, categories: List<ConfigCategory>, saveFunction: Runnable) {
        val mod = CompatLoader.findFirstMod()
        CompatLoader.requireTranslations {
            parseConfig(
                text,
                categories,
                saveFunction,
                mod
            )?.let(ConfigManager.active()::register)
        }
    }

    private fun parseConfig(
        text: Text,
        categories: List<ConfigCategory>,
        saveFunction: Runnable,
        mod: OmniLoader.ModInfo?,
    ): Tree? {
        val root = Tree.tree()


        val allOptions = categories.flatMap(ConfigCategory::groups).flatMap(OptionGroup::options)
        val saveAll = { allOptions.forEach(Option<*>::applyValue) }

        root.title = text.stripped
        root.id = root.title
        mod?.let {
            val path = it.iconPath ?: return@let
            val stream = it.icon ?: return@let
            root.icon = DynamicPolyImage(path, stream)
        }
        root.saveFunction = Runnable {
            saveAll()
            saveFunction.run()
        }
        root.addMetadata("no_cache", true)

        categories.forEach { category -> parseConfigCategory(category, root) }

        return root
    }

    private fun parseConfigCategory(category: ConfigCategory, parent: Tree) {
        val tree = Tree.tree()

        tree.title = category.name().stripped
        tree.description = category.tooltip().stripped
        tree.subcategory = tree.title
        tree.category = tree.title

        tree.id = UUID.randomUUID().toString()

        parent.put(tree)
        category.groups().forEach { group -> if (group.isRoot) parseGroup(group, tree, category.name().stripped) else parseGroup(group, parent, category.name().stripped) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseGroup(group: OptionGroup, parent: Tree, category: String) {
        if (group.isRoot) {
            group.options().forEach { option -> parseOption(option as Option<Any>, parent) }
            return
        }

        val tree = Tree.tree()
        tree.subcategory = group.name().stripped
        tree.category = category
        tree.title = group.name().stripped.takeUnless(String::isEmpty) ?: "General"
        tree.id = UUID.randomUUID().toString()

        group.options().forEach { option -> parseOption(option as Option<Any>, tree) }

        parent.put(tree)
    }

    @Suppress("DEPRECATED")
    private fun parseOption(config: Option<Any>, parent: Tree) {
        val builder = YaclPropertyBuilder(config)

        val controller = config.controller()

        val visualizer: Class<out Visualizer>? = when (controller) {
            is TickBoxController, is BooleanController -> {
                builder.setter = { value -> (value as? Boolean)?.let { controller.option().requestAndSubmitSet(value) } }
                builder.getter = { controller.option().binding().value }
                Visualizer.SwitchVisualizer::class.java
            }

            is StringController -> {
                builder.setter = { value -> (value as? String)?.let { controller.setFromString(value); controller.option().applyValue() } }
                builder.getter = { controller.option().binding().value }
                Visualizer.TextVisualizer::class.java
            }

            is ISliderController -> {
                builder.metadata["max"] = controller.max().toFloat()
                builder.metadata["min"] = controller.min().toFloat()
                builder.setter = { value -> (value as? Number)?.let { controller.setPendingValue(value.toDouble()); controller.option().applyValue() } }
                builder.getter = { (controller.option().binding().value as? Number)?.toFloat() ?: 0 }
                Visualizer.SliderVisualizer::class.java
            }

            is ColorController -> { // todo alpha
                builder.setter =
                    { value -> (value as? PolyColor)?.let { controller.option().requestAndSubmitSet(Color(value.argb)) } }
                builder.getter = { controller.option().binding().value.toPolyColor().asMutable() }
                Visualizer.ColorVisualizer::class.java
            }

            is EnumController -> {
                builder.setter = { value -> (value as? Enum<*>)?.let { config.requestAndSubmitSet(value) } }
                builder.getter = { controller.option().binding().value }
                Visualizer.DropdownVisualizer::class.java
            }

            is LabelController -> {
                // todo
                builder.setter = {}
                builder.getter = {}
                Visualizer.InfoVisualizer::class.java
            }

            else -> handle(controller, builder)
        }

        visualizer ?: return
        val property = builder.build()
        property.visualizer = visualizer
        parent.put(property)
    }

    internal fun <T : Any> Option<T>.requestAndSubmitSet(value: T) {
        this.requestSet(value)
        this.applyValue()
    }
}

private val Text.stripped
    get() = this.string.replace(Regex("§."), "")

internal class YaclPropertyBuilder internal constructor(option: Option<*>) {
    val name: String? = option.name().stripped
    val description: String? = option.description().text().stripped

    lateinit var setter: (Any) -> Unit
    lateinit var getter: () -> Any

    val metadata: MutableMap<String, Any?> = mutableMapOf()

    operator fun set(key: String, value: Any?) = metadata.set(key, value)

    fun build() = Properties.functional(
        getter,
        setter,
        name = name,
        description = description,
        id = UUID.randomUUID().toString()
    ).apply {
        this@YaclPropertyBuilder.metadata.entries.forEach { (key, value) -> addMetadata(key, value) }
    }
}
//#endif