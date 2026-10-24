package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.Locale
import kotlin.math.roundToInt
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.key.KeyEventType
import org.polyfrost.compose.ui.input.pointer.PointerEvent
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.NumberOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.screens.UiClipboard
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

fun Float.toNumberType(type: Class<*>): Number = when (type) {
    Int::class.java, Integer::class.java, Integer.TYPE -> toInt()
    Long::class.java, java.lang.Long::class.java, java.lang.Long.TYPE -> toLong()
    Double::class.java, java.lang.Double::class.java, java.lang.Double.TYPE -> toDouble()
    Short::class.java, java.lang.Short::class.java, java.lang.Short.TYPE -> toInt().toShort()
    Byte::class.java, java.lang.Byte::class.java, java.lang.Byte.TYPE -> toInt().toByte()
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

fun formatSpinnerValue(v: Float): String =
    if (v == v.toLong().toFloat()) v.toLong().toString() else String.format(Locale.ROOT, "%.2f", v)

private fun TextFieldState.setText(s: String) { replaceText(s); moveCaret(s.length) }

internal fun focusOnPress(e: PointerEvent, state: TextFieldState) =
    e.type == PointerEventType.Press && e.button == 0 && run { state.requestFocus(); true }

internal fun editKeys(clipboard: UiClipboard, state: TextFieldState, onEnter: (() -> Unit)? = null, font: PolyFont? = null, onEdit: () -> Unit): (KeyEvent) -> Boolean = { ev ->
    if (onEnter != null && ev.type == KeyEventType.KeyDown && ev.key == KeyEvent.KEY_ENTER) { onEnter(); true } else {
        val before = state.text
        val used = state.onKey(ev, clipboard.read, clipboard.write, font)
        if (used && state.text != before) onEdit()
        used
    }
}

@Composable
private fun SpinnerArrow(icon: String, enabled: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val hoverInteraction = rememberInteractionSource()
    val hovered by hoverInteraction.collectIsHoveredAsState()
    val color = animateColorAsState(
        when {
            !enabled -> theme.textColorSecondary.copy(alpha = theme.textColorSecondary.alphaF * 0.4f)
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
        Icon(icon, modifier = Modifier.size(14.dp), color = color)
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
) {
    val theme = LocalTheme.current
    val clipboard = LocalUiClipboard.current
    val state = remember { TextFieldState(formatSpinnerValue(value)) }
    val focused = state.focused
    val formatted = if (format != null && !focused) format(value) else null
    val font = PolyFont(theme.typography.family.resolve(FontWeight.Normal), 13f)
    val wasFocused = remember { booleanArrayOf(false) }
    LaunchedEffect(value, focused) {
        if (!focused) state.setText(formatSpinnerValue(value))
        if (wasFocused[0] && !focused) onEditEnd?.invoke()
        wasFocused[0] = focused
    }

    val arrowLo = maxOf(min, arrowMin)
    val arrowHi = maxOf(arrowLo, minOf(max, arrowMax))

    fun commit(v: Float) {
        val clamped = v.coerceIn(arrowLo, arrowHi)
        state.setText(formatSpinnerValue(clamped))
        onValueChange(clamped)
    }

    Row(
        modifier = Modifier
            .width(width)
            .background(theme.componentBackground, theme.sideBarNavigationEntryShape)
            .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .onPointerEvent { focusOnPress(it, state) }
                .padding(start = 8.dp, top = 5.dp, bottom = 5.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (formatted != null) {
                Text(formatted, color = theme.textColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                state, font,
                color = if (formatted == null) theme.textColor else Color.Transparent,
                caretColor = theme.textColor,
                selectionColor = Accent.copy(alpha = 0.4f),
                readClipboard = clipboard.read,
                writeClipboard = clipboard.write,
                modifier = Modifier.fillMaxWidth(),
                onKey = editKeys(clipboard, state) {
                    val filtered = filterNumberInput(state.text)
                    if (filtered != state.text) state.replaceText(filtered)
                    filtered.toFloatOrNull()?.coerceIn(min, max)?.let(onValueChange)
                },
            )
        }

        SpinnerArrows(value, arrowLo, arrowHi, step) { commit(it) }
    }
}

@Composable
fun NumberOption(data: NumberOptionData) {
    val step = remember(data.min, data.max) {
        if (data.min == data.min.roundToInt().toFloat() && data.max == data.max.roundToInt().toFloat()) 1f else 0.1f
    }
    var value by remember(data.prop) {
        mutableFloatStateOf(data.numProp.shown()?.toFloat()?.coerceIn(data.min, data.max) ?: data.min)
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
