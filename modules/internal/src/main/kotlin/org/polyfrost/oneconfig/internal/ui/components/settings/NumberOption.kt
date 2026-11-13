package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.math.BigDecimal
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.TextField
import org.polyfrost.oneconfig.api.ui.v1.components.TextFieldDefaults
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.ui.api.settings.NumberOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

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

private const val SCRUB_PX_PER_STEP = 3f

private fun filterExpressionInput(input: String): String =
    input.filter { it.isDigit() || it in ".+-*/() " }

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

private fun evaluateNumberExpression(input: String): Float? =
    NumberExpression(input).evaluate()?.takeIf { it.isFinite() }?.toFloat()

private fun Modifier.scrubDrag(
    step: Float,
    onStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onEnd: () -> Unit,
): Modifier = pointerHoverIcon(PointerIcon.Hand).pointerInput(step) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var dragging = false
        var travelled = 0f
        var residual = 0f
        try {
            while (true) {
                val event = awaitPointerEvent()
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
                    onStart()
                }
                change.consume()
                val units = (residual / SCRUB_PX_PER_STEP).toInt()
                if (units == 0) continue
                residual -= units * SCRUB_PX_PER_STEP
                val multiplier = if (event.keyboardModifiers.isShiftPressed) 10f else 1f
                onScrub(units * step * multiplier)
            }
        } finally {
            if (dragging) onEnd()
        }
    }
}

fun formatSpinnerValue(v: Float, step: Float = 0f): String {
    if (v == v.toLong().toFloat()) return v.toLong().toString()
    val stepDecimals = if (step > 0f) BigDecimal(step.toString()).stripTrailingZeros().scale() else 0
    return String.format(Locale.ROOT, "%.${maxOf(2, stepDecimals)}f", v)
}

@Composable
private fun SpinnerArrow(icon: String, enabled: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val hoverInteraction = rememberInteractionSource()
    val hovered by hoverInteraction.collectIsHoveredAsState()
    val color = Motion.animateColorAsState(
        when {
            !enabled -> theme.textColorSecondary.copy(alpha = 0.4f)
            hovered -> theme.textColor
            else -> theme.textColorSecondary
        }
    )

    Box(
        modifier = Modifier
            .size(18.dp, 13.dp)
            .hoverable(hoverInteraction)
            .onClick(rememberInteractionSource(), enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, modifier = Modifier.size(14.dp), tint = { color.value })
    }
}

@Composable
fun SpinnerArrows(value: Float, min: Float, max: Float, step: Float, onStep: (Float) -> Unit) {
    Column(modifier = Modifier.padding(end = 2.dp)) {
        SpinnerArrow("up", value < max) { onStep((value + step).coerceIn(min, max)) }
        SpinnerArrow("down", value > min) { onStep((value - step).coerceIn(min, max)) }
    }
}

