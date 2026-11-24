@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.polyfrost.compose.host.controller.ControllerAxes
import org.polyfrost.compose.host.controller.controllerArrows
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle
import kotlin.math.abs
import kotlin.math.roundToInt

@ExperimentalOneConfigUiApi
@Composable
public fun RangeSlider(
    start: Float,
    end: Float,
    onValueChange: (start: Float, end: Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float = 0f,
    enabled: Boolean = true,
    label: String? = null,
    valueText: ((Float) -> String)? = null,
    interactionSource: MutableInteractionSource? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    size: ControlSize = SliderDefaults.size,
    keyStep: Float = 0f,
    contentDescription: String? = null,
    valueDescription: ((Float) -> String)? = null,
    trackColor: Color = Color.Unspecified,
    sound: UiSound? = UiSound.SliderTick,
) {
    val theme = OneConfigTheme.current
    val min = range.start
    val max = range.endInclusive
    val span = max - min
    val low = minOf(start, end)
    val high = maxOf(start, end)
    val latestLow by rememberUpdatedState(low)
    val latestHigh by rememberUpdatedState(high)
    val latestChange by rememberUpdatedState(onValueChange)
    val latestFinished by rememberUpdatedState(onValueChangeFinished)
    val source = interactionSource ?: remember { MutableInteractionSource() }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbWidth = rangeThumbWidth(size)
    val thumbHeight = rangeThumbHeight(size)
    val lowFraction = animateFloatAsState(sliderFraction(low, min, max), Motion.spec(spring()), label = "rangeStart")
    val highFraction = animateFloatAsState(sliderFraction(high, min, max), Motion.spec(spring()), label = "rangeEnd")

    fun describe(v: Float): String = valueDescription?.invoke(v) ?: valueText?.invoke(v) ?: formatSliderValue(v)

    val delta = sliderKeyStep(keyStep, step, span)
    val header = valueText?.let { "${it(low)} - ${it(high)}" }
    val startWord = layoutString("oneconfig.ui.range.start", "Start")
    val endWord = layoutString("oneconfig.ui.range.end", "End")

    fun thumbModifier(isEnd: Boolean): Modifier {
        val current = if (isEnd) high else low
        val lower = if (isEnd) low else min
        val upper = if (isEnd) max else high
        val spoken = describe(current)
        val name = listOfNotNull(contentDescription ?: label, if (isEnd) endWord else startWord).joinToString(", ")
        return Modifier
            .semantics {
                stateDescription = spoken
                progressBarRangeInfo = ProgressBarRangeInfo(current.coerceIn(min, max), min..max, sliderSteps(step, span))
                if (enabled) {
                    setProgress { target ->
                        val snapped = snapSliderValue(target, min, max, step).coerceIn(lower, upper)
                        if (isEnd) latestChange(latestLow, snapped) else latestChange(snapped, latestHigh)
                        latestFinished?.invoke()
                        true
                    }
                } else {
                    disabled()
                }
            }
            .a11y(name, A11yRole.Slider, null, null, theme.circleShape, true)
            .onKeyEvent { event ->
                if (!enabled || span <= 0f) return@onKeyEvent false
                val now = if (isEnd) latestHigh else latestLow
                val floor = if (isEnd) latestLow else min
                val ceiling = if (isEnd) max else latestHigh
                val next = sliderKeyTarget(event, now, min, max, delta) ?: return@onKeyEvent false
                val snapped = snapSliderValue(next, min, max, step).coerceIn(floor, ceiling)
                if (snapped != now) {
                    if (isEnd) latestChange(latestLow, snapped) else latestChange(snapped, latestHigh)
                    sound?.let(::playUiSound)
                    A11y.announce(describe(snapped))
                }
                latestFinished?.invoke()
                true
            }
            .controllerArrows(ControllerAxes.Horizontal)
            .focusable(enabled)
    }

    val startThumb = thumbModifier(false)
    val endThumb = thumbModifier(true)

    Column(
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha },
        verticalArrangement = Arrangement.spacedBy(theme.spacing.s),
    ) {
        if (label != null || header != null) SliderHeader(label, header)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeight + THUMB_CLEARANCE)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .hoverable(source, enabled)
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .pointerInput(enabled, min, max, step, source, thumbWidth, sound) {
                    if (!enabled) return@pointerInput
                    val thumbPx = thumbWidth.toPx()
                    val ticker = SliderTicker(sound)
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val drag = DragInteraction.Start()
                        source.tryEmit(drag)
                        var dragLow = latestLow
                        var dragHigh = latestHigh
                        fun valueAt(x: Float): Float = snapSliderValue(sliderValueAt(x, trackWidthPx, thumbPx, min, max), min, max, step)
                        val pressed = valueAt(down.position.x)
                        var draggingEnd: Boolean? = if (dragLow == dragHigh) null else abs(pressed - dragHigh) <= abs(pressed - dragLow)
                        fun apply(value: Float) {
                            if (draggingEnd == null && value != dragLow) draggingEnd = value > dragLow
                            when (draggingEnd) {
                                true -> dragHigh = value.coerceAtLeast(dragLow)
                                false -> dragLow = value.coerceAtMost(dragHigh)
                                null -> return
                            }
                            ticker.tick(value)
                            if (dragLow != latestLow || dragHigh != latestHigh) latestChange(dragLow, dragHigh)
                        }
                        try {
                            apply(pressed)
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        apply(valueAt(change.position.x))
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        } finally {
                            source.tryEmit(DragInteraction.Stop(drag))
                        }
                        latestFinished?.invoke()
                    }
                },
        ) {
            val trackHeight = SliderDefaults.trackHeight(size)
            val shape = theme.checkBoxShape
            val brush = theme.accentBrush
            val sheen = if (theme.style == ThemeStyle.Glass) RangeSheenBrush else null
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .align(Alignment.CenterStart)
                    .clip(shape)
                    .background(if (trackColor.isSpecified) trackColor else theme.chipBackground),
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .layout { measurable, constraints ->
                        val thumbPx = thumbWidth.toPx()
                        val usable = (trackWidthPx - thumbPx).coerceAtLeast(0f)
                        val from = lowFraction.value * usable + thumbPx
                        val to = highFraction.value * usable
                        val width = (to - from).roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        layout(to.roundToInt().coerceAtLeast(width), placeable.height) { placeable.place(from.roundToInt(), 0) }
                    }
                    .height(trackHeight)
                    .clip(shape)
                    .drawBehind {
                        drawRect(brush)
                        if (sheen != null) drawRect(sheen)
                    },
            )
            RangeThumb(false, thumbWidth, thumbHeight, { lowFraction.value }, { trackWidthPx }, startThumb)
            RangeThumb(true, thumbWidth, thumbHeight, { highFraction.value }, { trackWidthPx }, endThumb)
        }
    }
}

