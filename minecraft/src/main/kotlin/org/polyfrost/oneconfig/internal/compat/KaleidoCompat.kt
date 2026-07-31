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
import java.lang.reflect.Array as ReflectArray

object KaleidoCompat {

    private val LOGGER = LogManager.getLogger("OneConfig/Kaleido-Compat")

    private const val PKG = "folk.sisby.kaleido.lib.quiltconfig"
    private const val CONFIGS_IMPL = "$PKG.impl.util.ConfigsImpl"
    private const val TRACKED_VALUE = "$PKG.api.values.TrackedValue"
    private const val SECTION = "$PKG.api.values.ValueTreeNode\$Section"
    private const val VALUE_LIST = "$PKG.api.values.ValueList"
    private const val COMMENTS = "$PKG.api.metadata.Comments"
    private const val DISPLAY_NAME = "$PKG.api.metadata.DisplayName"
    private const val RANGE_CONSTRAINT = "$PKG.api.Constraint\$Range"

    private const val DEFAULT_CATEGORY = "General"

    private var enabled = false

    private val registered = HashSet<String>()

    private val usedTreeIds = HashSet<String>()

    @JvmStatic
    fun enable() {
        if (enabled) return
        enabled = true
        if (loadClass(CONFIGS_IMPL) == null) return
        CompatLoader.requireTranslations(skip = true) { scan() }
    }

    @JvmStatic
    fun scan() {
        val configs = loadClass(CONFIGS_IMPL) ?: return
        val all = runCatching { configs.getMethod("getAll").invoke(null) as? Iterable<*> }
            .onFailure { LOGGER.warn("Failed to read Kaleido config registry", it) }
            .getOrNull() ?: return

        for (config in all) {
            if (config == null) continue
            runCatching {
                parseConfig(config)?.let(CompatSnapshots::register)
            }.onFailure { LOGGER.warn("Failed to parse Kaleido config", it) }
        }
    }

    private fun parseConfig(config: Any): Tree? {
        val family = (call(config, "family") as? String).orEmpty()
        val id = (call(config, "id") as? String).orEmpty()
        if (id.isBlank()) return null
        if (!registered.add(if (family.isBlank()) id else "$family/$id")) return null

        val mod = resolveMod(family, id)
        val modId = mod?.id ?: id
        if (CompatLoader.nativeLoadedConfigs.contains(modId)) return null

        val tree = Tree.tree()
        tree.id = uniqueTreeId(modId, id)
        tree.title = mod?.name?.takeIf { it.isNotBlank() }
            ?: translateOrNull("$modId.title")
            ?: prettify(id)
        tree.noCache = true
        tree.saveFunction = Runnable { runCatching { call(config, "save") } }
        mod?.extractIconFile()?.let { tree.addMetadata("icon_path", it) }
        CompatLoader.originalScreenOpener(modId)?.let { tree.addMetadata("open_original_screen", it) }

        val nodes = call(config, "nodes") as? Iterable<*> ?: return null
        var added = 0
        for (node in nodes) {
            if (node == null) continue
            added += walk(node, modId, tree, category = null, subcategory = null)
        }

        return if (added > 0) tree else null
    }

    private fun walk(node: Any, modId: String, tree: Tree, category: String?, subcategory: String?): Int {
        val key = keyOf(node)
        if (isInstance(SECTION, node)) {
            val label = sectionLabel(modId, node, key)
            val children = node as? Iterable<*> ?: return 0
            var added = 0
            for (child in children) {
                if (child == null) continue
                added += walk(
                    child, modId, tree,
                    category = category ?: label,
                    subcategory = subcategory ?: category?.let { label },
                )
            }
            return added
        }
        if (!isInstance(TRACKED_VALUE, node)) return 0

        val property = runCatching { parseValue(node, modId, key) }
            .onFailure { LOGGER.warn("Failed to parse Kaleido value '{}'", key.joinToString("."), it) }
            .getOrNull() ?: return 0

        property.category = category ?: DEFAULT_CATEGORY
        property.subcategory = subcategory ?: category ?: DEFAULT_CATEGORY
        tree.put(property)
        return 1
    }

