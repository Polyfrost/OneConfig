package org.polyfrost.oneconfig.internal.compat

//? dandelion_compat {
/*import net.azureaaron.dandelion.api.ButtonOption
import net.azureaaron.dandelion.api.ConfigCategory
import net.azureaaron.dandelion.api.Option
import net.azureaaron.dandelion.api.OptionGroup
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
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
import java.util.function.Supplier

object DandelionCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/Dandelion-Compat")

    @JvmStatic
    fun initialize(title: Component, categories: MutableList<ConfigCategory>, save: Supplier<Boolean>) = CompatLoader.requireTranslations {
        LOGGER.info("Silly thing loaded")
        val mod = CompatLoader.findFirstMod()
        val tree = parseConfig(title, categories, mod, save)
        if (tree != null) {
            ConfigManager.active().register(tree)
            CompatLoader.markFirstModAsSkip()
        }
    }

    fun parseConfig(title: Component, categories: List<ConfigCategory>, mod: ModInfo?, save: Supplier<Boolean>): Tree? {
        val tree = Tree.tree()
        tree.id = mod?.id ?: return null
        tree.title = mod.name
        tree.noCache = true
        tree.saveFunction = { save }

        categories.forEach { parseCategory(it, tree) }

        return tree
    }

    fun parseCategory(category: ConfigCategory, root: Tree) {
        category.rootGroup()?.let { parseGroup(it, category, true, root) }
        category.groups().forEach { parseGroup(it, category, false, root) }
    }

    fun parseGroup(group: OptionGroup, category: ConfigCategory, isRootGroup: Boolean, root: Tree) {
        val groupTree = Tree.tree(category.id().toString())
        groupTree.title = category.name().string
        groupTree.description = category.description()
        groupTree.addMetadata("category", category.id().toString())

        group.options().forEach {
            parseOption(
                it,
                root,
                category.name().string,
                if (isRootGroup) ConfigVisualizer.DEFAULT_SUBCATEGORY else group.name().string
            )
        }

        root.put(groupTree)
    }

    fun <T : Any> parseOption(option: Option<T>, root: Tree, category: String, subcategory: String) = runCatching  {


        if (option is ButtonOption) {
                val property = Properties.dummy(id = UUID.randomUUID().toString())
                property.title = option.name()
                property.description = option.description()
                property.visualizer = Visualizer.ButtonVisualizer::class.java
                property.metadata?.put("runnable", Runnable { option.action().accept(Platform.screen().current<Screen>()!!) })
                root.put(property)

            return@runCatching
        }

        val binding =  option.binding()


        val getter: () -> T = binding::get
        val setter: (T) -> Unit = binding::set
        val defaultValue: T = binding.defaultValue()

        // ButtonOption and similar value-less options expose a binding whose getValue() throws
        // UnsupportedOperationException (EmptyBinderImpl). Skip them instead of logging a warning.
        val currentValue = runCatching { getter() }.getOrNull() ?: return@runCatching

        val visualizer: Class<out Visualizer> = when (currentValue) {
            is Boolean -> Visualizer.SwitchVisualizer::class.java
            is Int, is Float, is Double, is Long -> Visualizer.SliderVisualizer::class.java
            is String -> Visualizer.TextVisualizer::class.java
            is Enum<*> -> Visualizer.DropdownVisualizer::class.java
            is java.awt.Color -> Visualizer.ColorVisualizer::class.java
            else -> return@runCatching // Skip unsupported types
        }

        val property = Properties.functional(
            getter = { getter() },
            setter = { value -> setter(value) },
            id = UUID.randomUUID().toString(),
            name = option.name(),
            description = option.description(),
        )

        property.addMetadata("visualizer", visualizer)
        defaultValue.let { property.addMetadata("default", it) }
        property.category = category
        property.subcategory = subcategory

        when (currentValue) {
            is Enum<*> -> {
                val constants = currentValue::class.java.enumConstants
                property.addMetadata("options", constants?.map { it.toString() } ?: emptyList<String>())
            }

            is Int, is Float, is Double, is Long -> {
                property.addMetadata("min", 0f)
                property.addMetadata("max", 100f)
            }
        }

        root.put(property)
    }

}
*///? }