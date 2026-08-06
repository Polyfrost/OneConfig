package org.polyfrost.oneconfig.internal.ui.api.settings

import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.localizedString
import org.polyfrost.oneconfig.internal.ui.components.localizedText
import org.polyfrost.oneconfig.internal.ui.components.settings.formatSpinnerValue
import org.polyfrost.oneconfig.internal.ui.components.settings.toNumberType
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.roundToInt
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

/**
 * A start/end pair held in a single property, as a two-element numeric array or list. Both ends are written
 * together and in the container shape the property already holds, so the backing field's type is preserved.
 */
class RangeSliderOptionData(prop: Property<*>) : OptionData(prop) {
    val min: Float get() = prop.getMetadata("min") ?: 0f
    val max: Float get() = prop.getMetadata("max") ?: 100f
    val step: Float get() = prop.getMetadata("step") ?: 0f

    /** The current `start to end`, or null when the property does not hold a two-element numeric pair. */
    fun read(): Pair<Float, Float>? {
        val values = prop.readNumbers() ?: return null
        if (values.size < 2) return null
        return values[0] to values[1]
    }

    fun write(start: Float, end: Float) = prop.writeNumbers(listOf(start, end))
}

/**
 * An ordered, editable chain of numbers held in a single property, as a numeric array or list — reorder,
 * edit a value in place, remove an entry, or append the next one.
 *
 * [lockedLeading] entries at the front are fixed: they cannot be moved, edited or removed, which suits chains
 * that always begin from a known state. Whatever the widget writes still goes through the property's setter, so
 * a backing config is free to normalize further (dedupe, clamp, enforce a minimum length).
 */
class NumberChainOptionData(prop: Property<*>) : OptionData(prop) {
    val min: Float get() = prop.getMetadata("min") ?: 0f
    val max: Float get() = prop.getMetadata("max") ?: 100f
    val maxEntries: Int get() = prop.getMetadata("maxEntries") ?: Int.MAX_VALUE
    val lockedLeading: Int get() = prop.getMetadata("lockedLeading") ?: 0

    /** Drawn after the number while an entry is being edited. [label] is expected to include it already. */
    val unit: String? get() = localizedString(prop.getMetadata("unitKey"), prop.getMetadata<String>("unit")).takeIf { it.isNotBlank() }

    /** Renders one entry as a chip label. Falls back to the bare number. */
    fun label(value: Float): String {
        val labeller = prop.getMetadata<Function<Number, String>>("entryLabel")
            ?: return formatSpinnerValue(value)
        return runCatching { labeller.apply(value) }.getOrElse { formatSpinnerValue(value) }
    }

    /** The value "add" appends. Defaults to [max] when the property does not supply one. */
    fun nextValue(current: List<Float>): Float {
        val next = prop.getMetadata<Function<List<Number>, Number>>("nextValue") ?: return max
        return runCatching { next.apply(current).toFloat() }.getOrElse { max }
    }

    fun read(): List<Float> = prop.readNumbers() ?: emptyList()

    fun write(values: List<Float>) = prop.writeNumbers(values)
}

private fun Property<*>.readNumbers(): List<Float>? = when (val value = get()) {
    is FloatArray -> value.toList()
    is DoubleArray -> value.map { it.toFloat() }
    is IntArray -> value.map { it.toFloat() }
    is LongArray -> value.map { it.toFloat() }
    is Array<*> -> value.map { (it as? Number)?.toFloat() ?: return null }
    is Iterable<*> -> value.map { (it as? Number)?.toFloat() ?: return null }
    else -> null
}

/** Writes [values] back in whatever container shape the property already holds. */
@Suppress("UNCHECKED_CAST")
private fun Property<*>.writeNumbers(values: List<Float>) {
    val written: Any = when (val current = get()) {
        is FloatArray -> values.toFloatArray()
        is DoubleArray -> values.map { it.toDouble() }.toDoubleArray()
        is IntArray -> values.map { it.roundToInt() }.toIntArray()
        is LongArray -> values.map { it.toLong() }.toLongArray()
        is Array<*> -> {
            val component = current.javaClass.componentType
            ReflectArray.newInstance(component, values.size).also { array ->
                values.forEachIndexed { index, value -> ReflectArray.set(array, index, value.toNumberType(component)) }
            }
        }
        is List<*> -> {
            val component = (current.firstOrNull() ?: 0f).javaClass
            values.map { it.toNumberType(component) }
        }
        else -> return
    }
    (this as Property<Any>).set(written)
}

