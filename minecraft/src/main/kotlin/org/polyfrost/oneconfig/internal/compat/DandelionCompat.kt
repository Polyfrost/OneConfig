package org.polyfrost.oneconfig.internal.compat

//? dandelion_compat {
/*import net.azureaaron.dandelion.api.ButtonOption
import net.azureaaron.dandelion.api.ConfigCategory
import net.azureaaron.dandelion.api.LabelOption
import net.azureaaron.dandelion.api.Option
import net.azureaaron.dandelion.api.OptionGroup
import net.azureaaron.dandelion.api.OptionListener
import net.azureaaron.dandelion.api.controllers.BooleanController
import net.azureaaron.dandelion.api.controllers.ColourController
import net.azureaaron.dandelion.api.controllers.EnumController
import net.azureaaron.dandelion.api.controllers.FloatController
import net.azureaaron.dandelion.api.controllers.IntegerController
import net.azureaaron.dandelion.api.controllers.ItemController
import net.azureaaron.dandelion.api.controllers.NumberController
import net.azureaaron.dandelion.api.controllers.StringController
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Property.Display
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import java.util.UUID
import java.util.function.Function
import java.util.function.Supplier

object DandelionCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Dandelion-Compat")

    @JvmStatic
    fun initialize(title: Component, categories: MutableList<ConfigCategory>, save: Supplier<Boolean>) =
        CompatLoader.requireTranslations {
            LOGGER.info("Dandelion compat loaded for $title")
            val mod = CompatLoader.findFirstMod()
            val tree = parseConfig(title, categories, mod, save)
            if (tree != null) {
                CompatSnapshots.register(tree)
                CompatLoader.markFirstModAsSkip()
            }
        }

    fun parseConfig(title: Component, categories: List<ConfigCategory>, mod: ModInfo?, save: Supplier<Boolean>): Tree? {
        val tree = Tree.tree()
        tree.id = mod?.id ?: return null
        tree.title = mod.name
        tree.noCache = true
        tree.saveFunction = { save.get() }
        // Dandelion exposes no icon, so fall back to the mod's icon (same source Mod Menu uses).
        mod.extractIconFile()?.let {
            tree.addMetadata("icon_path", it)
        }

        mod?.extractIconFile()?.let {
            tree.addMetadata("icon_path", it)
        }

        categories.forEach { parseCategory(it, tree) }

        return tree
    }

    fun parseCategory(category: ConfigCategory, root: Tree) {
        category.rootGroup()?.let { parseGroup(it, category, true, root) }
        category.groups().forEach { parseGroup(it, category, false, root) }
    }

    fun parseGroup(group: OptionGroup, category: ConfigCategory, isRootGroup: Boolean, root: Tree) {
        group.options().forEach {
            parseOption(
                it,
                root,
                category.name().string,
                if (isRootGroup) ConfigVisualizer.DEFAULT_SUBCATEGORY else group.name().string
            )
        }
    }

    fun <T : Any> parseOption(option: Option<T>, root: Tree, category: String, subcategory: String) = runCatching {

        val controller = runCatching { option.controller() }.getOrNull()

        when (option) {
            is ButtonOption -> {
                val property = Properties.dummy(id = option.id()?.toString() ?: UUID.randomUUID().toString())
                property.title = option.name()
                property.description = option.description()
                property.visualizer = Visualizer.ButtonVisualizer::class.java
                property.category = category
                property.subcategory = subcategory
                property.addMetadata("textKey", option.prompt())
                property.metadata?.put(
                    "runnable",
                    Runnable { option.action().accept(Platform.screen().current<Screen>()!!) })
                root.put(property)
            }

            is LabelOption -> {
                val property = Properties.dummy(id = option.id()?.toString() ?: UUID.randomUUID().toString())
                property.title = option.name()
                property.description = option.description()
                property.category = category
                property.subcategory = subcategory
                property.visualizer = Visualizer.InfoVisualizer::class.java
                root.put(property)
            }

            else if controller == null -> {
                val property = Properties.dummy(id = option.id()?.toString() ?: UUID.randomUUID().toString())
                property.title = option.name()
                property.description =
                    Component.literal("Failed to create compat entry for option! ").append(option.name())
                        .append("\nPlease report this to the Polyfrost Discord: https://discord.gg/polyfrost")
                property.visualizer = Visualizer.InfoVisualizer::class.java
                property.category = category
                property.subcategory = subcategory
                property.addMetadata("type", "error")
                root.put(property)
            }

            else -> {
                val binding = option.binding()
                val setter: (T) -> Unit = {
                    binding.set(it)
                    option.listeners().forEach {
                        it.onUpdate(option, OptionListener.UpdateType.VALUE_CHANGE)
                    }
                    option.flags().forEach {
                        it.accept(Minecraft.getInstance())
                    }
                }
                val getter: () -> T = binding::get
                val defaultValue: T = binding.defaultValue()
                val property = Properties.functional(
                    getter = { getter() },
                    setter = { value -> setter(value) },
                    id = UUID.randomUUID().toString(),
                    name = option.name(),
                    description = option.description(),
                )

                property.addMetadata("searchTags", option.tags())
                property.category = category
                property.subcategory = subcategory
                property.addDisplayCondition { if (option.modifiable()) Display.SHOWN else Display.DISABLED }

                when (controller) {
                    is BooleanController -> property.visualizer = Visualizer.SwitchVisualizer::class.java
                    is ColourController -> {
                        property.visualizer = Visualizer.ColorVisualizer::class.java
                        property.addMetadata("noAlpha", Unit)
                    }
                    is EnumController<*> if defaultValue is Enum<*> -> {
                        property.visualizer = Visualizer.DropdownVisualizer::class.java
                        property.addMetadata("optionLabels", defaultValue.javaClass.enumConstants.map { (controller.formatter() as Function<T, Component>).apply(it) })
                    }
                    is NumberController<*> -> {
                        if (controller.slider()) {
                            property.visualizer = Visualizer.SliderVisualizer::class.java
                        } else {
                            property.visualizer = Visualizer.NumberVisualizer::class.java
                        }
                        property.addMetadata("step", controller.step().toFloat())
                        property.addMetadata("min", controller.min().toFloat())
                        property.addMetadata("max", controller.max().toFloat())
                    }
                    is StringController -> property.visualizer = Visualizer.TextVisualizer::class.java
                    else -> {
                        LOGGER.warn("Unsupported: ${option.name()} - ${controller.javaClass.simpleName}")
                        root.put(unsupportedOptionProperty(option, controller, category, subcategory))
                        return@runCatching
                    }
                }
                root.put(property)
            }
        }
    }

    private fun <T : Any> unsupportedOptionProperty(
        option: Option<T>,
        controller: Any,
        category: String,
        subcategory: String
    ): Property<Void> {
        val property = Properties.dummy(id = option.id()?.toString() ?: UUID.randomUUID().toString())
        property.title = option.name()
        property.description =
            Component.literal("Option currently not supported by OneConfig")
                .append("\nType: ").append(controller.javaClass.simpleName ?: "Unknown")
                .append("\nIf you need to access this option, please open the mod config manually via Mod Menu.")
                .append("\nThe Polyfrost team will be working on adding support for this option soon!")
        property.visualizer = Visualizer.InfoVisualizer::class.java
        property.category = category
        property.subcategory = subcategory
        property.addMetadata("type", "warning")
        return property
    }

}
*///? }
