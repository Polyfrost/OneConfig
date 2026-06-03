package org.polyfrost.oneconfig.internal.ui.api.settings

import org.polyfrost.oneconfig.api.config.v1.Property

sealed class OptionData(val prop: Property<*>) {
    val title: Any get() = prop.title ?: prop.id ?: ""
    val description: Any? get() = prop.description
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
    val buttonText: Any? get() = prop.getMetadata("text")
    val runnable: Runnable? get() = prop.getMetadata("runnable") ?: prop.getAs()
}

class InfoOptionData(prop: Property<*>) : OptionData(prop) {
    enum class Type { Info, Success, Warning, Error }

    /** The message body shown beside the icon. */
    val message: Any get() = prop.description ?: title

    val type: Type
        get() = when (prop.getMetadata<Any>("type")?.toString()?.lowercase()) {
            "success" -> Type.Success
            "warning" -> Type.Warning
            "error", "danger" -> Type.Error
            else -> Type.Info
        }
}

class DraggableListOptionData(prop: Property<*>) : OptionData(prop) {
    val options: Array<String>? get() = prop.getMetadata<Array<String>>("options")?.takeIf { it.isNotEmpty() }
    val checkable: Boolean get() = prop.getMetadata("checkable") ?: false
}

class MultiSelectDropdownOptionData(prop: Property<*>) : OptionData(prop) {
    val options: Array<String>? get() = prop.getMetadata<Array<String>>("options")?.takeIf { it.isNotEmpty() }
    val checkable: Boolean get() = prop.getMetadata("checkable") ?: true
}

