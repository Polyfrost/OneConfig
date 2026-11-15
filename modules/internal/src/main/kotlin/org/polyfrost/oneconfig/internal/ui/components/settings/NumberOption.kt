package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.NumberField
import org.polyfrost.oneconfig.internal.ui.api.settings.NumberOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.roundToInt

fun Float.toNumberType(type: Class<*>): Number = when (type) {
    Int::class.java, Int::class.javaObjectType -> toInt()
    Long::class.java, Long::class.javaObjectType -> toLong()
    Double::class.java, Double::class.javaObjectType -> toDouble()
    Short::class.java, Short::class.javaObjectType -> toInt().toShort()
    Byte::class.java, Byte::class.javaObjectType -> toInt().toByte()
    else -> this
}

fun isIntegralType(type: Class<*>): Boolean = when (type) {
    Int::class.java, Int::class.javaObjectType, Long::class.java, Long::class.javaObjectType,
    Short::class.java, Short::class.javaObjectType, Byte::class.java, Byte::class.javaObjectType -> true
    else -> false
}

fun filterNumberInput(input: String): String {
    val sb = StringBuilder()
    var hasDot = false
    for ((i, c) in input.withIndex()) {
        when {
            c == '-' && i == 0 -> sb.append(c)
            c == '.' && !hasDot -> { hasDot = true; sb.append(c) }
            c.isDigit() -> sb.append(c)
        }
    }
    return sb.toString()
}

/** How far a spinner's arrows move when the option has no step of its own, whole bounds step by 1 and others by 0.1 */
fun spinnerStep(step: Float, min: Float, max: Float, integral: Boolean = false): Float = when {
    integral && step > 0f -> maxOf(1f, step.roundToInt().toFloat())
    step > 0f -> step
    min == min.roundToInt().toFloat() && max == max.roundToInt().toFloat() -> 1f
    else -> 0.1f
}

fun formatSpinnerValue(v: Float, step: Float = 0f): String {
    if (v == v.toLong().toFloat()) return v.toLong().toString()
    val stepDecimals = if (step > 0f) BigDecimal(step.toString()).stripTrailingZeros().scale() else 0
    return String.format(Locale.ROOT, "%.${maxOf(2, stepDecimals)}f", v)
}

@Composable
fun NumberOption(data: NumberOptionData) {
    NumberOption(data, true)
}

@Composable
fun NumberOption(data: NumberOptionData, enabled: Boolean) {
    val integral = isIntegralType(data.prop.type)
    val step = remember(data.step, data.min, data.max) {
        spinnerStep(data.step, data.min, data.max, integral)
    }
    var value by remember(data.prop) {
        mutableStateOf(data.numProp.shown()?.toFloat()?.coerceIn(data.min, data.max) ?: data.min)
    }

    NumberField(
        value = value,
        onValueChange = { value = it; data.numProp.set(it.toNumberType(data.prop.type)) },
        modifier = Modifier.height(OptionControlHeight),
        range = data.min..data.max,
        step = step,
        roundToDecimals = integral,
        unit = data.unit,
        format = data.format,
        enabled = enabled,
        size = ControlSize.Compact,
    )
}

@Suppress("DEPRECATION")
@Composable
internal fun rememberUnitSuffix(unit: String?): VisualTransformation {
    val color = LocalTheme.current.textColorSecondary
    return remember(unit, color) {
        if (unit == null) {
            VisualTransformation.None
        } else {
            VisualTransformation { typed ->
                val shown = AnnotatedString.Builder(typed).apply {
                    pushStyle(SpanStyle(color = color))
                    append(unit)
                    pop()
                }.toAnnotatedString()
                TransformedText(
                    shown,
                    object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int = offset
                        override fun transformedToOriginal(offset: Int): Int = offset.coerceAtMost(typed.length)
                    },
                )
            }
        }
    }
}
