package org.polyfrost.compose.ui.graphics.drawscope

import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyPath
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.ui.geometry.CornerRadius
import org.polyfrost.compose.ui.geometry.Offset
import org.polyfrost.compose.ui.geometry.Rect
import org.polyfrost.compose.ui.geometry.Size
import org.polyfrost.compose.ui.graphics.Path
import org.polyfrost.compose.ui.graphics.StrokeCap
import org.polyfrost.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

sealed class DrawStyle

object Fill : DrawStyle()

class Stroke(val width: Float = HairlineWidth, val cap: StrokeCap = DefaultCap) : DrawStyle() {
    companion object {
        const val HairlineWidth = 0f
        val DefaultCap = StrokeCap.Butt
    }
}

class DrawScope(val context: RenderContext, left: Float, top: Float, val size: Size) {
    private var a = 1f
    private var b = 0f
    private var c = 0f
    private var d = 1f
    private var tx = left
    private var ty = top

    val center: Offset get() = size.center

    fun Dp.toPx() = value

    fun drawRect(color: PolyColor, topLeft: Offset = Offset.Zero, size: Size = this.size.minus(topLeft), alpha: Float = 1f, style: DrawStyle = Fill) =
        roundRect(color.multiplyAlpha(alpha), topLeft, size, 0f, style)

    fun drawRoundRect(
        color: PolyColor, topLeft: Offset = Offset.Zero, size: Size = this.size.minus(topLeft), cornerRadius: CornerRadius = CornerRadius.Zero,
        style: DrawStyle = Fill, alpha: Float = 1f,
    ) = roundRect(color.multiplyAlpha(alpha), topLeft, size, cornerRadius.x, style)

    fun drawCircle(color: PolyColor, radius: Float = size.minDimension / 2f, center: Offset = this.center, alpha: Float = 1f, style: DrawStyle = Fill) {
        val col = color.multiplyAlpha(alpha)
        val s = scale()
        val r = radius * s
        val cx = mapX(center.x, center.y); val cy = mapY(center.x, center.y)
        when (style) {
            Fill -> context.rect(cx - r, cy - r, r * 2f, r * 2f, col, r)
            is Stroke -> context.rectStroke(cx - r, cy - r, r * 2f, r * 2f, col, strokeWidth(style) * s, r)
        }
    }

    fun drawOval(color: PolyColor, topLeft: Offset = Offset.Zero, size: Size = this.size.minus(topLeft), alpha: Float = 1f, style: DrawStyle = Fill) =
        drawPath(Path().apply { addOval(Rect(topLeft, size)) }, color, alpha, style)

    fun drawArc(
        color: PolyColor, startAngle: Float, sweepAngle: Float, useCenter: Boolean, topLeft: Offset = Offset.Zero,
        size: Size = this.size.minus(topLeft), alpha: Float = 1f, style: DrawStyle = Fill,
    ) {
        val rect = Rect(topLeft, size)
        val path = Path()
        if (useCenter) path.moveTo(rect.center.x, rect.center.y)
        path.arcTo(rect, startAngle, sweepAngle.coerceIn(-360f, 360f), !useCenter)
        if (useCenter || style == Fill) path.close()
        drawPath(path, color, alpha, style)
    }

    fun drawLine(color: PolyColor, start: Offset, end: Offset, strokeWidth: Float = Stroke.HairlineWidth, cap: StrokeCap = Stroke.DefaultCap, alpha: Float = 1f) =
        stroke(floatArrayOf(start.x, start.y, end.x, end.y), false, color.multiplyAlpha(alpha), Stroke(strokeWidth, cap))

    fun drawPath(path: Path, color: PolyColor, alpha: Float = 1f, style: DrawStyle = Fill) {
        val col = color.multiplyAlpha(alpha)
        path.forEachContour { pts, closed ->
            when (style) {
                Fill -> fill(pts, col)
                is Stroke -> stroke(pts, closed, col, style)
            }
        }
    }

    fun rotate(degrees: Float, pivot: Offset = center, block: DrawScope.() -> Unit) {
        val r = Math.toRadians(degrees.toDouble())
        val cos = cos(r).toFloat(); val sin = sin(r).toFloat()
        transformed(cos, sin, -sin, cos, pivot.x - cos * pivot.x + sin * pivot.y, pivot.y - sin * pivot.x - cos * pivot.y, block)
    }

