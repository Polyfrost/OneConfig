package org.polyfrost.oneconfig.internal.ui.api.settings

import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer

sealed class OptionData(val prop: Property<*>) {
    val title: String get() = prop.title ?: prop.id ?: ""
    val description: String? get() = prop.description
    val icon: String? get() = prop.getMetadata("icon")
}

class BooleanOptionData(prop: Property<*>, val style: Style) : OptionData(prop) {
    enum class Style { Switch, Checkbox }

    @Suppress("UNCHECKED_CAST")
    val boolProp: Property<Boolean> get() = prop as Property<Boolean>
}

class SliderOptionData(prop: Property<*>) : OptionData(prop) {
    val min: Float get() = prop.getMetadata("min") ?: 0f
    val max: Float get() = prop.getMetadata("max") ?: 100f
    val step: Float get() = prop.getMetadata("step") ?: 0f

    @Suppress("UNCHECKED_CAST")
    val numProp: Property<Number> get() = prop as Property<Number>
}

class NumberOptionData(prop: Property<*>) : OptionData(prop) {
    val min: Float get() = prop.getMetadata("min") ?: -10f
    val max: Float get() = prop.getMetadata("max") ?: 100f
    val unit: String? get() = prop.getMetadata("unit")
    val placeholder: String? get() = prop.getMetadata("placeholder")

    @Suppress("UNCHECKED_CAST")
    val numProp: Property<Number> get() = prop as Property<Number>
}

class TextOptionData(prop: Property<*>) : OptionData(prop) {
    val placeholder: String? get() = prop.getMetadata("placeholder")
    val regex: String? get() = prop.getMetadata("regex")

    @Suppress("UNCHECKED_CAST")
    val strProp: Property<String> get() = prop as Property<String>
}

class DropdownOptionData(prop: Property<*>) : OptionData(prop) {
    val options: Array<String>? get() = prop.getMetadata("options")
    val isEnum: Boolean get() = prop.type.isEnum || prop.type.superclass?.isEnum == true
}

class RadioButtonOptionData(prop: Property<*>) : OptionData(prop) {
    val options: Array<String>? get() = prop.getMetadata("options")
    val isEnum: Boolean get() = prop.type.isEnum || prop.type.superclass?.isEnum == true
}

class ColorOptionData(prop: Property<*>) : OptionData(prop)

class KeybindOptionData(prop: Property<*>) : OptionData(prop)

class ButtonOptionData(prop: Property<*>) : OptionData(prop) {
    val buttonText: String? get() = prop.getMetadata("text")
    val runnable: Runnable? get() = prop.getMetadata("runnable") ?: prop.getAs()
}

class InfoOptionData(prop: Property<*>) : OptionData(prop)

fun optionDataFrom(prop: Property<*>): OptionData? {
    val vis = prop.getMetadata<Class<*>>("visualizer")
    if (vis != null) {
        return when {
            Visualizer.SwitchVisualizer::class.java.isAssignableFrom(vis) ->
                BooleanOptionData(prop, BooleanOptionData.Style.Switch)
            Visualizer.CheckboxVisualizer::class.java.isAssignableFrom(vis) ->
                BooleanOptionData(prop, BooleanOptionData.Style.Checkbox)
            Visualizer.SliderVisualizer::class.java.isAssignableFrom(vis) ->
                SliderOptionData(prop)
            Visualizer.NumberVisualizer::class.java.isAssignableFrom(vis) ->
                NumberOptionData(prop)
            Visualizer.TextVisualizer::class.java.isAssignableFrom(vis) ->
                TextOptionData(prop)
            Visualizer.DropdownVisualizer::class.java.isAssignableFrom(vis) ->
                DropdownOptionData(prop)
            Visualizer.RadioVisualizer::class.java.isAssignableFrom(vis) ->
                RadioButtonOptionData(prop)
            Visualizer.ColorVisualizer::class.java.isAssignableFrom(vis) ->
                ColorOptionData(prop)
            Visualizer.KeybindVisualizer::class.java.isAssignableFrom(vis) ->
                KeybindOptionData(prop)
            Visualizer.ButtonVisualizer::class.java.isAssignableFrom(vis) ->
                ButtonOptionData(prop)
            Visualizer.InfoVisualizer::class.java.isAssignableFrom(vis) ->
                InfoOptionData(prop)
            else -> null
        }
    }
    // Type-based fallback when no visualizer metadata is set
    val type = prop.type ?: return null
    return when {
        type == Boolean::class.java || type == Boolean::class.javaPrimitiveType ->
            BooleanOptionData(prop, BooleanOptionData.Style.Switch)
        type == String::class.java ->
            TextOptionData(prop)
        type.isEnum ->
            DropdownOptionData(prop)
        Number::class.java.isAssignableFrom(type) ||
            type == Int::class.javaPrimitiveType ||
            type == Float::class.javaPrimitiveType ||
            type == Double::class.javaPrimitiveType ||
            type == Long::class.javaPrimitiveType ->
            NumberOptionData(prop)
        else -> null
    }
}
