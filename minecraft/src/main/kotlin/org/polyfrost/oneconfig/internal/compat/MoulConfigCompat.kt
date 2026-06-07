//? if > 1.21.10 && fabric && moul_compat {

/*package org.polyfrost.oneconfig.internal.compat

import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.gui.editors.*
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.Visualizer.*
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.internal.utils.MoulConfigGuiOptionEditorDropdownAccessor
import java.awt.Color
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass
// do not remove the im
import org.polyfrost.oneconfig.internal.compat.MoulPropertyBuilder
import io.github.notenoughupdates.moulconfig.Config as MoulConfig
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig as Moulconfig

@Moulconfig
data object MoulConfigCompat {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/$this")

    @JvmStatic
    fun parseMoulconfig(processor: MoulConfigProcessor<*>, config: MoulConfig) {
        LOGGER.info("Loading compat.")
        runCatching {
            val categories = processor.allCategories.values
            val tree = parseConfigTree(config, categories)
            ConfigManager.active().register(tree)
            CompatLoader.markFirstModAsSkip()
        }.onFailure {
            LOGGER.error("Failed to load moulconfig compat for $this due to $it")
        }
    }

    @JvmStatic
    fun parseMoulconfigFromEditor(categories: Collection<ProcessedCategory>, config: MoulConfig) {
        LOGGER.info("Loading editor compat.")
        val mod = CompatLoader.findFirstMod()
        if (mod != null && CompatLoader.nativeLoadedConfigs.contains(mod.id)) {
            return
        }
        runCatching {
            val tree = parseConfigTree(config, categories)
            ConfigManager.active().register(tree)
            CompatLoader.markFirstModAsSkip()
        }.onFailure {
            LOGGER.error("Failed to load moulconfig editor compat for $this due to $it")
        }
    }

    @JvmStatic
    fun parseMoulconfigFromUnknownEditor(categories: Collection<*>, config: Any?) {
        if (config == null) return
        val configClass = config::class.java.name
        val (candidates, forcedModIds) = when {
            configClass.startsWith("moe.nea.firmament.deps.moulconfig.") ->
                listOf("MoulConfigCompat_firmament") to listOf("firmament")

            configClass.startsWith("moe.nea.firmament.compat.moulconfig.") ->
                listOf("MoulConfigCompat_firmament") to listOf("firmament")

            configClass.startsWith("net.azureaaron.dandelion_bp.deps.moulconfig.") ->
                listOf("MoulConfigCompat_dandelion_bp", "MoulConfigCompat_dandelion") to listOf(
                    "skyblocker",
                    "dandelion-bp"
                )

            configClass.startsWith("net.azureaaron.dandelion_bp.impl.moulconfig.") ->
                listOf("MoulConfigCompat_dandelion_bp", "MoulConfigCompat_dandelion") to listOf(
                    "skyblocker",
                    "dandelion-bp"
                )

            configClass.startsWith("net.azureaaron.dandelion.deps.moulconfig.") ->
                listOf("MoulConfigCompat_dandelion") to listOf("skyblocker", "dandelion-bp")

            configClass.startsWith("at.hannibal2.skyhanni.deps.moulconfig.") ->
                listOf("MoulConfigCompat_skyhanni") to listOf("skyhanni")

            else -> emptyList<String>() to emptyList()
        }
        if (candidates.isEmpty()) return
        val forcedModId = forcedModIds.firstOrNull { CompatLoader.hasMod(it) }

        val basePackage = MoulConfigCompat::class.java.`package`.name
        for (candidate in candidates) {
            val fqcn = "$basePackage.$candidate"
            runCatching {
                val compatClass = Class.forName(fqcn)
                val method = compatClass.methods.firstOrNull {
                    it.name == "parseMoulconfigFromEditor" && it.parameterCount == 2
                } ?: error("parseMoulconfigFromEditor not found on $fqcn")
                CompatLoader.withForcedModId(forcedModId) {
                    method.invoke(null, categories, config)
                }
                return
            }
        }
    }

    fun parseConfigTree(config: MoulConfig, children: Iterable<ProcessedCategory>): Tree = Tree.tree().apply {
        val map = mutableMapOf<String?, Tree>()
        val mod = CompatLoader.findFirstMod()
        LOGGER.info("Loading for ${mod?.id ?: "unknown"}")
        this.id = mod?.id ?: config.toString()
        this.saveFunction = Runnable { config.saveNow() }
        this.noCache = true
        this.title = mod?.name?.takeIf { it.isNotBlank() } ?: config::class.java.simpleName.takeIf { it.isNotBlank() }
                ?: "MoulConfig"
        mod?.extractIconFile()?.let {
            this.addMetadata("icon_path", it)
        }

        children.forEach {
            val tree = parseCategory(config, it, this) { parent -> map[parent] ?: this }
            map[it.identifier] = tree
            this.put(tree)
        }
    }

    fun parseCategory(
        config: MoulConfig,
        category: ProcessedCategory,
        root: Tree,
        parentResolver: (String?) -> Tree,
    ): Tree {
        val displayName = resolveDisplayName(category)
        val referenceParent = parentResolver(category.parentCategoryId)
        val categoryName = referenceParent.takeUnless { it === root }?.category ?: displayName

        val accordionMap = mutableMapOf<Int, Tree>()

        category.options.forEach { option ->
            parseOption(config, option, categoryName, displayName, root, accordionMap)
        }

        return Tree.tree().apply {
            id = UUID.randomUUID().toString()
            this.category = categoryName
            this.title = displayName
            this.subcategory = displayName
        }
    }

    fun parseOption(
        config: MoulConfig,
        children: ProcessedOption,
        categoryName: String,
        subcategoryName: String,
        root: Tree,
        accordionMap: MutableMap<Int, Tree>,
    ) {
        val property = MoulPropertyBuilder(children)

        @Suppress("DEPRECATION")
        val visualizer: Class<out Visualizer> = when (val editor = children.editor) {
            is GuiOptionEditorAccordion -> {
                val accordionTree = Tree.tree()
                accordionTree.id = UUID.randomUUID().toString()
                accordionTree.title = property.name?.takeIf { it.isNotBlank() } ?: "Section"
                accordionTree.category = categoryName
                accordionTree.subcategory = subcategoryName

                val parentTarget = if (children.accordionId >= 0) {
                    accordionMap[children.accordionId] ?: root
                } else {
                    root
                }
                parentTarget.put(accordionTree)
                accordionMap[editor.accordionId] = accordionTree
                return
            }

            is GuiOptionEditorBoolean -> SwitchVisualizer::class.java
            is GuiOptionEditorButton -> {
                property.metadata["runnable"] = Runnable { editor.onClick() }
                ButtonVisualizer::class.java
            }

            is GuiOptionEditorColour -> {
                property.getter = {
                    val colour = when (children.type) {
                        String::class.java -> ChromaColour.forLegacyString(children.get() as String)
                        ChromaColour::class.java -> children.get() as ChromaColour
                        else -> null
                    }
                    colour?.let {
                        val rgb = Color.HSBtoRGB(it.hue, it.saturation, it.brightness)
                        (it.alpha shl 24) or (rgb and 0x00FFFFFF)
                    } ?: 0xFFFFFFFF.toInt()
                }
                property.setter = setter@{
                    val argb = it as? Int ?: return@setter
                    val awtColor = Color(argb, true)
                    val hsb = Color.RGBtoHSB(awtColor.red, awtColor.green, awtColor.blue, null)
                    val colour = ChromaColour(hsb[0], hsb[1], hsb[2], 0, awtColor.alpha)
                    when (children.type) {
                        String::class.java -> children.set(colour.toLegacyString())
                        ChromaColour::class.java -> children.set(colour)
                    }
                }
                ColorVisualizer::class.java
            }

            is MoulConfigGuiOptionEditorDropdownAccessor -> {
                fun getIndex(): Int {
                    val selectedObject: Any = children.get() ?: return -1

                    return if (editor.`oneconfig$useOrdinal`()) {
                        selectedObject as Int
                    } else if (editor.`oneconfig$constants`() != null) {
                        (selectedObject as Enum<*>).ordinal
                    } else {
                        editor.`oneconfig$values`().indexOf(selectedObject)
                    }
                }

                fun setIndex(index: Int) {
                    if (editor.`oneconfig$constants`() != null) {
                        children.set(editor.`oneconfig$constants`()[index])
                    } else if (editor.`oneconfig$useOrdinal`()) {
                        children.set(index)
                    } else {
                        children.set(editor.`oneconfig$values`()[index])
                    }
                }

                property.getter = ::getIndex
                property.setter = setter@{
                    val index = it as? Int ?: return@setter
                    setIndex(index)
                }

                property.metadata["options"] = editor.`oneconfig$values`()

                DropdownVisualizer::class.java
            }


            is GuiOptionEditorSliderAccessor -> {
                property.metadata["min"] = editor.`oneconfig$minValue`
                property.metadata["max"] = editor.`oneconfig$maxValue`
                property.getter = { (children.get() as? Number)?.toFloat() ?: editor.`oneconfig$maxValue` }
                property.setter = setter@{ value ->
                    val numberValue = value as? Number ?: return@setter
                    fun isAny(type: Type, numberType: KClass<out Number>): Boolean {
                        return numberType == type || numberType.java == type || numberType.javaObjectType == type || numberType.javaPrimitiveType == type
                    }

                    val type = children.type
                    when {
                        isAny(type, Int::class) -> children.set(numberValue.toInt())
                        isAny(type, Float::class) -> children.set(numberValue.toFloat())
                        isAny(type, Short::class) -> children.set(numberValue.toShort())
                        isAny(type, Long::class) -> children.set(numberValue.toLong())
                        isAny(type, Double::class) -> children.set(numberValue.toDouble())
                        else -> null
                    }
                }

                SliderVisualizer::class.java
            }

            is GuiOptionEditorInfoText -> return
            is GuiOptionEditorText -> TextVisualizer::class.java
            is GuiOptionEditorDraggableList -> return
            else -> {
                println("Skipping ${children.path} - ${editor::class.java}")
                return
            }
        }

        property.metadata["visualizer"] = visualizer
        val built = property.build()
        built.category = categoryName
        built.subcategory = subcategoryName

        val parentTarget = if (children.accordionId >= 0) {
            accordionMap[children.accordionId] ?: root
        } else {
            root
        }
        parentTarget.put(built)
    }

    private fun resolveDisplayName(category: ProcessedCategory): String {
        val raw = runCatching {
            category::class.java.getMethod("getDisplayName").invoke(category)
        }.getOrNull()
        return resolveText(raw) ?: category.identifier
    }

    private fun resolveText(value: Any?): String? {
        if (value == null) return null
        if (value is String) return value
        val fromGetText = runCatching {
            value::class.java.getMethod("getText").invoke(value)
        }.getOrNull()
        return when (fromGetText) {
            null -> value.toString()
            is String -> fromGetText
            else -> fromGetText.toString()
        }
    }

}
*///? }
