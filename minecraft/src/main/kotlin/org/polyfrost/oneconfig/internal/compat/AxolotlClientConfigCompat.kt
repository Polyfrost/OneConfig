//? axolotlclient_config_compat {
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
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId
import java.lang.reflect.Method
import java.util.Collections
import java.util.IdentityHashMap

internal object AxolotlClientConfigCompat {

    private val LOGGER = LogManager.getLogger("OneConfig/AxolotlClientConfig-Compat")

    private const val ROOT_CATEGORY = "General"

    private val seen: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
    private val usedTreeIds = HashSet<String>()
    private val treesByOwner = HashMap<String, Int>()

    @JvmStatic
    fun addManager(manager: Any) {
        runCatching {
            if (!seen.add(manager)) return
            val mod = CompatLoader.findFirstMod()
            if (mod != null && CompatLoader.nativeLoadedConfigs.contains(mod.id)) return
            CompatLoader.requireTranslations(skip = true) {
                runCatching {
                    val tree = parseManager(manager, mod) ?: return@requireTranslations
                    CompatSnapshots.register(tree)
                    mod?.id?.let { CompatLoader.nativeLoadedConfigs.add(it) }
                }.onFailure { LOGGER.warn("Failed to parse AxolotlClient config {}", manager, it) }
            }
        }.onFailure { LOGGER.warn("Failed to prepare an AxolotlClient config wrapper", it) }
    }

    private fun parseManager(manager: Any, mod: ModInfo?): Tree? {
        val root = invoke(manager, "getRoot") ?: return null
        val rootName = invoke(root, "getName") as? String ?: return null
        val suppressed = (invoke(manager, "getSuppressedNames") as? Collection<*>)
            ?.mapNotNull { it as? String }
            ?.toSet()
            .orEmpty()

        val entries = ArrayList<Entry>()
        collect(root, suppressed, null, null, emptyList(), emptyList(), entries, identitySet())
        if (entries.isEmpty()) return null

        val tree = Tree.tree()
        tree.id = uniqueId(usedTreeIds, idPart(rootName, "axolotlclient_config"))
        tree.title = titleOf(mod, rootName)
        tree.noCache = true
        tree.saveFunction = Runnable {
            runCatching { invoke(manager, "save") }
                .onFailure { LOGGER.warn("Failed to save AxolotlClient config {}", rootName, it) }
        }
        tree.addMetadata(CompatSnapshots.CUSTOM_RESET_METADATA, Runnable {
            runCatching {
                entries.forEach { entry -> runCatching { invoke(entry.option, "setDefault") } }
                invoke(manager, "save")
            }.onFailure { LOGGER.warn("Failed to reset AxolotlClient config {}", rootName, it) }
        })
        mod?.extractIconFile()?.let { tree.addMetadata("icon_path", it) }
        mod?.id?.let { id ->
            CompatLoader.originalScreenOpener(id)?.let { tree.addMetadata("open_original_screen", it) }
        }

        val usedIds = HashSet<String>()
        var added = 0
        for (entry in entries) {
            runCatching {
                val id = uniqueId(usedIds, entry.path)
                val property = property(entry, id) ?: return@runCatching
                tree.put(property)
                added++
            }.onFailure { LOGGER.warn("Failed to parse AxolotlClient option {}", entry.name, it) }
        }

        return if (added > 0) tree else null
    }

    private fun collect(
        category: Any,
        suppressed: Set<String>,
        categoryName: String?,
        categoryKey: String?,
        subNames: List<String>,
        subKeys: List<String>,
        out: MutableList<Entry>,
        visited: MutableSet<Any>,
    ) {
        if (!visited.add(category)) return

        val subcategory = subNames.joinToString(" / ").ifEmpty { categoryName ?: ROOT_CATEGORY }
        val pathParts = listOfNotNull(categoryKey?.let { idPart(it, "category") }) +
            subKeys.map { idPart(it, "subcategory") }
        val path = pathParts.ifEmpty { listOf("general") }.joinToString("/")

        for (option in (invoke(category, "getOptions") as? Collection<*>).orEmpty()) {
            if (option == null) continue
            val name = invoke(option, "getName") as? String ?: continue
            if (name in suppressed) continue
            out.add(
                Entry(
                    option = option,
                    name = name,
                    category = categoryName ?: ROOT_CATEGORY,
                    categoryKey = categoryKey,
                    subcategory = subcategory,
                    subcategoryKey = subKeys.singleOrNull() ?: categoryKey.takeIf { subKeys.isEmpty() },
                    path = "$path/${idPart(name, "option")}",
                )
            )
        }

        for (sub in (invoke(category, "getSubCategories") as? Collection<*>).orEmpty()) {
            if (sub == null) continue
            val subName = invoke(sub, "getName") as? String ?: continue
            if (subName in suppressed) continue
            if (categoryName == null) {
                collect(sub, suppressed, label(subName) ?: prettify(subName), subName, emptyList(), emptyList(), out, visited)
            } else {
                collect(
                    sub, suppressed, categoryName, categoryKey,
                    subNames + (label(subName) ?: prettify(subName)), subKeys + subName, out, visited,
                )
            }
        }
    }

