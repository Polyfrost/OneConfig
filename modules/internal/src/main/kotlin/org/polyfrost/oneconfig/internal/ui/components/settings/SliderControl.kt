package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

/**
 * The draggable track of a slider, without any surrounding label or value field.
 * Used both by the standalone [SliderOption] and by each row of a slider list.
 */
@Composable
fun SliderControl(
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    modifier: Modifier = Modifier,
    thumbSize: Dp = 19.dp,
    trackHeight: Dp = 5.dp,
) {
    val theme = LocalTheme.current
    var trackWidthPx by remember { mutableStateOf(0f) }
    val fraction by animateFloatAsState(
        ((value - min) / (max - min)).coerceIn(0f, 1f),
        animationSpec = spring(),
    )

    Box(
        modifier = modifier
            .height(thumbSize)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(min, max, step) {
                val thumbPx = thumbSize.toPx()
                var lastTickValue = Float.NaN
                var lastTickAt = 0L
                fun maybeTick(newValue: Float) {
                    if (newValue == lastTickValue) return
                    lastTickValue = newValue
                    val now = System.currentTimeMillis()
                    if (now - lastTickAt >= 70L) {
                        lastTickAt = now
                        UiSounds.play(UiSoundEvent.SLIDER_TICK)
                    }
                }
                fun snap(raw: Float): Float {
                    val clamped = raw.coerceIn(min, max)
                    if (step <= 0f) return clamped
                    return (min + ((clamped - min) / step).roundToInt() * step).coerceIn(min, max)
                }
                fun xToValue(x: Float): Float {
                    val usable = (trackWidthPx - thumbPx).coerceAtLeast(1f)
                    return snap(min + ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f) * (max - min))
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    xToValue(down.position.x).let { maybeTick(it); onValueChange(it) }
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { ch ->
                            if (ch.pressed) {
                                xToValue(ch.position.x).let { maybeTick(it); onValueChange(it) }
                                ch.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxWidth().height(trackHeight)
                .align(Alignment.CenterStart)
                .clip(theme.checkBoxShape)
                .background(theme.controlTrackColor)
        )
        Box(
            Modifier
                .fillMaxWidth(fraction).height(trackHeight)
                .align(Alignment.CenterStart)
                .clip(theme.checkBoxShape)
                .background(Accent)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((fraction * (trackWidthPx - thumbSize.toPx())).toInt(), 0) }
                .size(thumbSize)
                .background(theme.controlThumbColor, theme.circleShape)
        )
    }
}
