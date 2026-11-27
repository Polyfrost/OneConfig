package org.polyfrost.oneconfig.internal.ui.components.settings

import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.MetaKeys
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.get
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.settings.DropdownOptionData
import org.polyfrost.oneconfig.internal.ui.api.settings.RadioButtonOptionData
import org.polyfrost.oneconfig.internal.ui.components.translated
import java.util.Locale
import java.util.Objects

private const val MAX_TEXT_DEFAULT_LENGTH = 24

internal fun optionDiffersFromDefault(prop: Property<*>): Boolean {
    val value = runCatching { prop.get() }.getOrNull()
    if (value is OneConfigKeybind) return false
    val default = prop.getMetadata<Any?>("default")
        ?: return prop.getMetadata<Boolean>("defaultNull") == true && value != null && prop.type != Void.TYPE && prop.type != Void::class.java
    return sameValue(value, default) == false
}

private fun sameValue(a: Any?, b: Any?): Boolean? = when {
    a === b -> true
    a == null || b == null -> false
    a is PolyColor && b is PolyColor -> a.rawArgb == b.rawArgb && a.chroma == b.chroma && a.chromaSpeed == b.chromaSpeed &&
        a.gradientEnabled == b.gradientEnabled && (!a.gradientEnabled || a.gradientArgb == b.gradientArgb)
    a is Number && b is Number -> a.toDouble() == b.toDouble()
    a is Boolean || a is String || a is Char || a is Enum<*> -> a == b
    a.javaClass.isArray && b.javaClass.isArray && a.javaClass.componentType.isPrimitive -> Objects.deepEquals(a, b)
    a is Collection<*> && b is Collection<*> && (a + b).all(::isSimple) -> a == b
    else -> null
}

private fun isSimple(value: Any?): Boolean =
    value == null || value is Number || value is Boolean || value is String || value is Char || value is Enum<*>

internal fun optionDefaultLabel(prop: Property<*>): String? {
    val default = prop.getMetadata<Any?>("default") ?: return null
    if (default is OneConfigKeybind) return null
    choiceLabel(prop, default)?.let { return it }
    return when (default) {
        is Boolean -> if (default) translated("oneconfig.control.on", "On") else translated("oneconfig.control.off", "Off")
        is PolyColor -> colorHex(default)
        is Float, is Double -> formatDecimal((default as Number).toDouble())
        is Number -> default.toString()
        is Enum<*> -> default.toString()
        is String -> default.takeIf { it.isNotBlank() && it.length <= MAX_TEXT_DEFAULT_LENGTH }
        else -> null
    }
}

private fun choiceLabel(prop: Property<*>, default: Any): String? {
    val labels = when {
        prop.isVisualizedBy(Visualizer.DropdownVisualizer::class.java) -> DropdownOptionData(prop).options
        prop.isVisualizedBy(Visualizer.RadioVisualizer::class.java) -> RadioButtonOptionData(prop).options
        else -> return null
    }.orEmpty()
    val enumClass = when {
        prop.type.isEnum -> prop.type
        prop.type.superclass?.isEnum == true -> prop.type.superclass
        else -> null
    }
    val optionValues = DropdownOptionData(prop).optionValues
    val index = when {
        enumClass != null -> (optionValues ?: enumClass.enumConstants.toList()).indexOf(default)
        default is String -> (optionValues ?: labels.map { it.asPlainLabel() }).indexOf(default)
        default is Number -> default.toInt()
        else -> -1
    }
    return labels.getOrNull(index)?.asPlainLabel() ?: (default as? Enum<*>)?.name
}

private fun Property<*>.isVisualizedBy(type: Class<out Visualizer>): Boolean = when (val visualizer = this[MetaKeys.VISUALIZER]) {
    is Class<*> -> type.isAssignableFrom(visualizer)
    is Visualizer -> type.isInstance(visualizer)
    else -> false
}

private fun formatDecimal(value: Double): String {
    if (value == Math.rint(value) && !value.isInfinite()) return value.toLong().toString()
    return String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
}

private fun colorHex(color: PolyColor): String {
    val argb = color.rawArgb
    return if (argb ushr 24 == 0xFF) "#%06X".format(argb and 0xFFFFFF) else "#%08X".format(argb.toLong() and 0xFFFFFFFFL)
}