    private fun titleOf(mod: ModInfo?, rootName: String): String {
        val rootLabel = label(rootName)
        val modName = mod?.name?.takeIf { it.isNotBlank() }
        val count = treesByOwner.merge(mod?.id ?: rootName, 1) { a, b -> a + b } ?: 1
        return when {
            modName == null -> rootLabel ?: prettify(rootName)
            count > 1 && rootLabel != null -> "$modName - $rootLabel"
            else -> modName
        }
    }

    private fun property(entry: Entry, id: String): Property<*>? {
        val option = entry.option
        val name = label(entry.name) ?: prettify(entry.name)
        val tooltipKey = (invoke(option, "getTooltip") as? String)?.takeIf { it.isNotBlank() }
        val description = tooltipKey?.let { translateOrNull(it) }

        val property = when (invoke(option, "getWidgetIdentifier") as? String) {
            "boolean" -> booleanProperty(option, id, name, description)
            "integer" -> numberProperty(option, id, name, description, Integer::class.java)
            "float" -> numberProperty(option, id, name, description, java.lang.Float::class.java)
            "double" -> numberProperty(option, id, name, description, java.lang.Double::class.java)
            "string" -> stringProperty(option, id, name, description)
            "string[]" -> choiceProperty(option, id, name, description, stringArrayValues(option))
            "enum" -> choiceProperty(option, id, name, description, enumValues(option))
            "color" -> colorProperty(option, id, name, description)
            else -> null
        } ?: return null

        property.addMetadata("titleKey", entry.name)
        if (description != null) tooltipKey?.let { property.addMetadata("descriptionKey", it) }
        property.category = entry.category
        entry.categoryKey?.let { property.addMetadata("categoryKey", it) }
        property.subcategory = entry.subcategory
        entry.subcategoryKey?.let { property.addMetadata("subcategoryKey", it) }
        return property
    }

    private fun booleanProperty(option: Any, id: String, name: String, description: String?): Property<*> {
        val property = Properties.functional(
            getter = { read(option) as? Boolean ?: false },
            setter = { value: Boolean -> write(option, value) },
            id = id,
            name = name,
            description = description,
            type = Boolean::class.javaObjectType,
        )
        property.visualizer = Visualizer.SwitchVisualizer::class.java
        (invoke(option, "getDefault") as? Boolean)?.let { property.addMetadata("default", it) }
        return property
    }

    private fun numberProperty(
        option: Any,
        id: String,
        name: String,
        description: String?,
        type: Class<*>,
    ): Property<*> {
        val min = (invoke(option, "getMin") as? Number)?.toFloat()
        val max = (invoke(option, "getMax") as? Number)?.toFloat()

        @Suppress("UNCHECKED_CAST")
        val property = Properties.functional<Any?>(
            getter = { coerceNumber(read(option) as? Number, type) },
            setter = { value -> write(option, coerceNumber(value as? Number, type)) },
            id = id,
            name = name,
            description = description,
            type = type as Class<Any?>,
        )
        if (min != null && max != null) {
            property.visualizer = Visualizer.SliderVisualizer::class.java
            property.addMetadata("min", min)
            property.addMetadata("max", max)
            if (type == Integer::class.java) property.addMetadata("step", 1f)
        } else {
            property.visualizer = Visualizer.NumberVisualizer::class.java
        }
        (invoke(option, "getDefault") as? Number)?.let { property.addMetadata("default", coerceNumber(it, type)) }
        return property
    }

    private fun stringProperty(option: Any, id: String, name: String, description: String?): Property<*> {
        val property = Properties.functional(
            getter = { read(option) as? String ?: "" },
            setter = { value: String -> write(option, value) },
            id = id,
            name = name,
            description = description,
            type = String::class.java,
        )
        property.visualizer = Visualizer.TextVisualizer::class.java
        (invoke(option, "getMaxLength") as? Number)?.let { property.addMetadata("maxLength", it.toInt()) }
        (invoke(option, "getDefault") as? String)?.let { property.addMetadata("default", it) }
        return property
    }

