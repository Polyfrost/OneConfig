package org.polyfrost.compose.ui.graphics

import org.polyfrost.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

enum class StrokeCap { Butt, Round, Square }

class Path {
    internal val contours = ArrayList<FloatArray>()
    internal val closed = ArrayList<Boolean>()
    private var cur = FloatArray(16)
    private var n = 0
    private var lastX = 0f
    private var lastY = 0f

    val isEmpty get() = contours.isEmpty() && n == 0

    fun moveTo(x: Float, y: Float) {
        finish(false)
        add(x, y)
    }

    fun lineTo(x: Float, y: Float) {
        if (n == 0) add(lastX, lastY)
        add(x, y)
    }

    fun quadraticTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        val x0 = lastX; val y0 = lastY
        for (i in 1..CURVE_SEGMENTS) {
            val t = i.toFloat() / CURVE_SEGMENTS; val u = 1f - t
            lineTo(u * u * x0 + 2f * u * t * x1 + t * t * x2, u * u * y0 + 2f * u * t * y1 + t * t * y2)
        }
    }

    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val x0 = lastX; val y0 = lastY
        for (i in 1..CURVE_SEGMENTS) {
            val t = i.toFloat() / CURVE_SEGMENTS; val u = 1f - t
            val a = u * u * u; val b = 3f * u * u * t; val c = 3f * u * t * t; val d = t * t * t
            lineTo(a * x0 + b * x1 + c * x2 + d * x3, a * y0 + b * y1 + c * y2 + d * y3)
        }
    }

    fun arcTo(rect: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float, forceMoveTo: Boolean) {
        val cx = rect.center.x; val cy = rect.center.y
        val rx = rect.width / 2f; val ry = rect.height / 2f
        val start = Math.toRadians(startAngleDegrees.toDouble()).toFloat()
        val sweep = Math.toRadians(sweepAngleDegrees.toDouble()).toFloat()
        val segs = arcSegments(sweep)
        for (i in 0..segs) {
            val a = start + sweep * i / segs
            val x = cx + cos(a) * rx; val y = cy + sin(a) * ry
            if (i == 0 && (forceMoveTo || n == 0)) moveTo(x, y) else lineTo(x, y)
        }
    }

    fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) = arcTo(oval, startAngleDegrees, sweepAngleDegrees, true)

    fun addOval(oval: Rect) {
        arcTo(oval, 0f, 360f, true)
        close()
    }

    fun addRect(rect: Rect) {
        moveTo(rect.left, rect.top)
        lineTo(rect.right, rect.top)
        lineTo(rect.right, rect.bottom)
        lineTo(rect.left, rect.bottom)
        close()
    }

    fun close() {
        if (n > 0) { lastX = cur[0]; lastY = cur[1] }
        finish(true)
    }

    fun reset() {
        contours.clear()
        closed.clear()
        n = 0
    }

    internal inline fun forEachContour(block: (FloatArray, Boolean) -> Unit) {
        for (i in contours.indices) block(contours[i], closed[i])
        if (n >= 4) block(cur.copyOf(n), false)
    }

    private fun add(x: Float, y: Float) {
        if (n + 2 > cur.size) cur = cur.copyOf(cur.size * 2)
        cur[n++] = x; cur[n++] = y
        lastX = x; lastY = y
    }

    private fun finish(close: Boolean) {
        if (n >= 4) {
            contours.add(cur.copyOf(n))
            closed.add(close)
        }
        n = 0
    }

    internal companion object {
        const val CURVE_SEGMENTS = 16

        fun arcSegments(sweepRadians: Float) = ceil(abs(sweepRadians) / (Math.PI.toFloat() / 24f)).toInt().coerceAtLeast(2)
    }
}
