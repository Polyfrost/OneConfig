package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.NumberOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

fun Float.toNumberType(type: Class<*>): Number = when (type) {
    Int::class.java, java.lang.Integer::class.java, Integer.TYPE -> toInt()
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

fun formatSpinnerValue(v: Float): String =
    if (v == v.toLong().toFloat()) v.toLong().toString() else "%.2f".format(v)

@Composable
fun NumberSpinner(
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp = 87.dp,
) {
    val theme = LocalTheme.current
    var text by remember(value) { mutableStateOf(formatSpinnerValue(value)) }

    fun commit(raw: String) {
        val v = raw.toFloatOrNull() ?: return
        val clamped = v.coerceIn(min, max)
        text = formatSpinnerValue(clamped)
        onValueChange(clamped)
    }

    Row(
        modifier = Modifier
            .width(width)
            .background(theme.componentBackground, theme.sideBarNavigationEntryShape)
            .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { input ->
                val filtered = filterNumberInput(input)
                text = filtered
                filtered.toFloatOrNull()?.coerceIn(min, max)?.let(onValueChange)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                color = theme.textColor,
                fontSize = 13.sp,
                fontFamily = theme.typography.family,
            ),
            cursorBrush = SolidColor(theme.textColor),
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 5.dp, bottom = 5.dp),
        )

        Column(modifier = Modifier.padding(end = 2.dp)) {
            val upInteraction = rememberInteractionSource()
            val downInteraction = rememberInteractionSource()
            val upHovered by upInteraction.collectIsHoveredAsState()
            val downHovered by downInteraction.collectIsHoveredAsState()
            val upColor by animateColorAsState(if (upHovered) theme.textColor else theme.textColorSecondary)
            val downColor by animateColorAsState(if (downHovered) theme.textColor else theme.textColorSecondary)

            Box(
                modifier = Modifier
                    .size(18.dp, 13.dp)
                    .onClick(upInteraction, value < max) { commit(formatSpinnerValue((value + step).coerceIn(min, max))) },
                contentAlignment = Alignment.Center,
            ) {
                Icon("up", modifier = Modifier.size(14.dp), color = upColor)
            }

            Box(
                modifier = Modifier
                    .size(18.dp, 13.dp)
                    .onClick(downInteraction, value > min) { commit(formatSpinnerValue((value - step).coerceIn(min, max))) },
                contentAlignment = Alignment.Center,
            ) {
                Icon("down", modifier = Modifier.size(14.dp), color = downColor)
            }
        }
    }
}

@Composable
fun NumberOption(data: NumberOptionData) {
    val step = remember(data.min, data.max) {
        if (data.min == data.min.roundToInt().toFloat() && data.max == data.max.roundToInt().toFloat()) 1f else 0.1f
    }
    var value by remember(data.prop) {
        mutableStateOf(data.numProp.get()?.toFloat()?.coerceIn(data.min, data.max) ?: data.min)
    }

    NumberSpinner(
        value = value,
        onValueChange = { value = it; data.numProp.set(it.toNumberType(data.prop.type)) },
        min = data.min,
        max = data.max,
        step = step,
        width = 87.dp,
    )
}
