package org.polyfrost.compose.ui.graphics

import org.polyfrost.compose.render.PolyColor
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

sealed class Brush {
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

    companion object {
        fun horizontalGradient(colors: List<PolyColor>) = LinearGradient(colors, angle = 0f)
        fun horizontalGradient(vararg colors: PolyColor) = horizontalGradient(colors.toList())
        fun horizontalGradient(vararg colorStops: Pair<Float, PolyColor>) = LinearGradient(colorStops.map { it.second }, colorStops.map { it.first }, 0f)
        fun verticalGradient(colors: List<PolyColor>) = LinearGradient(colors, angle = 90f)
        fun verticalGradient(vararg colors: PolyColor) = verticalGradient(colors.toList())
        fun verticalGradient(vararg colorStops: Pair<Float, PolyColor>) = LinearGradient(colorStops.map { it.second }, colorStops.map { it.first }, 90f)

        fun linearGradient(colors: List<PolyColor>, stops: List<Float>? = null, angle: Float = 45f) = LinearGradient(colors, stops, angle)

        fun radialGradient(colors: List<PolyColor>, stops: List<Float>? = null, centerX: Float = 0.5f, centerY: Float = 0.5f, radius: Float = 0.5f) =
            RadialGradient(colors, stops, centerX, centerY, radius)

        fun horizontalGradient(vararg colors: Int) = horizontalGradient(colors.map { PolyColor(it) })
        fun verticalGradient(vararg colors: Int) = verticalGradient(colors.map { PolyColor(it) })

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

data class LinearGradient(
    override val colors: List<PolyColor>,
    override val stops: List<Float>? = null,
    val angle: Float = 0f,
) : Brush() {
    override val affine get() = colors.size <= 2 && stops == null
    override val linear get() = true

    override fun position(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Float {
        val rad = Math.toRadians(angle.toDouble())
        val dx = (w / 2f * cos(rad)).toFloat() * 2f
        val dy = (h / 2f * sin(rad)).toFloat() * 2f
        val len2 = dx * dx + dy * dy
        if (len2 <= 0f) return 0f
        val sx = x + w / 2f - dx / 2f
        val sy = y + h / 2f - dy / 2f
        return ((px - sx) * dx + (py - sy) * dy) / len2
    }
}

data class RadialGradient(
    override val colors: List<PolyColor>,
    override val stops: List<Float>? = null,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val radius: Float = 0.5f,
) : Brush() {
    override fun position(px: Float, py: Float, x: Float, y: Float, w: Float, h: Float): Float {
        val r = radius * minOf(w, h)
        if (r <= 0f) return 1f
        return hypot(px - (x + centerX * w), py - (y + centerY * h)) / r
    }
}

data class PointGradient(
    override val colors: List<PolyColor>,
    override val stops: List<Float>?,
    val x0: Float, val y0: Float, val x1: Float, val y1: Float,
) : Brush() {
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

fun SolidColor(value: PolyColor): Brush = LinearGradient(listOf(value, value), angle = 0f)
