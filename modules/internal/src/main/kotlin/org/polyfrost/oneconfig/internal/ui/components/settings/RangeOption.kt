package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.min
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.spring
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.render.PolyPath
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.pointer.PointerEvent
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.RangeOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/**
 * A start and end pair shown either as a two-thumb slider with an input per end or as the inputs alone
 *
 * Whatever is shown the stored pair always has start at or below end
 */
@Composable
fun RangeOption(data: RangeOptionData) {
    val initial = remember(data.prop) { data.read() }
    var start by remember(data.prop) { mutableFloatStateOf(initial?.first ?: data.min) }
    var end by remember(data.prop) { mutableFloatStateOf(initial?.second ?: data.max) }

    fun update(newStart: Float, newEnd: Float) {
        start = newStart
        end = newEnd
        data.write(minOf(newStart, newEnd), maxOf(newStart, newEnd))
    }

    val step = spinnerStep(data.step, data.min, data.max)

    when (data.style) {
        RangeOptionData.Style.Slider -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RangeSliderTrack(
                start = start,
                end = end,
                onValueChange = ::update,
                min = data.min,
                max = data.max,
                step = data.step,
                modifier = Modifier
                    // the spinners beside the track outgrow narrow columns like the HUD editor's
                    .weight(1f, fill = false)
                    .width(LocalOptionWidth.current),
            )
            RangeSpinners(start, end, ::update, data.min, data.max, step, width = 80.dp, format = data.format)
        }
        RangeOptionData.Style.Spinner -> Row(
            modifier = Modifier.width(LocalOptionWidth.current),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RangeSpinners(
                start, end, ::update, data.min, data.max, step,
                width = (LocalOptionWidth.current - RangeSeparatorWidth) / 2,
                format = data.format,
                separated = true,
            )
        }
    }
}

internal val RangeSeparatorWidth = 20.dp

/**
 * An input for each end of a range laid out in the caller's row
 *
 * Typing is free to pass the other end so a number is never clamped midway through being typed and the pair
 * is put back in order once the input loses focus while the arrows stop at the other end unless the pair is
 * already out of order
 *
 * [separated] puts a dash between the inputs for rows that have no track to read them against
 */
@Composable
internal fun RangeSpinners(
    start: Float,
    end: Float,
    onValueChange: (start: Float, end: Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    width: Dp,
    format: ((Float) -> String)?,
    separated: Boolean = false,
) {
    val reorder = { if (start > end) onValueChange(end, start) }
    NumberSpinner(
        value = start,
        onValueChange = { onValueChange(it, end) },
        min = min,
        max = max,
        step = step,
        width = width,
        format = format,
        arrowMax = if (start <= end) end else max,
        onEditEnd = reorder,
    )
    if (separated) {
        Box(Modifier.width(RangeSeparatorWidth), contentAlignment = Alignment.Center) {
            Text("-", color = LocalTheme.current.textColorSecondary, fontSize = 13.sp)
        }
    }
    NumberSpinner(
        value = end,
        onValueChange = { onValueChange(start, it) },
        min = min,
        max = max,
        step = step,
        width = width,
        format = format,
        arrowMin = if (start <= end) start else min,
        onEditEnd = reorder,
    )
}

private class RangeDrag {
    var dragStart = 0f
    var dragEnd = 0f
    var draggingEnd: Boolean? = null
}

/**
 * A track with a thumb at each end of a highlighted span
 *
 * Dragging grabs whichever thumb is nearer the press and the ends are clamped so start never passes end
 */
@Composable
internal fun RangeSliderTrack(
    start: Float,
    end: Float,
    onValueChange: (start: Float, end: Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    modifier: Modifier = Modifier,
    thumbWidth: Dp = 13.dp,
    thumbHeight: Dp = 16.dp,
    trackHeight: Dp = 5.dp,
) {
    val theme = LocalTheme.current
    val ticker = remember { SliderTicker() }
    val drag = remember { RangeDrag() }

    val span = (max - min).takeIf { it != 0f } ?: 1f
    val startFraction by animateFloatAsState(((minOf(start, end) - min) / span).coerceIn(0f, 1f), animationSpec = spring())
    val endFraction by animateFloatAsState(((maxOf(start, end) - min) / span).coerceIn(0f, 1f), animationSpec = spring())
    val thumbW = thumbWidth.value
    val thumbH = thumbHeight.value
    val track = trackHeight.value
    val trackRadius = min(theme.checkBoxShape.radius, track / 2f)

    fun apply(value: Float) {
        if (drag.draggingEnd == null && value != drag.dragStart) drag.draggingEnd = value > drag.dragStart
        when (drag.draggingEnd) {
            true -> drag.dragEnd = value.coerceAtLeast(drag.dragStart)
            false -> drag.dragStart = value.coerceAtMost(drag.dragEnd)
            null -> return
        }
        ticker.maybeTick(value)
        onValueChange(drag.dragStart, drag.dragEnd)
    }
    fun valueAt(e: PointerEvent) = xToValue(e.x - e.node.x, e.node.width, thumbW, min, max, step)

    Canvas(
        modifier
            .height(thumbHeight + 3.dp)
            .onPointerEvent { e ->
                when (e.type) {
                    PointerEventType.Press -> e.button == 0 && run {
                        drag.dragStart = minOf(start, end)
                        drag.dragEnd = maxOf(start, end)
                        val pressed = valueAt(e)
                        drag.draggingEnd = if (drag.dragStart == drag.dragEnd) null else abs(pressed - drag.dragEnd) <= abs(pressed - drag.dragStart)
                        apply(pressed)
                        true
                    }
                    PointerEventType.Move -> { apply(valueAt(e)); true }
                    PointerEventType.Release -> true
                    else -> false
                }
            }
    ) { x, y, w, h ->
        val trackY = y + (h - track) / 2f
        rect(x, trackY, w, track, theme.chipBackground, trackRadius)
        val usable = (w - thumbW).coerceAtLeast(0f)
        val startTip = startFraction * usable + thumbW
        val endTip = endFraction * usable
        if (endTip > startTip) rect(x + startTip, trackY, endTip - startTip, track, Accent, min(trackRadius, (endTip - startTip) / 2f))
        val thumbY = y + (h - thumbH) / 2f
        fillPath(triangleThumb(x + startFraction * usable, thumbY, thumbW, thumbH, pointsLeft = false), theme.controlThumbColor)
        fillPath(triangleThumb(x + endFraction * usable, thumbY, thumbW, thumbH, pointsLeft = true), theme.controlThumbColor)
    }
}

private fun triangleThumb(x: Float, y: Float, width: Float, height: Float, pointsLeft: Boolean, cornerRadius: Float = 2f): PolyPath {
    val corners = if (pointsLeft) {
        floatArrayOf(x + width, y, x, y + height / 2f, x + width, y + height)
    } else {
        floatArrayOf(x, y, x + width, y + height / 2f, x, y + height)
    }
    return PolyPath().apply {
        moveTo((corners[4] + corners[0]) / 2f, (corners[5] + corners[1]) / 2f)
        for (i in 0 until 3) {
            val next = (i + 1) % 3
            tangentArcTo(corners[i * 2], corners[i * 2 + 1], corners[next * 2], corners[next * 2 + 1], cornerRadius)
        }
        close()
    }
}
