//? midnightlib_compat {
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
import org.polyfrost.oneconfig.api.platform.v1.Platform
import java.lang.reflect.Field
import java.util.UUID

object MidnightLibCompat {

    private val LOGGER = LogManager.getLogger("OneConfig/MidnightLib-Compat")

    private const val MIDNIGHT_CONFIG = "eu.midnightdust.lib.config.MidnightConfig"
    private const val ENTRY_ANNOTATION = "$MIDNIGHT_CONFIG\$Entry"

    @JvmStatic
    fun onInit(modid: String, config: Class<*>) {
        if (CompatLoader.nativeLoadedConfigs.contains(modid)) return
        CompatLoader.requireTranslations {
            runCatching {
                parseConfig(modid, config)?.let(CompatSnapshots::register)
            }.onFailure {
                LOGGER.warn("Failed to parse MidnightLib config for {}", modid, it)
            }
        }
    }

    private fun parseConfig(modid: String, config: Class<*>): Tree? {
        val entriesField = findField(config, "entries")?.apply { isAccessible = true } ?: return null
        val entries = entriesField.get(null)?.let {
            (it as? Map<*, *>)?.values ?: it as? Collection<*>
        } ?: return null

        val mod = ModInfo.loadedMods.firstOrNull { it.id == modid }

        val tree = Tree.tree()
        tree.id = modid
        tree.title = mod?.name?.takeIf { it.isNotBlank() }
            ?: translateOrNull("$modid.midnightconfig.title")
            ?: modid
        tree.noCache = true
        val write = findWriteMethod(config)
        if (write != null) tree.saveFunction = Runnable { runCatching { write.invoke(null, modid) } }
        mod?.extractIconFile()?.let { tree.addMetadata("icon_path", it) }
        CompatLoader.originalScreenOpener(modid)?.let { tree.addMetadata("open_original_screen", it) }

        var added = false
        for (entry in entries) {
            if (entry == null) continue
            runCatching {
                if (parseEntry(entry, modid, tree)) added = true
            }.onFailure { LOGGER.warn("Failed to parse MidnightLib entry", it) }
        }

        return if (added) tree else null
    }

    private fun parseEntry(entry: Any, modid: String, tree: Tree): Boolean {
        if ((readString(entry, "modid") ?: readString(entry, "id")) != modid) return false
        val field = (readField(entry, "field")?.get(entry) as? Field)?.apply { isAccessible = true } ?: return false

        val entryAnnotation = field.annotations.firstOrNull { it.annotationClass.java.name == ENTRY_ANNOTATION }
            ?: return false

        val currentValue = runCatching { field.get(null) }.getOrNull() ?: return false

        val isColor = readAnnotationBoolean(entryAnnotation, "isColor")
        val isSlider = readAnnotationBoolean(entryAnnotation, "isSlider")
        val isNumber = currentValue is Int || currentValue is Long || currentValue is Float || currentValue is Double

        val visualizer: Class<out Visualizer> = when {
            isColor && (isNumber || currentValue is String) -> Visualizer.ColorVisualizer::class.java
            currentValue is Boolean -> Visualizer.SwitchVisualizer::class.java
            isNumber && isSlider -> Visualizer.SliderVisualizer::class.java
            isNumber -> Visualizer.NumberVisualizer::class.java
            currentValue is String -> Visualizer.TextVisualizer::class.java
            currentValue is Enum<*> -> Visualizer.DropdownVisualizer::class.java
            else -> return false // lists, nested objects, unsupported types
        }

        val fieldName = field.name
        val name = translateOrNull("$modid.midnightconfig.$fieldName") ?: prettify(fieldName)
        val description = translateOrNull("$modid.midnightconfig.$fieldName.tooltip")

        val property = Properties.functional(
            getter = { runCatching { field.get(null) }.getOrNull() },
            setter = { v -> runCatching { field.set(null, v) } },
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
        )

        property.addMetadata("visualizer", visualizer)
        readField(entry, "defaultValue")?.let { runCatching { it.get(entry) }.getOrNull() }
            ?.let { property.addMetadata("default", it) }

        val rawCategory = readAnnotationString(entryAnnotation, "category")?.takeIf { it.isNotBlank() && it != "default" }
        val category = rawCategory?.let { translateOrNull("$modid.midnightconfig.category.$it") ?: prettify(it) } ?: "General"
        property.category = category
        property.subcategory = category

        when {
            isColor -> {}
            currentValue is Enum<*> ->
                property.addMetadata("options", currentValue::class.java.enumConstants?.map { it.toString() } ?: emptyList<String>())
            isNumber -> {
                property.addMetadata("min", readAnnotationDouble(entryAnnotation, "min")?.toFloat() ?: 0f)
                property.addMetadata("max", readAnnotationDouble(entryAnnotation, "max")?.toFloat() ?: 100f)
            }
        }

        tree.put(property)
        return true
    }

    private fun prettify(name: String): String =
        name.replace('_', ' ').replaceFirstChar { it.uppercase() }

    private fun translateOrNull(key: String): String? =
        if (Platform.i18n().hasTranslation(key)) Platform.i18n().translateString(key)?.takeIf { it.isNotBlank() && it != key } else null

    private fun readString(obj: Any, name: String): String? =
        readField(obj, name)?.let { runCatching { it.get(obj) as? String }.getOrNull() }

    private fun readField(obj: Any, name: String): Field? =
        findField(obj.javaClass, name)?.apply { isAccessible = true }

    private fun readAnnotationBoolean(annotation: Any, method: String): Boolean =
        runCatching { annotation.javaClass.getMethod(method).invoke(annotation) as? Boolean }.getOrNull() ?: false

    private fun readAnnotationString(annotation: Any, method: String): String? =
        runCatching { annotation.javaClass.getMethod(method).invoke(annotation) as? String }.getOrNull()

    private fun readAnnotationDouble(annotation: Any, method: String): Double? =
        runCatching { (annotation.javaClass.getMethod(method).invoke(annotation) as? Number)?.toDouble() }.getOrNull()

    private fun findWriteMethod(config: Class<*>) =
        runCatching {
            var cls: Class<*>? = config
            while (cls != null && cls != Any::class.java) {
                cls.declaredMethods.firstOrNull { it.name == "write" && it.parameterCount == 1 }?.let {
                    it.isAccessible = true
                    return@runCatching it
                }
                cls = cls.superclass
            }
            null
        }.getOrNull()

    private fun findField(cls: Class<*>, name: String): Field? {
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            current.declaredFields.firstOrNull { it.name == name }?.let { return it }
            current = current.superclass
        }
        return null
    }
}
//? }
