//? malilib_compat {
package org.polyfrost.oneconfig.internal.compat

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import java.lang.reflect.Method
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID

object MaLiLibCompat {

    private val LOGGER = LogManager.getLogger("OneConfig/MaLiLib-Compat")

    private const val CONFIG_MANAGER = "fi.dy.masa.malilib.config.ConfigManager"

    private val trees = HashMap<String, Tree>()
    private val seen = HashMap<String, MutableSet<Any>>()

    @JvmStatic
    fun onScreen(screen: Any) {
        runCatching {
            val modId = invoke(screen, "getModId") as? String ?: return
            if (CompatLoader.nativeLoadedConfigs.contains(modId)) return

            @Suppress("UNCHECKED_CAST")
            val wrappers = invoke(screen, "getConfigs") as? Collection<*> ?: return
            if (wrappers.isEmpty()) return

            val tree = trees.getOrPut(modId) { createTree(modId) }
            val added = seen.getOrPut(modId) { Collections.newSetFromMap(IdentityHashMap()) }
            val category = resolveTitle(screen).nonBlankOrNull() ?: (ModInfo.loadedMods.firstOrNull { it.id == modId }?.name ?: modId)

            var subcategory: String? = null
            var newProps = false
            for (wrapper in wrappers) {
                if (wrapper == null) continue
                val type = (invoke(wrapper, "getType") as? Enum<*>)?.name
                if (type == "LABEL") {
                    subcategory = (invoke(wrapper, "getLabel") as? String)?.nonBlankOrNull()
                    continue
                }
                val config = invoke(wrapper, "getConfig") ?: continue
                if (!added.add(config)) continue
                runCatching {
                    if (parseConfig(config, category, subcategory, modId, tree)) newProps = true
                }.onFailure { LOGGER.warn("Failed to parse MaLiLib config", it) }
            }

            if (newProps && tree.getMetadata<Any>("oc_malilib_registered") == null) {
                tree.addMetadata("oc_malilib_registered", true)
                CompatSnapshots.register(tree)
            }
        }.onFailure { LOGGER.warn("Failed to parse MaLiLib screen", it) }
    }

    private fun createTree(modId: String): Tree {
        val mod = ModInfo.loadedMods.firstOrNull { it.id == modId }
        val tree = Tree.tree()
        tree.id = modId
        tree.title = mod?.name?.takeIf { it.isNotBlank() } ?: modId
        tree.noCache = true
        tree.saveFunction = Runnable {
            runCatching {
                val cm = Class.forName(CONFIG_MANAGER)
                val instance = cm.getMethod("getInstance").invoke(null)
                runCatching { cm.getMethod("onConfigsChanged", String::class.java).invoke(instance, modId) }
                cm.getMethod("saveAllConfigs").invoke(instance)
            }.onFailure { LOGGER.warn("Failed to save MaLiLib configs for {}", modId, it) }
        }
        mod?.extractIconFile()?.let { tree.addMetadata("icon_path", it) }
        CompatLoader.originalScreenOpener(modId)?.let { tree.addMetadata("open_original_screen", it) }
        return tree
    }

