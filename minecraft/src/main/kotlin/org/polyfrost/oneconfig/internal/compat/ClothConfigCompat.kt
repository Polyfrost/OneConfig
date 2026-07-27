//? clothconfig_compat {
package org.polyfrost.oneconfig.internal.compat

import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import java.lang.reflect.Field
import java.util.*
import java.util.function.Consumer

/**
 * Compatibility layer for Cloth Config and AutoConfig.
 *
 */
object ClothConfigCompat {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/Cloth-Compat")

    @JvmStatic
    fun parseClothBuilder(builder: Any) {
        runCatching {
            val mod = CompatLoader.findFirstMod()
            if (mod != null && CompatLoader.nativeLoadedConfigs.contains(mod.id)) {
                return
            }
            val tree = parseBuilder(builder, mod) ?: return
            CompatSnapshots.register(tree)
            CompatLoader.markFirstModAsSkip()
        }.onFailure {
            LOGGER.warn("Failed to parse Cloth config", it)
        }
    }

    private fun parseBuilder(builder: Any, mod: ModInfo?): Tree? {
        val builderClass = builder::class.java

        val categoryMapField = findField(builderClass, "categoryMap")?.apply { isAccessible = true } ?: return null
        val categoryMap = categoryMapField.get(builder) as? Map<*, *> ?: return null
        if (categoryMap.isEmpty()) return null

        val savingRunnable = runCatching {
            builderClass.getMethod("getSavingRunnable").invoke(builder) as? Runnable
        }.getOrNull()
        val builderTitle = runCatching {
            resolveComponent(builderClass.getMethod("getTitle").invoke(builder))
        }.getOrNull()

        val tree = Tree.tree()
        tree.id = mod?.id ?: builderClass.name
        tree.title = mod?.name?.takeIf { it.isNotBlank() }
            ?: builderTitle?.takeIf { it.isNotBlank() }
            ?: "Cloth Config"
        tree.noCache = true
        if (savingRunnable != null) {
            tree.saveFunction = savingRunnable
        }
        // Cloth Config exposes no icon, so fall back to the mod's icon (same source Mod Menu uses).
        mod?.extractIconFile()?.let {
            tree.addMetadata("icon_path", it)
        }

        var added = false
        for (category in categoryMap.values) {
            if (category == null) continue
            val rawCategoryName = runCatching {
                resolveComponent(category.javaClass.getMethod("getCategoryKey").invoke(category))
            }.getOrNull()?.takeIf { it.isNotBlank() }
            val categoryName = cleanName(rawCategoryName, "General")

            @Suppress("UNCHECKED_CAST")
            val entries = runCatching {
                category.javaClass.getMethod("getEntries").invoke(category) as? Collection<*>
            }.getOrNull() ?: continue

            for (entry in entries) {
                if (entry == null) continue
                runCatching {
                    if (parseEntry(entry, categoryName, categoryName, tree, tree)) added = true
                }.onFailure { LOGGER.warn("Failed to parse Cloth entry", it) }
            }
        }

        return if (added) tree else null
    }

