package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
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
fun spinnerStep(step: Float, min: Float, max: Float): Float = when {
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
    current: () -> Float,
    step: Float,
    min: Float,
    max: Float,
    onScrub: (Float) -> Unit,
    onEnd: () -> Unit,
): Modifier = pointerHoverIcon(PointerIcon.Hand).pointerInput(step, min, max) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var dragging = false
        var travelled = 0f
        var residual = 0f
        var value = current()
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                if (dragging) {
                    change.consume()
                    onEnd()
                }
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
            }
            change.consume()
            val units = (residual / SCRUB_PX_PER_STEP).toInt()
            if (units == 0) continue
            residual -= units * SCRUB_PX_PER_STEP
            val multiplier = if (event.keyboardModifiers.isShiftPressed) 10f else 1f
            value = (value + units * step * multiplier).coerceIn(min, max)
            onScrub(value)
        }
    }
}

fun formatSpinnerValue(v: Float): String =
    if (v == v.toLong().toFloat()) v.toLong().toString() else String.format(Locale.ROOT, "%.2f", v)

// only consumed by a value-based BasicTextField, see the note on its call site
@Suppress("DEPRECATION")
@Composable
private fun rememberUnitSuffix(unit: String?): VisualTransformation {
    val color = LocalTheme.current.textColorSecondary
    return remember(unit, color) {
        if (unit == null) VisualTransformation.None else VisualTransformation { typed ->
            val shown = AnnotatedString.Builder(typed).apply {
                pushStyle(SpanStyle(color = color))
                append(unit)
                pop()
            }.toAnnotatedString()
            TransformedText(shown, object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset
                override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(typed.length)
            })
        }
    }
}

@Composable
private fun SpinnerArrow(icon: String, enabled: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val hoverInteraction = rememberInteractionSource()
    val hovered by hoverInteraction.collectIsHoveredAsState()
    val color = animateColorAsState(
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
) {
    val theme = LocalTheme.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val formatted = if (format != null && !focused) format(value) else null
    val textState = remember(if (math && focused) null else value) { mutableStateOf(formatSpinnerValue(value)) }
    var text by textState
    val latestTextState by rememberUpdatedState(textState)
    var edited by remember { mutableStateOf(false) }
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnEditEnd by rememberUpdatedState(onEditEnd)

    val arrowLo = maxOf(min, arrowMin)
    val arrowHi = maxOf(arrowLo, minOf(max, arrowMax))

    fun commit(v: Float) {
        val clamped = v.coerceIn(arrowLo, arrowHi)
        text = formatSpinnerValue(clamped)
        onValueChange(clamped)
    }

    fun commitTyped() {
        if (!edited) return
        edited = false
        val result = evaluateNumberExpression(text)
        if (result == null) {
            text = formatSpinnerValue(value)
            return
        }
        val clamped = result.coerceIn(min, max)
        text = formatSpinnerValue(clamped)
        onValueChange(clamped)
    }

    val scrubModifier = if (scrub && enabled) {
        Modifier.scrubDrag(
            current = { latestValue },
            step = step,
            min = arrowLo,
            max = arrowHi,
            onScrub = { v ->
                latestTextState.value = formatSpinnerValue(v)
                edited = false
                latestOnValueChange(v)
            },
            onEnd = { latestOnEditEnd?.invoke() },
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .width(width)
            .background(theme.componentBackground, theme.sideBarNavigationEntryShape)
            .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TextFieldState needs its own migration, as it changes how every field hoists its state
        @Suppress("DEPRECATION")
        BasicTextField(
            value = text,
            onValueChange = { input ->
                val filtered = if (math) filterExpressionInput(input) else filterNumberInput(input)
                if (filtered != text) edited = true
                text = filtered
                if (!math) filtered.toFloatOrNull()?.coerceIn(min, max)?.let(onValueChange)
            },
            readOnly = !enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                color = theme.textColor,
                fontSize = 13.sp,
                fontFamily = theme.typography.family,
            ),
            cursorBrush = SolidColor(theme.textColor),
            visualTransformation = rememberUnitSuffix(unit),
            // the field itself stays in the tree while the formatted text covers it so clicking it still focuses it for editing
            decorationBox = { innerTextField ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (label != null) Text(label, modifier = scrubModifier, color = theme.textColor, fontSize = 13.sp)
                    if (icon != null) Icon(icon, color = theme.textColor, modifier = scrubModifier.size(12.dp))
                    Box {
                        Box(Modifier.alpha(if (formatted == null) 1f else 0f)) { innerTextField() }
                        if (formatted != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatted, color = theme.textColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (unit != null) Text(unit, color = theme.textColorSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            modifier = Modifier.trackTextInputFocus()
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
                }
                .weight(1f)
                .padding(start = 8.dp, top = 5.dp, bottom = 5.dp),
        )

        SpinnerArrows(value, arrowLo, arrowHi, step) { if (enabled) commit(it) }
    }
}

@Composable
fun NumberOption(data: NumberOptionData) {
    val step = remember(data.min, data.max) {
        if (data.min == data.min.roundToInt().toFloat() && data.max == data.max.roundToInt().toFloat()) 1f else 0.1f
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
    )
}
