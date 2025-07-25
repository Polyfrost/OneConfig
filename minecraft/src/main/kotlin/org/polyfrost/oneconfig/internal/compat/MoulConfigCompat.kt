package org.polyfrost.oneconfig.internal.compat

import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.gui.editors.*
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.Visualizer.*
import org.polyfrost.oneconfig.internal.DynamicPolyImage
import org.polyfrost.oneconfig.internal.mixin.compat.moulconfig.Accessor_GuiOptionEditorDropdown
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig as Moulconfig
import org.polyfrost.oneconfig.utils.v1.dsl.*
import org.polyfrost.polyui.color.PolyColor
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import io.github.notenoughupdates.moulconfig.Config as MoulConfig

@Moulconfig
object MoulConfigCompat {

    @JvmStatic
    fun parseMoulconfig(processor: MoulConfigProcessor<*>, config: MoulConfig) {
        CompatLoader.markFirstModAsSkip()
        ConfigManager.active().register(parseConfigTree(config, processor.allCategories.values))
    }

    fun parseConfigTree(config: MoulConfig, children: Iterable<ProcessedCategory>): Tree = Tree.tree().apply {
        val map = mutableMapOf<String?, Tree>()
        val mod = CompatLoader.findFirstMod()
        this.id = mod?.id ?: config.toString()
        this.saveFunction = Runnable { config.saveNow() }
        this.noCache = true
        this.title = mod?.name ?: ""
        mod?.let {
            val path = it.iconPath ?: return@let
            val stream = it.icon ?: return@let
            this.icon = DynamicPolyImage(path, stream)
        }

        children.forEach {
            val tree = parseCategory(config, it, this) { parent -> map[parent] ?: this }
            map[it.identifier] = tree
        }
    }

    fun parseCategory(
        config: MoulConfig,
        category: ProcessedCategory,
        root: Tree,
        parentResolver: (String?) -> Tree,
    ): Tree {
        val tree = Tree.tree()
        val parent = parentResolver(category.parentCategoryId)
        tree.id = UUID.randomUUID().toString()
        tree.title = category.displayName
        tree.category = parent.takeUnless { it === root }?.category ?: category.displayName
        tree.subcategory = category.displayName

        val map = mutableMapOf<Int?, Tree>()

        category.options.forEach { category ->
            val (id, node) = parseOption(config, category) { parent -> map[parent] ?: tree } ?: return@forEach
            map[id] = node
        }

        parent.put(tree)
        return tree
    }

    fun parseOption(config: MoulConfig, children: ProcessedOption, parentResolver: (Int?) -> Tree): (Pair<Int, Tree>)? {
        val property = MoulPropertyBuilder(children)

        val editor = children.editor

        // moulconfig uses a few deprecated things internally, to fully support it we need to carry those over.
        @Suppress("DEPRECATION")
        val visualizer: Class<out Visualizer> = when (editor) {
            is GuiOptionEditorAccordion -> return children.accordionId to Tree.tree()
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
                        PolyColor.Chroma(
                            it.hue,
                            it.saturation,
                            it.brightness,
                            it.alpha / 255f,
                            it.timeForFullRotationInMillis.milliseconds.inWholeNanoseconds
                        )
                    } ?: PolyColor.WHITE
                }
                property.setter = setter@{
                    val color = it as? PolyColor ?: return@setter
                    val colour = ChromaColour(
                        color.hue,
                        color.saturation,
                        color.brightness,
                        (color as? PolyColor.Chroma)?.speedNanos?.nanoseconds?.inWholeMilliseconds?.toInt() ?: 1000,
                        (color.alpha * 255).toInt().coerceIn(0..255)
                    )

                    when (children.type) {
                        String::class.java -> children.set(colour.toLegacyString())
                        ChromaColour::class.java -> children.set(colour)
                    }
                }
                ColorVisualizer::class.java
            }

            is Accessor_GuiOptionEditorDropdown -> {
                fun getIndex(): Int {
                    val selectedObject: Any? = children.get()
                    if (selectedObject == null) return -1

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
                        else -> null // do nothing/unknown number format?
                    }
                }

                SliderVisualizer::class.java
            }

            is GuiOptionEditorInfoText -> return null
            is GuiOptionEditorText -> TextVisualizer::class.java
            is GuiOptionEditorDraggableList -> return null
            else -> {
                println("Skipping ${children.path} - ${editor::class.java}")
                return null // editor type either unsupported or unknown
            }
        }

        property.metadata["visualizer"] = visualizer
        parentResolver(null).put(property.build())
        return null
    }

}

