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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val THUMB_SIZE = 19.dp
private val TRACK_HEIGHT = 5.dp
private const val TICK_INTERVAL_MILLIS = 70L
private const val KEY_STEPS_WITHOUT_STEP = 100
private const val PAGE_STEPS = 10

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
) {
    val theme = OneConfigTheme.current
    val typography = theme.typography
    val min = range.start
    val max = range.endInclusive
    val span = max - min
    val latestChange by rememberUpdatedState(onValueChange)
    val latestFinished by rememberUpdatedState(onValueChangeFinished)
    val latestValue by rememberUpdatedState(value)
    val source = interactionSource ?: remember { MutableInteractionSource() }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val fraction = animateFloatAsState(
        if (span > 0f) ((value - min) / span).coerceIn(0f, 1f) else 0f,
        Motion.spec(spring()),
        label = "sliderFraction",
    )

    fun snap(raw: Float): Float {
        val clamped = raw.coerceIn(min, max)
        if (step <= 0f) return clamped
        return (min + ((clamped - min) / step).roundToInt() * step).coerceIn(min, max)
    }

    fun describe(v: Float): String = valueText?.invoke(v) ?: formatSliderValue(v)

    val keyStep = if (step > 0f) step else span / KEY_STEPS_WITHOUT_STEP
    val spoken = describe(value)
    val shape = theme.checkBoxShape

    Column(
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha },
        verticalArrangement = Arrangement.spacedBy(theme.spacing.s),
    ) {
        if (label != null || valueText != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label ?: "",
                    Modifier.weight(1f),
                    theme.textColor,
                    OneConfigTheme.typography.body.copy(fontSize = typography.label.fontSize, fontWeight = typography.label.fontWeight ?: FontWeight.Normal),
                    1,
                    TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                )
                if (valueText != null) {
                    Text(
                        valueText(value),
                        Modifier,
                        theme.textColorSecondary,
                        OneConfigTheme.typography.body.copy(fontSize = typography.small.fontSize, fontWeight = typography.small.fontWeight ?: FontWeight.Normal),
                        1,
                        TextOverflow.Clip,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(THUMB_SIZE)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .semantics {
                    stateDescription = spoken
                    progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(min, max), min..max, if (step > 0f && span > 0f) (span / step).roundToInt() - 1 else 0)
                    if (enabled) {
                        setProgress { target ->
                            val snapped = snap(target)
                            if (snapped != latestValue) latestChange(snapped)
                            latestFinished?.invoke()
                            true
                        }
                    } else {
                        disabled()
                    }
                }
                .a11y(label ?: spoken, A11yRole.Slider, null, null, shape, true)
                .onKeyEvent { event ->
                    if (!enabled || event.type != KeyEventType.KeyDown || span <= 0f) return@onKeyEvent false
                    val current = latestValue
                    val next = when (event.key) {
                        Key.DirectionLeft, Key.DirectionDown -> current - keyStep
                        Key.DirectionRight, Key.DirectionUp -> current + keyStep
                        Key.PageDown -> current - keyStep * PAGE_STEPS
                        Key.PageUp -> current + keyStep * PAGE_STEPS
                        Key.MoveHome -> min
                        Key.MoveEnd -> max
                        else -> return@onKeyEvent false
                    }
                    val snapped = snap(next)
                    if (snapped != current) {
                        latestChange(snapped)
                        playUiSound(UiSound.SliderTick)
                        A11y.announce(describe(snapped))
                    }
                    latestFinished?.invoke()
                    true
                }
                .focusable(enabled, source)
                .hoverable(source, enabled)
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .pointerInput(enabled, min, max, step, source) {
                    if (!enabled) return@pointerInput
                    val thumbPx = THUMB_SIZE.toPx()
                    var lastTickValue = Float.NaN
                    var lastTickAt = 0L
                    fun emit(x: Float) {
                        val usable = (trackWidthPx - thumbPx).coerceAtLeast(1f)
                        val newValue = snap(min + ((x - thumbPx / 2f) / usable).coerceIn(0f, 1f) * span)
                        if (newValue != lastTickValue) {
                            lastTickValue = newValue
                            val now = System.currentTimeMillis()
                            if (now - lastTickAt >= TICK_INTERVAL_MILLIS) {
                                lastTickAt = now
                                playUiSound(UiSound.SliderTick)
                            }
                        }
                        if (newValue != latestValue) latestChange(newValue)
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val drag = DragInteraction.Start()
                        source.tryEmit(drag)
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(TRACK_HEIGHT)
                    .align(Alignment.CenterStart)
                    .clip(shape)
                    .background(theme.controlTrackColor),
            )
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val w = (constraints.maxWidth * fraction.value).roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth)
                        val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                        layout(w, placeable.height) { placeable.place(0, 0) }
                    }
                    .height(TRACK_HEIGHT)
                    .align(Alignment.CenterStart)
                    .clip(shape)
                    .background(theme.accentBrush),
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((fraction.value * (trackWidthPx - THUMB_SIZE.toPx())).roundToInt(), 0) }
                    .size(THUMB_SIZE)
                    .background(theme.controlThumbColor, theme.circleShape),
            )
        }
    }
}

private const val WHOLE_EPSILON = 0.0001f

private fun formatSliderValue(value: Float): String {
    val whole = value.roundToInt()
    if (abs(value - whole) < WHOLE_EPSILON) return whole.toString()
    return "%.2f".format(value).trimEnd('0').trimEnd('.', ',')
}
