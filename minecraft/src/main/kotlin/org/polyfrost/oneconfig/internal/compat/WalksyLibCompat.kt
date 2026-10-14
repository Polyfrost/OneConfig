//? walksylib_compat {
package org.polyfrost.oneconfig.internal.compat

import java.awt.Color
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import net.minecraft.client.Minecraft
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
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId
import org.polyfrost.oneconfig.internal.ui.api.MASTER_TOGGLE
import org.polyfrost.oneconfig.internal.ui.api.isMasterSwitchName

object WalksyLibCompat {

    private val LOGGER = LogManager.getLogger("OneConfig/WalksyLib-Compat")

    @JvmStatic
    fun onLoad(entryPointList: Any) {
        CompatLoader.requireTranslations {
            runCatching {
                val getMethod = entryPointList.javaClass.methods.firstOrNull { (it.name == "get" || it.name == "getMods") && it.parameterCount == 0 } ?: return@runCatching
                val mods = getMethod.invoke(entryPointList) as? Collection<*> ?: return@runCatching
                for (mod in mods) {
                    if (mod == null) continue
                    runCatching { parseMod(mod)?.let(CompatSnapshots::register) }
                        .onFailure { LOGGER.warn("Failed to parse WalksyLib mod config", it) }
                }
            }.onFailure { LOGGER.warn("Failed to load WalksyLib configs", it) }
        }
    }

    private fun parseMod(mod: Any): Tree? {
        val container = invoke(mod, "getContainer") ?: return null
        val metadata = invoke(container, "getMetadata") ?: return null
        val modid = invoke(metadata, "getId") as? String ?: return null
        if (CompatLoader.nativeLoadedConfigs.contains(modid)) return null

        if (invoke(mod, "hasConfig") == false) return null
        val config = invoke(mod, "getConfig") ?: return null

        val categories = invoke(config, "categories") as? Collection<*> ?: return null
        if (categories.isEmpty()) return null

        val modInfo = ModInfo.loadedMods.firstOrNull { it.id == modid }

        val tree = Tree.tree()
        tree.id = modid
        tree.title = modInfo?.name?.takeIf { it.isNotBlank() }
            ?: (invoke(metadata, "getName") as? String)?.takeIf { it.isNotBlank() }
            ?: modid
        tree.noCache = true
        buildSaveFunction(config)?.let { tree.saveFunction = it }
        modInfo?.extractIconFile()?.let { tree.addMetadata("icon_path", it) }
        CompatLoader.originalScreenOpener(modid)?.let { tree.addMetadata("open_original_screen", it) }

        var added = false
        val usedIds = HashSet<String>()
        for (category in categories) {
            if (category == null) continue
            val categoryName = invoke(category, "name") as? String ?: continue
            val categoryPath = idPart(categoryName, "general")

            (invoke(category, "options") as? Collection<*>)?.forEach { option ->
                if (option != null && parseOption(option, categoryName, null, categoryPath, tree, usedIds)) added = true
            }

            ((invoke(category, "optionGroups") ?: invoke(category, "groups")) as? Collection<*>)?.forEach { group ->
                if (group == null) return@forEach
                val groupName = invoke(group, "getName") as? String
                val groupPath = "$categoryPath/${idPart(groupName, "group")}"
                (invoke(group, "getOptions") as? Collection<*>)?.forEach { option ->
                    if (option != null && parseOption(option, categoryName, groupName, groupPath, tree, usedIds)) added = true
                }
            }
        }

        val firstCategory = categories.firstNotNullOfOrNull { it?.let { c -> invoke(c, "name") as? String } }
        tree.map.values.firstOrNull {
            it is Property<*> && it.type == java.lang.Boolean::class.java && it.category == firstCategory &&
                isMasterSwitchName(it.title as? String ?: "")
        }?.addMetadata(MASTER_TOGGLE, true)

        return if (added) tree else null
    }