    private fun parseValue(value: Any, modId: String, key: List<String>): Property<*>? {
        val current = call(value, "value")
        val default = call(value, "getDefaultValue")
        val type = (current ?: default)?.javaClass ?: return null

        val nameKey = translationKeyOf(modId, key)
        val name = nameKey?.let(::translateOrNull) ?: displayName(modId, value) ?: prettify(key.last())
        val description = nameKey?.let { base ->
            translateOrNull("$base.tooltip") ?: translateOrNull("$base.description") ?: translateOrNull("$base.desc")
        } ?: comments(value)
        val id = key.joinToString(".")

        val range = rangeOf(value)
        val extraMetadata = HashMap<String, Any>()
        val visualizer: Class<out Visualizer>
        val property: Property<*>

        when {
            current is Boolean || default is Boolean -> {
                visualizer = Visualizer.SwitchVisualizer::class.java
                property = functional(value, id, name, description, java.lang.Boolean::class.java) { it as? Boolean ?: false }
            }

            current is Enum<*> || default is Enum<*> -> {
                visualizer = Visualizer.DropdownVisualizer::class.java
                val enumClass = enumClassOf(type)
                val constants = (enumClass.enumConstants ?: emptyArray()).filterIsInstance<Enum<*>>()
                extraMetadata["options"] = constants.map { it.name }
                extraMetadata["optionLabels"] = constants.map { enumLabel(modId, key, it) }
                @Suppress("UNCHECKED_CAST")
                property = functional(value, id, name, description, enumClass as Class<Any>) { raw ->
                    when (raw) {
                        is Enum<*> -> raw
                        is Number -> constants.getOrNull(raw.toInt())
                        is String -> constants.firstOrNull { it.name == raw }
                        else -> null
                    } ?: current
                }
            }

            current is Number || default is Number -> {
                visualizer = if (range != null) Visualizer.SliderVisualizer::class.java else Visualizer.NumberVisualizer::class.java
                val whole = type == Integer::class.java || type == java.lang.Long::class.java ||
                        type == java.lang.Short::class.java || type == java.lang.Byte::class.java
                extraMetadata["min"] = range?.first?.toFloat() ?: 0f
                extraMetadata["max"] = range?.second?.toFloat() ?: 100f
                if (whole) extraMetadata["step"] = 1f
                @Suppress("UNCHECKED_CAST")
                property = functional(value, id, name, description, type as Class<Any>) { coerceNumber(it, type) }
            }

            current is String || default is String -> {
                visualizer = Visualizer.TextVisualizer::class.java
                property = functional(value, id, name, description, String::class.java) { it?.toString() ?: "" }
            }

            current is List<*> || default is List<*> -> {
                val elementType = ((current as? List<*>) ?: (default as? List<*>))
                    ?.firstOrNull { it != null }?.javaClass ?: return null
                when {
                    Number::class.java.isAssignableFrom(elementType) -> {
                        visualizer = if (range != null) Visualizer.SliderListVisualizer::class.java
                        else Visualizer.NumberListVisualizer::class.java
                        extraMetadata["min"] = range?.first?.toFloat() ?: 0f
                        extraMetadata["max"] = range?.second?.toFloat() ?: 100f
                    }

                    elementType == String::class.java -> visualizer = Visualizer.TextListVisualizer::class.java
                    else -> return null
                }
                property = listProperty(value, id, name, description, elementType) ?: return null
            }

            // maps and other complex values have no OneConfig equivalent
            else -> return null
        }

        property.addMetadata("visualizer", visualizer)
        extraMetadata.forEach { (k, v) -> property.addMetadata(k, v) }
        default?.let { property.addMetadata("default", it) }
        return property
    }

