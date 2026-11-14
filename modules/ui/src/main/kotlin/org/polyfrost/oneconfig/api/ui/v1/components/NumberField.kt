@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val NUMBER_FIELD_WIDTH = 87.dp
private val NUMBER_FIELD_START_PADDING = 8.dp
private val NUMBER_FIELD_END_PADDING = 2.dp
private const val FREE_DECIMALS = 4
private const val SHIFT_STEP_MULTIPLIER = 10f
private const val ARROW_FADE_MILLIS = 150

@ExperimentalOneConfigUiApi
@Composable
public fun NumberField(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float>? = null,
    step: Float = 1f,
    decimals: Int = 0,
    roundToDecimals: Boolean = true,
    unit: String? = null,
    format: ((Float) -> String)? = null,
    enabled: Boolean = true,
    size: ControlSize = ControlSize.Regular,
    label: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val latestChange by rememberUpdatedState(onValueChange)
    var field by remember { mutableStateOf(TextFieldValue(formatNumber(value, decimals, roundToDecimals, step))) }
    var edited by remember { mutableStateOf(false) }

    fun normalise(v: Float): Float {
        val rounded = if (roundToDecimals) {
            val factor = 10.0.pow(decimals.coerceAtLeast(0))
            ((v * factor).roundToLong() / factor).toFloat()
        } else {
            v
        }
        return range?.let { rounded.coerceIn(it) } ?: rounded
    }

    fun show(v: Float) {
        val text = formatNumber(v, decimals, roundToDecimals, step)
        field = TextFieldValue(text, TextRange(text.length))
    }

    fun commitTyped(): Float {
        if (!edited) return value
        edited = false
        val result = NumberExpression(field.text).evaluate()?.toFloat()?.takeIf { it.isFinite() }
        if (result == null) {
            show(value)
            return value
        }
        val next = normalise(result)
        show(next)
        if (next != value) latestChange(next)
        return next
    }

    fun stepBy(delta: Float, from: Float) {
        if (!enabled) return
        edited = false
        val next = normalise(from + delta)
        show(next)
        if (next != from) latestChange(next)
    }

    val external = formatNumber(value, decimals, roundToDecimals, step)
    LaunchedEffect(external, focused) {
        if (!focused && !edited && field.text != external) field = TextFieldValue(external, TextRange(external.length))
    }
    val formatted = if (format != null && !focused && !edited) runCatching { format(value) }.getOrNull() else null
    val shown = when {
        formatted != null -> TextFieldValue(formatted, TextRange(formatted.length))
        !focused && !edited && field.text != external -> TextFieldValue(external, TextRange(external.length))
        else -> field
    }
    val canIncrease = range == null || value < range.endInclusive
    val canDecrease = range == null || value > range.start
    val unitSuffix = rememberUnitSuffix(unit)
    val shape = TextFieldDefaults.shape
    val frameAlpha = if (enabled) 1f else theme.disabledAlpha
    val fill = theme.componentBackground
    val border by animateColorAsState(
        if (focused) TextFieldDefaults.focusBorder else TextFieldDefaults.border,
        Motion.spec(tween(ARROW_FADE_MILLIS)),
    )

    Row(
        modifier
            .width(NUMBER_FIELD_WIDTH)
            .heightIn(min = size.height)
            .background(fill.copy(alpha = fill.alpha * frameAlpha), shape)
            .border(1.dp, border.copy(alpha = border.alpha * frameAlpha), shape)
            .padding(start = NUMBER_FIELD_START_PADDING, end = NUMBER_FIELD_END_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldCore(
            shown,
            { next ->
                if (formatted != null) return@FieldCore
                val filtered = next.text.filter { it.isDigit() || it in ".+-*/() " }
                if (filtered != field.text) edited = true
                field = if (filtered == next.text) next else TextFieldValue(filtered, TextRange(filtered.length))
                filtered.takeIf { text -> text.all { it.isDigit() || it == '.' } }?.toFloatOrNull()?.let { typed ->
                    val clamped = range?.let { typed.coerceIn(it) } ?: typed
                    if (clamped != value) latestChange(clamped)
                }
            },
            Modifier
                .weight(1f)
                .onFocusChanged { if (!it.isFocused) commitTyped() }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !enabled) return@onPreviewKeyEvent false
                    val multiplier = if (event.isShiftPressed) SHIFT_STEP_MULTIPLIER else 1f
                    when (event.key) {
                        Key.DirectionUp -> {
                            stepBy(step * multiplier, commitTyped())
                            true
                        }
                        Key.DirectionDown -> {
                            stepBy(-step * multiplier, commitTyped())
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            commitTyped()
                            focusManager.clearFocus()
                            true
                        }
                        else -> false
                    }
                },
            null,
            enabled,
            false,
            true,
            size,
            label,
            null,
            null,
            null,
            false,
            Int.MAX_VALUE,
            false,
            TextUnit.Unspecified,
            null,
            null,
            source,
            transformation = if (formatted != null) null else unitSuffix,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Column {
            StepArrow("up", numberString("oneconfig.ui.number.increase", "Increase"), enabled && canIncrease) {
                stepBy(step, commitTyped())
            }
            StepArrow("down", numberString("oneconfig.ui.number.decrease", "Decrease"), enabled && canDecrease) {
                stepBy(-step, commitTyped())
            }
        }
    }
}