/**
 * A slider whose value may instead be "inherited" from a broader setting.
 *
 * While inheriting, the property holds [inheritSentinel] (`null` unless the metadata says otherwise) and the
 * slider shows [inheritedValue] greyed out. Picking a value writes it normally; clearing goes back to the
 * sentinel. [inheritedValue] may be given as a `Supplier` so it tracks whatever it inherits from.
 */
class InheritableSliderOptionData(prop: Property<*>) : OptionData(prop) {
    val min: Float get() = prop.getMetadata("min") ?: 0f
    val max: Float get() = prop.getMetadata("max") ?: 100f
    val step: Float get() = prop.getMetadata("step") ?: 0f
    val inheritLabel: String
        get() = localizedString(prop.getMetadata("inheritLabelKey"), prop.getMetadata<String>("inheritLabel"))
            .takeIf { it.isNotBlank() } ?: "Inherit"

    private val inheritSentinel: Any? get() = prop.getMetadata("inheritSentinel")

    val inheritedValue: Float
        get() = when (val inherited = prop.getMetadata<Any>("inheritedValue")) {
            is Number -> inherited.toFloat()
            is Supplier<*> -> (inherited.get() as? Number)?.toFloat() ?: min
            else -> min
        }

    val isInherited: Boolean
        get() {
            val value = prop.get() ?: return true
            val sentinel = inheritSentinel ?: return false
            return value is Number && sentinel is Number && value.toDouble() == sentinel.toDouble()
        }

    /** The value the slider should show: the explicit one, or what it inherits while unset. */
    val shownValue: Float get() = if (isInherited) inheritedValue else (prop.get() as? Number)?.toFloat() ?: inheritedValue

    @Suppress("UNCHECKED_CAST")
    fun set(value: Float) = (prop as Property<Any>).set(value.toNumberType(prop.type))

    @Suppress("UNCHECKED_CAST")
    fun inherit() = (prop as Property<Any?>).set(inheritSentinel?.let { (it as? Number)?.toFloat()?.toNumberType(prop.type) })
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
    val multiline: Boolean get() = prop.getMetadata("multiline") ?: false

    @Suppress("UNCHECKED_CAST")
    val strProp: Property<String> get() = prop as Property<String>
}

class TextListOptionData(prop: Property<*>) : ListOptionData(prop) {
    val placeholder: String? get() = localizedString(prop.getMetadata("placeholderKey"), prop.getMetadata<String>("placeholder")).takeIf { it.isNotBlank() }
    val regex: String? get() = prop.getMetadata<String>("regex")?.takeIf { it.isNotBlank() }

    fun strings(): List<String> = elements().map { it?.toString() ?: "" }
}

class ItemListOptionData(prop: Property<*>) : ListOptionData(prop) {
    fun ids(): List<String> = org.polyfrost.oneconfig.internal.ui.components.settings.item.normalizeItemIds(
        elements().mapNotNull { it as? String }
    )

    fun setIds(values: List<String>) = prop.setItemListElements(
        org.polyfrost.oneconfig.internal.ui.components.settings.item.normalizeItemIds(values)
    )
}

class FileOptionData(prop: Property<*>) : OptionData(prop) {
    val placeholder: String? get() = localizedString(prop.getMetadata("placeholderKey"), prop.getMetadata<String>("placeholder")).takeIf { it.isNotBlank() }
    val directory: Boolean get() = prop.getMetadata("directory") ?: false
    val filterName: String? get() = prop.getMetadata<String>("filterName")?.takeIf { it.isNotBlank() }

    val dialogTitle: String get() = title.asRenderText()

    val filterPatterns: Array<String> get() = prop.filterPatterns(directory)

    @Suppress("UNCHECKED_CAST")
    val strProp: Property<String> get() = prop as Property<String>
}

sealed class ListOptionData(prop: Property<*>) : OptionData(prop) {
    val maxEntries: Int get() = prop.getMetadata<Int>("maxEntries") ?: 0
    val reorderable: Boolean get() = prop.getMetadata("reorderable") ?: true
    val addText: Any get() = localizedText(prop.getMetadata("addTextKey"), prop.getMetadata<String>("addText")?.takeIf { it.isNotBlank() } ?: "Add")

    val isList: Boolean get() = java.util.List::class.java.isAssignableFrom(prop.type)

