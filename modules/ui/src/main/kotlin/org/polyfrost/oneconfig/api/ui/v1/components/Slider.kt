@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TICK_INTERVAL_MILLIS = 70L
private const val KEY_STEPS_WITHOUT_STEP = 100
private const val PAGE_STEPS = 10
private const val MUTED_FILL_ALPHA = 0.4f
private const val SHEEN_TOP_ALPHA = 0.2f
private const val SHEEN_BOTTOM_ALPHA = 0.04f

private val SheenBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = SHEEN_TOP_ALPHA), Color.White.copy(alpha = SHEEN_BOTTOM_ALPHA)))

@ExperimentalOneConfigUiApi
public object SliderDefaults {
    public val size: ControlSize = ControlSize.Regular

    public const val MUTED_THUMB_ALPHA: Float = 0.5f

    public fun thumbSize(size: ControlSize): Dp = when (size) {
        ControlSize.Compact -> 14.dp
        ControlSize.Regular -> 19.dp
        ControlSize.Large -> 21.dp
        ControlSize.ExtraLarge -> 24.dp
    }

    public fun trackHeight(size: ControlSize): Dp = when (size) {
        ControlSize.Compact -> 4.dp
        ControlSize.Regular -> 5.dp
        ControlSize.Large -> 6.dp
        ControlSize.ExtraLarge -> 7.dp
    }

    public val trackColor: Color
        @Composable get() = restTrackColor(OneConfigTheme.current)

    public val mutedFillColor: Color
        @Composable get() = OneConfigTheme.current.textColorSecondary.copy(alpha = MUTED_FILL_ALPHA)
}

@ExperimentalOneConfigUiApi
@Composable
public fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
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
    muted: Boolean = false,
    trackColor: Color = Color.Unspecified,
    sound: UiSound? = UiSound.SliderTick,
    fillColor: Color = Color.Unspecified,
) {
    val theme = OneConfigTheme.current
    val min = range.start
    val max = range.endInclusive
    val span = max - min
    val latestChange by rememberUpdatedState(onValueChange)
    val latestFinished by rememberUpdatedState(onValueChangeFinished)
    val latestValue by rememberUpdatedState(value)
    val source = interactionSource ?: remember { MutableInteractionSource() }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbSize = SliderDefaults.thumbSize(size)
    val fraction = animateFloatAsState(sliderFraction(value, min, max), Motion.spec(spring()), label = "sliderFraction")
    val mutedAmount = animateFloatAsState(if (muted) 1f else 0f, Motion.tweenSpec(Motion.standard), label = "sliderMuted")

    fun describe(v: Float): String = valueDescription?.invoke(v) ?: valueText?.invoke(v) ?: formatSliderValue(v)

    val delta = sliderKeyStep(keyStep, step, span)
    val spoken = describe(value)
    val shape = theme.checkBoxShape

    Column(
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha },
        verticalArrangement = Arrangement.spacedBy(theme.spacing.s),
    ) {
        if (label != null || valueText != null) SliderHeader(label, valueText?.invoke(value))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbSize)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .semantics {
                    stateDescription = spoken
                    progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(min, max), min..max, sliderSteps(step, span))
                    if (enabled) {
                        setProgress { target ->
                            val snapped = snapSliderValue(target, min, max, step)
                            if (snapped != latestValue) latestChange(snapped)
                            latestFinished?.invoke()
                            true
                        }
                    } else {
                        disabled()
                    }
                }
                .a11y(contentDescription ?: label ?: spoken, A11yRole.Slider, null, null, shape, true)
                .onKeyEvent { event ->
                    if (!enabled || span <= 0f) return@onKeyEvent false
                    val current = latestValue
                    val next = sliderKeyTarget(event, current, min, max, delta) ?: return@onKeyEvent false
                    val snapped = snapSliderValue(next, min, max, step)
                    if (snapped != current) {
                        latestChange(snapped)
                        sound?.let(::playUiSound)
                        A11y.announce(describe(snapped))
                    }
                    latestFinished?.invoke()
                    true
                }
                .focusable(enabled, source)
                .hoverable(source, enabled)
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .pointerInput(enabled, min, max, step, source, thumbSize, sound) {
                    if (!enabled) return@pointerInput
                    val thumbPx = thumbSize.toPx()
                    val ticker = SliderTicker(sound)
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val drag = DragInteraction.Start()
                        source.tryEmit(drag)
                        fun emit(x: Float) {
                            val next = snapSliderValue(sliderValueAt(x, trackWidthPx, thumbPx, min, max), min, max, step)
                            ticker.tick(next)
                            if (next != latestValue) latestChange(next)
                        }
                        try {
                            emit(down.position.x)
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        emit(change.position.x)
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
            SliderTrackLayers(
                size,
                if (trackColor.isSpecified) trackColor else restTrackColor(theme),
                { trackWidthPx },
                null,
                { fraction.value },
                mutedAmount,
                sheen = size == ControlSize.Compact && !fillColor.isSpecified,
                fillColor = fillColor,
            )
            SliderThumb(size, { fraction.value }, { trackWidthPx }, mutedAmount, Modifier)
        }
    }
}

