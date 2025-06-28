package org.polyfrost.oneconfig.utils.v1.internal.compat

import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.gui.editors.*
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.Visualizer.*
import org.polyfrost.oneconfig.internal.mixin.compat.moulconfig.Accessor_GuiOptionEditorDropdown
import org.polyfrost.polyui.color.PolyColor
import java.lang.reflect.Type
import kotlin.jvm.java
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import io.github.notenoughupdates.moulconfig.Config as MoulConfig

internal interface GuiOptionEditorSliderAccessor {
    val `oneconfig$minValue`: Float
    val `oneconfig$maxValue`: Float
    val `oneconfig$minStep`: Float
}

object MoulConfigCompat {

    @JvmStatic
    fun parseMoulconfig(processor: MoulConfigProcessor<*>, config: MoulConfig) {
        ConfigManager.active().register(parseConfigTree(config, processor.allCategories.values))
    }

    fun parseConfigTree(config: MoulConfig, children: Iterable<ProcessedCategory>): Tree = Tree.tree().apply {
        val map = mutableMapOf<String?, Tree>()

        children.forEach {
            val tree = parseCategory(config, it) { parent -> map[parent] ?: this } ?: return@forEach
            map[it.identifier] = tree
        }
    }

    fun parseCategory(config: MoulConfig, category: ProcessedCategory, parentResolver: (String?) -> Tree): Tree? {
        val tree = Tree.tree()
        val map = mutableMapOf<Int?, Tree>()

        category.options.forEach { category ->
            val (id, node) = parseOption(config, category) { parent -> map[parent] ?: tree } ?: return@forEach
            map[id] = node
        }

        parentResolver(category.parentCategoryId).put(tree)
        return tree
    }

    fun parseOption(config: MoulConfig, children: ProcessedOption, parentResolver: (Int?) -> Tree): (Pair<Int, Tree>)? {
        val property = MoulConfigPropertyBuilder(children)


        val editor = children.editor

        // moulconfig uses a few deprecated things internally, to fully support it we need to carry those over.
        @Suppress("DEPRECATION")
        val visualizer: KClass<out Visualizer> = when (editor) {
            is GuiOptionEditorAccordion -> return children.accordionId to Tree.tree()
            is GuiOptionEditorBoolean -> SwitchVisualizer::class
            is GuiOptionEditorButton -> {
                property.metadata["runnable"] = editor::onClick
                ButtonVisualizer::class
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
                ColorVisualizer::class
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

                DropdownVisualizer::class
            }

            is GuiOptionEditorInfoText -> {
                InfoVisualizer::class // TODO
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

                SliderVisualizer::class
            }

            is GuiOptionEditorText -> TextVisualizer::class
            is GuiOptionEditorDraggableList -> DraggableListVisualizer::class
            else -> return null // editor type either unsupported or unknown
        }

        property.metadata["visualizer"] = visualizer
        parentResolver(children.accordionId).put(property.build())
        return null
    }

    class MoulConfigPropertyBuilder internal constructor(option: ProcessedOption) {
        val name: String? = option.name
        val description: String? = option.description

        var setter: (Any) -> Unit = option::set
        var getter: () -> Any = option::get

        val metadata: MutableMap<String, Any> = mutableMapOf()

        fun build() = Properties.functional(getter, setter, name = null, description = description).apply {
            this@MoulConfigPropertyBuilder.metadata.entries.forEach { (key, value) -> addMetadata(key, value) }
        }
    }

}