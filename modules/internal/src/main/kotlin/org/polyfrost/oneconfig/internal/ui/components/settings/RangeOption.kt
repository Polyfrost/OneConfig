package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.internal.ui.api.settings.RangeOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/**
 * A start and end pair shown either as a two-thumb slider with an input per end or as the inputs alone
 *
 * Whatever is shown the stored pair always has start at or below end
 */
@Composable
fun RangeOption(data: RangeOptionData) {
    val initial = data.read()
    var start by remember(data.prop) { mutableStateOf(initial?.first ?: data.min) }
    var end by remember(data.prop) { mutableStateOf(initial?.second ?: data.max) }

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
    val currentStart by rememberUpdatedState(start)
    val currentEnd by rememberUpdatedState(end)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    var trackWidthPx by remember { mutableStateOf(0f) }

    val thumbPx = with(LocalDensity.current) { thumbWidth.toPx() }
    val span = (max - min).takeIf { it != 0f } ?: 1f
    val startFraction by animateFloatAsState(((minOf(start, end) - min) / span).coerceIn(0f, 1f), animationSpec = spring())
    val endFraction by animateFloatAsState(((maxOf(start, end) - min) / span).coerceIn(0f, 1f), animationSpec = spring())

    Box(
        modifier = modifier
            .height(thumbHeight + 3.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(min, max, step) {
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
                    var dragStart = minOf(currentStart, currentEnd)
                    var dragEnd = maxOf(currentStart, currentEnd)
                    val pressed = xToValue(down.position.x)
                    var draggingEnd = if (dragStart == dragEnd) null else abs(pressed - dragEnd) <= abs(pressed - dragStart)
                    fun apply(value: Float) {
                        if (draggingEnd == null && value != dragStart) draggingEnd = value > dragStart
                        when (draggingEnd) {
                            true -> dragEnd = value.coerceAtLeast(dragStart)
                            false -> dragStart = value.coerceAtMost(dragEnd)
                            null -> return
                        }
                        maybeTick(value)
                        currentOnValueChange(dragStart, dragEnd)
                    }
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
                .fillMaxWidth().height(trackHeight)
                .align(Alignment.CenterStart)
                .clip(theme.checkBoxShape)
                .background(theme.chipBackground)
        )
        val usablePx = (trackWidthPx - thumbPx).coerceAtLeast(0f)
        val startTipPx = startFraction * usablePx + thumbPx
        val endTipPx = endFraction * usablePx
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(startTipPx.toInt(), 0) }
                .fillMaxWidth(
                    if (trackWidthPx <= 0f) 0f else ((endTipPx - startTipPx) / trackWidthPx).coerceAtLeast(0f)
                )
                .height(trackHeight)
                .clip(theme.checkBoxShape)
                .background(Accent)
        )
        listOf(startFraction to false, endFraction to true).forEach { (fraction, pointsLeft) ->
            TriangleThumb(
                pointsLeft = pointsLeft,
                color = theme.controlThumbColor,
                width = thumbWidth,
                height = thumbHeight,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((fraction * usablePx).toInt(), 0) },
            )
        }
    }
}

@Composable
private fun TriangleThumb(
    pointsLeft: Boolean,
    color: Color,
    width: Dp,
    height: Dp,
    cornerRadius: Dp = 2.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(width, height)) {
        val radius = cornerRadius.toPx()
        val corners = if (pointsLeft) {
            listOf(
                Offset(size.width, 0f),
                Offset(0f, size.height / 2f),
                Offset(size.width, size.height),
            )
        } else {
            listOf(
                Offset(0f, 0f),
                Offset(size.width, size.height / 2f),
                Offset(0f, size.height),
            )
        }
        val path = Path().apply {
            corners.forEachIndexed { index, corner ->
                val point = corner.insetTowards(
                    corners[(index + corners.size - 1) % corners.size],
                    corners[(index + 1) % corners.size],
                    radius,
                )
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
        drawPath(path, color)
        drawPath(path, color, style = Stroke(width = radius * 2f, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

private fun Offset.insetTowards(previous: Offset, next: Offset, radius: Float): Offset {
    val toPrevious = (previous - this).normalizedOrZero()
    val toNext = (next - this).normalizedOrZero()
    val bisector = (toPrevious + toNext).normalizedOrZero()
    if (bisector == Offset.Zero) return this
    val sinHalfAngle = abs(toPrevious.x * bisector.y - toPrevious.y * bisector.x)
    if (sinHalfAngle < 1e-4f) return this
    return this + bisector * (radius / sinHalfAngle)
}

private fun Offset.normalizedOrZero(): Offset {
    val length = getDistance()
    return if (length < 1e-4f) Offset.Zero else this / length
}