private val THUMB_CLEARANCE = 3.dp
private val THUMB_MAX_CORNER = 2.dp
private const val SHEEN_TOP_ALPHA = 0.2f
private const val SHEEN_BOTTOM_ALPHA = 0.04f

private val RangeSheenBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = SHEEN_TOP_ALPHA), Color.White.copy(alpha = SHEEN_BOTTOM_ALPHA)))

private fun rangeThumbWidth(size: ControlSize): Dp = if (size == ControlSize.Compact) 11.dp else 13.dp

private fun rangeThumbHeight(size: ControlSize): Dp = if (size == ControlSize.Compact) 14.dp else 16.dp

@Composable
private fun BoxScope.RangeThumb(
    pointsLeft: Boolean,
    width: Dp,
    height: Dp,
    fraction: () -> Float,
    trackWidthPx: () -> Float,
    modifier: Modifier,
) {
    val theme = OneConfigTheme.current
    val color = theme.controlThumbColor
    val cornerShape = theme.buttonShape
    Canvas(
        Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset((fraction() * (trackWidthPx() - width.toPx()).coerceAtLeast(0f)).roundToInt(), 0) }
            .size(width, height)
            .then(modifier),
    ) {
        val shapeRadius = (cornerShape.createOutline(size, layoutDirection, this) as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
        val radius = minOf(THUMB_MAX_CORNER.toPx(), shapeRadius)
        val corners = if (pointsLeft) {
            listOf(Offset(size.width, 0f), Offset(0f, size.height / 2f), Offset(size.width, size.height))
        } else {
            listOf(Offset(0f, 0f), Offset(size.width, size.height / 2f), Offset(0f, size.height))
        }
        val path = Path().apply {
            corners.forEachIndexed { index, corner ->
                val point = corner.insetTowards(corners[(index + corners.size - 1) % corners.size], corners[(index + 1) % corners.size], radius)
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
        drawPath(path, color)
        if (radius > 0f) drawPath(path, color, style = Stroke(width = radius * 2f, join = StrokeJoin.Round, cap = StrokeCap.Round))
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
