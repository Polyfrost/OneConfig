//? tr7zw_compat {
package org.polyfrost.oneconfig.internal.compat

import net.minecraft.network.chat.Component
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId
import java.lang.reflect.Field
import java.util.function.Consumer
import java.util.function.DoubleConsumer
import java.util.function.DoubleSupplier
import java.util.function.IntConsumer
import java.util.function.IntSupplier
import java.util.function.Supplier

object Tr7zwConfigCompat {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/Tr7zw-Compat")

    private const val DEFAULT_CATEGORY = "General"

    @JvmStatic
    fun parseOptionList(screen: Any, options: List<*>?) {
        runCatching {
            if (options.isNullOrEmpty()) return
            val mod = CompatLoader.findFirstMod()
            if (mod != null && CompatLoader.nativeLoadedConfigs.contains(mod.id)) {
                return
            }
            val tree = parseScreen(screen, options, mod) ?: return
            CompatSnapshots.register(tree)
            CompatLoader.markFirstModAsSkip()
        }.onFailure {
            LOGGER.warn("Failed to parse tr7zw config", it)
        }
    }

    private fun parseScreen(screen: Any, options: List<*>, mod: ModInfo?): Tree? {
        val tree = Tree.tree()
        tree.id = mod?.id ?: screen.javaClass.name
        tree.title = mod?.name?.takeIf { it.isNotBlank() }
            ?: readScreenTitle(screen)?.takeIf { it.isNotBlank() }
            ?: "tr7zw Config"
        tree.noCache = true
        // tr7zw/trender configs expose no icon, so fall back to the mod's icon (same source Mod Menu uses).
        mod?.extractIconFile()?.let {
            tree.addMetadata("icon_path", it)
        }

        findMethod(screen.javaClass, "save")?.let { saveMethod ->
            tree.saveFunction = Runnable { runCatching { saveMethod.invoke(screen) } }
        }
        findMethod(screen.javaClass, "reset")?.let { resetMethod ->
            tree.addMetadata(CompatSnapshots.CUSTOM_RESET_METADATA, Runnable { resetMethod.invoke(screen) })
        }

        var category = DEFAULT_CATEGORY
        var categoryPath = idPart(DEFAULT_CATEGORY, "general")
        var added = false
        val usedIds = HashSet<String>()
        for (option in options) {
            if (option == null) continue
            if (option.javaClass.simpleName == "SplitLine") {
                val splitKey = readTranslationKey(option)
                category = splitKey?.let { resolveTranslation(it) ?: cleanKey(it) } ?: DEFAULT_CATEGORY
                categoryPath = idPart(splitKey, "general")
                continue
            }
            runCatching {
                if (parseOption(option, category, categoryPath, tree, usedIds)) added = true
            }.onFailure { LOGGER.warn("Failed to parse tr7zw option", it) }
        }

        return if (added) tree else null
    }

    private fun parseOption(
        option: Any,
        category: String,
        categoryPath: String,
        root: Tree,
        usedIds: MutableSet<String>,
    ): Boolean {
        val key = readTranslationKey(option) ?: return false
        val name = resolveTranslation(key) ?: cleanKey(key)
        val description = resolveTranslation("$key.tooltip")

        val builder = when (option.javaClass.simpleName) {
            "Toggle" -> toggleProperty(option)
            "IntOption" -> intProperty(option)
            "DoubleOption" -> doubleProperty(option)
            "EnumOption" -> enumProperty(option, key)
            else -> null
        } ?: return false

        val property = Properties.functional(
            getter = builder.getter,
            setter = builder.setter,
            id = uniqueId(usedIds, "$categoryPath/${idPart(key, "option")}"),
            name = name,
            description = description,
        )
        property.addMetadata("visualizer", builder.visualizer)
        builder.metadata.forEach { (mk, mv) -> property.addMetadata(mk, mv) }
        property.category = category
        property.subcategory = category

        root.put(property)
        return true
    }