    private fun parseEntry(entry: Any, categoryName: String, subcategoryName: String, dest: Tree, root: Tree): Boolean {
        val name = runCatching {
            resolveComponent(entry.javaClass.getMethod("getFieldName").invoke(entry))
        }.getOrNull() ?: return false

        val value = runCatching { invokeGetValue(entry) }.getOrNull()

        if (isSubCategory(entry, value)) {
            val children = value as? Collection<*> ?: return false
            val rawSubName = runCatching {
                resolveComponent(entry.javaClass.getMethod("getCategoryName").invoke(entry))
            }.getOrNull()?.takeIf { it.isNotBlank() }
            val subName = cleanName(rawSubName, name)

            val accordion = if (dest === root) {
                Tree.tree(UUID.randomUUID().toString()).also {
                    it.title = subName
                    it.addMetadata("category", categoryName)
                    if (!isExpanded(entry)) it.addMetadata("collapsed", true)
                }
            } else {
                dest
            }

            var added = false
            for (child in children) {
                if (child == null) continue
                runCatching {
                    if (parseEntry(child, categoryName, subName, accordion, root)) added = true
                }.onFailure { LOGGER.warn("Failed to parse Cloth sub-entry", it) }
            }

            if (added && accordion !== dest) root.put(accordion)
            return added
        }

        val currentValue = value ?: return false

        if (currentValue is List<*>) {
            return parseListEntry(entry, currentValue, name, categoryName, subcategoryName, dest)
        }

        val isColor = isColorEntry(entry)

        val isNumber = currentValue is Int || currentValue is Float || currentValue is Double || currentValue is Long
        val isSlider = isNumber && isSliderEntry(entry)

        val visualizer: Class<out Visualizer> = when {
            isColor -> Visualizer.ColorVisualizer::class.java
            currentValue is Boolean -> Visualizer.SwitchVisualizer::class.java
            isSlider -> Visualizer.SliderVisualizer::class.java
            isNumber -> Visualizer.NumberVisualizer::class.java
            currentValue is String -> Visualizer.TextVisualizer::class.java
            currentValue is Enum<*> -> Visualizer.DropdownVisualizer::class.java
            currentValue is java.awt.Color -> Visualizer.ColorVisualizer::class.java
            else -> return false // Skip unsupported types (lists, nested objects, etc.)
        }

        val saveCallbackField = findField(entry.javaClass, "saveCallback")?.apply { isAccessible = true }

        val property = Properties.functional(
            getter = { runCatching { invokeGetValue(entry) }.getOrNull() },
            setter = { v ->
                @Suppress("UNCHECKED_CAST")
                (saveCallbackField?.get(entry) as? Consumer<Any?>)?.accept(v)
            },
            id = UUID.randomUUID().toString(),
            name = name,
            description = resolveTooltip(entry),
        )

        property.addMetadata("visualizer", visualizer)
        runCatching {
            (entry.javaClass.getMethod("getDefaultValue").invoke(entry) as? Optional<*>)?.orElse(null)
        }.getOrNull()?.let { property.addMetadata("default", it) }
        property.category = categoryName
        property.subcategory = subcategoryName

        when {
            isColor -> {} // ColorVisualizer needs no slider/dropdown metadata.
            currentValue is Enum<*> -> {
                val constants = currentValue::class.java.enumConstants
                property.addMetadata("options", constants?.map { it.toString() } ?: emptyList<String>())
            }
            isSlider -> {
                property.addMetadata("min", readNumberField(entry, "minimum") ?: 0f)
                property.addMetadata("max", readNumberField(entry, "maximum") ?: 100f)
            }
            isNumber -> {
                readNumberField(entry, "minimum")?.let { property.addMetadata("min", it) }
                readNumberField(entry, "maximum")?.let { property.addMetadata("max", it) }
            }
        }

        dest.put(property)
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseListEntry(
        entry: Any,
        currentValue: List<*>,
        name: String,
        categoryName: String,
        subcategoryName: String,
        dest: Tree,
    ): Boolean {
        val element = listElementType(entry, currentValue) ?: return false
        val numeric = Number::class.java.isAssignableFrom(element)
        if (!numeric && element != String::class.java) return false

        val saveCallbackField = findField(entry.javaClass, "saveCallback")?.apply { isAccessible = true }

        val property = Properties.functional(
            getter = {
                val list = runCatching { invokeGetValue(entry) as? List<*> }.getOrNull() ?: emptyList<Any?>()
                ArrayList(list.map { if (numeric) it as? Number ?: 0 else it?.toString() ?: "" })
            },
            setter = { v: List<Any?> ->
                val values = v.mapTo(ArrayList()) { if (numeric) coerceNumber(it, element) else it?.toString() ?: "" }
                (saveCallbackField?.get(entry) as? Consumer<Any?>)?.accept(values)
            },
            id = UUID.randomUUID().toString(),
            name = name,
            description = resolveTooltip(entry),
            type = java.util.List::class.java as Class<List<Any?>>,
        )

        property.addMetadata(
            "visualizer",
            if (numeric) Visualizer.NumberListVisualizer::class.java else Visualizer.TextListVisualizer::class.java,
        )
        if (numeric) {
            property.addMetadata("min", readNumberField(entry, "minimum") ?: 0f)
            property.addMetadata("max", readNumberField(entry, "maximum") ?: 100f)
        }
        runCatching {
            (entry.javaClass.getMethod("getDefaultValue").invoke(entry) as? Optional<*>)?.orElse(null)
        }.getOrNull()?.let { property.addMetadata("default", it) }
        property.category = categoryName
        property.subcategory = subcategoryName

        dest.put(property)
        return true
    }

    /**
     * The type of a single list entry. Taken from the values already present, falling back to the
     * Cloth entry class itself so that empty lists still render with the right widget.
     */
    private fun listElementType(entry: Any, currentValue: List<*>): Class<*>? {
        currentValue.firstOrNull { it != null }?.let { return it.javaClass }
        return when {
            entryClassMatches(entry, "StringListListEntry") -> String::class.java
            entryClassMatches(entry, "IntegerListListEntry") -> Integer::class.java
            entryClassMatches(entry, "LongListListEntry") -> java.lang.Long::class.java
            entryClassMatches(entry, "FloatListListEntry") -> java.lang.Float::class.java
            entryClassMatches(entry, "DoubleListListEntry") -> java.lang.Double::class.java
            else -> null
        }
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

    private fun isExpanded(entry: Any): Boolean {
        val method = entry.javaClass.methods.firstOrNull {
            it.name == "isExpanded" && it.parameterCount == 0 &&
                (it.returnType == Boolean::class.java || it.returnType == Boolean::class.javaPrimitiveType)
        } ?: return false
        return runCatching { method.invoke(entry) as? Boolean }.getOrNull() ?: false
    }

    private fun cleanName(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        if (!looksLikeTranslationKey(raw)) return raw
        val segment = raw.substringAfterLast('.')
        return when {
            segment.isBlank() || segment.equals("default", ignoreCase = true) -> fallback
            else -> segment.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }

    private fun looksLikeTranslationKey(s: String): Boolean =
        !s.any { it.isWhitespace() } && s.count { it == '.' } >= 2

    private fun resolveTooltip(entry: Any): String? {
        val method = entry.javaClass.methods.firstOrNull {
            it.name == "getTooltip" && it.parameterCount == 0
        }?.apply { isAccessible = true } ?: return null
        val optional = runCatching { method.invoke(entry) as? Optional<*> }.getOrNull() ?: return null
        val components = optional.orElse(null) as? Array<*> ?: return null
        val lines = components.mapNotNull { resolveComponent(it)?.takeIf { line -> line.isNotBlank() } }
        return lines.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun invokeGetValue(entry: Any): Any? {
        val method = entry.javaClass.methods.firstOrNull {
            it.name == "getValue" && it.parameterCount == 0
        }?.apply { isAccessible = true } ?: return null
        return method.invoke(entry)
    }

    private fun isSubCategory(entry: Any, value: Any?): Boolean {
        if (entry.javaClass.simpleName == "SubCategoryListEntry") return true
        // Defensive: a value that is purely a collection of config entries (each has getFieldName).
        return value is Collection<*> && value.isNotEmpty() && value.all { it != null && hasGetFieldName(it) }
    }

    private fun hasGetFieldName(o: Any): Boolean =
        o.javaClass.methods.any { it.name == "getFieldName" && it.parameterCount == 0 }

    private fun isColorEntry(entry: Any): Boolean = entryClassMatches(entry, "ColorEntry")

    private fun isSliderEntry(entry: Any): Boolean = entryClassMatches(entry, "Slider")

    private fun entryClassMatches(entry: Any, needle: String): Boolean {
        var cls: Class<*>? = entry.javaClass
        while (cls != null && cls != Any::class.java) {
            if (cls.simpleName.contains(needle)) return true
            cls = cls.superclass
        }
        return false
    }

    private fun readNumberField(entry: Any, name: String): Float? =
        findField(entry.javaClass, name)?.let {
            it.isAccessible = true
            (it.get(entry) as? Number)?.toFloat()
        }

    private fun findField(cls: Class<*>, name: String): Field? {
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            current.declaredFields.firstOrNull { it.name == name }?.let { return it }
            current = current.superclass
        }
        return null
    }

    private fun resolveComponent(value: Any?): String? {
        if (value == null) return null
        if (value is String) return value

        runCatching {
            return value::class.java.getMethod("getString").invoke(value) as? String
        }
        runCatching {
            return value::class.java.getMethod("string").invoke(value) as? String
        }

        return value.toString()
    }
}

//? }
