package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.polyfrost.compose.host.LocalWheelHandler
import org.polyfrost.compose.host.WheelHandler
import org.polyfrost.compose.host.WheelInput
import org.polyfrost.compose.host.wheelInput
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import kotlin.math.abs
import kotlin.math.sign

object WheelGlide {
    const val NOTCH_DP = 96f

    const val DURATION_SECONDS = 0.2f

    const val EASE_START = 2f

    const val UNKNOWN_VIEWPORT_DP = 480f

    val PRECISE_DP: Float = if (DesktopHelper.isWindows) NOTCH_DP else 10f
}

fun PointerEvent.wheelScrollDp(): Offset {
    var delta = Offset.Zero
    for (i in changes.indices) delta += changes[i].scrollDelta
    return delta * when (wheelInput) {
        WheelInput.Direct -> 1f
        WheelInput.Precise -> WheelGlide.PRECISE_DP
        else -> WheelGlide.NOTCH_DP
    }
}

class WheelGlideRouter : WheelHandler {
    private var routing = false
    private var claimant: WheelScrollNode? = null
    private var blocked = false
    private var reached = false
    private var reachedWidthDp = 0f
    private var reachedHeightDp = 0f

    private val fallbackX = WheelSpring()
    private val fallbackY = WheelSpring()
    private var fallbackPushNanos = 0L
    private var fallbackFrameNanos = -1L

    override fun dispatch(delta: Offset, precise: Boolean, send: () -> Unit): Boolean {
        if (precise) {
            stop()
            send()
            return false
        }
        routing = true
        claimant = null
        blocked = false
        reached = false
        reachedWidthDp = 0f
        reachedHeightDp = 0f
        try {
            send()
        } finally {
            routing = false
        }
        val node = claimant
        claimant = null
        val distance = delta * WheelGlide.NOTCH_DP
        when {
            node != null -> {
                stop()
                node.push(distance)
            }
            reached && !blocked -> pushFallback(distance)
        }
        return node != null || blocked || reached
    }

    override fun Density.scrollOffset(event: PointerEvent, bounds: IntSize): Offset? {
        var delta = Offset.Zero
        val changes = event.changes
        for (i in changes.indices) delta += changes[i].scrollDelta
        if (event.wheelInput == WheelInput.Precise) return delta * (-WheelGlide.PRECISE_DP * density)
        if (!routing || Motion.reduced) return delta * (-WheelGlide.NOTCH_DP * density)
        if (!reached) {
            reached = true
            reachedWidthDp = bounds.width / density
            reachedHeightDp = bounds.height / density
        }
        return Offset.Zero
    }

    override fun frame(scroll: (Offset) -> Unit) {
        if (!fallbackX.active && !fallbackY.active) {
            fallbackFrameNanos = -1L
            return
        }
        val now = System.nanoTime()
        val nanos = if (fallbackFrameNanos < 0) now - fallbackPushNanos else now - fallbackFrameNanos
        fallbackFrameNanos = now
        val seconds = nanos / 1_000_000_000f
        val step = Offset(fallbackX.advance(seconds), fallbackY.advance(seconds))
        if (step != Offset.Zero) scroll(step)
    }

    override fun stop() {
        fallbackX.reset()
        fallbackY.reset()
        fallbackFrameNanos = -1L
    }

    internal val isRouting: Boolean get() = routing

    internal fun claim(node: WheelScrollNode) {
        claimant = node
        blocked = false
    }

    internal fun block() {
        claimant = null
        blocked = true
    }

    private fun pushFallback(delta: Offset) {
        if (delta.x != 0f) fallbackX.push(delta.x, capOf(reachedWidthDp))
        if (delta.y != 0f) fallbackY.push(delta.y, capOf(reachedHeightDp))
        fallbackPushNanos = System.nanoTime()
    }
}

fun Modifier.wheelScroll(
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
    private var viewportPx = 0

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
        if (pass != PointerEventPass.Initial || pointerEvent.type != PointerEventType.Scroll) return
        val router = currentValueOf(LocalWheelHandler) as? WheelGlideRouter ?: return
        if (!router.isRouting) return
        val amount = amountOf(pointerEvent.changes.fold(Offset.Zero) { sum, change -> sum + change.scrollDelta })
        if (amount == 0f) return
        val canScroll = if (amount > 0f) state.canScrollForward else state.canScrollBackward
        when {
            canScroll -> router.claim(this)
            contain() -> router.block()
            else -> return
        }
        viewportPx = if (orientation == Orientation.Vertical) bounds.height else bounds.width
        pointerEvent.changes.forEach { it.consume() }
    }

    override fun onCancelPointerInput() {}

    fun push(delta: Offset) {
        val density = currentValueOf(LocalDensity).density
        if (Motion.reduced) {
            val step = amountOf(delta) * density
            coroutineScope.launch { state.scrollBy(step) }
            return
        }
        spring.push(amountOf(delta), capOf(viewportPx / density))
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
                        if (step != 0f && abs(scrollBy(step) - step) > 0.5f) spring.reset()
                    }
                }
            }
        } finally {
            spring.reset()
        }
    }

    override fun onDetach() {
        spring.reset()
    }

    private fun amountOf(delta: Offset) = if (orientation == Orientation.Vertical) delta.y else delta.x
}

private fun capOf(viewportDp: Float): Float = if (viewportDp > 0f) viewportDp else WheelGlide.UNKNOWN_VIEWPORT_DP

private class WheelSpring {
    private var position = 0f
    private var target = 0f
    private var startPosition = 0f
    private var startVelocity = 0f
    private var elapsed = 0f

    var active = false
        private set

    fun push(delta: Float, cap: Float) {
        if (!active) {
            reset()
            active = true
        }
        val velocity = velocity()
        if (delta * (target - position) < 0f) target = position
        target += delta
        val remaining = target - position
        if (abs(remaining) > cap) target = position + sign(remaining) * cap
        val span = target - position
        val eased = span * WheelGlide.EASE_START / WheelGlide.DURATION_SECONDS
        val kept = if (velocity * span > 0f) velocity else 0f
        val limit = abs(span) * MAX_START / WheelGlide.DURATION_SECONDS
        startVelocity = (if (abs(kept) > abs(eased)) kept else eased).coerceIn(-limit, limit)
        startPosition = position
        elapsed = 0f
    }

    fun advance(seconds: Float): Float {
        if (!active) return 0f
        elapsed += seconds
        val duration = WheelGlide.DURATION_SECONDS
        if (elapsed >= duration) {
            val step = target - position
            reset()
            return step
        }
        val t = elapsed / duration
        val next = startPosition + startVelocity * duration * (t * t * t - 2f * t * t + t) + (target - startPosition) * (3f * t * t - 2f * t * t * t)
        val step = next - position
        position = next
        return step
    }

    private fun velocity(): Float {
        if (!active) return 0f
        val duration = WheelGlide.DURATION_SECONDS
        val t = (elapsed / duration).coerceIn(0f, 1f)
        return startVelocity * (3f * t * t - 4f * t + 1f) + (target - startPosition) / duration * (6f * t - 6f * t * t)
    }

    fun reset() {
        position = 0f
        target = 0f
        startPosition = 0f
        startVelocity = 0f
        elapsed = 0f
        active = false
    }

    private companion object {
        const val MAX_START = 3f
    }
}