    fun translate(left: Float = 0f, top: Float = 0f, block: DrawScope.() -> Unit) = transformed(1f, 0f, 0f, 1f, left, top, block)

    fun scale(scaleX: Float, scaleY: Float, pivot: Offset = center, block: DrawScope.() -> Unit) =
        transformed(scaleX, 0f, 0f, scaleY, pivot.x - scaleX * pivot.x, pivot.y - scaleY * pivot.y, block)

    fun scale(scale: Float, pivot: Offset = center, block: DrawScope.() -> Unit) = scale(scale, scale, pivot, block)

    private inline fun transformed(a2: Float, b2: Float, c2: Float, d2: Float, tx2: Float, ty2: Float, block: DrawScope.() -> Unit) {
        val sa = a; val sb = b; val sc = c; val sd = d; val stx = tx; val sty = ty
        a = sa * a2 + sc * b2; b = sb * a2 + sd * b2
        c = sa * c2 + sc * d2; d = sb * c2 + sd * d2
        tx = sa * tx2 + sc * ty2 + stx; ty = sb * tx2 + sd * ty2 + sty
        try {
            block()
        } finally {
            a = sa; b = sb; c = sc; d = sd; tx = stx; ty = sty
        }
    }

    private fun mapX(x: Float, y: Float) = a * x + c * y + tx
    private fun mapY(x: Float, y: Float) = b * x + d * y + ty
    private fun scale() = sqrt(abs(a * d - b * c))
    private fun strokeWidth(s: Stroke) = if (s.width <= 0f) 1f else s.width

    private fun map(pts: FloatArray): FloatArray {
        val out = FloatArray(pts.size)
        for (i in pts.indices step 2) {
            out[i] = mapX(pts[i], pts[i + 1])
            out[i + 1] = mapY(pts[i], pts[i + 1])
        }
        return out
    }

    private fun roundRect(color: PolyColor, topLeft: Offset, size: Size, radius: Float, style: DrawStyle) {
        if (b == 0f && c == 0f) {
            val x0 = mapX(topLeft.x, topLeft.y); val y0 = mapY(topLeft.x, topLeft.y)
            val x1 = mapX(topLeft.x + size.width, topLeft.y + size.height); val y1 = mapY(topLeft.x + size.width, topLeft.y + size.height)
            val x = min(x0, x1); val y = min(y0, y1); val w = abs(x1 - x0); val h = abs(y1 - y0)
            val s = scale()
            return when (style) {
                Fill -> context.rect(x, y, w, h, color, radius * s)
                is Stroke -> context.rectStroke(x, y, w, h, color, strokeWidth(style) * s, radius * s)
            }
        }
        val path = Path()
        val l = topLeft.x; val t = topLeft.y; val r = l + size.width; val bt = t + size.height
        val rad = max(0f, min(radius, min(size.width, size.height) / 2f))
        if (rad <= 0f) path.addRect(Rect(l, t, r, bt)) else {
            path.arcTo(Rect(l, t, l + rad * 2f, t + rad * 2f), 180f, 90f, true)
            path.arcTo(Rect(r - rad * 2f, t, r, t + rad * 2f), 270f, 90f, false)
            path.arcTo(Rect(r - rad * 2f, bt - rad * 2f, r, bt), 0f, 90f, false)
            path.arcTo(Rect(l, bt - rad * 2f, l + rad * 2f, bt), 90f, 90f, false)
            path.close()
        }
        path.forEachContour { pts, _ -> if (style is Stroke) stroke(pts, true, color, style) else fill(pts, color) }
    }

    private fun fill(pts: FloatArray, color: PolyColor) {
        if (pts.size < 6) return
        val p = map(pts)
        val path = PolyPath()
        path.moveTo(p[0], p[1])
        for (i in 2 until p.size step 2) path.lineTo(p[i], p[i + 1])
        path.close()
        context.fillPath(path, color)
    }

    private fun stroke(pts: FloatArray, closed: Boolean, color: PolyColor, style: Stroke) =
        context.strokePolyline(map(pts), closed, color, strokeWidth(style) * scale(), style.cap)
}

private fun Size.minus(offset: Offset) = Size(width - offset.x, height - offset.y)
