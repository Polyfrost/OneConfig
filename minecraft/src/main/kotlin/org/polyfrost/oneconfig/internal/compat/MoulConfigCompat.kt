//? if > 1.21.10 && fabric && moul_compat {

package org.polyfrost.oneconfig.internal.compat

import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.common.KeyboardConstants
import io.github.notenoughupdates.moulconfig.gui.editors.*
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.Visualizer.*
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId
import org.polyfrost.oneconfig.internal.utils.MoulConfigGuiOptionEditorDropdownAccessor
import java.awt.Color
import java.lang.reflect.Type
import kotlin.reflect.KClass
// do not remove this import even though the IDE marks it redundant
import org.polyfrost.oneconfig.internal.compat.MoulPropertyBuilder
import io.github.notenoughupdates.moulconfig.Config as MoulConfig
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig as Moulconfig

@Moulconfig
data object MoulConfigCompat {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/$this")

    @JvmStatic
    fun parseMoulconfig(processor: MoulConfigProcessor<*>, config: MoulConfig) {
        LOGGER.info("Loading compat.")
        register(processor.allCategories.values, config, "compat")
    }

    @JvmStatic
    fun parseMoulconfigFromEditor(categories: Collection<ProcessedCategory>, config: MoulConfig) {
        LOGGER.info("Loading editor compat.")
        register(categories, config, "editor compat")
    }

    private fun register(categories: Collection<ProcessedCategory>, config: MoulConfig, what: String) {
        val mod = CompatLoader.findFirstMod()
        if (mod != null && CompatLoader.nativeLoadedConfigs.contains(mod.id)) return
        mod?.id?.let { CompatLoader.nativeLoadedConfigs.add(it) }
        CompatLoader.requireTranslations(skip = true) {
            runCatching {
                CompatLoader.withForcedModId(mod?.id) {
                    CompatSnapshots.register(parseConfigTree(config, categories))
                }
            }.onFailure {
                LOGGER.error("Failed to load moulconfig $what for ${mod?.id ?: "unknown"} due to $it")
            }
        }
    }

    fun parseConfigTree(config: MoulConfig, children: Iterable<ProcessedCategory>): Tree = Tree.tree().apply {
        val map = mutableMapOf<String?, Tree>()
        val usedIds = HashSet<String>()
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
            val tree = parseCategory(config, it, this, usedIds) { parent -> map[parent] ?: this }
            map[it.identifier] = tree
            this.put(tree)
        }
    }

    fun parseCategory(
        config: MoulConfig,
        category: ProcessedCategory,
        root: Tree,
        usedIds: MutableSet<String>,
        parentResolver: (String?) -> Tree,
    ): Tree {
        val displayName = resolveDisplayName(category)
        val referenceParent = parentResolver(category.parentCategoryId)
        val categoryName = referenceParent.takeUnless { it === root }?.category ?: displayName

        val accordionMap = mutableMapOf<Int, Tree>()

        category.options.forEach { option ->
            parseOption(config, option, categoryName, displayName, root, accordionMap, usedIds)
        }

        return Tree.tree().apply {
            id = uniqueId(usedIds, idPart(category.identifier, "category"))
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
        usedIds: MutableSet<String>,
    ) {
        val editor = children.editor
        if (editor is GuiOptionEditorAccordion) {
            val builder = MoulPropertyBuilder(children)
            val accordionTree = Tree.tree()
            accordionTree.id = uniqueId(usedIds, idPart(builder.path ?: builder.name, "section"))
            accordionTree.title = builder.name?.takeIf { it.isNotBlank() } ?: "Section"
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

        val built = buildOptionProperty(config, children, categoryName, subcategoryName, usedIds) ?: return

        val parentTarget = if (children.accordionId >= 0) {
            accordionMap[children.accordionId] ?: root
        } else {
            root
        }
        parentTarget.put(built)
    }

    private fun buildOptionProperty(
        config: MoulConfig,
        children: ProcessedOption,
        categoryName: String,
        subcategoryName: String,
        usedIds: MutableSet<String>,
    ): Property<*>? {
        val property = MoulPropertyBuilder(children)

        @Suppress("DEPRECATION")
        val visualizer: Class<out Visualizer> = when (val editor = children.editor) {
            is GuiOptionEditorAccordion -> return null

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

            is GuiOptionEditorKeybind -> {
                // MoulConfig keeps a keybind as one int GLFW key code where a code <= 0 means unbound
                // the action is a no-op stub because MoulConfig owns the actual bind firing
                property.getter = {
                    val code = (children.get() as? Number)?.toInt() ?: KeyboardConstants.none
                    if (code <= 0) {
                        OneConfigKeybind(null, null, KeyModifiers.NONE, 0L) { true }
                    } else {
                        OneConfigKeybind(intArrayOf(code), null, KeyModifiers.NONE, 0L) { true }
                    }
                }
                property.setter = setter@{ value ->
                    val keybind = value as? OneConfigKeybind ?: return@setter
                    val code = keybind.keyCodes?.firstOrNull()
                        ?: keybind.mouseBtns?.firstOrNull()
                        ?: KeyboardConstants.none
                    children.set(code)
                }
                KeybindVisualizer::class.java
            }

            is GuiOptionEditorInfoText -> return null
            is GuiOptionEditorText -> TextVisualizer::class.java
            is GuiOptionEditorDraggableList -> return null
            else -> {
                println("Skipping ${children.path} - ${editor::class.java}")
                return null
            }
        }

        property.metadata["visualizer"] = visualizer
        val built = property.build(usedIds)
        built.category = categoryName
        built.subcategory = subcategoryName
        return built
    }

    @JvmStatic
    fun buildPropertiesForClass(
        processor: MoulConfigProcessor<*>,
        config: MoulConfig,
        declaringClass: Class<*>,
        category: String? = null,
        subcategory: String? = null,
    ): List<Property<*>> {
        val out = ArrayList<Property<*>>()
        val usedIds = HashSet<String>()
        runCatching {
            processor.allCategories.values.forEach { processed ->
                val catName = category ?: resolveDisplayName(processed)
                val subName = subcategory ?: catName
                processed.options.forEach { option ->
                    if (MoulPropertyBuilder(option).declaringClass != declaringClass) return@forEach
                    buildOptionProperty(config, option, catName, subName, usedIds)?.let(out::add)
                }
            }
        }.onFailure {
            LOGGER.error("Failed to build properties for ${declaringClass.name}: $it")
        }
        return out
    }

    @JvmStatic
    fun buildPropertiesForOptions(
        config: MoulConfig,
        options: List<ProcessedOption>,
        category: String,
        subcategory: String,
    ): List<Property<*>> {
        val out = ArrayList<Property<*>>()
        val usedIds = HashSet<String>()
        options.forEach { option ->
            runCatching { buildOptionProperty(config, option, category, subcategory, usedIds) }
                .onFailure { LOGGER.error("Failed to build property for ${option.path}: $it") }
                .getOrNull()?.let(out::add)
        }
        return out
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
//? }