    private fun parseConfig(config: Any, category: String, subcategory: String?, modId: String, tree: Tree): Boolean {
        val type = (invoke(config, "getType") as? Enum<*>)?.name ?: return false
        val name = (invoke(config, "getConfigGuiDisplayName") as? String)?.nonBlankOrNull()
            ?: (invoke(config, "getName") as? String)?.nonBlankOrNull() ?: return false
        val desc = (invoke(config, "getComment") as? String)?.nonBlankOrNull()

        val spec = when (type) {
            "BOOLEAN" -> PropSpec(
                Visualizer.SwitchVisualizer::class.java,
                { invoke(config, "getBooleanValue") },
                { v -> invokeSet(config, "setBooleanValue", Boolean::class.javaPrimitiveType!!, v) },
                default = { invoke(config, "getDefaultBooleanValue") },
            )
            "INTEGER" -> PropSpec(
                Visualizer.SliderVisualizer::class.java,
                { invoke(config, "getIntegerValue") },
                { v -> invokeSet(config, "setIntegerValue", Int::class.javaPrimitiveType!!, (v as Number).toInt()) },
                default = { invoke(config, "getDefaultIntegerValue") },
                min = { (invoke(config, "getMinIntegerValue") as? Number)?.toFloat() },
                max = { (invoke(config, "getMaxIntegerValue") as? Number)?.toFloat() },
            )
            "DOUBLE" -> PropSpec(
                Visualizer.SliderVisualizer::class.java,
                { invoke(config, "getDoubleValue") },
                { v -> invokeSet(config, "setDoubleValue", Double::class.javaPrimitiveType!!, (v as Number).toDouble()) },
                default = { invoke(config, "getDefaultDoubleValue") },
                min = { (invoke(config, "getMinDoubleValue") as? Number)?.toFloat() },
                max = { (invoke(config, "getMaxDoubleValue") as? Number)?.toFloat() },
            )
            "FLOAT" -> PropSpec(
                Visualizer.SliderVisualizer::class.java,
                { invoke(config, "getFloatValue") },
                { v -> invokeSet(config, "setFloatValue", Float::class.javaPrimitiveType!!, (v as Number).toFloat()) },
                default = { invoke(config, "getDefaultFloatValue") },
                min = { (invoke(config, "getMinFloatValue") as? Number)?.toFloat() },
                max = { (invoke(config, "getMaxFloatValue") as? Number)?.toFloat() },
            )
            "STRING" -> PropSpec(
                Visualizer.TextVisualizer::class.java,
                { invoke(config, "getStringValue") },
                { v -> invokeSet(config, "setValueFromString", String::class.java, v?.toString() ?: "") },
                default = { invoke(config, "getDefaultStringValue") },
            )
            "COLOR" -> PropSpec(
                Visualizer.ColorVisualizer::class.java,
                { invoke(config, "getIntegerValue") },
                { v -> invokeSet(config, "setIntegerValue", Int::class.javaPrimitiveType!!, (v as Number).toInt()) },
                default = { invoke(config, "getDefaultIntegerValue") },
            )
            "OPTION_LIST" -> optionListSpec(config)
            else -> null // HOTKEY, STRING_LIST, COLOR_LIST, LOCKED_LIST, TABLE: unsupported
        } ?: return false

        val current = runCatching { spec.getter() }.getOrNull() ?: return false

        val property = Properties.functional(
            getter = { runCatching { spec.getter() }.getOrNull() },
            setter = { v -> runCatching { spec.setter(v) } },
            id = UUID.randomUUID().toString(),
            name = name,
            description = desc,
        )
        property.addMetadata("visualizer", spec.visualizer)
        runCatching { spec.default() }.getOrNull()?.let { property.addMetadata("default", it) }
        property.category = category
        property.subcategory = subcategory

        spec.min?.let { property.addMetadata("min", runCatching { it() }.getOrNull() ?: 0f) }
        spec.max?.let { property.addMetadata("max", runCatching { it() }.getOrNull() ?: 100f) }
        spec.options?.let { property.addMetadata("options", it) }
        spec.optionLabels?.let { property.addMetadata("optionLabels", it) }

        tree.put(property)
        return true
    }

    private fun optionListSpec(config: Any): PropSpec? {
        val current = invoke(config, "getOptionListValue") ?: return null
        val constants = current::class.java.enumConstants ?: return null
        if (constants.isEmpty()) return null
        val options = constants.map { it.toString() }
        val labels = constants.map {
            (invoke(it, "getDisplayName") as? String)?.nonBlankOrNull()
                ?: (invoke(it, "getStringValue") as? String)?.nonBlankOrNull()
                ?: it.toString()
        }
        return PropSpec(
            Visualizer.DropdownVisualizer::class.java,
            { invoke(config, "getOptionListValue") },
            { v -> if (v != null) invokeSet(config, "setOptionListValue", v::class.java, v) },
            default = { invoke(config, "getDefaultOptionListValue") },
            options = options,
            optionLabels = labels,
        )
    }

    private class PropSpec(
        val visualizer: Class<out Visualizer>,
        val getter: () -> Any?,
        val setter: (Any?) -> Unit,
        val default: () -> Any? = { null },
        val min: (() -> Float?)? = null,
        val max: (() -> Float?)? = null,
        val options: List<String>? = null,
        val optionLabels: List<String>? = null,
    )

    private fun resolveTitle(screen: Any): String? =
        runCatching {
            var cls: Class<*>? = screen.javaClass
            while (cls != null && cls != Any::class.java) {
                cls.declaredFields.firstOrNull { it.name == "title" && it.type == String::class.java }?.let {
                    it.isAccessible = true
                    return@runCatching it.get(screen) as? String
                }
                cls = cls.superclass
            }
            invoke(screen, "getTitleString") as? String
        }.getOrNull()

    private fun invoke(target: Any, name: String): Any? {
        val method = findMethod(target.javaClass, name) ?: return null
        return runCatching { method.invoke(target) }.getOrNull()
    }

    private fun invokeSet(target: Any, name: String, paramType: Class<*>, value: Any?) {
        val method = target.javaClass.methods.firstOrNull {
            it.name == name && it.parameterCount == 1 &&
                (it.parameterTypes[0] == paramType || it.parameterTypes[0].isAssignableFrom(paramType))
        }?.apply { isAccessible = true } ?: return
        method.invoke(target, value)
    }

    private fun findMethod(cls: Class<*>, name: String): Method? =
        cls.methods.firstOrNull { it.name == name && it.parameterCount == 0 }?.apply { isAccessible = true }

    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
//? }