    fun elements(): List<Any?> = prop.listElements()

    fun setElements(values: List<Any?>) = prop.setListElements(values)
}

class FileListOptionData(prop: Property<*>) : ListOptionData(prop) {
    val placeholder: String? get() = localizedString(prop.getMetadata("placeholderKey"), prop.getMetadata<String>("placeholder")).takeIf { it.isNotBlank() }
    val directory: Boolean get() = prop.getMetadata("directory") ?: false
    val filterName: String? get() = prop.getMetadata<String>("filterName")?.takeIf { it.isNotBlank() }

    val dialogTitle: String get() = title.asRenderText()

    val filterPatterns: Array<String> get() = prop.filterPatterns(directory)

    fun strings(): List<String> = elements().map { it?.toString() ?: "" }
}

class ColorListOptionData(prop: Property<*>) : ListOptionData(prop) {
    val alpha: Boolean get() = prop.getMetadata<Any>("noAlpha") == null

    fun argb(): List<Int> = elements().map {
        when (it) {
            is Number -> it.toInt()
            is java.awt.Color -> it.rgb
            else -> -1
        }
    }

    fun setArgb(values: List<Int>) {
        val component = if (prop.type.isArray) prop.type.componentType else Integer::class.java
        setElements(
            if (component == java.awt.Color::class.java) values.map { java.awt.Color(it, true) } else values
        )
    }
}

class NumberListOptionData(prop: Property<*>, val style: Style) : ListOptionData(prop) {
    enum class Style { Slider, Spinner }

    val min: Float get() = prop.getMetadata("min") ?: 0f
    val max: Float get() = prop.getMetadata("max") ?: 100f
    val step: Float get() = prop.getMetadata("step") ?: 0f

    val elementType: Class<*>
        get() {
            if (prop.type.isArray) return prop.type.componentType
            elements().firstOrNull { it is Number }?.let { return it.javaClass }
            val whole = min == min.toInt().toFloat() && max == max.toInt().toFloat() &&
                    step == step.toInt().toFloat()
            return if (whole) Integer::class.java else java.lang.Double::class.java
        }

    fun floats(): List<Float> = elements().map { (it as? Number)?.toFloat() ?: min }
}

class DropdownOptionData(prop: Property<*>) : OptionData(prop) {
    val options: List<Any>? get() = prop.optionLabels()
    val optionValues: List<Any>? get() = prop.optionValues()
    val isEnum: Boolean get() = prop.type.isEnum || prop.type.superclass?.isEnum == true
}

class RadioButtonOptionData(prop: Property<*>) : OptionData(prop) {
    val options: List<Any>? get() = prop.optionLabels()
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

    /** Whether a real title was set (not the default placeholder). */
    private val hasTitle: Boolean
        get() = !prop.getMetadata<String>("titleKey").isNullOrBlank() ||
                (prop.title != null && prop.title != "polyui.info")

    private val descriptionParts: Pair<String?, String?>
        get() {
            val lines = description?.asRenderText()?.lines() ?: return null to null
            return lines.firstOrNull()?.takeIf { it.isNotBlank() } to
                    lines.drop(1).joinToString("\n").takeIf { it.isNotBlank() }
        }

    /** The bold heading shown beside the icon. Falls back to the description when no title was set. */
    val heading: Any get() = if (hasTitle) title else (descriptionParts.first ?: title)

    /** The secondary line shown below the heading, or null when no title was set. */
    val body: Any? get() = if (hasTitle) description?.asRenderText() else descriptionParts.second

    /** The message body shown beside the icon. */
    @Deprecated("Use heading/body", ReplaceWith("heading"))
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
    val optionLabels: Map<String, Any> get() = prop.optionLabelMap()
    val checkable: Boolean get() = prop.getMetadata("checkable") ?: false
}

class MultiSelectDropdownOptionData(prop: Property<*>) : OptionData(prop) {
    val options: Array<String>? get() = prop.getMetadata<Array<String>>("options")?.takeIf { it.isNotEmpty() }
    val optionLabels: List<Any>? get() = prop.optionLabels()
    val checkable: Boolean get() = prop.getMetadata("checkable") ?: true
}

private fun Property<*>.filterPatterns(directory: Boolean): Array<String> {
    if (directory) return emptyArray()
    val raw = getMetadata<Array<String>>("types") ?: return emptyArray()
    return raw.mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toFilterPattern() }.toTypedArray()
}

