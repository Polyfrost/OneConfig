//? midnightlib_compat {
package org.polyfrost.oneconfig.internal.compat

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
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
import java.util.function.Supplier

object MidnightLibCompat {

    private val LOGGER = LogManager.getLogger("OneConfig/MidnightLib-Compat")

    private const val MIDNIGHT_CONFIG = "eu.midnightdust.lib.config.MidnightConfig"
    private const val ENTRY_ANNOTATION = "$MIDNIGHT_CONFIG\$Entry"
    private const val COMMENT_ANNOTATION = "$MIDNIGHT_CONFIG\$Comment"

    private class Pending(
        val property: Property<*>,
        val conditions: List<Any>,
    )

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

        val subcategoryByCategory = HashMap<String, String>()
        val propertyByField = HashMap<String, Property<*>>()
        val pending = ArrayList<Pending>()

        for (entry in entries) {
            if (entry == null) continue
            runCatching {
                if ((readString(entry, "modid") ?: readString(entry, "id")) != modid) return@runCatching

                val field = (readField(entry, "field")?.get(entry) as? Field)?.apply { isAccessible = true }
                    ?: return@runCatching

                val comment = readAnnotation(field, entry, "comment", COMMENT_ANNOTATION)
                if (comment != null) {
                    val category = categoryOf(modid, readAnnotationString(comment, "category"))
                    subcategoryByCategory[category] = commentLabel(modid, field.name, comment)
                    return@runCatching
                }

                val entryAnnotation = readAnnotation(field, entry, "entry", ENTRY_ANNOTATION)
                    ?: return@runCatching

                val property = parseEntry(entry, modid, field, entryAnnotation, subcategoryByCategory)
                    ?: return@runCatching
                tree.put(property)
                propertyByField[field.name] = property
                pending += Pending(property, readConditions(entry))
            }.onFailure { LOGGER.warn("Failed to parse MidnightLib entry", it) }
        }

        wireConditions(pending, propertyByField)

