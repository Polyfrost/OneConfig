//? yacl_compat {
package org.polyfrost.oneconfig.internal.compat

import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import java.util.*

object YACLCompat {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/YACL-Compat")

    @JvmStatic
    fun parseYACL(yaclInstance: Any) {
        runCatching {
            val mod = CompatLoader.findFirstMod()
            if (mod != null && CompatLoader.nativeLoadedConfigs.contains(mod.id)) {
                return
            }
            val tree = parseYACLInstance(yaclInstance, mod)
            if (tree != null) {
                ConfigManager.active().register(tree)
                CompatLoader.markFirstModAsSkip()
            }
        }.onFailure {
            LOGGER.warn("Failed to parse YACL config", it)
        }
    }

    private fun parseYACLInstance(yaclInstance: Any, mod: ModInfo?): Tree? {
        val yaclClass = yaclInstance::class.java

        val categoriesMethod = yaclClass.methods.firstOrNull {
            it.name == "categories" && it.parameterCount == 0
        } ?: return null

        @Suppress("UNCHECKED_CAST")
        val categories = categoriesMethod.invoke(yaclInstance) as? Collection<*> ?: return null
        if (categories.isEmpty()) return null

        val saveMethod = yaclClass.methods.firstOrNull {
            it.name == "saveFunction" && it.parameterCount == 0
        }
        val saveRunnable = saveMethod?.let {
            runCatching { it.invoke(yaclInstance) as? Runnable }.getOrNull()
        }

        val tree = Tree.tree()
        tree.id = mod?.id ?: yaclInstance::class.java.name
        tree.title = mod?.name?.takeIf { it.isNotBlank() } ?: "YACL Config"
        tree.noCache = true
        if (saveRunnable != null) {
            tree.saveFunction = saveRunnable
        }

        for (category in categories) {
            if (category == null) continue
            parseCategory(category, tree)
        }

        return tree
    }

    private fun parseCategory(category: Any, root: Tree) {
        val categoryClass = category::class.java

        val nameMethod = categoryClass.methods.firstOrNull { it.name == "name" && it.parameterCount == 0 }
        val categoryName = nameMethod?.let { resolveComponent(it.invoke(category)) }?.nonBlankOrNull() ?: "General"

        val groupsMethod = categoryClass.methods.firstOrNull {
            it.name == "groups" && it.parameterCount == 0
        }

        @Suppress("UNCHECKED_CAST")
        val groups = groupsMethod?.invoke(category) as? Collection<*> ?: return

        for (group in groups) {
            if (group == null) continue
            parseGroup(group, categoryName, root)
        }
    }

    private fun parseGroup(group: Any, categoryName: String, root: Tree) {
        val groupClass = group::class.java

        val nameMethod = groupClass.methods.firstOrNull { it.name == "name" && it.parameterCount == 0 }
        val groupName = nameMethod?.let { resolveComponent(it.invoke(group)) }?.nonBlankOrNull() ?: categoryName

        val optionsMethod = groupClass.methods.firstOrNull {
            it.name == "options" && it.parameterCount == 0
        }

        @Suppress("UNCHECKED_CAST")
        val options = optionsMethod?.invoke(group) as? Collection<*> ?: return
        if (options.isEmpty()) return

        // Resolve description
        val descMethod = groupClass.methods.firstOrNull { it.name == "description" && it.parameterCount == 0 }
        val groupDesc = descMethod?.let {
            runCatching {
                val descResult = it.invoke(group)
                val textMethod = descResult?.javaClass?.methods?.firstOrNull { m -> m.name == "text" && m.parameterCount == 0 }
                textMethod?.let { tm -> resolveComponent(tm.invoke(descResult)) }?.nonBlankOrNull()
            }.getOrNull()
        }

        // Resolve collapsed
        val collapsedMethod = groupClass.methods.firstOrNull {
            (it.name == "isCollapsed" || it.name == "collapsed" || it.name == "isCollapsedByDefault" || it.name == "collapsedByDefault") &&
            it.parameterCount == 0 &&
            (it.returnType == Boolean::class.java || it.returnType == Boolean::class.javaPrimitiveType)
        }
        val isCollapsed = collapsedMethod?.let {
            runCatching { it.invoke(group) as? Boolean }.getOrNull()
        } ?: false

        val groupTree = Tree.tree(UUID.randomUUID().toString())
        groupTree.title = groupName
        groupTree.description = groupDesc
        groupTree.addMetadata("category", categoryName)
        groupTree.addMetadata("subcategory", ConfigVisualizer.DEFAULT_SUBCATEGORY)
        if (isCollapsed) {
            groupTree.addMetadata("collapsed", true)
        }

        for (option in options) {
            if (option == null) continue
            runCatching { parseOption(option, categoryName, groupName, groupTree) }
                .onFailure { LOGGER.warn("Failed to parse YACL option", it) }
        }

        if (groupTree.map.isNotEmpty()) {
            root.put(groupTree)
        }
    }