    private fun <T> functional(
        value: Any,
        id: String,
        name: String,
        description: String?,
        type: Class<T>,
        coerce: (Any?) -> Any?,
    ): Property<T> = Properties.functional(
        getter = {
            @Suppress("UNCHECKED_CAST")
            (call(value, "value") as T)
        },
        setter = { v: T -> runCatching { setValue(value, coerce(v)) } },
        id = id, name = name, description = description, type = type,
    )

    private fun listProperty(
        value: Any,
        id: String,
        name: String,
        description: String?,
        elementType: Class<*>,
    ): Property<*>? {
        val valueList = loadClass(VALUE_LIST) ?: return null
        val create = runCatching {
            valueList.getMethod("create", Any::class.java, Array<Any>::class.java)
        }.getOrNull() ?: return null

        @Suppress("UNCHECKED_CAST")
        return Properties.functional(
            getter = { ArrayList((call(value, "value") as? List<Any?>).orEmpty()) },
            setter = { v: List<Any?> ->
                runCatching {
                    val elements = v.map { coerceElement(it, elementType) }
                    val fallback = elements.firstOrNull()
                        ?: (call(value, "getDefaultValue") as? List<*>)?.firstOrNull { it != null }
                        ?: return@runCatching
                    val array = ReflectArray.newInstance(Any::class.java, elements.size) as Array<Any?>
                    elements.forEachIndexed { index, element -> array[index] = element }
                    setValue(value, create.invoke(null, fallback, array))
                }
            },
            id = id, name = name, description = description,
            type = java.util.List::class.java as Class<List<Any?>>,
        )
    }

    private fun setValue(value: Any, newValue: Any?) {
        val method = value.javaClass.methods.firstOrNull {
            it.name == "setValue" && it.parameterCount == 2
        } ?: value.javaClass.methods.first { it.name == "setValue" && it.parameterCount == 1 }
        method.isAccessible = true
        if (method.parameterCount == 2) method.invoke(value, newValue, true) else method.invoke(value, newValue)
    }

    private fun coerceElement(value: Any?, elementType: Class<*>): Any? =
        if (Number::class.java.isAssignableFrom(elementType)) coerceNumber(value, elementType)
        else value?.toString().orEmpty()

    private fun coerceNumber(value: Any?, type: Class<*>): Any {
        val number = value as? Number ?: (value as? String)?.toDoubleOrNull() ?: 0
        return when (type) {
            Integer::class.java, Integer.TYPE -> number.toInt()
            java.lang.Long::class.java, java.lang.Long.TYPE -> number.toLong()
            java.lang.Short::class.java, java.lang.Short.TYPE -> number.toShort()
            java.lang.Byte::class.java, java.lang.Byte.TYPE -> number.toByte()
            java.lang.Float::class.java, java.lang.Float.TYPE -> number.toFloat()
            else -> number.toDouble()
        }
    }

    private fun enumClassOf(type: Class<*>): Class<*> =
        if (type.isEnum) type else type.superclass?.takeIf { it.isEnum } ?: type

    private fun rangeOf(value: Any): Pair<Number, Number>? {
        val constraints = runCatching { call(value, "constraints") as? Iterable<*> }.getOrNull() ?: return null
        for (constraint in constraints) {
            if (constraint == null || !isInstance(RANGE_CONSTRAINT, constraint)) continue
            val min = call(constraint, "min") as? Number ?: continue
            val max = call(constraint, "max") as? Number ?: continue
            return min to max
        }
        return null
    }

    private fun metadataOfType(node: Any, className: String): Any? {
        val map = runCatching { call(node, "metadata") as? Map<*, *> }.getOrNull() ?: return null
        return map.values.firstOrNull { it != null && isInstance(className, it) }
    }