        return if (pending.isNotEmpty()) tree else null
    }

    private fun parseEntry(
        entry: Any,
        modid: String,
        field: Field,
        entryAnnotation: Any,
        subcategoryByCategory: Map<String, String>,
    ): Property<*>? {
        val currentValue = runCatching { field.get(null) }.getOrNull()

        val isColor = readAnnotationBoolean(entryAnnotation, "isColor")
        val isSlider = readAnnotationBoolean(entryAnnotation, "isSlider")
        val isNumber = currentValue is Int || currentValue is Long || currentValue is Float || currentValue is Double

        val fieldName = field.name
        val name = translateOrNull("$modid.midnightconfig.$fieldName") ?: prettify(fieldName)
        val description = translateOrNull("$modid.midnightconfig.$fieldName.tooltip")
        val id = UUID.randomUUID().toString()

        val property: Property<*>
        val visualizer: Class<out Visualizer>
        val extraMetadata = HashMap<String, Any>()
        var defaultValue = readField(entry, "defaultValue")?.let { runCatching { it.get(entry) }.getOrNull() }

        when {
            isColor && currentValue is String -> {
                visualizer = Visualizer.ColorVisualizer::class.java
                extraMetadata["noAlpha"] = true
                property = Properties.functional(
                    getter = { parseColorHex(field.get(null) as? String) },
                    setter = { v: Int -> runCatching { field.set(null, formatColorHex(v)) } },
                    id = id, name = name, description = description, type = Int::class.javaObjectType,
                )
                defaultValue = (defaultValue as? String)?.let(::parseColorHex)
            }

            isColor && isNumber -> {
                visualizer = Visualizer.ColorVisualizer::class.java
                property = functionalField(field, id, name, description)
            }

            currentValue is Boolean -> {
                visualizer = Visualizer.SwitchVisualizer::class.java
                property = functionalField(field, id, name, description)
            }

            isNumber -> {
                visualizer = if (isSlider) Visualizer.SliderVisualizer::class.java else Visualizer.NumberVisualizer::class.java
                extraMetadata["min"] = readAnnotationDouble(entryAnnotation, "min")?.toFloat() ?: 0f
                extraMetadata["max"] = readAnnotationDouble(entryAnnotation, "max")?.toFloat() ?: 100f
                property = functionalField(field, id, name, description)
            }

            currentValue is Enum<*> -> {
                visualizer = Visualizer.DropdownVisualizer::class.java
                val constants = currentValue::class.java.enumConstants?.map { it.toString() } ?: emptyList()
                extraMetadata["options"] = constants
                extraMetadata["optionLabels"] = constants.map { enumLabel(modid, currentValue::class.java, it) }
                property = functionalField(field, id, name, description)
            }

            currentValue is String -> {
                visualizer = Visualizer.TextVisualizer::class.java
                property = functionalField(field, id, name, description)
            }

            currentValue is List<*> && currentValue.all { it is String } -> {
                visualizer = Visualizer.TextVisualizer::class.java
                extraMetadata["multiline"] = true
                property = Properties.functional(
                    getter = { (field.get(null) as? List<*>)?.joinToString("\n") ?: "" },
                    setter = { v: String ->
                        runCatching {
                            field.set(null, v.split('\n').map(String::trim).filter(String::isNotEmpty).toMutableList())
                        }
                    },
                    id = id, name = name, description = description, type = String::class.java,
                )
                @Suppress("UNCHECKED_CAST")
                defaultValue = (defaultValue as? List<String>)?.joinToString("\n")
            }

            else -> return null // nested objects / unsupported types
        }

        property.addMetadata("visualizer", visualizer)
        extraMetadata.forEach { (k, v) -> property.addMetadata(k, v) }
        defaultValue?.let { property.addMetadata("default", it) }

        val category = categoryOf(modid, readAnnotationString(entryAnnotation, "category"))
        property.category = category
        property.subcategory = subcategoryByCategory[category] ?: category
        return property
    }

    private fun functionalField(field: Field, id: String, name: String, description: String?): Property<*> =
        Properties.functional(
            getter = { runCatching { field.get(null) }.getOrNull() },
            setter = { v: Any? -> runCatching { field.set(null, v) } },
            id = id, name = name, description = description,
        )

    private fun wireConditions(pending: List<Pending>, propertyByField: Map<String, Property<*>>) {
        for (p in pending) {
            for (condition in p.conditions) {
                val requiredOption = readAnnotationString(condition, "requiredOption")?.takeIf { it.isNotBlank() } ?: continue
                val requiredValues = readAnnotationStringArray(condition, "requiredValue")
                val locked = readAnnotationBoolean(condition, "visibleButLocked")
                val source = propertyByField[requiredOption] ?: continue

                val evaluate = Supplier {
                    val value = runCatching { source.get() }.getOrNull()?.toString()
                    when {
                        value != null && requiredValues.contains(value) -> Property.Display.SHOWN
                        locked -> Property.Display.DISABLED
                        else -> Property.Display.HIDDEN
                    }
                }
                p.property.addDisplayCondition(evaluate)
                @Suppress("UNCHECKED_CAST")
                (source as Property<Any?>).addCallback { p.property.revaluateDisplay(); false }
            }
        }
    }

    private fun categoryOf(modid: String, raw: String?): String {
        val cat = raw?.takeIf { it.isNotBlank() && it != "default" } ?: return "General"
        return translateOrNull("$modid.midnightconfig.category.$cat") ?: prettify(cat)
    }

    private fun commentLabel(modid: String, fieldName: String, comment: Any): String {
        readAnnotationString(comment, "name")?.takeIf { it.isNotBlank() }?.let { return it }
        return translateOrNull("$modid.midnightconfig.$fieldName")
            ?: prettify(fieldName.trimStart('_'))
    }

    private fun enumLabel(modid: String, enumClass: Class<*>, constant: String): String =
        translateOrNull("$modid.midnightconfig.enum.${enumClass.simpleName}.$constant") ?: prettify(constant)

    private fun parseColorHex(hex: String?): Int {
        val h = hex?.trim()?.removePrefix("#") ?: return 0xFF000000.toInt()
        return runCatching {
            when (h.length) {
                6 -> 0xFF000000.toInt() or h.toInt(16)
                8 -> h.toLong(16).toInt()
                else -> 0xFF000000.toInt()
            }
        }.getOrDefault(0xFF000000.toInt())
    }

    private fun formatColorHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    private fun prettify(name: String): String =
        name.replace('_', ' ').trim().replaceFirstChar { it.uppercase() }

    private fun translateOrNull(key: String): String? =
        if (Platform.i18n().hasTranslation(key)) Platform.i18n().translateString(key)?.takeIf { it.isNotBlank() && it != key } else null

    private fun readString(obj: Any, name: String): String? =
        readField(obj, name)?.let { runCatching { it.get(obj) as? String }.getOrNull() }

    private fun readAnnotation(field: Field, entry: Any, entryInfoField: String, annotationName: String): Any? {
        readField(entry, entryInfoField)?.let { runCatching { it.get(entry) }.getOrNull() }?.let { return it }
        return field.annotations.firstOrNull { it.annotationClass.java.name == annotationName }
    }

    private fun readConditions(entry: Any): List<Any> {
        val raw = readField(entry, "conditions")?.let { runCatching { it.get(entry) }.getOrNull() } ?: return emptyList()
        return (raw as? Array<*>)?.filterNotNull() ?: emptyList()
    }

    private fun readField(obj: Any, name: String): Field? =
        findField(obj.javaClass, name)?.apply { isAccessible = true }

    private fun readAnnotationBoolean(annotation: Any, method: String): Boolean =
        runCatching { annotation.javaClass.getMethod(method).invoke(annotation) as? Boolean }.getOrNull() ?: false

    private fun readAnnotationString(annotation: Any, method: String): String? =
        runCatching { annotation.javaClass.getMethod(method).invoke(annotation) as? String }.getOrNull()

    private fun readAnnotationStringArray(annotation: Any, method: String): List<String> =
        runCatching { (annotation.javaClass.getMethod(method).invoke(annotation) as? Array<*>)?.filterIsInstance<String>() }
            .getOrNull() ?: emptyList()

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