    private fun choiceProperty(
        option: Any,
        id: String,
        name: String,
        description: String?,
        values: List<Any>,
    ): Property<*>? {
        if (values.isEmpty()) return null
        val keys = values.map { it.toString() }

        val property = Properties.functional(
            getter = { values.indexOf(read(option)).coerceAtLeast(0) },
            setter = { index: Int -> values.getOrNull(index)?.let { write(option, it) } },
            id = id,
            name = name,
            description = description,
            type = Int::class.javaObjectType,
        )
        property.visualizer = Visualizer.DropdownVisualizer::class.java
        property.addMetadata("options", keys)
        property.addMetadata("optionsKey", keys)
        invoke(option, "getDefault")?.let { default ->
            values.indexOf(default).takeIf { it >= 0 }?.let { property.addMetadata("default", it) }
        }
        return property
    }

    private fun colorProperty(option: Any, id: String, name: String, description: String?): Property<*> {
        val property = Properties.functional(
            getter = { awtColor(storedColor(option)) },
            setter = { value: java.awt.Color -> writeColor(option, value) },
            id = id,
            name = name,
            description = description,
            type = java.awt.Color::class.java,
        )
        property.visualizer = Visualizer.ColorVisualizer::class.java
        return property
    }

    private fun storedColor(option: Any): Any? = invoke(option, "getOriginal") ?: read(option)

    private fun awtColor(color: Any?): java.awt.Color {
        color ?: return java.awt.Color.WHITE
        val red = (invoke(color, "getRed") as? Number)?.toInt() ?: 255
        val green = (invoke(color, "getGreen") as? Number)?.toInt() ?: 255
        val blue = (invoke(color, "getBlue") as? Number)?.toInt() ?: 255
        val alpha = (invoke(color, "getAlpha") as? Number)?.toInt() ?: 255
        return java.awt.Color(red, green, blue, alpha)
    }

    private fun writeColor(option: Any, value: java.awt.Color) {
        val stored = storedColor(option) ?: return
        val set = method(stored.javaClass, "set", 4)
        if (set != null) {
            runCatching { set.invoke(stored, value.red, value.green, value.blue, value.alpha) }
                .onFailure { return }
            write(option, stored)
            return
        }
        runCatching {
            val replacement = stored.javaClass
                .getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                .newInstance(value.red, value.green, value.blue, value.alpha)
            write(option, replacement)
        }.onFailure { LOGGER.warn("Failed to write an AxolotlClient color option", it) }
    }

    private fun stringArrayValues(option: Any): List<Any> =
        (invoke(option, "getValues") as? Array<*>)?.filterNotNull().orEmpty()

    private fun enumValues(option: Any): List<Any> =
        (invoke(option, "getClazz") as? Class<*>)?.enumConstants?.filterNotNull().orEmpty()

    private fun read(option: Any): Any? = invoke(option, "get")

    private fun write(option: Any, value: Any?) {
        val set = method(option.javaClass, "set", 1) ?: return
        runCatching { set.invoke(option, value) }
            .onFailure { LOGGER.warn("Failed to write AxolotlClient option {}", invoke(option, "getName"), it) }
    }

    private fun coerceNumber(value: Number?, type: Class<*>): Any {
        val number = value ?: 0
        return when (type) {
            Integer::class.java -> number.toInt()
            java.lang.Float::class.java -> number.toFloat()
            java.lang.Double::class.java -> number.toDouble()
            else -> number
        }
    }

    private fun invoke(target: Any, name: String, vararg args: Any?): Any? {
        val found = method(target.javaClass, name, args.size) ?: return null
        return runCatching { found.invoke(target, *args) }.getOrNull()
    }

    private fun method(cls: Class<*>, name: String, params: Int): Method? {
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            current.declaredMethods.firstOrNull { it.name == name && it.parameterCount == params }?.let {
                it.isAccessible = true
                return it
            }
            current = current.superclass
        }
        return null
    }

    private fun label(raw: String?): String? {
        val key = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        translateOrNull(key)?.let { return it }
        if (key.contains('.') && key.none(Char::isWhitespace)) return prettify(key.substringAfterLast('.'))
        return key
    }

    private fun translateOrNull(key: String): String? =
        if (Platform.i18n().hasTranslation(key)) {
            Platform.i18n().translateString(key)?.takeIf { it.isNotBlank() && it != key }
        } else {
            null
        }

    private fun prettify(name: String): String =
        name.replace('_', ' ').replace('-', ' ').trim().replaceFirstChar { it.uppercase() }

    private fun identitySet(): MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())

    private class Entry(
        val option: Any,
        val name: String,
        val category: String,
        val categoryKey: String?,
        val subcategory: String,
        val subcategoryKey: String?,
        val path: String,
    )
}
//? }
