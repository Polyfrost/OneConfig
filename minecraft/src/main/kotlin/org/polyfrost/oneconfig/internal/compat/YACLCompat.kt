//? yacl_compat {
package org.polyfrost.oneconfig.internal.compat

import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
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
            LOGGER.info("Creating config wrapper for " + mod?.id)
            val tree = parseYACLInstance(yaclInstance, mod)
            if (tree != null) {
                CompatSnapshots.register(tree)
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
        // YACL exposes no icon, so fall back to the mod's icon (same source Mod Menu uses).
        mod?.extractIconFile()?.let {
            tree.addMetadata("icon_path", it)
        }
        mod?.id?.let { id ->
            CompatLoader.originalScreenOpener(id)?.let { tree.addMetadata("open_original_screen", it) }
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
        val groupName = nameMethod?.let { resolveComponent(it.invoke(group)) }?.nonBlankOrNull()
        val isRoot = groupClass.methods.firstOrNull {
            it.name == "isRoot" &&
                it.parameterCount == 0 &&
                (it.returnType == Boolean::class.java || it.returnType == Boolean::class.javaPrimitiveType)
        }?.let { runCatching { it.invoke(group) as? Boolean }.getOrNull() } ?: false
        val subcategoryName = if (isRoot) null else groupName

        if (isListOption(groupClass)) {
            runCatching { parseOption(group, categoryName, null, root) }
                .onFailure { LOGGER.warn("Failed to parse YACL list option", it) }
            return
        }

        val optionsMethod = groupClass.methods.firstOrNull {
            it.name == "options" && it.parameterCount == 0
        }

        @Suppress("UNCHECKED_CAST")
        val options = optionsMethod?.invoke(group) as? Collection<*> ?: return
        if (options.isEmpty()) return

        for (option in options) {
            if (option == null) continue
            runCatching { parseOption(option, categoryName, subcategoryName, root) }
                .onFailure { LOGGER.warn("Failed to parse YACL option", it) }
        }
    }

    private fun parseOption(option: Any, categoryName: String, subcategoryName: String?, root: Tree) {
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

        // ButtonOption exposes no readable value (its binding throws), so handle it before the
        // binding logic. Detect it by its interface name and render it as a clickable button.
        if (isButtonOption(optionClass)) {
            parseButtonOption(option, name, desc, categoryName, subcategoryName, root)
            return
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

        if (currentValue is List<*>) {
            parseListOption(option, currentValue, name, desc, getter, setter, defaultValue, categoryName, subcategoryName, root)
            return
        }

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
                resolveEnumLabels(option, constants)?.let { property.addMetadata("optionLabels", it) }
            }
            is Int, is Float, is Double, is Long -> {
                val range = resolveSliderRange(option)
                property.addMetadata("min", range?.first ?: 0f)
                property.addMetadata("max", range?.second ?: 100f)
                range?.third?.let { property.addMetadata("step", it) }
            }
        }

        root.put(property)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseListOption(
        option: Any,
        currentValue: List<*>,
        name: String,
        desc: String?,
        getter: () -> Any?,
        setter: (Any?) -> Unit,
        defaultValue: Any?,
        categoryName: String,
        subcategoryName: String?,
        root: Tree,
    ) {
        val element = currentValue.firstOrNull { it != null }?.javaClass ?: String::class.java
        val numeric = Number::class.java.isAssignableFrom(element)
        val color = element == java.awt.Color::class.java

        val visualizer: Class<out Visualizer> = when {
            color -> Visualizer.ColorListVisualizer::class.java
            numeric -> Visualizer.NumberListVisualizer::class.java
            element == String::class.java -> Visualizer.TextListVisualizer::class.java
            else -> return // enum/record entries have no OneConfig list equivalent
        }

        fun read(value: Any?): Any? = when {
            color -> (value as? java.awt.Color)?.rgb ?: -1
            numeric -> value as? Number ?: 0
            else -> value?.toString() ?: ""
        }

        fun write(value: Any?): Any? = when {
            color -> java.awt.Color((value as? Number)?.toInt() ?: -1, true)
            numeric -> coerceNumber(value, element)
            else -> value?.toString() ?: ""
        }

        val property = Properties.functional(
            getter = {
                val list = runCatching { getter() as? List<*> }.getOrNull() ?: emptyList<Any?>()
                ArrayList(list.map(::read))
            },
            setter = { value: List<Any?> -> setter(value.mapTo(ArrayList(), ::write)) },
            id = UUID.randomUUID().toString(),
            name = name,
            description = desc,
            type = List::class.java,
        )

        property.addMetadata("visualizer", visualizer)
        (defaultValue as? List<*>)?.let { property.addMetadata("default", ArrayList(it.map(::read))) }
        maximumNumberOfEntries(option)?.let { property.addMetadata("maxEntries", it) }
        if (numeric) {
            val range = resolveEntryRange(option)
            property.addMetadata("min", range?.first ?: -Float.MAX_VALUE)
            property.addMetadata("max", range?.second ?: Float.MAX_VALUE)
            range?.third?.let { property.addMetadata("step", it) }
        }
        property.category = categoryName
        property.subcategory = subcategoryName

        root.put(property)
    }

    private fun isListOption(optionClass: Class<*>): Boolean {
        var cls: Class<*>? = optionClass
        while (cls != null && cls != Any::class.java) {
            if (cls.simpleName == "ListOption") return true
            if (cls.interfaces.any { isListOption(it) }) return true
            cls = cls.superclass
        }
        return false
    }

    private fun maximumNumberOfEntries(option: Any): Int? {
        val method = option::class.java.methods.firstOrNull {
            it.name == "maximumNumberOfEntries" && it.parameterCount == 0
        } ?: return null
        val max = runCatching { (method.invoke(option) as? Number)?.toInt() }.getOrNull() ?: return null
        return if (max <= 0 || max == Int.MAX_VALUE) 0 else max
    }

    private fun resolveEntryRange(option: Any): Triple<Float, Float, Float?>? {
        val optionsMethod = option::class.java.methods.firstOrNull {
            it.name == "options" && it.parameterCount == 0
        } ?: return null
        val entries = runCatching { optionsMethod.invoke(option) as? Collection<*> }.getOrNull() ?: return null
        val entry = entries.firstOrNull { it != null } ?: return null
        return resolveSliderRange(entry)
    }

    private fun coerceNumber(value: Any?, type: Class<*>): Any {
        val number = value as? Number ?: 0
        return when (type) {
            Integer::class.java -> number.toInt()
            java.lang.Long::class.java -> number.toLong()
            java.lang.Float::class.java -> number.toFloat()
            java.lang.Double::class.java -> number.toDouble()
            else -> number
        }
    }

    private fun isButtonOption(optionClass: Class<*>): Boolean {
        var cls: Class<*>? = optionClass
        while (cls != null && cls != Any::class.java) {
            if (cls.simpleName == "ButtonOption") return true
            if (cls.interfaces.any { isButtonOption(it) }) return true
            cls = cls.superclass
        }
        return false
    }

    private fun parseButtonOption(
        option: Any,
        name: String,
        desc: String?,
        categoryName: String,
        subcategoryName: String?,
        root: Tree,
    ) {
        val optionClass = option::class.java

        val label = optionClass.methods.firstOrNull { it.name == "text" && it.parameterCount == 0 }
            ?.let { runCatching { resolveComponent(it.invoke(option)) }.getOrNull() }
            ?.nonBlankOrNull() ?: name

        val actionMethod = optionClass.methods.firstOrNull { it.name == "action" && it.parameterCount == 0 }
        val action = actionMethod?.let { runCatching { it.invoke(option) }.getOrNull() }

        val property = Properties.dummy(
            id = UUID.randomUUID().toString(),
            name = name,
            description = desc,
        )
        property.addMetadata("visualizer", Visualizer.ButtonVisualizer::class.java)
        property.addMetadata("textKey", label)
        property.category = categoryName
        property.subcategory = subcategoryName

        if (action != null) {
            val acceptMethod = action::class.java.methods.firstOrNull {
                it.name == "accept" && (it.parameterCount == 1 || it.parameterCount == 2)
            }?.apply { isAccessible = true }
            if (acceptMethod != null) {
                property.metadata?.put("runnable", Runnable {
                    runCatching {
                        val screen = Platform.screen().current<Any>()
                        if (acceptMethod.parameterCount == 2) {
                            acceptMethod.invoke(action, screen, option)
                        } else {
                            acceptMethod.invoke(action, screen)
                        }
                    }.onFailure { LOGGER.warn("Failed to run YACL button action", it) }
                })
            }
        }

        root.put(property)
    }

    private fun resolveController(option: Any): Any? {
        val controllerMethod = option::class.java.methods.firstOrNull {
            it.name == "controller" && it.parameterCount == 0
        } ?: return null
        // controller() returns a supplier-bound Controller; some YACL versions wrap it in a Supplier.
        var controller = runCatching { controllerMethod.invoke(option) }.getOrNull() ?: return null
        (controller as? java.util.function.Supplier<*>)?.let { controller = it.get() ?: return null }
        return controller
    }

    private fun resolveEnumLabels(option: Any, constants: Array<out Any>?): List<String>? {
        if (constants.isNullOrEmpty()) return null
        val controller = resolveController(option) ?: return null
        val (formatter, formatMethod) = resolveValueFormatter(controller) ?: return null
        return constants.map {
            runCatching { resolveComponent(formatMethod.invoke(formatter, it)) }
                .getOrNull()?.nonBlankOrNull() ?: it.toString()
        }
    }

    private fun resolveValueFormatter(controller: Any): Pair<Any, java.lang.reflect.Method>? {
        var cls: Class<*>? = controller::class.java
        while (cls != null && cls != Any::class.java) {
            val field = cls.declaredFields.firstOrNull { it.type.name.endsWith("ValueFormatter") }
            if (field != null) {
                val formatter = runCatching { field.apply { isAccessible = true }.get(controller) }.getOrNull()
                val format = formatter?.let {
                    field.type.methods.firstOrNull { m -> m.name == "format" && m.parameterCount == 1 }
                        ?.apply { isAccessible = true }
                }
                if (formatter != null && format != null) return formatter to format
            }
            cls = cls.superclass
        }
        return null
    }

    private fun resolveSliderRange(option: Any): Triple<Float, Float, Float?>? {
        val controller = resolveController(option) ?: return null
        val c = controller
        val min = invokeNumber(c, "min") ?: return null
        val max = invokeNumber(c, "max") ?: return null
        val step = invokeNumber(c, "interval") ?: invokeNumber(c, "step")
        return Triple(min, max, step)
    }

    private fun invokeNumber(target: Any, name: String): Float? {
        val method = target::class.java.methods.firstOrNull {
            it.name == name && it.parameterCount == 0
        } ?: return null
        return runCatching { (method.invoke(target) as? Number)?.toFloat() }.getOrNull()
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
