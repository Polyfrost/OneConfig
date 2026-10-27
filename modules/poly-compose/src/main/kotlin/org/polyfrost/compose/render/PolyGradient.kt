package org.polyfrost.compose.render

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

sealed class PolyGradient {
    abstract val colors: List<PolyColor>
    abstract val stops: List<Float>?

    abstract fun position(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Float

    open val affine: Boolean get() = false

    open val linear: Boolean get() = false

    fun colorAt(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Int = sample(position(px, py, x, y, w, h))

    fun sample(t: Float): Int {
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

    data class Linear(
        override val colors: List<PolyColor>,
        override val stops: List<Float>? = null,
        val angle: Float = 0f,
    ) : PolyGradient() {
        override val affine get() = colors.size <= 2 && stops == null
        override val linear get() = true

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

    data class Radial(
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

    data class Points(
        override val colors: List<PolyColor>,
        override val stops: List<Float>?,
        val x0: Float, val y0: Float, val x1: Float, val y1: Float,
    ) : PolyGradient() {
        override val affine get() = colors.size <= 2 && stops == null
        override val linear get() = true

        override fun position(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Float {
            val dx = x1 - x0
            val dy = y1 - y0
            val len2 = dx * dx + dy * dy
            if (len2 <= 0f) return 0f
            return ((px - x0) * dx + (py - y0) * dy) / len2
        }
    }

    companion object {
        fun horizontal(vararg colors: PolyColor) = Linear(colors.toList(), angle = 0f)
        fun vertical(vararg colors: PolyColor) = Linear(colors.toList(), angle = 90f)
        fun diagonal(vararg colors: PolyColor) = Linear(colors.toList(), angle = 45f)
        fun radial(vararg colors: PolyColor) = Radial(colors.toList())

        fun horizontal(vararg colors: Int) = horizontal(*colors.map { PolyColor(it) }.toTypedArray())
        fun vertical(vararg colors: Int) = vertical(*colors.map { PolyColor(it) }.toTypedArray())

        @JvmStatic
        fun lerpArgb(a: Int, b: Int, t: Float): Int {
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
