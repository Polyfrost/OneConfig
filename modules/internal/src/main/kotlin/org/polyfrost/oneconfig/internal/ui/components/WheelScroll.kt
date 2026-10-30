package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.exp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Hands each mouse wheel notch to the innermost [wheelScroll] list under the pointer that can still scroll its way */
class WheelRouter {
    internal var isRouting = false
        private set

    private var claimant: WheelScrollNode? = null

    /** Glides the list that claims this [delta] dp notch while [dispatch] delivers it */
    fun route(delta: Offset, dispatch: () -> Unit) {
        isRouting = true
        claimant = null
        try {
            dispatch()
        } finally {
            isRouting = false
        }
        val node = claimant
        claimant = null
        node?.push(delta)
    }

    internal fun claim(node: WheelScrollNode) {
        claimant = node
    }

    /** Makes the notch scroll nothing, for a contained list that's already at its end */
    internal fun block() {
        claimant = null
    }
}

val LocalWheelRouter = staticCompositionLocalOf<WheelRouter?> { null }

/**
 * Smoothly scrolls [state] for mouse wheel notches over this list, routed by [LocalWheelRouter].
 *
 * Once the list reaches its end, further notches scroll the list around it instead, unless [contain] returns true.
 *
 * Keep this before the list's own scroll modifier so it sees each notch before Compose's scrolling does.
 */
internal fun Modifier.wheelScroll(
    state: ScrollableState,
    orientation: Orientation = Orientation.Vertical,
    contain: () -> Boolean = { false },
): Modifier = this then WheelScrollElement(state, orientation, contain)

private data class WheelScrollElement(
    val state: ScrollableState,
    val orientation: Orientation,
    val contain: () -> Boolean,
) : ModifierNodeElement<WheelScrollNode>() {
    override fun create() = WheelScrollNode(state, orientation, contain)

    override fun update(node: WheelScrollNode) {
        node.update(state, orientation, contain)
    }
}

internal class WheelScrollNode(
    private var state: ScrollableState,
    private var orientation: Orientation,
    private var contain: () -> Boolean,
) : Modifier.Node(), PointerInputModifierNode, CompositionLocalConsumerModifierNode {
    private val spring = WheelSpring()
    private var glide: Job? = null
    private var pushNanos = 0L

    fun update(state: ScrollableState, orientation: Orientation, contain: () -> Boolean) {
        if (state !== this.state || orientation != this.orientation) {
            glide?.cancel()
            spring.reset()
        }
        this.state = state
        this.orientation = orientation
        this.contain = contain
    }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        // the initial pass reaches outer lists first, so the innermost claim is the last one made
        if (pass != PointerEventPass.Initial || pointerEvent.type != PointerEventType.Scroll) return
        val router = currentValueOf(LocalWheelRouter) ?: return
        if (!router.isRouting) return
        val amount = amountOf(pointerEvent.changes.fold(Offset.Zero) { sum, change -> sum + change.scrollDelta })
        if (amount == 0f) return
        val canScroll = if (amount > 0f) state.canScrollForward else state.canScrollBackward
        when {
            canScroll -> router.claim(this)
            contain() -> router.block()
            else -> return
        }
        // keeps Compose's own scrolling from also applying the notch
        pointerEvent.changes.forEach { it.consume() }
    }

    override fun onCancelPointerInput() {}

    fun push(delta: Offset) {
        spring.push(amountOf(delta))
        pushNanos = System.nanoTime()
        if (glide?.isActive != true) glide = coroutineScope.launch { glide() }
    }

    private suspend fun glide() {
        try {
            state.scroll(MutatePriority.Default) {
                var last = -1L
                while (spring.active) {
                    withFrameNanos { now ->
                        val nanos = if (last < 0) System.nanoTime() - pushNanos else now - last
                        last = now
                        val step = spring.advance(nanos / 1_000_000_000f) * currentValueOf(LocalDensity).density
                        // using less than asked for means the list reached its end
                        if (step != 0f && abs(scrollBy(step) - step) > 0.5f) spring.reset()
                    }
                }
            }
        } finally {
            // also ends the glide when another scroll of the list takes over
            spring.reset()
        }
    }

    override fun onDetach() {
        spring.reset()
    }

    private fun amountOf(delta: Offset) = if (orientation == Orientation.Vertical) delta.y else delta.x
}

/** Turns wheel notches into per-frame scroll steps, using Freya's critically damped spring */
private class WheelSpring {
    private var target = 0f
    private var position = 0f
    private var velocity = 0f

    var active = false
        private set

    fun push(delta: Float) {
        if (!active) {
            reset()
            active = true
        }
        // Reversing scroll cancels the rest of the glide
        if (delta * (target - position) < 0f) {
            target = position
            velocity = 0f
        }
        target += delta
    }

    fun advance(seconds: Float): Float {
        if (!active) return 0f
        val omega = 2f / SMOOTH_TIME
        val decay = exp(-omega * seconds)
        val change = position - target
        val linear = (velocity + change * omega) * seconds
        val nextVelocity = (velocity - linear * omega) * decay
        val next = target + (change + linear) * decay

        if (abs(target - next) < SETTLE_DISTANCE && abs(nextVelocity) < SETTLE_SPEED) {
            val step = target - position
            reset()
            return step
        }

        val step = next - position
        position = next
        velocity = nextVelocity
        return step
    }

    fun reset() {
        target = 0f
        position = 0f
        velocity = 0f
        active = false
    }

    private companion object {
        const val SMOOTH_TIME = 0.06f
        const val SETTLE_DISTANCE = 0.5f
        const val SETTLE_SPEED = 20f
    }
}