    private fun displayName(modId: String, node: Any): String? {
        val displayName = metadataOfType(node, DISPLAY_NAME) ?: return null
        val name = call(displayName, "getName") as? String ?: return null
        val translatable = call(displayName, "isTranslatable") as? Boolean ?: false
        if (!translatable) return name.takeIf { it.isNotBlank() }
        return translateOrNull(name) ?: translateOrNull("$modId.$name")
    }

    private fun comments(node: Any): String? {
        val comments = metadataOfType(node, COMMENTS) as? Iterable<*> ?: return null
        return comments.filterIsInstance<String>().joinToString("\n").takeIf { it.isNotBlank() }
    }

    private fun translationKeyOf(modId: String, key: List<String>): String? {
        val paths = LinkedHashSet<String>()
        for (base in listOf(key.joinToString("."), key.last())) {
            paths += base
            paths += base.split('.').joinToString(".") { camelToSnake(it) }
        }
        for (path in paths) {
            for (candidate in listOf(
                "$modId.option.$path",
                "$modId.config.$path",
                "config.$modId.$path",
                "option.$modId.$path",
                "$modId.$path",
            )) {
                if (Platform.i18n().hasTranslation(candidate)) return candidate
            }
        }
        return null
    }

    private fun sectionLabel(modId: String, node: Any, key: List<String>): String {
        displayName(modId, node)?.let { return it }
        val name = key.lastOrNull() ?: return DEFAULT_CATEGORY
        for (candidate in listOf(camelToSnake(name), name)) {
            translateOrNull("$modId.category.$candidate")?.let { return it }
            translateOrNull("$modId.$candidate")?.let { return it }
        }
        return prettify(name)
    }

    private fun enumLabel(modId: String, key: List<String>, constant: Enum<*>): String {
        val name = constant.name
        for (base in listOf(key.joinToString("."), key.last())) {
            for (spelling in listOf(name, name.lowercase())) {
                translateOrNull("$modId.option.$base.$spelling")?.let { return it }
                translateOrNull("$modId.$base.$spelling")?.let { return it }
            }
        }
        translateOrNull("$modId.enum.${constant.javaClass.simpleName}.$name")?.let { return it }
        return prettify(name)
    }

    private fun resolveMod(family: String, id: String): ModInfo? {
        val candidates = listOf(id, family).filter { it.isNotBlank() }
        val mods = ModInfo.loadedMods
        mods.firstOrNull { mod -> candidates.any { it == mod.id } }?.let { return it }
        return mods.firstOrNull { mod -> candidates.any { normalize(it) == normalize(mod.id) } }
    }

    private fun uniqueTreeId(modId: String, configId: String): String {
        if (usedTreeIds.add(modId)) return modId
        var candidate = "$modId-$configId"
        var suffix = 2
        while (!usedTreeIds.add(candidate)) {
            candidate = "$modId-$configId-$suffix"
            suffix++
        }
        return candidate
    }

    @Suppress("UNCHECKED_CAST")
    private fun keyOf(node: Any): List<String> =
        (call(node, "key") as? Iterable<String>)?.toList()?.takeIf { it.isNotEmpty() } ?: listOf("value")

    private fun normalize(id: String): String = id.replace("-", "").replace("_", "").lowercase()

    private fun camelToSnake(name: String): String =
        name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()

    private fun prettify(name: String): String =
        camelToSnake(name).replace('_', ' ').trim().replaceFirstChar { it.uppercase() }

    private fun translateOrNull(key: String): String? =
        if (Platform.i18n().hasTranslation(key)) Platform.i18n().translateString(key)?.takeIf { it.isNotBlank() && it != key }
        else null

    private fun call(target: Any, name: String, vararg args: Any?): Any? {
        val method = target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == args.size }
            ?: return null
        method.isAccessible = true
        return method.invoke(target, *args)
    }

    private fun isInstance(className: String, value: Any): Boolean =
        loadClass(className)?.isInstance(value) ?: false

    private fun loadClass(name: String): Class<*>? =
        runCatching { Class.forName(name, false, KaleidoCompat::class.java.classLoader) }.getOrNull()
}