private fun String.toFilterPattern(): String = when {
    startsWith("*.") -> this
    this == "*" -> this
    startsWith(".") -> "*$this"
    else -> "*.$this"
}

private fun Property<*>.listElements(): List<Any?> {
    val value = get() ?: return emptyList()
    return when {
        value.javaClass.isArray -> List(ReflectArray.getLength(value)) { ReflectArray.get(value, it) }
        value is Iterable<*> -> value.toList()
        else -> emptyList()
    }
}

@Suppress("UNCHECKED_CAST")
private fun Property<*>.setListElements(values: List<Any?>) {
    val prop = this as Property<Any>
    if (!type.isArray) {
        prop.set(ArrayList(values))
        return
    }
    val component = type.componentType
    val array = ReflectArray.newInstance(component, values.size)
    values.forEachIndexed { index, value ->
        ReflectArray.set(array, index, if (value is Number) value.toComponentType(component) else value)
    }
    prop.set(array)
}

@Suppress("UNCHECKED_CAST")
private fun Property<*>.setItemListElements(values: List<String>) {
    if (type.isArray) {
        (this as Property<Any>).set(values.toTypedArray())
        return
    }

    val replacement = ArrayList(values)
    if (type.isInstance(replacement)) {
        (this as Property<Any>).set(replacement)
        return
    }

    val candidates = listOfNotNull(get()?.javaClass, type).distinct()
    for (candidate in candidates) {
        if (!type.isAssignableFrom(candidate)) continue

        runCatching {
            candidate.getConstructor(Collection::class.java).newInstance(values)
        }.getOrNull()?.let { instance ->
            if (instance is List<*>) {
                (this as Property<Any>).set(instance)
                return
            }
        }

        runCatching {
            candidate.getConstructor().newInstance()
        }.getOrNull()?.let { instance ->
            if (instance is MutableList<*>) {
                (instance as MutableList<Any?>).addAll(values)
                (this as Property<Any>).set(instance)
                return
            }
        }
    }

    throw IllegalArgumentException("List property type ${type.name} cannot be replaced with an editable list")
}

private fun Number.toComponentType(component: Class<*>): Any = when (component) {
    Integer.TYPE, Integer::class.java -> toInt()
    java.lang.Long.TYPE, java.lang.Long::class.java -> toLong()
    java.lang.Double.TYPE, java.lang.Double::class.java -> toDouble()
    java.lang.Float.TYPE, java.lang.Float::class.java -> toFloat()
    java.lang.Short.TYPE, java.lang.Short::class.java -> toShort()
    java.lang.Byte.TYPE, java.lang.Byte::class.java -> toByte()
    else -> this
}

private fun Property<*>.optionLabels(): List<Any>? {
    // Explicit display labels (e.g. compat layers that resolve labels themselves) take precedence
    // over deriving them from the raw option values. Positionally aligned with "options".
    getMetadata<Any>("optionLabels").optionList().takeIf { it.isNotEmpty() }?.let { return it }

    val raw = getMetadata<Any>("options") ?: return null
    val keys = getMetadata<Any>("optionsKey").optionList().map { it.toString() }
    val labels = when {
        raw is Iterable<*> -> raw.map { it ?: "" }
        raw.javaClass.isArray -> List(ReflectArray.getLength(raw)) { index ->
            ReflectArray.get(raw, index) ?: ""
        }
        else -> return null
    }.mapIndexed { index, option -> localizedText(keys.getOrNull(index), option) }.filter { it.asRenderText().isNotBlank() }

    return labels.takeIf { it.isNotEmpty() }
}

private fun Property<*>.optionValues(): List<Any>? = getMetadata<Any>("optionValues").optionList().takeIf { it.isNotEmpty() }

private fun Property<*>.optionLabelMap(): Map<String, Any> {
    val options = getMetadata<Any>("options").optionList().map { it.toString() }
    val labels = optionLabels() ?: return emptyMap()
    return options.zip(labels).toMap()
}

private fun Any?.optionList(): List<Any> {
    val raw = this ?: return emptyList()
    return when {
        raw is Iterable<*> -> raw.map { it ?: "" }
        raw.javaClass.isArray -> List(ReflectArray.getLength(raw)) { index ->
            ReflectArray.get(raw, index) ?: ""
        }
        else -> emptyList()
    }.filter { it.asRenderText().isNotBlank() }
}
