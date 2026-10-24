package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlin.math.min
import kotlin.math.roundToInt
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.spring
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.input.pointer.PointerEvent
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.cornerRadiusOrNull

internal class SliderTicker {
    private var lastTickValue = Float.NaN
    private var lastTickAt = 0L

    fun maybeTick(newValue: Float) {
        if (newValue == lastTickValue) return
        lastTickValue = newValue
        val now = System.currentTimeMillis()
        if (now - lastTickAt >= 70L) {
            lastTickAt = now
            UiSounds.play(UiSoundEvent.SLIDER_TICK)
        }
    }
}

private fun snap(raw: Float, min: Float, max: Float, step: Float): Float {
    val clamped = raw.coerceIn(min, max)
    if (step <= 0f) return clamped
    return (min + ((clamped - min) / step).roundToInt() * step).coerceIn(min, max)
}

internal fun xToValue(x: Float, trackWidth: Float, thumbWidth: Float, min: Float, max: Float, step: Float): Float {
    val usable = (trackWidth - thumbWidth).coerceAtLeast(1f)
    return snap(min + ((x - thumbWidth / 2f) / usable).coerceIn(0f, 1f) * (max - min), min, max, step)
}

/**
 * The draggable track of a slider without any surrounding label or value field
 *
 * Used by the standalone [SliderOption] and by each row of a slider list
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
    trackColor: Color = LocalTheme.current.controlTrackColor,
    fillColor: Color = Accent,
    thumbColor: Color = LocalTheme.current.controlThumbColor,
) {
    val theme = LocalTheme.current
    val ticker = remember { SliderTicker() }
    val span = (max - min).takeIf { it != 0f } ?: 1f
    val fraction by animateFloatAsState(
        ((value - min) / span).coerceIn(0f, 1f),
        animationSpec = spring(),
    )
    val thumb = thumbSize.value
    val track = trackHeight.value
    val trackRadius = min(theme.checkBoxShape.radius, track / 2f)
    val thumbRadius = min(theme.circleShape.cornerRadiusOrNull()?.value ?: Float.MAX_VALUE, thumb / 2f)

    fun drag(e: PointerEvent) =
        xToValue(e.x - e.node.x, e.node.width, thumb, min, max, step).let { ticker.maybeTick(it); onValueChange(it) }

    Canvas(
        modifier
            .height(thumbSize)
            .onPointerEvent { e ->
                when (e.type) {
                    PointerEventType.Press -> e.button == 0 && run { drag(e); true }
                    PointerEventType.Move -> { drag(e); true }
                    PointerEventType.Release -> true
                    else -> false
                }
            }
    ) { x, y, w, h ->
        val trackY = y + (h - track) / 2f
        rect(x, trackY, w, track, trackColor, trackRadius)
        val filled = fraction * w
        if (filled > 0f) rect(x, trackY, filled, track, fillColor, min(trackRadius, filled / 2f))
        rect(x + fraction * (w - thumb), y + (h - thumb) / 2f, thumb, thumb, thumbColor, thumbRadius)
    }
}