@Composable
fun NumberSpinner(
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp = 87.dp,
    format: ((Float) -> String)? = null,
    arrowMin: Float = min,
    arrowMax: Float = max,
    onEditEnd: (() -> Unit)? = null,
    unit: String? = null,
    label: String? = null,
    icon: String? = null,
    scrub: Boolean = false,
    math: Boolean = false,
    enabled: Boolean = true,
    onEditStart: (() -> Unit)? = null,
    liveValue: (() -> Float)? = null,
) {
    val theme = LocalTheme.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val formatted = if (format != null && !focused) format(value) else null
    val textState = remember(if (math && focused) null else value) { mutableStateOf(formatSpinnerValue(value, step)) }
    var text by textState
    var edited by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }

    val arrowLo = maxOf(min, arrowMin)
    val arrowHi = maxOf(arrowLo, minOf(max, arrowMax))

    fun showLiveValue() {
        if (liveValue != null) text = formatSpinnerValue(liveValue(), step)
    }

    fun commit(v: Float) {
        val clamped = v.coerceIn(arrowLo, arrowHi)
        text = formatSpinnerValue(clamped, step)
        onValueChange(clamped)
        showLiveValue()
    }

    fun commitTyped() {
        if (!edited) return
        edited = false
        val result = evaluateNumberExpression(text)
        if (result == null) {
            text = formatSpinnerValue(value, step)
            return
        }
        val clamped = result.coerceIn(min, max)
        text = formatSpinnerValue(clamped, step)
        onValueChange(clamped)
        showLiveValue()
    }

    fun endScrub() {
        if (!scrubbing) return
        scrubbing = false
        onEditEnd?.invoke()
    }

    val scrubStart by rememberUpdatedState<() -> Unit>({
        if (math) commitTyped()
        if (!scrubbing) {
            scrubbing = true
            onEditStart?.invoke()
        }
    })
    val scrubStep by rememberUpdatedState<(Float) -> Unit>({ delta ->
        val target = ((liveValue?.invoke() ?: value) + delta).coerceIn(arrowLo, arrowHi)
        edited = false
        onValueChange(target)
        text = formatSpinnerValue(liveValue?.invoke() ?: target, step)
    })
    val flushEdit by rememberUpdatedState<() -> Unit>({
        if (math) commitTyped()
        endScrub()
    })

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) flushEdit()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            flushEdit()
        }
    }

    val scrubModifier = if (scrub && enabled) {
        Modifier.scrubDrag(
            step = step,
            onStart = { scrubStart() },
            onScrub = { scrubStep(it) },
            onEnd = { flushEdit() },
        )
    } else {
        Modifier
    }

    val disabledAlpha = OneConfigTheme.current.disabledAlpha
    val frameShape = TextFieldDefaults.shape
    val frameBorder = Motion.animateColorAsState(if (focused) TextFieldDefaults.focusBorder else TextFieldDefaults.border)

    Row(
        modifier = Modifier
            .graphicsLayer { alpha = if (enabled) 1f else disabledAlpha }
            .size(width, OptionControlHeight)
            .background(theme.componentBackground, frameShape)
            .border(1.dp, frameBorder.value, frameShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (label != null) Text(label, modifier = scrubModifier, color = theme.textColor, fontSize = 13.sp)
            if (icon != null) Icon(icon, color = theme.textColor, modifier = scrubModifier.size(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                TextField(
                    text,
                    { input ->
                        if (!enabled) return@TextField
                        val filtered = if (math) filterExpressionInput(input) else filterNumberInput(input)
                        if (filtered != text) edited = true
                        text = filtered
                        if (!math) filtered.toFloatOrNull()?.coerceIn(min, max)?.let(onValueChange)
                    },
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (formatted == null) 1f else 0f)
                        .focusProperties { canFocus = enabled }
                        .onPreviewKeyEvent {
                            if (!math || it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            if (it.key != Key.Enter && it.key != Key.NumPadEnter) return@onPreviewKeyEvent false
                            commitTyped()
                            focusManager.clearFocus()
                            true
                        }
                        .onFocusChanged {
                            if (focused && !it.isFocused) {
                                if (math) commitTyped()
                                onEditEnd?.invoke()
                            }
                            if (!focused && it.isFocused) edited = false
                            focused = it.isFocused
                        },
                    framed = false,
                    fontSize = 13.sp,
                    trailing = unit?.let { suffix -> @Composable { Text(suffix, color = theme.textColorSecondary, fontSize = 13.sp) } },
                )
                if (formatted != null) {
                    Text(formatted, color = theme.textColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        SpinnerArrows(value, arrowLo, arrowHi, step) { if (enabled) commit(it) }
    }
}

@Composable
fun NumberOption(data: NumberOptionData) {
    val step = remember(data.step, data.min, data.max) {
        spinnerStep(data.step, data.min, data.max, isIntegralType(data.prop.type))
    }
    var value by remember(data.prop) {
        mutableStateOf(data.numProp.shown()?.toFloat()?.coerceIn(data.min, data.max) ?: data.min)
    }

    NumberSpinner(
        value = value,
        onValueChange = { value = it; data.numProp.set(it.toNumberType(data.prop.type)) },
        min = data.min,
        max = data.max,
        step = step,
        width = 87.dp,
        format = data.format,
        unit = data.unit,
    )
}