    private fun parseOption(
        option: Any,
        categoryName: String,
        subcategoryName: String?,
        groupPath: String,
        tree: Tree,
        usedIds: MutableSet<String>,
    ): Boolean {
        val optionClass = option::class.java
        val type = invoke(option, "getType") as? Class<*> ?: return false
        val name = (invoke(option, "getName") as? String)?.takeIf { it.isNotBlank() } ?: return false
        val description = resolveDescription(option)
        val optionPath = "$groupPath/${idPart(name, "option")}"

        if (type == Runnable::class.java) {
            return parseButton(option, name, description, uniqueId(usedIds, optionPath), categoryName, subcategoryName, tree)
        }

        if (type.name in COLOR_CLASSES) {
            return parseColor(option, type, name, description, uniqueId(usedIds, optionPath), categoryName, subcategoryName, tree)
        }

        if (type.name == SPRITE_CLASS) {
            return parseSprite(option, type, name, description, uniqueId(usedIds, optionPath), categoryName, subcategoryName, tree)
        }

        if (isTexture(type)) {
            return parseTexture(option, type, name, description, uniqueId(usedIds, optionPath), categoryName, subcategoryName, tree)
        }

        if (java.util.List::class.java.isAssignableFrom(type)) {
            return parseStringList(option, name, description, uniqueId(usedIds, optionPath), categoryName, subcategoryName, tree)
        }

        val min = invoke(option, "getMin") as? Number
        val max = invoke(option, "getMax") as? Number
        val visualizer: Class<out Visualizer> = when {
            type == java.lang.Boolean::class.java -> Visualizer.SwitchVisualizer::class.java
            type in NUMBER_TYPES ->
                if (min != null && max != null) Visualizer.SliderVisualizer::class.java else Visualizer.NumberVisualizer::class.java
            type == String::class.java -> Visualizer.TextVisualizer::class.java
            Enum::class.java.isAssignableFrom(type) -> Visualizer.DropdownVisualizer::class.java
            else -> return false // pixel grid and other WalksyLib-only types
        }

        val getValueM = optionClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }
            ?.apply { isAccessible = true } ?: return false
        val setValueM = optionClass.methods.firstOrNull { it.name == "setValue" && it.parameterCount == 1 }
            ?.apply { isAccessible = true } ?: return false

        @Suppress("UNCHECKED_CAST")
        val property = Properties.functional(
            { runCatching { getValueM.invoke(option) }.getOrNull() },
            { value -> runCatching { setValueM.invoke(option, coerce(value, type)) } },
            uniqueId(usedIds, optionPath),
            name,
            description,
            type as Class<Any?>,
        )

        property.addMetadata("visualizer", visualizer)
        (invoke(option, "getDefaultValue"))?.let { property.addMetadata("default", it) }
        property.category = categoryName
        property.subcategory = subcategoryName

        when {
            Enum::class.java.isAssignableFrom(type) ->
                property.addMetadata("options", type.enumConstants?.map { it.toString() } ?: emptyList<String>())
            type in NUMBER_TYPES -> {
                property.addMetadata("min", min?.toFloat() ?: -Float.MAX_VALUE)
                property.addMetadata("max", max?.toFloat() ?: Float.MAX_VALUE)
                (invoke(option, "getIncrement") as? Number)?.let { property.addMetadata("step", it.toFloat()) }
            }
        }

