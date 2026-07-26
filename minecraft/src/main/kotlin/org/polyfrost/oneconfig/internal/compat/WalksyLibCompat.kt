//? walksylib_compat {
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
import net.minecraft.client.Minecraft
import java.util.UUID

object WalksyLibCompat {

    private val LOGGER = LogManager.getLogger("OneConfig/WalksyLib-Compat")

    @JvmStatic
    fun onLoad(entryPointList: Any) {
        CompatLoader.requireTranslations {
            runCatching {
                val getMethod = entryPointList.javaClass.methods.firstOrNull { it.name == "get" && it.parameterCount == 0 } ?: return@runCatching
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

        val hasConfig = invoke(mod, "hasConfig") as? Boolean ?: false
        if (!hasConfig) return null
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
        for (category in categories) {
            if (category == null) continue
            val categoryName = invoke(category, "name") as? String ?: continue

            (invoke(category, "options") as? Collection<*>)?.forEach { option ->
                if (option != null && parseOption(option, categoryName, null, tree)) added = true
            }

            (invoke(category, "optionGroups") as? Collection<*>)?.forEach { group ->
                if (group == null) return@forEach
                val groupName = invoke(group, "getName") as? String
                (invoke(group, "getOptions") as? Collection<*>)?.forEach { option ->
                    if (option != null && parseOption(option, categoryName, groupName, tree)) added = true
                }
            }
        }

        return if (added) tree else null
    }

    private fun parseOption(option: Any, categoryName: String, subcategoryName: String?, tree: Tree): Boolean {
        val optionClass = option::class.java
        val type = invoke(option, "getType") as? Class<*> ?: return false
        val name = (invoke(option, "getName") as? String)?.takeIf { it.isNotBlank() } ?: return false
        val description = resolveDescription(option)

        if (type == Runnable::class.java) {
            return parseButton(option, name, description, categoryName, subcategoryName, tree)
        }

        if (type.name == COLOR_CLASS) {
            return parseColor(option, type, name, description, categoryName, subcategoryName, tree)
        }

        if (type.name == SPRITE_CLASS) {
            return parseSprite(option, type, name, description, categoryName, subcategoryName, tree)
        }

        if (java.util.List::class.java.isAssignableFrom(type)) {
            return parseStringList(option, name, description, categoryName, subcategoryName, tree)
        }

        val visualizer: Class<out Visualizer> = when {
            type == java.lang.Boolean::class.java -> Visualizer.SwitchVisualizer::class.java
            type == Integer::class.java || type == java.lang.Double::class.java || type == java.lang.Float::class.java ->
                Visualizer.SliderVisualizer::class.java
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
            UUID.randomUUID().toString(),
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
            visualizer == Visualizer.SliderVisualizer::class.java -> {
                (invoke(option, "getMin") as? Number)?.let { property.addMetadata("min", it.toFloat()) }
                (invoke(option, "getMax") as? Number)?.let { property.addMetadata("max", it.toFloat()) }
                (invoke(option, "getIncrement") as? Number)?.let { property.addMetadata("step", it.toFloat()) }
            }
        }

        tree.put(property)
        return true
    }

    private const val COLOR_CLASS = "main.walksy.lib.core.config.local.options.type.WalksyLibColor"

    private fun parseColor(
        option: Any,
        colorClass: Class<*>,
        name: String,
        description: String?,
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

        fun toColor(wc: Any?): java.awt.Color? {
            wc ?: return null
            return runCatching {
                java.awt.Color(
                    getRed.invoke(wc) as Int,
                    getGreen.invoke(wc) as Int,
                    getBlue.invoke(wc) as Int,
                    getAlpha.invoke(wc) as Int,
                )
            }.getOrNull()
        }

        val property = Properties.functional(
            { runCatching { toColor(getValueM.invoke(option)) }.getOrNull() ?: java.awt.Color.WHITE },
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
            UUID.randomUUID().toString(),
            name,
            description,
            java.awt.Color::class.java,
        )

        property.addMetadata("visualizer", Visualizer.ColorVisualizer::class.java)
        toColor(invoke(option, "getDefaultValue"))?.let { property.addMetadata("default", it) }
        property.category = categoryName
        property.subcategory = subcategoryName
        tree.put(property)
        return true
    }

    private fun colorMethod(colorClass: Class<*>, name: String): java.lang.reflect.Method? =
        colorClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 }?.apply { isAccessible = true }

    private const val SPRITE_CLASS = "main.walksy.lib.core.utils.IdentifierWrapper"
    private const val CONFIG_MANAGER_CLASS = "main.walksy.lib.core.manager.WalksyLibConfigManager"
    private val SPRITE_TYPES = arrayOf("png")

    private fun parseSprite(
        option: Any,
        wrapperClass: Class<*>,
        name: String,
        description: String?,
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

        fun cachedDir(): java.nio.file.Path? = runCatching { cachedImageDirM.invoke(null) as? java.nio.file.Path }.getOrNull()

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
                    val source = java.io.File(path)
                    if (!source.isFile) return@runCatching
                    val fileName = source.name
                    val dest = cachedDir()?.resolve(fileName)
                    if (dest != null && source.toPath().toAbsolutePath() != dest.toAbsolutePath()) {
                        java.nio.file.Files.copy(source.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    }
                    Minecraft.getInstance().execute {
                        runCatching {
                            val identifier = loadTextureM.invoke(null, fileName) ?: return@execute
                            setValueM.invoke(option, wrapperCtor.newInstance(identifier, fileName))
                        }.onFailure { LOGGER.warn("Failed to apply WalksyLib sprite value", it) }
                    }
                }.onFailure { LOGGER.warn("Failed to set WalksyLib sprite value", it) }
            },
            UUID.randomUUID().toString(),
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

    private fun parseStringList(
        option: Any,
        name: String,
        description: String?,
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
            UUID.randomUUID().toString(),
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
        categoryName: String,
        subcategoryName: String?,
        tree: Tree,
    ): Boolean {
        val action = invoke(option, "getValue") as? Runnable ?: return false
        val property = Properties.dummy(UUID.randomUUID().toString(), name, description)
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

        val persist = method("onSave")
        val notify = method("runSave")
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
