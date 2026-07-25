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
import org.polyfrost.oneconfig.internal.ui.api.settings.RangeSliderOptionData
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A slider with a thumb at each end of a highlighted span. Dragging grabs whichever thumb is nearer to the
 * press, and the two ends are clamped against each other so start never passes end.
 */
@Composable
fun RangeSliderOption(data: RangeSliderOptionData) {
    val theme = LocalTheme.current

    val initial = data.read()
    var start by remember(data.prop) { mutableStateOf(initial?.first ?: data.min) }
    var end by remember(data.prop) { mutableStateOf(initial?.second ?: data.max) }
    var trackWidthPx by remember { mutableStateOf(0f) }
    // Which thumb the in-flight drag grabbed, so it keeps following the pointer past the other thumb.
    var draggingEnd by remember { mutableStateOf(false) }

    val thumbDp = 19.dp
    val span = (data.max - data.min).takeIf { it != 0f } ?: 1f
    val startFraction by animateFloatAsState(((start - data.min) / span).coerceIn(0f, 1f), animationSpec = spring())
    val endFraction by animateFloatAsState(((end - data.min) / span).coerceIn(0f, 1f), animationSpec = spring())

    fun commit() = data.write(start, end)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .width(LocalOptionWidth.current)
                .height(19.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(data.min, data.max, data.step) {
                    val thumbPx = thumbDp.toPx()
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
                        val clamped = raw.coerceIn(data.min, data.max)
                        if (data.step <= 0f) return clamped
                        return (data.min + ((clamped - data.min) / data.step).roundToInt() * data.step)
                            .coerceIn(data.min, data.max)
                    }
                    fun xToValue(x: Float): Float {
                        val usable = (trackWidthPx - thumbPx).coerceAtLeast(1f)
                        return snap(data.min + ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f) * (data.max - data.min))
                    }
                    fun apply(value: Float) {
                        if (draggingEnd) end = value.coerceAtLeast(start) else start = value.coerceAtMost(end)
                        maybeTick(value)
                        commit()
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val pressed = xToValue(down.position.x)
                        draggingEnd = abs(pressed - end) <= abs(pressed - start)
                        apply(pressed)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
                                if (change.pressed) {
                                    apply(xToValue(change.position.x))
                                    change.consume()
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
                    .clip(theme.checkBoxShape)
                    .background(theme.chipBackground)
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((startFraction * (trackWidthPx - thumbDp.toPx())).toInt(), 0) }
                    .fillMaxWidth((endFraction - startFraction).coerceAtLeast(0f)).height(5.dp)
                    .clip(theme.checkBoxShape)
                    .background(Accent)
            )
            listOf(startFraction, endFraction).forEach { fraction ->
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset { IntOffset((fraction * (trackWidthPx - thumbDp.toPx())).toInt(), 0) }
                        .size(thumbDp)
                        .background(theme.controlThumbColor, theme.circleShape)
                )
            }
        }
        NumberSpinner(
            value = start,
            onValueChange = { start = it.coerceAtMost(end); commit() },
            min = data.min,
            max = data.max,
            step = if (data.step > 0f) data.step else 1f,
            width = 80.dp,
        )
        NumberSpinner(
            value = end,
            onValueChange = { end = it.coerceAtLeast(start); commit() },
            min = data.min,
            max = data.max,
            step = if (data.step > 0f) data.step else 1f,
            width = 80.dp,
        )
    }
}