    private fun parseOption(option: Any, categoryName: String, subcategoryName: String, root: Tree) {
        val optionClass = option::class.java

        val nameMethod = optionClass.methods.firstOrNull { it.name == "name" && it.parameterCount == 0 }
        val descMethod = optionClass.methods.firstOrNull { it.name == "description" && it.parameterCount == 0 }
        val name = nameMethod?.let { resolveComponent(it.invoke(option)) }?.nonBlankOrNull() ?: return
        val desc = descMethod?.let {
            runCatching {
                val descResult = it.invoke(option)
                // description() returns OptionDescription which has text() -> Component
                val textMethod = descResult?.javaClass?.methods?.firstOrNull { m -> m.name == "text" && m.parameterCount == 0 }
                textMethod?.let { tm -> resolveComponent(tm.invoke(descResult)) }?.nonBlankOrNull()
            }.getOrNull()
        }

        // Older YACL exposes a Binding (getValue/setValue/defaultValue). Newer YACL uses a state
        // manager and throws UnsupportedOperationException from binding(), so fall back to the
        // Option-level pendingValue()/requestSet(T) accessors in that case.
        val bindingMethod = optionClass.methods.firstOrNull { it.name == "binding" && it.parameterCount == 0 }
        val binding = bindingMethod?.let { runCatching { it.invoke(option) }.getOrNull() }

        val getter: () -> Any?
        val setter: (Any?) -> Unit
        val defaultValue: Any?

        if (binding != null) {
            val bindingClass = binding::class.java
            val getValueMethod = bindingClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }?.apply { isAccessible = true } ?: return
            val setValueMethod = bindingClass.methods.firstOrNull { it.name == "setValue" && it.parameterCount == 1 }?.apply { isAccessible = true } ?: return
            getter = { getValueMethod.invoke(binding) }
            setter = { value -> setValueMethod.invoke(binding, value) }
            defaultValue = bindingClass.methods
                .firstOrNull { it.name == "defaultValue" && it.parameterCount == 0 }
                ?.apply { isAccessible = true }
                ?.let { runCatching { it.invoke(binding) }.getOrNull() }
        } else {
            // New state-manager API: read/write through the Option directly.
            val pendingMethod = optionClass.methods.firstOrNull { it.name == "pendingValue" && it.parameterCount == 0 }?.apply { isAccessible = true } ?: return
            val requestSetMethod = optionClass.methods.firstOrNull { it.name == "requestSet" && it.parameterCount == 1 }?.apply { isAccessible = true } ?: return
            getter = { pendingMethod.invoke(option) }
            setter = { value -> requestSetMethod.invoke(option, value) }
            defaultValue = null
        }

        // ButtonOption and similar value-less options expose a binding whose getValue() throws
        // UnsupportedOperationException (EmptyBinderImpl). Skip them instead of logging a warning.
        val currentValue = runCatching { getter() }.getOrNull() ?: return

        val visualizer: Class<out Visualizer> = when (currentValue) {
            is Boolean -> Visualizer.SwitchVisualizer::class.java
            is Int, is Float, is Double, is Long -> Visualizer.SliderVisualizer::class.java
            is String -> Visualizer.TextVisualizer::class.java
            is Enum<*> -> Visualizer.DropdownVisualizer::class.java
            is java.awt.Color -> Visualizer.ColorVisualizer::class.java
            else -> return // Skip unsupported types
        }

        val property = Properties.functional(
            getter = { getter() },
            setter = { value -> setter(value) },
            id = UUID.randomUUID().toString(),
            name = name,
            description = desc,
        )

        property.addMetadata("visualizer", visualizer)
        defaultValue?.let { property.addMetadata("default", it) }
        property.category = categoryName
        property.subcategory = subcategoryName

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

    private fun resolveComponent(value: Any?): String? {
        if (value == null) return null
        if (value is String) return value

        runCatching {
            val method = value::class.java.getMethod("getString")
            return method.invoke(value) as? String
        }

        runCatching {
            val method = value::class.java.getMethod("string")
            return method.invoke(value) as? String
        }

        return value.toString()
    }

    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

//? }