@Composable
internal fun SliderHeader(label: String?, value: String?) {
    val theme = OneConfigTheme.current
    val typography = theme.typography
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label ?: "", Modifier.weight(1f), theme.textColor, typography.label, 1, TextOverflow.Ellipsis, textAlign = TextAlign.Start)
        if (value != null) {
            Text(value, Modifier, theme.textColorSecondary, typography.small, 1, TextOverflow.Clip, textAlign = TextAlign.End)
        }
    }
}

@Composable
internal fun BoxScope.SliderTrackLayers(
    size: ControlSize,
    trackColor: Color,
    trackWidthPx: () -> Float,
    fillStart: (() -> Float)?,
    fillEnd: () -> Float,
    muted: State<Float>,
    sheen: Boolean = true,
    fillColor: Color = Color.Unspecified,
) {
    val theme = OneConfigTheme.current
    val shape = theme.checkBoxShape
    val brush = if (fillColor.isSpecified) SolidColor(fillColor) else theme.accentBrush
    val sheenBrush = if (sheen && theme.style == ThemeStyle.Glass) SheenBrush else null
    val mutedFill = SliderDefaults.mutedFillColor
    val trackHeight = SliderDefaults.trackHeight(size)
    val thumbSize = SliderDefaults.thumbSize(size)
    Box(
        Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .align(Alignment.CenterStart)
            .clip(shape)
            .background(trackColor),
    )
    Box(
        Modifier
            .align(Alignment.CenterStart)
            .layout { measurable, constraints ->
                val thumbPx = thumbSize.toPx()
                val usable = (trackWidthPx() - thumbPx).coerceAtLeast(0f)
                val from = fillStart?.let { it() * usable + thumbPx / 2f } ?: 0f
                val to = if (fillStart == null) fillEnd() * trackWidthPx() else (fillEnd() * usable + thumbPx / 2f).coerceAtLeast(from)
                val width = (to - from).roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                layout(to.roundToInt().coerceAtLeast(width), placeable.height) { placeable.place(from.roundToInt(), 0) }
            }
            .height(trackHeight)
            .clip(shape)
            .drawBehind {
                val m = muted.value
                if (m < 1f) {
                    drawRect(brush, alpha = 1f - m)
                    if (sheenBrush != null) drawRect(sheenBrush, alpha = 1f - m)
                }
                if (m > 0f) drawRect(mutedFill, alpha = m)
            },
    )
}

@Composable
internal fun BoxScope.SliderThumb(
    size: ControlSize,
    fraction: () -> Float,
    trackWidthPx: () -> Float,
    muted: State<Float>,
    modifier: Modifier,
) {
    val theme = OneConfigTheme.current
    val thumbSize = SliderDefaults.thumbSize(size)
    Box(
        Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset((fraction() * (trackWidthPx() - thumbSize.toPx()).coerceAtLeast(0f)).roundToInt(), 0) }
            .size(thumbSize)
            .then(modifier)
            .graphicsLayer { alpha = 1f - muted.value * (1f - SliderDefaults.MUTED_THUMB_ALPHA) }
            .background(theme.controlThumbColor, theme.circleShape),
    )
}

internal class SliderTicker(private val sound: UiSound? = UiSound.SliderTick) {
    private var lastValue = Float.NaN
    private var lastAt = 0L

    fun tick(value: Float) {
        if (value == lastValue) return
        lastValue = value
        val now = System.currentTimeMillis()
        if (now - lastAt >= TICK_INTERVAL_MILLIS) {
            lastAt = now
            sound?.let(::playUiSound)
        }
    }
}

internal fun sliderFraction(value: Float, min: Float, max: Float): Float {
    val span = max - min
    return if (span > 0f) ((value - min) / span).coerceIn(0f, 1f) else 0f
}

internal fun snapSliderValue(raw: Float, min: Float, max: Float, step: Float): Float {
    val clamped = raw.coerceIn(min, max)
    if (step <= 0f) return clamped
    return (min + ((clamped - min) / step).roundToInt() * step).coerceIn(min, max)
}

internal fun sliderValueAt(x: Float, trackWidthPx: Float, thumbPx: Float, min: Float, max: Float): Float {
    val usable = (trackWidthPx - thumbPx).coerceAtLeast(1f)
    return min + ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f) * (max - min)
}

internal fun sliderKeyStep(keyStep: Float, step: Float, span: Float): Float = when {
    keyStep > 0f -> keyStep
    step > 0f -> step
    else -> span / KEY_STEPS_WITHOUT_STEP
}

internal fun sliderSteps(step: Float, span: Float): Int =
    if (step > 0f && span > 0f) ((span / step).roundToInt() - 1).coerceAtLeast(0) else 0

internal fun sliderKeyTarget(event: KeyEvent, current: Float, min: Float, max: Float, delta: Float): Float? {
    if (event.type != KeyEventType.KeyDown) return null
    return when (event.key) {
        Key.DirectionLeft, Key.DirectionDown -> current - delta
        Key.DirectionRight, Key.DirectionUp -> current + delta
        Key.PageDown -> current - delta * PAGE_STEPS
        Key.PageUp -> current + delta * PAGE_STEPS
        Key.MoveHome -> min
        Key.MoveEnd -> max
        else -> null
    }
}

private const val WHOLE_EPSILON = 0.0001f

internal fun formatSliderValue(value: Float): String {
    val whole = value.roundToInt()
    if (abs(value - whole) < WHOLE_EPSILON) return whole.toString()
    return "%.2f".format(value).trimEnd('0').trimEnd('.', ',')
}
