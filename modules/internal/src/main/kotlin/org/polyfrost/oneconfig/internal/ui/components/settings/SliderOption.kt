package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.api.settings.SliderOptionData
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

@Composable
fun SliderOption(data: SliderOptionData) {
    val theme = LocalTheme.current

    var value by remember(data.prop) { mutableStateOf(data.numProp.get()?.toFloat() ?: data.min) }
    var trackWidthPx by remember { mutableStateOf(0f) }

    val thumbDp = 19.dp
    val fraction by animateFloatAsState(((value - data.min) / (data.max - data.min)).coerceIn(0f, 1f),
        animationSpec = spring()
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .height(19.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(data.min, data.max, data.step) {
                    val thumbPx = thumbDp.toPx()
                    fun snap(raw: Float): Float {
                        val clamped = raw.coerceIn(data.min, data.max)
                        if (data.step <= 0f) return clamped
                        return (data.min + ((clamped - data.min) / data.step).roundToInt() * data.step)
                            .coerceIn(data.min, data.max)
                    }
                    fun xToValue(x: Float): Float {
                        val usable = (trackWidthPx - thumbPx).coerceAtLeast(1f)
                        return snap(data.min + ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f) * (data.max - data.min))
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        value = xToValue(down.position.x)
                        data.numProp.set(value.toNumberType(data.prop.type))
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { ch ->
                                if (ch.pressed) {
                                    value = xToValue(ch.position.x)
                                    data.numProp.set(value.toNumberType(data.prop.type))
                                    ch.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            Box(
                Modifier
                    .fillMaxWidth().height(5.dp)
                    .align(Alignment.CenterStart)
                    .clip(LocalTheme.current.checkBoxShape)
                    .background(theme.chipBackground)
            )
            Box(
                Modifier
                    .fillMaxWidth(fraction).height(5.dp)
                    .align(Alignment.CenterStart)
                    .clip(LocalTheme.current.checkBoxShape)
                    .background(Accent)
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(((fraction * (trackWidthPx - thumbDp.toPx()))).toInt(), 0) }
                    .size(thumbDp)
//                    .onClick(interactionSource){}
                    .background(theme.controlThumbColor, LocalTheme.current.circleShape)
            ) {
//                Box(
//                    Modifier
//                        .align(Alignment.Center)
//                        .size(innerThumbDp)
//                        .background(Accent, LocalTheme.current.circleShape)
//                )
            }
        }
        NumberSpinner(
            value = value,
            onValueChange = { value = it; data.numProp.set(it.toNumberType(data.prop.type)) },
            min = data.min,
            max = data.max,
            step = if (data.step > 0f) data.step else 1f,
            width = 80.dp,
        )
    }
}
