//? osl_config_compat {
/*package org.polyfrost.oneconfig.internal.compat

import net.ornithemc.osl.config.api.ConfigManager
import net.ornithemc.osl.config.api.ConfigScope
import net.ornithemc.osl.config.api.config.Config
import net.ornithemc.osl.config.api.config.option.BaseOption
import net.ornithemc.osl.config.api.config.option.ListOption
import net.ornithemc.osl.config.api.config.option.Option
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
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID

internal object OslConfigCompat {

    private val LOGGER = LogManager.getLogger("OneConfig/OSL-Compat")

    private val seen: MutableSet<Config> = Collections.newSetFromMap(IdentityHashMap<Config, Boolean>())
    private val usedTreeIds = HashSet<String>()
    private val treesByOwner = HashMap<String, Int>()

    @JvmStatic
    fun addConfig(config: Config) {
        runCatching {
            if (config.scope != ConfigScope.GLOBAL) {
                LOGGER.debug("Skipping {} scoped OSL config {}", config.scope, config)
                return
            }
            if (!seen.add(config)) return
            val mod = CompatLoader.findModByClass(config.javaClass) ?: CompatLoader.findFirstMod()
            if (mod != null && CompatLoader.nativeLoadedConfigs.contains(mod.id)) return
            CompatLoader.requireTranslations {
                runCatching { parseConfig(config, mod)?.let(CompatSnapshots::register) }
                    .onFailure { LOGGER.warn("Failed to parse OSL config {}", config, it) }
            }
        }.onFailure { LOGGER.warn("Failed to prepare an OSL config wrapper", it) }
    }

    private fun parseConfig(config: Config, mod: ModInfo?): Tree? {
        val groups = config.groups.filter { it.options.isNotEmpty() }
        if (groups.isEmpty()) return null

        val namespace = config.namespace?.takeIf { it.isNotBlank() } ?: config.name
        val tree = Tree.tree()
        tree.id = uniqueId(usedTreeIds, idPart(namespace, "osl_config"))
        tree.title = titleOf(config, mod, namespace)
        tree.noCache = true
        tree.saveFunction = Runnable { runCatching { ConfigManager.save(config) } }
        tree.addMetadata(CompatSnapshots.CUSTOM_RESET_METADATA, Runnable {
            runCatching {
                config.resetAll()
                ConfigManager.save(config)
            }
        })
        mod?.extractIconFile()?.let { tree.addMetadata("icon_path", it) }

        val usedIds = HashSet<String>()
        var added = 0
        for (group in groups) {
            val category = label(group.name) ?: prettify(group.name)
            val groupPath = idPart(group.name, "general")
            for (option in group.options) {
                if (option == null) continue
                runCatching {
                    val path = "$groupPath/${idPart(option.name, "option")}"
                    val property = parseOption(option, path, usedIds) ?: return@runCatching
                    property.category = category
                    property.subcategory = category
                    tree.put(property)
                    added++
                }.onFailure { LOGGER.warn("Failed to parse OSL option {} of {}", option.name, config, it) }
            }
        }

        return if (added > 0) tree else null
    }

    private fun titleOf(config: Config, mod: ModInfo?, namespace: String): String {
        val configLabel = label(config.name)
        val modName = mod?.name?.takeIf { it.isNotBlank() }
        val count = treesByOwner.merge(mod?.id ?: namespace, 1) { a, b -> a + b } ?: 1
        return when {
            modName == null -> configLabel ?: prettify(namespace)
            count > 1 && configLabel != null -> "$modName - $configLabel"
            else -> modName
        }
    }

    private fun parseOption(option: Option, path: String, usedIds: MutableSet<String>): Property<*>? {
        if (option !is BaseOption<*>) return null
        @Suppress("UNCHECKED_CAST")
        val opt = option as BaseOption<Any?>
        val default = opt.default ?: return null
        val id = uniqueId(usedIds, path)
        val name = label(option.name) ?: prettify(option.name)
        val description = option.description?.let { label(it) }

        val property: Property<*>
        val visualizer: Class<out Visualizer>

        when (default) {
            is Boolean -> {
                visualizer = Visualizer.SwitchVisualizer::class.java
                property = wrap(opt, id, name, description, java.lang.Boolean::class.java, { it }, { it })
            }

            is Number -> {
                visualizer = Visualizer.NumberVisualizer::class.java
                val numberType = default.javaClass
                property = wrap(opt, id, name, description, numberType, { it }, { coerceNumber(it, numberType) })
                property.addMetadata("min", -Float.MAX_VALUE)
                property.addMetadata("max", Float.MAX_VALUE)
            }

            is String -> {
                visualizer = Visualizer.TextVisualizer::class.java
                property = wrap(opt, id, name, description, String::class.java, { it }, { it?.toString().orEmpty() })
            }

            is Char -> {
                visualizer = Visualizer.TextVisualizer::class.java
                property = wrap(
                    opt, id, name, description, String::class.java,
                    { it?.toString().orEmpty() },
                    { (it as? String)?.firstOrNull() ?: default },
                )
            }

            is UUID -> {
                visualizer = Visualizer.TextVisualizer::class.java
                property = wrap(
                    opt, id, name, description, String::class.java,
                    { it?.toString().orEmpty() },
                    { value -> runCatching { UUID.fromString(value as String) }.getOrDefault(default) },
                )
            }

            is Path -> {
                visualizer = Visualizer.FileVisualizer::class.java
                property = wrap(
                    opt, id, name, description, String::class.java,
                    { it?.toString().orEmpty() },
                    { value -> runCatching { Paths.get(value as String) }.getOrDefault(default) },
                )
            }

            is List<*> -> {
                val elementType = boxed(
                    (option as? ListOption<*>)?.elementType
                        ?: default.firstOrNull { it != null }?.javaClass
                        ?: return null
                )
                val numeric = Number::class.java.isAssignableFrom(elementType)
                if (!numeric && elementType != String::class.java) return null
                visualizer = if (numeric) {
                    Visualizer.NumberListVisualizer::class.java
                } else {
                    Visualizer.TextListVisualizer::class.java
                }
                property = wrap(
                    opt, id, name, description, java.util.List::class.java,
                    { value -> readList(value, numeric) },
                    { value -> writeList(value, numeric, elementType) },
                )
                if (numeric) {
                    property.addMetadata("min", -Float.MAX_VALUE)
                    property.addMetadata("max", Float.MAX_VALUE)
                }
            }

            else -> return null
        }

        property.visualizer = visualizer
        return property
    }

    @Suppress("UNCHECKED_CAST")
    private fun wrap(
        opt: BaseOption<Any?>,
        id: String,
        name: String,
        description: String?,
        type: Class<*>,
        read: (Any?) -> Any?,
        write: (Any?) -> Any?,
    ): Property<Any?> {
        val fallback = read(opt.default)
        val property = Properties.functional<Any?>(
            getter = { runCatching { read(opt.get()) }.getOrNull() ?: fallback },
            setter = { value -> runCatching { opt.set(write(value)) } },
            id = id,
            name = name,
            description = description,
            type = type as Class<Any?>,
        )
        fallback?.let { property.addMetadata("default", it) }
        return property
    }

    private fun readList(value: Any?, numeric: Boolean): ArrayList<Any> {
        val list = value as? List<*> ?: emptyList<Any?>()
        return list.mapTo(ArrayList()) { element ->
            if (numeric) element as? Number ?: 0 else element?.toString().orEmpty()
        }
    }

    private fun writeList(value: Any?, numeric: Boolean, elementType: Class<*>): ArrayList<Any> {
        val list = value as? List<*> ?: emptyList<Any?>()
        return list.mapTo(ArrayList()) { element ->
            if (numeric) coerceNumber(element, elementType) else element?.toString().orEmpty()
        }
    }

    private fun coerceNumber(value: Any?, type: Class<*>): Any {
        val number = value as? Number ?: 0
        return when (boxed(type)) {
            java.lang.Byte::class.java -> number.toByte()
            java.lang.Short::class.java -> number.toShort()
            Integer::class.java -> number.toInt()
            java.lang.Long::class.java -> number.toLong()
            java.lang.Float::class.java -> number.toFloat()
            java.lang.Double::class.java -> number.toDouble()
            else -> number
        }
    }

    private fun boxed(type: Class<*>): Class<*> = when (type) {
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        Integer.TYPE -> Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        Character.TYPE -> Character::class.java
        else -> type
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
}
*///? }
