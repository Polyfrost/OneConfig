@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.polyfrost.compose.host.controller.Controllers
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import androidx.compose.ui.input.pointer.isShiftPressed as isPointerShiftPressed

private val NUMBER_FIELD_WIDTH = 87.dp
private val NUMBER_FIELD_START_PADDING = 8.dp
private val NUMBER_FIELD_END_PADDING = 2.dp
private const val FREE_DECIMALS = 4
private const val SHIFT_STEP_MULTIPLIER = 10f
private const val SCRUB_PX_PER_STEP = 3f

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
    invalid: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    scrub: Boolean = false,
    width: Dp = NUMBER_FIELD_WIDTH,
    stepRange: ClosedFloatingPointRange<Float>? = null,
    onScrubStart: (() -> Unit)? = null,
    onEditEnd: (() -> Unit)? = null,
    fractionDigits: Int? = null,
    liveValue: (() -> Float)? = null,
    commitOnType: Boolean = true,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    val focusManager = LocalFocusManager.current
    val latestChange by rememberUpdatedState(onValueChange)
    val latestScrubStart by rememberUpdatedState(onScrubStart)
    val latestEditEnd by rememberUpdatedState(onEditEnd)
    val latestLive by rememberUpdatedState(liveValue)
    var field by remember { mutableStateOf(TextFieldValue(formatNumber(value, decimals, roundToDecimals, step, fractionDigits))) }
    var edited by remember { mutableStateOf(false) }
    val session = remember { NumberFieldSession(value) }
    SideEffect { session.base = liveValue?.invoke() ?: value }
    val stepBounds = when {
        stepRange == null -> range
        range == null -> stepRange
        else -> {
            val low = maxOf(range.start, stepRange.start)
            low..maxOf(low, minOf(range.endInclusive, stepRange.endInclusive))
        }
    }

    fun normalise(v: Float, bounds: ClosedFloatingPointRange<Float>?): Float {
        val rounded = if (roundToDecimals) {
            val factor = 10.0.pow(decimals.coerceAtLeast(0))
            ((v * factor).roundToLong() / factor).toFloat()
        } else {
            v
        }
        return bounds?.let { rounded.coerceIn(it) } ?: rounded
    }

    fun show(v: Float) {
        val text = formatNumber(v, decimals, roundToDecimals, step, fractionDigits)
        field = TextFieldValue(text, TextRange(text.length))
    }

    fun current(): Float = latestLive?.invoke() ?: value

    fun settle(next: Float): Float {
        val live = latestLive?.invoke() ?: next
        show(live)
        session.base = live
        return live
    }

    fun commitTyped(): Float {
        if (!edited) return current()
        edited = false
        val result = NumberExpression(field.text).evaluate()?.toFloat()?.takeIf { it.isFinite() }
        if (result == null) {
            val now = current()
            show(now)
            return now
        }
        val next = normalise(result, range)
        if (next != current()) latestChange(next)
        return settle(next)
    }

    fun stepBy(delta: Float, from: Float) {
        if (!enabled) return
        edited = false
        val next = normalise(from + delta, stepBounds)
        if (next != from) latestChange(next)
        settle(next)
    }

    fun startScrub() {
        if (session.scrubbing) return
        session.base = commitTyped()
        session.scrubbing = true
        latestScrubStart?.invoke()
    }

    fun endScrub() {
        if (!session.scrubbing) return
        session.scrubbing = false
        latestEditEnd?.invoke()
    }

    val flush by rememberUpdatedState {
        commitTyped()
        endScrub()
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) flush()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            flush()
        }
    }

    val external = formatNumber(value, decimals, roundToDecimals, step, fractionDigits)
    LaunchedEffect(external, focused) {
        if (!focused && !edited && field.text != external) field = TextFieldValue(external, TextRange(external.length))
    }
    val formatted = if (format != null && !focused && !edited) runCatching { format(value) }.getOrNull() else null
    val shown = when {
        formatted != null -> TextFieldValue(formatted, TextRange(formatted.length))
        !focused && !edited && field.text != external -> TextFieldValue(external, TextRange(external.length))
        else -> field
    }
    val canIncrease = stepBounds == null || value < stepBounds.endInclusive
    val canDecrease = stepBounds == null || value > stepBounds.start
    val unitSuffix = rememberUnitSuffix(unit)
    val frameAlpha = if (enabled) 1f else theme.disabledAlpha
    val fill = FieldDefaults.fill
    val scrubbable = scrub && enabled
    val onScrub: (Int, Boolean) -> Unit = { units, shift ->
        stepBy(units * step * (if (shift) SHIFT_STEP_MULTIPLIER else 1f), session.base)
    }

    Row(
        modifier
            .width(width)
            .heightIn(min = size.height)
            .hoverable(source, enabled)
            .fieldFrame(FieldState.of(hovered, focused, invalid), fill.copy(alpha = fill.alpha * frameAlpha), enabled = enabled)
            .padding(start = NUMBER_FIELD_START_PADDING, end = NUMBER_FIELD_END_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                if (scrubbable) Modifier.pointerHoverIcon(PointerIcon.Hand).scrubDrag(PointerEventPass.Main, ::startScrub, onScrub, ::endScrub) else Modifier,
                contentAlignment = Alignment.Center,
            ) {
                leading()
            }
            Spacer(Modifier.width(FieldDefaults.contentSpacing))
        }
        FieldCore(
            shown,
            { next ->
                if (formatted != null) return@FieldCore
                val filtered = next.text.filter { it.isDigit() || it in ".+-*/() " }
                if (filtered != field.text) edited = true
                field = if (filtered == next.text) next else TextFieldValue(filtered, TextRange(filtered.length))
                if (commitOnType) filtered.takeIf { text -> text.all { it.isDigit() || it == '.' } }?.toFloatOrNull()?.let { typed ->
                    val clamped = range?.let { typed.coerceIn(it) } ?: typed
                    if (clamped != value) latestChange(clamped)
                }
            },
            Modifier
                .weight(1f)
                .then(if (scrubbable && !focused) Modifier.scrubDrag(PointerEventPass.Initial, ::startScrub, onScrub, ::endScrub) else Modifier)
                .onFocusChanged {
                    if (it.isFocused) {
                        session.focused = true
                    } else {
                        commitTyped()
                        if (session.focused) {
                            session.focused = false
                            latestEditEnd?.invoke()
                        }
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (focused && enabled && event.key != Key.Enter && event.key != Key.NumPadEnter) session.focused = true
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
                            if (Controllers.isActive) {
                                if (session.focused) {
                                    session.focused = false
                                    latestEditEnd?.invoke()
                                }
                            } else {
                                focusManager.clearFocus()
                            }
                            true
                        }
                        else -> false
                    }
                },
            null,
            enabled,
            invalid,
            true,
            size,
            label,
            null,
            null,
            null,
            false,
            Int.MAX_VALUE,
            false,
            fontSize,
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

