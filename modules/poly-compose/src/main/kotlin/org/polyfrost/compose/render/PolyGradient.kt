package org.polyfrost.compose.render

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

public sealed class PolyGradient {
    public abstract val colors: List<PolyColor>
    public abstract val stops: List<Float>?

    public abstract fun position(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Float

    public open val affine: Boolean get() = false

    public open val linear: Boolean get() = false

    public fun colorAt(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Int = sample(position(px, py, x, y, w, h))

    public fun sample(t: Float): Int {
        val n = colors.size
        if (n == 0) return 0
        if (n == 1) return colors[0].argb
        val tc = t.coerceIn(0f, 1f)
        val s = stops
        var i = 0
        while (i < n - 2 && tc > stop(s, i + 1, n)) i++
        val a = stop(s, i, n)
        val b = stop(s, i + 1, n)
        val f = if (b > a) ((tc - a) / (b - a)).coerceIn(0f, 1f) else 0f
        return lerpArgb(colors[i].argb, colors[i + 1].argb, f)
    }

    private fun stop(s: List<Float>?, i: Int, n: Int) = s?.getOrNull(i) ?: (i.toFloat() / (n - 1))

    public data class Linear(
        override val colors: List<PolyColor>,
        override val stops: List<Float>? = null,
        val angle: Float = 0f,
    ) : PolyGradient() {
        override val affine: Boolean get() = colors.size <= 2 && stops == null
        override val linear: Boolean get() = true

        private val cosA = cos(Math.toRadians(angle.toDouble()))
        private val sinA = sin(Math.toRadians(angle.toDouble()))

        override fun position(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Float {
            val dx = (w / 2f * cosA).toFloat() * 2f
            val dy = (h / 2f * sinA).toFloat() * 2f
            val len2 = dx * dx + dy * dy
            if (len2 <= 0f) return 0f
            val sx = x + w / 2f - dx / 2f
            val sy = y + h / 2f - dy / 2f
            return ((px - sx) * dx + (py - sy) * dy) / len2
        }
    }

    public data class Radial(
        override val colors: List<PolyColor>,
        override val stops: List<Float>? = null,
        val centerX: Float = 0.5f,
        val centerY: Float = 0.5f,
        val radius: Float = 0.5f,
    ) : PolyGradient() {
        override fun position(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Float {
            val r = radius * minOf(w, h)
            if (r <= 0f) return 1f
            return hypot(px - (x + centerX * w), py - (y + centerY * h)) / r
        }
    }

    public data class Sweep(
        override val colors: List<PolyColor>,
        override val stops: List<Float>? = null,
        val centerX: Float = 0.5f,
        val centerY: Float = 0.5f,
    ) : PolyGradient() {
        override fun position(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Float {
            val t = atan2(py - (y + centerY * h), px - (x + centerX * w)) / (2f * Math.PI.toFloat())
            return if (t < 0f) t + 1f else t
        }
    }

    public data class Points(
        override val colors: List<PolyColor>,
        override val stops: List<Float>?,
        val x0: Float, val y0: Float, val x1: Float, val y1: Float,
    ) : PolyGradient() {
        override val affine: Boolean get() = colors.size <= 2 && stops == null
        override val linear: Boolean get() = true

        override fun position(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Float {
            val dx = x1 - x0
            val dy = y1 - y0
            val len2 = dx * dx + dy * dy
            if (len2 <= 0f) return 0f
            return ((px - x0) * dx + (py - y0) * dy) / len2
        }
    }

    public companion object {
        public fun horizontal(vararg colors: PolyColor): Linear = Linear(colors.toList(), angle = 0f)
        public fun vertical(vararg colors: PolyColor): Linear = Linear(colors.toList(), angle = 90f)
        public fun diagonal(vararg colors: PolyColor): Linear = Linear(colors.toList(), angle = 45f)
        public fun radial(vararg colors: PolyColor): Radial = Radial(colors.toList())

        public fun horizontal(vararg colors: Int): Linear = horizontal(*colors.map { PolyColor(it) }.toTypedArray())
        public fun vertical(vararg colors: Int): Linear = vertical(*colors.map { PolyColor(it) }.toTypedArray())

        @JvmStatic
        public fun lerpArgb(a: Int, b: Int, t: Float): Int {
            if (a == b) return a
            var out = 0
            var shift = 0
            while (shift <= 24) {
                val ca = (a ushr shift) and 0xFF
                val cb = (b ushr shift) and 0xFF
                out = out or (((ca + (cb - ca) * t).roundToInt().coerceIn(0, 255)) shl shift)
                shift += 8
            }
            return out
        }
    }
}
