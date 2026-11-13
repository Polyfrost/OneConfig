package org.polyfrost.compose.render

import java.awt.Shape
import java.awt.geom.PathIterator
import kotlin.math.min
import kotlin.math.roundToInt

private const val SHAPE_FLATNESS = 0.02

public fun RenderContext.snappedRect(x: Float, y: Float, w: Float, h: Float): FloatArray {
    val m = deviceTransform()
    if (m == null || m[0] == 0f || m[1] == 0f) return floatArrayOf(x, y, w, h)
    val left = snapEdge(x, m[0], m[2])
    val top = snapEdge(y, m[1], m[3])
    val right = snapFarEdge(x, w, left, m[0], m[2])
    val bottom = snapFarEdge(y, h, top, m[1], m[3])
    return floatArrayOf(left, top, right - left, bottom - top)
}

private fun snapEdge(start: Float, scale: Float, translate: Float): Float =
    ((start * scale + translate).roundToInt() - translate) / scale

private fun snapFarEdge(start: Float, size: Float, snappedStart: Float, scale: Float, translate: Float): Float {
    val from = (snappedStart * scale + translate).roundToInt()
    val to = ((start + size) * scale + translate).roundToInt()
    val kept = if (to != from || size == 0f) to else if (size > 0f) from + 1 else from - 1
    return (kept - translate) / scale
}

public fun PolyPath.addShape(shape: Shape) {
    val iterator = shape.getPathIterator(null, SHAPE_FLATNESS)
    if (iterator.windingRule == PathIterator.WIND_EVEN_ODD) evenOdd = true
    val coords = FloatArray(6)
    while (!iterator.isDone) {
        when (iterator.currentSegment(coords)) {
            PathIterator.SEG_MOVETO -> moveTo(coords[0], coords[1])
            PathIterator.SEG_LINETO -> lineTo(coords[0], coords[1])
            PathIterator.SEG_CLOSE -> close()
        }
        iterator.next()
    }
}

public fun PolyPath.addRoundedRect(
    x: Float, y: Float, w: Float, h: Float,
    topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float,
) {
    val limit = min(w, h) / 2f
    val tl = topLeft.coerceIn(0f, limit)
    val tr = topRight.coerceIn(0f, limit)
    val br = bottomRight.coerceIn(0f, limit)
    val bl = bottomLeft.coerceIn(0f, limit)
    val r = x + w
    val b = y + h
    moveTo(x + tl, y)
    tangentArcTo(r, y, r, b, tr)
    tangentArcTo(r, b, x, b, br)
    tangentArcTo(x, b, x, y, bl)
    tangentArcTo(x, y, r, y, tl)
    close()
}

public fun RenderContext.fillShape(shape: Shape, color: PolyColor) {
    if (color.alpha == 0) return
    fillPath(PolyPath().also { it.addShape(shape) }, color)
}

public fun RenderContext.strokeShape(
    shape: Shape,
    color: PolyColor,
    strokeWidth: Float = 1f,
    cap: Int = PolyRenderer.CAP_BUTT,
    join: Int = PolyRenderer.JOIN_MITER,
) {
    if (color.alpha == 0 || strokeWidth <= 0f) return
    val argb = color.argb
    val iterator = shape.getPathIterator(null, SHAPE_FLATNESS)
    val coords = FloatArray(6)
    var points = FloatArray(32)
    var count = 0

    fun flush(closed: Boolean) {
        var n = count
        if (closed && n >= 4 && points[n - 2] == points[0] && points[n - 1] == points[1]) n -= 2
        if (n >= 4) renderer.strokePolyline(points.copyOf(n), closed, argb, strokeWidth, cap, join)
        count = 0
    }

    fun add(px: Float, py: Float) {
        if (count + 2 > points.size) points = points.copyOf(points.size * 2)
        points[count++] = px
        points[count++] = py
    }

    while (!iterator.isDone) {
        when (iterator.currentSegment(coords)) {
            PathIterator.SEG_MOVETO -> {
                flush(false)
                add(coords[0], coords[1])
            }
            PathIterator.SEG_LINETO -> add(coords[0], coords[1])
            PathIterator.SEG_CLOSE -> flush(true)
        }
        iterator.next()
    }
    flush(false)
}

public fun RenderContext.roundedRect(
    x: Float, y: Float, w: Float, h: Float, color: PolyColor,
    topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float,
) {
    if (color.alpha == 0 || w <= 0f || h <= 0f) return
    if (topLeft == topRight && topRight == bottomRight && bottomRight == bottomLeft) {
        rect(x, y, w, h, color, topLeft.coerceIn(0f, min(w, h) / 2f))
        return
    }
    fillPath(PolyPath().also { it.addRoundedRect(x, y, w, h, topLeft, topRight, bottomRight, bottomLeft) }, color)
}
