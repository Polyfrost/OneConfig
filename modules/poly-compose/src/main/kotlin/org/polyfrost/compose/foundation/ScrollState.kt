package org.polyfrost.compose.foundation

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class ScrollState(initial: Float = 0f) {
    var value = initial
        private set
    private var target = initial
    private var lastNanos = 0L

    var viewport = 0f
        internal set
    var content = 0f
        internal set

    val maxValue get() = max(0f, content - viewport)

    internal var laidOutValue = Float.NaN

    val animating get() = value != target

    fun scrollTo(v: Float) {
        value = v.coerceIn(0f, maxValue)
        target = value
    }

    fun animateBy(delta: Float) {
        if (!animating) lastNanos = 0L
        target = (target + delta).coerceIn(0f, maxValue)
    }

    fun wheel(notches: Float): Boolean {
        val delta = -wheelStep(notches, viewport)
        if (delta < 0f && target <= 0f || delta > 0f && target >= maxValue) return false
        animateBy(delta)
        return true
    }

    fun step(nanos: Long): Boolean {
        target = target.coerceIn(0f, maxValue)
        if (value == target) {
            value = value.coerceIn(0f, maxValue)
            return false
        }
        val dtMs = if (lastNanos == 0L) 16f else ((nanos - lastNanos) / 1e6f).coerceIn(0f, 100f)
        lastNanos = nanos
        val remaining = target - value
        val speed = max(abs(remaining) / 100f, 1f) * dtMs
        value = if (abs(remaining) <= speed) target else value + if (remaining > 0f) speed else -speed
        return value != target
    }

    companion object {
        private val os = System.getProperty("os.name", "").lowercase()

        @JvmStatic
        var wheelStep: (notches: Float, viewport: Float) -> Float = when {
            os.contains("mac") -> { n, _ -> n * 20f }
            os.contains("win") -> { n, v -> n * v * 0.4f }
            else -> { n, v -> n * 8f * sqrt(max(v, 0f)) }
        }

        @JvmStatic
        fun thumb(state: ScrollState, track: Float, minThumb: Float): Pair<Float, Float> {
            val total = max(state.content, state.viewport)
            if (total <= 0f) return track to 0f
            val len = min(track, max(minThumb, track * state.viewport / total))
            val max = state.maxValue
            val pos = if (max <= 0f) 0f else (track - len) * state.value / max
            return len to pos
        }
    }
}
