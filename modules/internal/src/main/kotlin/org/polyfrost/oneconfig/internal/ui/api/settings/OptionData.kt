package org.polyfrost.oneconfig.internal.ui.api.settings

import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.localizedString
import org.polyfrost.oneconfig.internal.ui.components.localizedText
import java.lang.reflect.Array as ReflectArray

sealed class OptionData(val prop: Property<*>) {
    val title: Any get() = localizedText(prop.getMetadata("titleKey"), prop.title ?: prop.id ?: "")
    val description: Any? get() = prop.localizedDescription()
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
    val unit: String? get() = localizedString(prop.getMetadata("unitKey"), prop.getMetadata<String>("unit")).takeIf { it.isNotBlank() }
    val placeholder: String? get() = localizedString(prop.getMetadata("placeholderKey"), prop.getMetadata<String>("placeholder")).takeIf { it.isNotBlank() }

    @Suppress("UNCHECKED_CAST")
    val numProp: Property<Number> get() = prop as Property<Number>
}

class TextOptionData(prop: Property<*>) : OptionData(prop) {
    val placeholder: String? get() = localizedString(prop.getMetadata("placeholderKey"), prop.getMetadata<String>("placeholder")).takeIf { it.isNotBlank() }
    val regex: String? get() = prop.getMetadata("regex")

    @Suppress("UNCHECKED_CAST")
    val strProp: Property<String> get() = prop as Property<String>
}

class DropdownOptionData(prop: Property<*>) : OptionData(prop) {
    val options: List<String>? get() = prop.optionLabels()
    val optionValues: List<String>? get() = prop.optionValues()
    val isEnum: Boolean get() = prop.type.isEnum || prop.type.superclass?.isEnum == true
}

class RadioButtonOptionData(prop: Property<*>) : OptionData(prop) {
    val options: List<String>? get() = prop.optionLabels()
    val isEnum: Boolean get() = prop.type.isEnum || prop.type.superclass?.isEnum == true
}

class ColorOptionData(prop: Property<*>) : OptionData(prop) {
    val alpha = prop.getMetadata<Any>("noAlpha") == null
}

class KeybindOptionData(prop: Property<*>) : OptionData(prop)

class ButtonOptionData(prop: Property<*>) : OptionData(prop) {
    val buttonText: Any? get() = localizedText(prop.getMetadata<Any?>("textKey")?.asRenderText(), prop.getMetadata<Any?>("text")?.asRenderText())
    val runnable: Runnable? get() = prop.getMetadata("runnable") ?: prop.getAs()
}

class InfoOptionData(prop: Property<*>) : OptionData(prop) {
    enum class Type { Info, Success, Warning, Error }

    /** The message body shown beside the icon. */
    val message: Any get() = description ?: title

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
    val optionLabels: Map<String, String> get() = prop.optionLabelMap()
    val checkable: Boolean get() = prop.getMetadata("checkable") ?: false
}

class MultiSelectDropdownOptionData(prop: Property<*>) : OptionData(prop) {
    val options: Array<String>? get() = prop.getMetadata<Array<String>>("options")?.takeIf { it.isNotEmpty() }
    val optionLabels: List<String>? get() = prop.optionLabels()
    val checkable: Boolean get() = prop.getMetadata("checkable") ?: true
}

private fun Property<*>.optionLabels(): List<String>? {
    // Explicit display labels (e.g. compat layers that resolve labels themselves) take precedence
    // over deriving them from the raw option values. Positionally aligned with "options".
    getMetadata<Any>("optionLabels").optionList().takeIf { it.isNotEmpty() }?.let { return it }

    val raw = getMetadata<Any>("options") ?: return null
    val keys = getMetadata<Any>("optionsKey").optionList()
    val labels = when {
        raw is Iterable<*> -> raw.map { it?.toString() ?: "" }
        raw.javaClass.isArray -> List(ReflectArray.getLength(raw)) { index ->
            ReflectArray.get(raw, index)?.toString() ?: ""
        }
        else -> return null
    }.mapIndexed { index, option -> localizedString(keys.getOrNull(index), option) }
        .filter { it.isNotBlank() }

    return labels.takeIf { it.isNotEmpty() }
}

private fun Property<*>.optionValues(): List<String>? = getMetadata<Any>("optionValues").optionList().takeIf { it.isNotEmpty() }

private fun Property<*>.optionLabelMap(): Map<String, String> {
    val options = getMetadata<Any>("options").optionList()
    val labels = optionLabels() ?: return emptyMap()
    return options.zip(labels).toMap()
}

private fun Any?.optionList(): List<String> {
    val raw = this ?: return emptyList()
    return when {
        raw is Iterable<*> -> raw.map { it?.toString() ?: "" }
        raw.javaClass.isArray -> List(ReflectArray.getLength(raw)) { index ->
            ReflectArray.get(raw, index)?.toString() ?: ""
        }
        else -> emptyList()
    }.filter { it.isNotBlank() }
}