    private class OptionBuilder(
        val getter: () -> Any?,
        val setter: (Any?) -> Unit,
        val visualizer: Class<out Visualizer>,
        val metadata: Map<String, Any?> = emptyMap(),
    )

    private fun toggleProperty(option: Any): OptionBuilder {
        @Suppress("UNCHECKED_CAST")
        val current = recordComponent(option, "current") as Supplier<Boolean>
        @Suppress("UNCHECKED_CAST")
        val update = recordComponent(option, "update") as Consumer<Boolean>
        return OptionBuilder(
            getter = { current.get() },
            setter = { v -> update.accept(v as Boolean) },
            visualizer = Visualizer.SwitchVisualizer::class.java,
        )
    }

    private fun intProperty(option: Any): OptionBuilder {
        val current = recordComponent(option, "current") as IntSupplier
        val update = recordComponent(option, "update") as IntConsumer
        val min = (recordComponent(option, "min") as Number).toFloat()
        val max = (recordComponent(option, "max") as Number).toFloat()
        return OptionBuilder(
            getter = { current.asInt },
            setter = { v -> update.accept((v as Number).toInt()) },
            visualizer = Visualizer.SliderVisualizer::class.java,
            metadata = mapOf("min" to min, "max" to max),
        )
    }

    private fun doubleProperty(option: Any): OptionBuilder {
        val current = recordComponent(option, "current") as DoubleSupplier
        val update = recordComponent(option, "update") as DoubleConsumer
        val min = (recordComponent(option, "min") as Number).toFloat()
        val max = (recordComponent(option, "max") as Number).toFloat()
        return OptionBuilder(
            getter = { current.asDouble.toFloat() },
            setter = { v -> update.accept((v as Number).toDouble()) },
            visualizer = Visualizer.SliderVisualizer::class.java,
            metadata = mapOf("min" to min, "max" to max),
        )
    }

    private fun enumProperty(option: Any, translationKey: String): OptionBuilder {
        @Suppress("UNCHECKED_CAST")
        val current = recordComponent(option, "current") as Supplier<Any?>
        @Suppress("UNCHECKED_CAST")
        val update = recordComponent(option, "update") as Consumer<Any?>
        val targetEnum = recordComponent(option, "targetEnum") as Class<*>
        val constants = (targetEnum.enumConstants ?: emptyArray()).filterIsInstance<Enum<*>>()
        val labels = constants.map { enumValue ->
            resolveTranslation("$translationKey.${enumValue.name}") ?: enumValue.toString()
        }
        return OptionBuilder(
            getter = { current.get() },
            setter = { v -> update.accept(v) },
            visualizer = Visualizer.DropdownVisualizer::class.java,
            metadata = mapOf("options" to labels),
        )
    }

    private fun recordComponent(option: Any, name: String): Any? =
        option.javaClass.getMethod(name).apply { isAccessible = true }.invoke(option)

    private fun readTranslationKey(option: Any): String? =
        runCatching { recordComponent(option, "translationKey") as? String }.getOrNull()

    private fun readScreenTitle(screen: Any): String? {
        val field = findField(screen.javaClass, "title")?.apply { isAccessible = true } ?: return null
        return (field.get(screen) as? Component)?.string
    }

    private fun resolveTranslation(key: String): String? {
        val translated = runCatching { Component.translatable(key).string }.getOrNull() ?: return null
        return if (translated == key || translated.isBlank()) null else translated
    }

    private fun cleanKey(key: String): String {
        val segment = key.substringAfterLast('.').ifBlank { key }
        return segment.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun findField(cls: Class<*>, name: String): Field? {
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            current.declaredFields.firstOrNull { it.name == name }?.let { return it }
            current = current.superclass
        }
        return null
    }

    private fun findMethod(cls: Class<*>, name: String): java.lang.reflect.Method? {
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            current.declaredMethods.firstOrNull { it.name == name && it.parameterCount == 0 }
                ?.let { it.isAccessible = true; return it }
            current = current.superclass
        }
        return null
    }
}

//? }