@Composable
private fun StepArrow(icon: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val color by animateColorAsState(
        when {
            !enabled -> theme.textColorSecondary.copy(alpha = theme.disabledAlpha)
            hovered -> theme.textColor
            else -> theme.textColorSecondary
        },
        Motion.spec(tween(ARROW_FADE_MILLIS)),
    )
    Box(
        modifier = Modifier
            .size(18.dp, 13.dp)
            .oneConfigClickable(label, onClick, enabled = enabled, shape = theme.shapes.small, indication = false, interactionSource = source),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = color)
    }
}

@Suppress("DEPRECATION")
@Composable
private fun rememberUnitSuffix(unit: String?): VisualTransformation {
    val color = OneConfigTheme.current.textColorSecondary
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

private fun formatNumber(value: Float, decimals: Int, round: Boolean, step: Float): String {
    if (round) return if (decimals <= 0) value.roundToLong().toString() else String.format(Locale.ROOT, "%.${decimals}f", value)
    if (!value.isFinite()) return value.toString()
    val minimum = decimals.coerceAtLeast(0)
    val stepScale = if (step > 0f && step.isFinite()) BigDecimal(step.toString()).stripTrailingZeros().scale() else 0
    val exact = BigDecimal(value.toString())
        .setScale(maxOf(minimum, FREE_DECIMALS, stepScale), RoundingMode.HALF_UP)
        .stripTrailingZeros()
    return exact.setScale(maxOf(minimum, exact.scale(), 0)).toPlainString()
}

private fun numberString(key: String, fallback: String): String =
    runCatching { Platform.i18n().translateString(key) }.getOrNull()?.takeIf { it != key } ?: fallback

private class NumberExpression(private val src: String) {
    private var pos = 0

    fun evaluate(): Double? {
        val v = expression() ?: return null
        return if (peek() == null) v else null
    }

    private fun peek(): Char? {
        while (pos < src.length && src[pos] == ' ') pos++
        return src.getOrNull(pos)
    }

    private fun expression(): Double? {
        var v = term() ?: return null
        while (true) {
            when (peek()) {
                '+' -> { pos++; v += term() ?: return null }
                '-' -> { pos++; v -= term() ?: return null }
                else -> return v
            }
        }
    }

    private fun term(): Double? {
        var v = factor() ?: return null
        while (true) {
            when (peek()) {
                '*' -> { pos++; v *= factor() ?: return null }
                '/' -> { pos++; v /= factor() ?: return null }
                else -> return v
            }
        }
    }

    private fun factor(): Double? {
        when (peek()) {
            '-' -> { pos++; return factor()?.let { -it } }
            '+' -> { pos++; return factor() }
            '(' -> {
                pos++
                val v = expression() ?: return null
                if (peek() != ')') return null
                pos++
                return v
            }
        }
        val start = pos
        while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
        return src.substring(start, pos).toDoubleOrNull()
    }
}
