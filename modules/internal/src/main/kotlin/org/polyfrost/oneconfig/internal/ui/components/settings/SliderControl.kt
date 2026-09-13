package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

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
) {
    val theme = LocalTheme.current
    var trackWidthPx by remember { mutableStateOf(0f) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val fraction by animateFloatAsState(
        ((value - min) / (max - min)).coerceIn(0f, 1f),
        animationSpec = spring(),
    )

    Box(
        modifier = modifier
            .height(thumbSize)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .then(remember(min, max, step, thumbSize) {
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
                SliderPointerInput(thumbSize) { fraction ->
                    snap(min + fraction * (max - min)).let { maybeTick(it); currentOnValueChange(it) }
                }
            })
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

private data class SliderPointerInput(
    val thumbSize: Dp,
    val onDrag: (Float) -> Unit,
) : ModifierNodeElement<SliderPointerNode>() {
    override fun create() = SliderPointerNode(this)

    override fun update(node: SliderPointerNode) {
        node.input = this
    }
}

private class SliderPointerNode(var input: SliderPointerInput) : Modifier.Node(), PointerInputModifierNode {
    private var pointer: PointerId? = null
    private var dragStartX = 0f
    private var dragWidth = 1f
    private var lastFraction = Float.NaN

    // Keep the active drag alive when UI scaling changes the density.
    // The drag continues using the slider bounds captured on mouse-down instead of the resized bounds.
    override fun onDensityChange() = Unit

    override fun onCancelPointerInput() {
        pointer = null
    }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Main) return
        val starting = pointer == null
        val change = if (starting) {
            pointerEvent.changes.firstOrNull { it.changedToDown() }?.also { pointer = it.id }
        } else {
            pointerEvent.changes.firstOrNull { it.id == pointer }
        } ?: return

        if (!change.pressed) {
            pointer = null
            return
        }

        val coordinates = requireLayoutCoordinates()
        if (starting) {
            val thumbPx = with(requireDensity()) { input.thumbSize.toPx() }
            dragStartX = coordinates.localToWindow(Offset(thumbPx / 2f, 0f)).x
            val endX = coordinates.localToWindow(Offset(bounds.width - thumbPx / 2f, 0f)).x
            dragWidth = (endX - dragStartX).coerceAtLeast(1f)
            lastFraction = Float.NaN
        }
        val windowX = coordinates.localToWindow(change.position).x
        val fraction = ((windowX - dragStartX) / dragWidth).coerceIn(0f, 1f)
        change.consume()
        if (fraction != lastFraction) {
            lastFraction = fraction
            input.onDrag(fraction)
        }
    }
}