private class NumberFieldSession(var base: Float) {
    var scrubbing = false
    var focused = false
}

@Composable
private fun Modifier.scrubDrag(
    pass: PointerEventPass,
    onStart: () -> Unit,
    onScrub: (units: Int, shift: Boolean) -> Unit,
    onEnd: () -> Unit,
): Modifier {
    val start by rememberUpdatedState(onStart)
    val scrub by rememberUpdatedState(onScrub)
    val end by rememberUpdatedState(onEnd)
    return pointerInput(pass) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = pass)
            var dragging = false
            var travelled = 0f
            var residual = 0f
            try {
                while (true) {
                    val event = awaitPointerEvent(pass)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        if (dragging) change.consume()
                        break
                    }
                    val dx = change.position.x - change.previousPosition.x
                    if (dragging) {
                        residual += dx
                    } else {
                        travelled += dx
                        if (abs(travelled) <= viewConfiguration.touchSlop) continue
                        dragging = true
                        residual = travelled
                        start()
                    }
                    change.consume()
                    val units = (residual / SCRUB_PX_PER_STEP).toInt()
                    if (units == 0) continue
                    residual -= units * SCRUB_PX_PER_STEP
                    scrub(units, event.keyboardModifiers.isPointerShiftPressed)
                }
            } finally {
                if (dragging) end()
            }
        }
    }
}

@Composable
private fun StepArrow(icon: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val color by Motion.animateColorAsState(
        when {
            !enabled -> theme.textColorSecondary.copy(alpha = theme.disabledAlpha)
            hovered -> theme.textColor
            else -> theme.textColorSecondary
        },
        Motion.colorSpec(),
    )
    Box(
        modifier = Modifier
            .size(18.dp, 13.dp)
            .oneConfigClickable(label, onClick, enabled = enabled, shape = theme.shapes.small, interactionSource = source, recipe = ControlDefaults.PressOnly),
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

private fun formatNumber(value: Float, decimals: Int, round: Boolean, step: Float, fractionDigits: Int?): String {
    if (round) return if (decimals <= 0) value.roundToLong().toString() else String.format(Locale.ROOT, "%.${decimals}f", value)
    if (!value.isFinite()) return value.toString()
    val minimum = decimals.coerceAtLeast(0)
    val stepScale = if (step > 0f && step.isFinite()) BigDecimal(step.toString()).stripTrailingZeros().scale() else 0
    if (fractionDigits != null) {
        if (value == value.toLong().toFloat()) return value.toLong().toString()
        return String.format(Locale.ROOT, "%.${maxOf(minimum, fractionDigits, stepScale)}f", value)
    }
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