        tree.put(property)
        return true
    }

    private val NUMBER_TYPES = setOf<Class<*>>(
        Integer::class.java, java.lang.Long::class.java, java.lang.Short::class.java, java.lang.Byte::class.java,
        java.lang.Double::class.java, java.lang.Float::class.java,
    )

    private val COLOR_CLASSES = setOf(
        "main.walksy.lib.core.config.local.options.type.WalksyLibColor",
        "main.walksy.lib.api.config.value.WLColor",
    )

    private fun parseColor(
        option: Any,
        colorClass: Class<*>,
        name: String,
        description: String?,
        id: String,
        categoryName: String,
        subcategoryName: String?,
        tree: Tree,
    ): Boolean {
        val optionClass = option::class.java
        val getValueM = optionClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }
            ?.apply { isAccessible = true } ?: return false
        val setValueM = optionClass.methods.firstOrNull { it.name == "setValue" && it.parameterCount == 1 }
            ?.apply { isAccessible = true } ?: return false

        val getRed = colorMethod(colorClass, "getRed") ?: return false
        val getGreen = colorMethod(colorClass, "getGreen") ?: return false
        val getBlue = colorMethod(colorClass, "getBlue") ?: return false
        val getAlpha = colorMethod(colorClass, "getAlpha") ?: return false
        val ctor = runCatching {
            colorClass.getConstructor(
                Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
            ).apply { isAccessible = true }
        }.getOrNull() ?: return false
        val getAdditions = colorMethod(colorClass, "getAdditions")
        val setAdditions = colorClass.methods.firstOrNull { it.name == "setAdditions" && it.parameterCount == 1 }
            ?.apply { isAccessible = true }

        fun toColor(wc: Any?): Color? {
            wc ?: return null
            return runCatching {
                Color(
                    getRed.invoke(wc) as Int,
                    getGreen.invoke(wc) as Int,
                    getBlue.invoke(wc) as Int,
                    getAlpha.invoke(wc) as Int,
                )
            }.getOrNull()
        }

        val property = Properties.functional(
            { runCatching { toColor(getValueM.invoke(option)) }.getOrNull() ?: Color.WHITE },
            { value ->
                runCatching {
                    val wc = ctor.newInstance(value.red, value.green, value.blue, value.alpha)
                    if (getAdditions != null && setAdditions != null) {
                        getValueM.invoke(option)?.let { old ->
                            runCatching { setAdditions.invoke(wc, getAdditions.invoke(old)) }
                        }
                    }
                    setValueM.invoke(option, wc)
                }
            },
            id,
            name,
            description,
            Color::class.java,
        )

        property.addMetadata("visualizer", Visualizer.ColorVisualizer::class.java)
        toColor(invoke(option, "getDefaultValue"))?.let { property.addMetadata("default", it) }
        property.category = categoryName
        property.subcategory = subcategoryName
        tree.put(property)
        return true
    }

    private fun colorMethod(colorClass: Class<*>, name: String): Method? =
        colorClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }?.apply { isAccessible = true }

    private const val SPRITE_CLASS = "main.walksy.lib.core.utils.IdentifierWrapper"
    private const val CONFIG_MANAGER_CLASS = "main.walksy.lib.core.manager.WalksyLibConfigManager"
    private val SPRITE_TYPES = arrayOf("png")

    private fun parseSprite(
        option: Any,
        wrapperClass: Class<*>,
        name: String,
        description: String?,
        id: String,
        categoryName: String,
        subcategoryName: String?,
        tree: Tree,
    ): Boolean {
        val optionClass = option::class.java
        val getValueM = optionClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }
            ?.apply { isAccessible = true } ?: return false
        val setValueM = optionClass.methods.firstOrNull { it.name == "setValue" && it.parameterCount == 1 }
            ?.apply { isAccessible = true } ?: return false

        val getFileName = wrapperClass.methods.firstOrNull { it.name == "getFileName" && it.parameterCount == 0 }
            ?.apply { isAccessible = true } ?: return false
        val wrapperCtor = wrapperClass.constructors.firstOrNull {
            it.parameterCount == 2 && it.parameterTypes[1] == String::class.java
        }?.apply { isAccessible = true } ?: return false

        val managerClass = runCatching { Class.forName(CONFIG_MANAGER_CLASS, false, wrapperClass.classLoader) }
            .getOrNull() ?: return false
        val cachedImageDirM = managerClass.methods.firstOrNull { it.name == "getCachedImageDir" && it.parameterCount == 0 }
            ?.apply { isAccessible = true } ?: return false
        val loadTextureM = managerClass.methods.firstOrNull {
            it.name == "loadTextureFromCache" && it.parameterCount == 1
        }?.apply { isAccessible = true } ?: return false

        fun cachedDir(): Path? = runCatching { cachedImageDirM.invoke(null) as? Path }.getOrNull()

        val defaultWrapper = invoke(option, "getDefaultValue")
        val defaultString = defaultWrapper?.let { runCatching { getFileName.invoke(it) as? String }.getOrNull() }
            ?.takeIf { it.isNotBlank() }?.let { cachedDir()?.resolve(it)?.toString() } ?: ""

        val property = Properties.functional(
            {
                runCatching {
                    val fileName = getFileName.invoke(getValueM.invoke(option)) as? String
                    fileName?.takeIf { it.isNotBlank() }?.let { cachedDir()?.resolve(it)?.toString() } ?: ""
                }.getOrNull() ?: ""
            },
            { value ->
                runCatching {
                    val path = value.takeIf { it.isNotBlank() }
                    if (path == null || (defaultWrapper != null && path == defaultString)) {
                        if (defaultWrapper != null) {
                            runCatching { setValueM.invoke(option, defaultWrapper) }
                                .onFailure { LOGGER.warn("Failed to reset WalksyLib sprite value", it) }
                        }
                        return@runCatching
                    }
                    val source = File(path)
                    if (!source.isFile) return@runCatching
                    val fileName = source.name
                    val dest = cachedDir()?.resolve(fileName)
                    if (dest != null && source.toPath().toAbsolutePath() != dest.toAbsolutePath()) {
                        Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING)
                    }
                    Minecraft.getInstance().execute {
                        runCatching {
                            val identifier = loadTextureM.invoke(null, fileName) ?: return@execute
                            setValueM.invoke(option, wrapperCtor.newInstance(identifier, fileName))
                        }.onFailure { LOGGER.warn("Failed to apply WalksyLib sprite value", it) }
                    }
                }.onFailure { LOGGER.warn("Failed to set WalksyLib sprite value", it) }
            },
            id,
            name,
            description,
            String::class.java,
        )

        property.addMetadata("visualizer", Visualizer.FileVisualizer::class.java)
        property.addMetadata("types", SPRITE_TYPES)
        property.addMetadata("filterName", "PNG Image")
        property.addMetadata("placeholder", "Select a .png texture")
        if (defaultWrapper != null) property.addMetadata("default", defaultString)
        property.category = categoryName
        property.subcategory = subcategoryName
        tree.put(property)
        return true
    }

    private const val CACHED_TEXTURES_CLASS = "main.walksy.lib.api.render.CachedTextures"
    private const val SPRITE_CACHE_CLASS = "main.walksy.lib.core.config.serialization.SpriteCache"
    private const val CROPPED_TEXTURE_CLASS = "main.walksy.lib.api.config.value.CroppedTexture"
    private const val DROPPED_PREFIX = "walksylib:dropped/"

    private val cachedTexturesClass: Class<*>? by lazy {
        runCatching { Class.forName(CACHED_TEXTURES_CLASS, false, WalksyLibCompat::class.java.classLoader) }.getOrNull()
    }

    private val identifierClass: Class<*>? by lazy {
        cachedTexturesClass?.methods?.firstOrNull { it.name == "isCacheBacked" && it.parameterCount == 1 }?.parameterTypes?.get(0)
    }

    private fun isTexture(type: Class<*>): Boolean =
        type.name == CROPPED_TEXTURE_CLASS || (identifierClass != null && type == identifierClass)

    private fun parseTexture(
        option: Any,
        type: Class<*>,
        name: String,
        description: String?,
        id: String,
        categoryName: String,
        subcategoryName: String?,
        tree: Tree,
    ): Boolean {
        val optionClass = option::class.java
        val loader = optionClass.classLoader
        val getValueM = optionClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }
            ?.apply { isAccessible = true } ?: return false
        val setValueM = optionClass.methods.firstOrNull { it.name == "setValue" && it.parameterCount == 1 }
            ?.apply { isAccessible = true } ?: return false

        val cached = cachedTexturesClass ?: return false
        val importFile = runCatching { cached.getMethod("importFile", Path::class.java) }.getOrNull() ?: return false
        val ensureLoaded = cached.methods.firstOrNull { it.name == "ensureLoaded" && it.parameterCount == 1 }
        val directory = runCatching { Class.forName(SPRITE_CACHE_CLASS, false, loader).getMethod("getDirectory") }
            .getOrNull() ?: return false
        val cropped = type.name == CROPPED_TEXTURE_CLASS
        val texture = if (cropped) runCatching { type.getMethod("texture") }.getOrNull() ?: return false else null
        val of = if (cropped) type.methods.firstOrNull { it.name == "of" && it.parameterCount == 1 } ?: return false else null
        val crop = if (cropped) runCatching { CROP_BOUNDS.map { type.getMethod(it) } }.getOrNull() ?: return false else null
        val withCrop = if (cropped) type.methods.firstOrNull { it.name == "withCrop" && it.parameterCount == 4 } ?: return false else null
        val tryParse = identifierClass?.methods?.firstOrNull {
            it.name == "tryParse" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
        }

        fun toPath(value: Any?): String {
            val identifier = (if (texture != null) value?.let { texture.invoke(it) } else value)?.toString() ?: return ""
            val path = if (!identifier.startsWith(DROPPED_PREFIX)) identifier
            else (directory.invoke(null) as Path).resolve(identifier.removePrefix(DROPPED_PREFIX)).toString()
            val bounds = crop?.map { it.invoke(value) as Float }?.takeIf { it != FULL_CROP } ?: return path
            return "$path#${bounds.joinToString(",")}"
        }

        val default = invoke(option, "getDefaultValue")
        val defaultString = runCatching { toPath(default) }.getOrDefault("")

        val property = Properties.functional(
            { runCatching { toPath(getValueM.invoke(option)) }.getOrNull() ?: "" },
            { value ->
                runCatching {
                    if (value.isBlank() || value == defaultString) {
                        if (default != null) setValueM.invoke(option, default)
                        return@runCatching
                    }
                    if (value == toPath(getValueM.invoke(option))) return@runCatching
                    val bounds = if (crop != null) splitCrop(value) else null
                    val path = bounds?.first ?: value
                    val source = File(path)
                    if (!source.isFile && !IDENTIFIER.matches(path)) {
                        LOGGER.debug("Ignoring WalksyLib texture value {}: not a file or identifier", path)
                        return@runCatching
                    }
                    val identifier = if (source.isFile) importFile.invoke(null, source.toPath())
                    else tryParse?.invoke(null, path) ?: return@runCatching
                    var newValue = of?.invoke(null, identifier) ?: identifier
                    if (bounds != null) newValue = withCrop!!.invoke(newValue, *bounds.second.toTypedArray())
                    setValueM.invoke(option, newValue)
                    if (ensureLoaded != null) Minecraft.getInstance().execute {
                        runCatching { ensureLoaded.invoke(null, identifier) }
                            .onFailure { LOGGER.warn("Failed to load WalksyLib texture", it) }
                    }
                }.onFailure { LOGGER.warn("Failed to set WalksyLib texture value", it) }
            },
            id,
            name,
            description,
            String::class.java,
        )

        property.addMetadata("visualizer", Visualizer.FileVisualizer::class.java)
        property.addMetadata("types", TEXTURE_TYPES)
        property.addMetadata("filterName", "Image")
        property.addMetadata("placeholder", "Select an image")
        if (default != null) property.addMetadata("default", defaultString)
        property.category = categoryName
        property.subcategory = subcategoryName
        tree.put(property)
        return true
    }

    private val TEXTURE_TYPES = arrayOf("png", "jpg", "jpeg", "bmp", "gif")

    private val CROP_BOUNDS = listOf("u0", "v0", "u1", "v1")
    private val FULL_CROP = listOf(0f, 0f, 1f, 1f)

    private fun splitCrop(value: String): Pair<String, List<Float>>? {
        val i = value.lastIndexOf('#').takeIf { it >= 0 } ?: return null
        val bounds = value.substring(i + 1).split(',').map { it.toFloatOrNull() ?: return null }
        return if (bounds.size == 4) value.substring(0, i) to bounds else null
    }

    private val IDENTIFIER = Regex("[a-z0-9_.-]+:[a-z0-9_.-][a-z0-9_./-]*")

    private fun parseStringList(
        option: Any,
        name: String,
        description: String?,
        id: String,
        categoryName: String,
        subcategoryName: String?,
        tree: Tree,
    ): Boolean {
        val optionClass = option::class.java
        val getValueM = optionClass.methods.firstOrNull { it.name == "getValue" && it.parameterCount == 0 }
            ?.apply { isAccessible = true } ?: return false
        val setValueM = optionClass.methods.firstOrNull { it.name == "setValue" && it.parameterCount == 1 }
            ?.apply { isAccessible = true } ?: return false

        fun stringsOf(value: Any?): ArrayList<String> =
            ArrayList((value as? Collection<*>)?.filterIsInstance<String>() ?: emptyList())

        @Suppress("UNCHECKED_CAST")
        val property = Properties.functional(
            { runCatching { stringsOf(getValueM.invoke(option)) }.getOrDefault(ArrayList()) },
            { value ->
                runCatching { setValueM.invoke(option, stringsOf(value)) }
                    .onFailure { LOGGER.warn("Failed to set WalksyLib string list value", it) }
            },
            id,
            name,
            description,
            java.util.List::class.java as Class<List<String>>,
        )

        property.addMetadata("visualizer", Visualizer.TextListVisualizer::class.java)
        invoke(option, "getDefaultValue")?.let { property.addMetadata("default", stringsOf(it)) }
        property.addMetadata("placeholder", "Enter a value...")
        property.category = categoryName
        property.subcategory = subcategoryName
        tree.put(property)
        return true
    }

    private fun parseButton(
        option: Any,
        name: String,
        description: String?,
        id: String,
        categoryName: String,
        subcategoryName: String?,
        tree: Tree,
    ): Boolean {
        val action = invoke(option, "getValue") as? Runnable ?: return false
        val property = Properties.dummy(id, name, description)
        property.addMetadata("visualizer", Visualizer.ButtonVisualizer::class.java)
        property.addMetadata("textKey", name)
        property.addMetadata("runnable", Runnable {
            runCatching { action.run() }.onFailure { LOGGER.warn("Failed to run WalksyLib button action", it) }
        })
        property.category = categoryName
        property.subcategory = subcategoryName
        tree.put(property)
        return true
    }

    private fun coerce(value: Any?, type: Class<*>): Any? = when {
        value is Number && type == Integer::class.java -> value.toInt()
        value is Number && type == java.lang.Long::class.java -> value.toLong()
        value is Number && type == java.lang.Short::class.java -> value.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        value is Number && type == java.lang.Byte::class.java -> value.toInt().coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()
        value is Number && type == java.lang.Double::class.java -> value.toDouble()
        value is Number && type == java.lang.Float::class.java -> value.toFloat()
        else -> value
    }

    private fun resolveDescription(option: Any): String? = runCatching {
        val desc = invoke(option, "getDescription") ?: return null
        val supplier = invoke(desc, "getStringSupplier") ?: return null
        (invoke(supplier, "get") as? String)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun buildSaveFunction(config: Any): Runnable? {
        fun method(name: String) = config.javaClass.methods
            .firstOrNull { it.name == name && it.parameterCount == 0 }
            ?.apply { isAccessible = true }

        val persist = method("saveAndNotify") ?: method("onSave")
        val notify = if (persist?.name == "saveAndNotify") null else method("runSave")
        if (persist == null && notify == null) return null
        return Runnable {
            persist?.let {
                runCatching { it.invoke(config) }
                    .onFailure { e -> LOGGER.warn("Failed to write WalksyLib config", e) }
            }
            notify?.let {
                runCatching { it.invoke(config) }
                    .onFailure { e -> LOGGER.warn("WalksyLib save callback failed", e) }
            }
        }
    }

    private fun invoke(target: Any, method: String): Any? =
        target.javaClass.methods.firstOrNull { it.name == method && it.parameterCount == 0 }
            ?.apply { isAccessible = true }
            ?.let { runCatching { it.invoke(target) }.getOrNull() }
}
//? }
