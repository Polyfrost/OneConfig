@file:OptIn(InternalComposeUiApi::class)

package org.polyfrost.compose.backend

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt
import org.polyfrost.compose.render.FilterQuality as PolyFilterQuality
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFade
import org.polyfrost.compose.render.PolyPath
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyShadow
import org.polyfrost.compose.render.RenderContext

interface PolyDrawCanvas : Canvas {
    fun polyOp(op: (PolyCanvas) -> Unit)
}

private val recolorCache = WeakHashMap<PolyBitmap, MutableMap<List<Float>, PolyBitmap>>()

internal var imageBlur = 0f

private val blurCache = WeakHashMap<PolyBitmap, MutableMap<Int, PolyBitmap>>()

private fun blurred(bitmap: PolyBitmap, sigma: Float): PolyBitmap = synchronized(blurCache) {
    val key = (sigma * 4f).toInt()
    if (key <= 0) return bitmap
    blurCache.getOrPut(bitmap) { HashMap() }.getOrPut(key) {
        val w = bitmap.width
        val h = bitmap.height
        val src = bitmap.pixels
        val buf = FloatArray(w * h * 4)
        for (i in 0 until w * h) {
            val a = (src[i * 4 + 3].toInt() and 0xFF) / 255f
            for (c in 0..2) buf[i * 4 + c] = (src[i * 4 + c].toInt() and 0xFF) * a
            buf[i * 4 + 3] = a * 255f
        }
        val tmp = FloatArray(buf.size)
        val ideal = sqrt(4f * sigma * sigma + 1f)
        for (pass in 0 until 3) {
            val r = ((ideal - 1f) / 2f).toInt().coerceAtLeast(1)
            boxBlur(buf, tmp, w, h, r, 4, w * 4)
            boxBlur(tmp, buf, h, w, r, w * 4, 4)
        }
        val out = ByteArray(src.size)
        for (i in 0 until w * h) {
            val a = buf[i * 4 + 3]
            if (a <= 0f) continue
            for (c in 0..2) out[i * 4 + c] = (buf[i * 4 + c] / (a / 255f)).toInt().coerceIn(0, 255).toByte()
            out[i * 4 + 3] = a.toInt().coerceIn(0, 255).toByte()
        }
        PolyBitmap(w, h, out)
    }
}

private fun boxBlur(src: FloatArray, dst: FloatArray, len: Int, lines: Int, r: Int, step: Int, lineStep: Int) {
    val norm = 1f / (2 * r + 1)
    for (line in 0 until lines) {
        val base = line * lineStep
        for (c in 0..3) {
            fun at(i: Int) = src[base + i.coerceIn(0, len - 1) * step + c]
            var sum = 0f
            for (i in -r..r) sum += at(i)
            for (i in 0 until len) {
                dst[base + i * step + c] = sum * norm
                sum += at(i + r + 1) - at(i - r)
            }
        }
    }
}

private fun recolored(bitmap: PolyBitmap, matrix: ColorMatrix): PolyBitmap = synchronized(recolorCache) {
    val m = matrix.values
    recolorCache.getOrPut(bitmap) { HashMap() }.getOrPut(m.toList()) {
        val src = bitmap.pixels
        val out = ByteArray(src.size)
        for (o in src.indices step 4) {
            val r = (src[o].toInt() and 0xFF).toFloat()
            val g = (src[o + 1].toInt() and 0xFF).toFloat()
            val b = (src[o + 2].toInt() and 0xFF).toFloat()
            val a = (src[o + 3].toInt() and 0xFF).toFloat()
            for (c in 0..3) {
                val i = c * 5
                out[o + c] = (m[i] * r + m[i + 1] * g + m[i + 2] * b + m[i + 3] * a + m[i + 4]).toInt().coerceIn(0, 255).toByte()
            }
        }
        PolyBitmap(bitmap.width, bitmap.height, out)
    }
}

private fun Paint.fadeMask(): PolyFade? = (shader?.platformShader as? GradientShader)?.fade(alpha, blendMode == BlendMode.Modulate)

private fun Paint.masks() = blendMode == BlendMode.DstIn || blendMode == BlendMode.Modulate

private class MaskOp(val fade: PolyFade) : (PolyCanvas) -> Unit {
    override fun invoke(c: PolyCanvas) = c.renderer.restore()
}

internal class DynamicOp(val block: (RenderContext) -> Unit) : (PolyCanvas) -> Unit {
    override fun invoke(c: PolyCanvas) = block(c.context)
}

fun DrawScope.drawPoly(block: (RenderContext) -> Unit) = drawIntoCanvas { c ->
    (c as? PolyDrawCanvas)?.polyOp(DynamicOp(block))
}

class PolyCanvas(val renderer: PolyRenderer) : PolyDrawCanvas {
    private val r get() = renderer
    val context by lazy(LazyThreadSafetyMode.NONE) { RenderContext(renderer) }

    override fun polyOp(op: (PolyCanvas) -> Unit) = op(this)

    override fun save() = r.save()
    override fun restore() = r.restore()

    override fun saveLayer(bounds: Rect, paint: Paint) = r.saveLayerAlpha(paint.alpha)

    override fun translate(dx: Float, dy: Float) = r.translate(dx, dy)
    override fun scale(sx: Float, sy: Float) = r.scale(sx, sy)

    override fun rotate(degrees: Float) = r.rotate(degrees)
    override fun skew(sx: Float, sy: Float) {}

    override fun concat(matrix: Matrix) {
        val v = matrix.values
        r.translate(v[Matrix.TranslateX], v[Matrix.TranslateY])
        val a = v[Matrix.ScaleX]
        val b = v[Matrix.SkewY]
        if (b == 0f && v[Matrix.SkewX] == 0f) return r.scale(a, v[Matrix.ScaleY])
        val sx = hypot(a, b)
        r.rotate(Math.toDegrees(atan2(b, a).toDouble()).toFloat())
        r.scale(sx, (a * v[Matrix.ScaleY] - b * v[Matrix.SkewX]) / sx)
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        if (clipOp == ClipOp.Intersect) r.clipRect(left, top, right - left, bottom - top)
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        if (clipOp != ClipOp.Intersect) return
        val p = path as PolyComposePath
        val rr = p.roundRect
        if (rr != null) r.clipRRect(rr.left, rr.top, rr.width, rr.height, rr.topLeftCornerRadius.x)
        else p.getBounds().let { r.clipRect(it.left, it.top, it.width, it.height) }
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) = segment(p1.x, p1.y, p2.x, p2.y, paint as PolyPaint)

    private fun segment(x1: Float, y1: Float, x2: Float, y2: Float, paint: PolyPaint) {
        if (paint.strokeCap == StrokeCap.Butt && paint.pathEffect !is DashEffect) return r.line(x1, y1, x2, y2, paint.argb(), paint.strokeWidth.coerceAtLeast(1f))
        strokeContour(floatArrayOf(x1, y1, x2, y2), false, paint)
    }

    private fun strokeContour(c: FloatArray, closed: Boolean, paint: PolyPaint) {
        val color = paint.argb()
        val sw = paint.strokeWidth.coerceAtLeast(1f)
        val cap = when (paint.strokeCap) { StrokeCap.Round -> PolyRenderer.CAP_ROUND; StrokeCap.Square -> PolyRenderer.CAP_SQUARE; else -> PolyRenderer.CAP_BUTT }
        val join = if (paint.strokeJoin == StrokeJoin.Round) PolyRenderer.JOIN_ROUND else PolyRenderer.JOIN_MITER
        val dash = paint.pathEffect as? DashEffect ?: return r.strokePolyline(c, closed, color, sw, cap, join)
        for (d in dashes(c, closed, dash)) r.strokePolyline(d, false, color, sw, cap, join)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) =
        clearing(paint) { shape(left, top, right - left, bottom - top, 0f, paint as PolyPaint) }

    private inline fun clearing(paint: Paint, draw: () -> Unit) {
        if (paint.blendMode != BlendMode.Clear) return draw()
        // Only image bitmaps can be cleared; Compose clears them before redrawing cached content
        val bitmap = r as? BitmapRenderer ?: return
        bitmap.erasing = true
        try { draw() } finally { bitmap.erasing = false }
    }

    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radiusX: Float, radiusY: Float, paint: Paint) =
        shape(left, top, right - left, bottom - top, radiusX, paint as PolyPaint)

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        val w = right - left
        val h = bottom - top
        if (abs(w - h) < 0.5f) shape(left, top, w, h, min(w, h) / 2f, paint as PolyPaint)
        else drawPath(PolyComposePath().apply { addOval(Rect(left, top, right, bottom)) }, paint)
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) =
        shape(center.x - radius, center.y - radius, radius * 2f, radius * 2f, radius, paint as PolyPaint)

    override fun drawArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint) {
        val p = PolyComposePath()
        val rect = Rect(left, top, right, bottom)
        if (useCenter) { p.moveTo(rect.center.x, rect.center.y); p.arcTo(rect, startAngle, sweepAngle, false); p.close() }
        else p.arcTo(rect, startAngle, sweepAngle, true)
        drawPath(p, paint)
    }

    override fun drawPath(path: Path, paint: Paint) = clearing(paint) { fillOrStroke(path as PolyComposePath, paint as PolyPaint) }

    private fun fillOrStroke(p: PolyComposePath, pp: PolyPaint) {
        p.roundRect?.let { return shape(it.left, it.top, it.width, it.height, it.topLeftCornerRadius.x, pp) }
        p.plainRect?.let { return shape(it.left, it.top, it.width, it.height, 0f, pp) }
        val color = pp.argb()
        if (pp.style == PaintingStyle.Stroke) {
            p.allContours().forEachIndexed { i, c -> strokeContour(c, p.closedAt(i), pp) }
            return
        }
        val poly = PolyPath()
        poly.evenOdd = p.fillType == PathFillType.EvenOdd
        for (c in p.allContours()) {
            if (c.size < 6) continue
            poly.moveTo(c[0], c[1])
            for (k in 2 until c.size step 2) poly.lineTo(c[k], c[k + 1])
            poly.close()
        }
        r.fillPath(poly, color)
    }

    private fun shape(x: Float, y: Float, w: Float, h: Float, radius: Float, paint: PolyPaint) {
        if (w <= 0f || h <= 0f || paint.masks()) return
        val blur = if (paint.blur > 0f) paint.blur
        else if (imageBlur > 0.5f && paint.style == PaintingStyle.Fill && paint.shader == null) (imageBlur - 0.5f) / 0.57735f
        else 0f
        if (blur > 0f) {
            r.shadow(x, y, w, h, radius, PolyShadow(PolyColor(paint.argb()), blur, 0f, 0f, 0f))
            return
        }
        if (paint.style == PaintingStyle.Stroke) {
            if (paint.pathEffect is DashEffect) {
                val outline = PolyComposePath().apply { addRoundRect(RoundRect(Rect(x, y, x + w, y + h), CornerRadius(radius))) }
                return strokeContour(outline.allContours()[0], true, paint)
            }
            val sw = paint.strokeWidth.coerceAtLeast(1f)
            val g = (paint.shader?.platformShader as? GradientShader)?.brushFor(x, y, w, h)
            if (g == null) {
                r.rectStroke(x, y, w, h, paint.argb(), sw, radius)
            } else {
                if (paint.alpha < 1f) r.saveLayerAlpha(paint.alpha) else r.save()
                r.rectStrokeGradient(x, y, w, h, g, sw, radius)
                r.restore()
            }
            return
        }
        when (val s = paint.shader?.platformShader) {
            is GradientShader -> {
                if (paint.alpha < 1f) r.saveLayerAlpha(paint.alpha) else r.save()
                r.rectGradient(x, y, w, h, s.brushFor(x, y, w, h), radius)
                r.restore()
            }
            is MaskedShader -> {
                if (paint.alpha < 1f) r.saveLayerAlpha(paint.alpha) else r.save()
                r.saveLayerFade(s.mask)
                r.rectGradient(x, y, w, h, s.color.brushFor(x, y, w, h), radius)
                r.restore()
                r.restore()
            }
            is ImageShader -> image(s.image, 0f, 0f, s.image.width.toFloat(), s.image.height.toFloat(), x, y, w, h, paint)
            else -> r.rect(x, y, w, h, paint.argb(), radius)
        }
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) =
        image(image, 0f, 0f, image.width.toFloat(), image.height.toFloat(), topLeftOffset.x, topLeftOffset.y, image.width.toFloat(), image.height.toFloat(), paint as PolyPaint)

    override fun drawImageRect(image: ImageBitmap, srcOffset: IntOffset, srcSize: IntSize, dstOffset: IntOffset, dstSize: IntSize, paint: Paint) =
        image(image, srcOffset.x.toFloat(), srcOffset.y.toFloat(), srcSize.width.toFloat(), srcSize.height.toFloat(),
            dstOffset.x.toFloat(), dstOffset.y.toFloat(), dstSize.width.toFloat(), dstSize.height.toFloat(), paint as PolyPaint)

    private fun image(image: ImageBitmap, sx: Float, sy: Float, sw: Float, sh: Float, x: Float, y: Float, w: Float, h: Float, paint: PolyPaint) {
        var bmp = (image as? PolyImageBitmap)?.bitmap ?: return
        val iw = bmp.width.toFloat()
        val ih = bmp.height.toFloat()
        if (imageBlur > 0f && w > 0f) bmp = blurred(bmp, imageBlur * sw / w)
        val filter = paint.colorFilter?.let { PolyPaint.filterPlatform(it) }
        val matrix = when {
            filter is MatrixFilter -> filter.matrix
            filter is TintFilter && filter.blendMode == BlendMode.Multiply -> filter.color.let { c ->
                val a = c.alpha
                ColorMatrix().apply { setToScale(1f - a + a * c.red, 1f - a + a * c.green, 1f - a + a * c.blue, 1f) }
            }
            else -> null
        }
        if (matrix != null) bmp = recolored(bmp, matrix)
        val tint = if (paint.colorFilter != null && matrix == null) paint.argb() else ((paint.alpha * 255f).toInt().coerceIn(0, 255) shl 24) or 0xFFFFFF
        val fq = when (paint.filterQuality) {
            FilterQuality.None -> PolyFilterQuality.None
            FilterQuality.Low -> PolyFilterQuality.Low
            FilterQuality.Medium -> PolyFilterQuality.Medium
            else -> PolyFilterQuality.High
        }
        r.image(bmp, x, y, w, h, sx / iw, sy / ih, (sx + sw) / iw, (sy + sh) / ih, tint, fq)
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        val raw = FloatArray(points.size * 2) { if (it % 2 == 0) points[it / 2].x else points[it / 2].y }
        drawRawPoints(pointMode, raw, paint)
    }

    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        val pp = paint as PolyPaint
        val sw = pp.strokeWidth.coerceAtLeast(1f)
        val c = pp.argb()
        when (pointMode) {
            PointMode.Points -> for (k in points.indices step 2) r.rect(points[k] - sw / 2f, points[k + 1] - sw / 2f, sw, sw, c, if (pp.strokeCap == StrokeCap.Round) sw / 2f else 0f)
            PointMode.Lines -> for (k in 0 until points.size - 3 step 4) segment(points[k], points[k + 1], points[k + 2], points[k + 3], pp)
            else -> if (points.size >= 4) strokeContour(points, false, pp)
        }
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {}
    override fun enableZ() {}
    override fun disableZ() {}
}

private fun dashes(c: FloatArray, closed: Boolean, e: DashEffect): List<FloatArray> {
    val iv = e.intervals
    var total = 0f
    for (v in iv) total += v
    val n = c.size / 2
    if (iv.size < 2 || total <= 0f || n < 2) return listOf(c)
    var k = 0
    var pos = ((e.phase % total) + total) % total
    while (pos > iv[k]) { pos -= iv[k]; k = (k + 1) % iv.size }
    var rem = iv[k] - pos
    var on = k % 2 == 0
    val out = ArrayList<FloatArray>()
    var cur = FloatArrayList()
    if (on) cur.add(c[0], c[1])
    for (i in 0 until if (closed) n else n - 1) {
        val j = (i + 1) % n
        val ax = c[i * 2]; val ay = c[i * 2 + 1]
        val bx = c[j * 2]; val by = c[j * 2 + 1]
        val len = hypot(bx - ax, by - ay)
        var t = 0f
        while (len - t > rem) {
            t += rem
            val x = ax + (bx - ax) * t / len; val y = ay + (by - ay) * t / len
            cur.add(x, y)
            if (on) { if (cur.size >= 4) out += cur.toArray(); cur = FloatArrayList() }
            on = !on
            k = (k + 1) % iv.size
            rem = iv[k]
        }
        rem -= len - t
        if (on) cur.add(bx, by)
    }
    if (on && cur.size >= 4) out += cur.toArray()
    return out
}

class RecordingCanvas : PolyDrawCanvas {
    val ops = ArrayList<(PolyCanvas) -> Unit>()

    override fun polyOp(op: (PolyCanvas) -> Unit) { ops += op }

    fun replay(canvas: PolyCanvas) = replay(canvas, null)

    internal fun replay(canvas: PolyCanvas, dynamic: ((DynamicOp) -> Unit)?) {
        for (i in ops.indices.reversed()) (ops[i] as? MaskOp)?.let { canvas.renderer.saveLayerFade(it.fade) }
        for (op in ops) if (dynamic != null && op is DynamicOp) dynamic(op) else op(canvas)
    }

    private fun Paint.snap() = (this as PolyPaint).copy()

    override fun save() = polyOp { it.save() }
    override fun restore() = polyOp { it.restore() }
    override fun saveLayer(bounds: Rect, paint: Paint) { val p = paint.snap(); polyOp { it.saveLayer(bounds, p) } }
    override fun translate(dx: Float, dy: Float) = polyOp { it.translate(dx, dy) }
    override fun scale(sx: Float, sy: Float) = polyOp { it.scale(sx, sy) }
    override fun rotate(degrees: Float) = polyOp { it.rotate(degrees) }
    override fun skew(sx: Float, sy: Float) {}
    override fun concat(matrix: Matrix) { val m = Matrix(matrix.values.copyOf()); polyOp { it.concat(m) } }
    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) = polyOp { it.clipRect(left, top, right, bottom, clipOp) }
    override fun clipPath(path: Path, clipOp: ClipOp) { val p = path.copyPath(); polyOp { it.clipPath(p, clipOp) } }
    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) { val p = paint.snap(); polyOp { it.drawLine(p1, p2, p) } }
    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        if (paint.masks()) { paint.fadeMask()?.let { ops += MaskOp(it) }; return }
        val p = paint.snap()
        polyOp { it.drawRect(left, top, right, bottom, p) }
    }
    override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radiusX: Float, radiusY: Float, paint: Paint) { val p = paint.snap(); polyOp { it.drawRoundRect(left, top, right, bottom, radiusX, radiusY, p) } }
    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) { val p = paint.snap(); polyOp { it.drawOval(left, top, right, bottom, p) } }
    override fun drawCircle(center: Offset, radius: Float, paint: Paint) { val p = paint.snap(); polyOp { it.drawCircle(center, radius, p) } }
    override fun drawArc(left: Float, top: Float, right: Float, bottom: Float, startAngle: Float, sweepAngle: Float, useCenter: Boolean, paint: Paint) { val p = paint.snap(); polyOp { it.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, p) } }
    override fun drawPath(path: Path, paint: Paint) { val p = paint.snap(); val c = path.copyPath(); polyOp { it.drawPath(c, p) } }
    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) { val p = paint.snap(); polyOp { it.drawImage(image, topLeftOffset, p) } }
    override fun drawImageRect(image: ImageBitmap, srcOffset: IntOffset, srcSize: IntSize, dstOffset: IntOffset, dstSize: IntSize, paint: Paint) { val p = paint.snap(); polyOp { it.drawImageRect(image, srcOffset, srcSize, dstOffset, dstSize, p) } }
    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) { val p = paint.snap(); val pts = points.toList(); polyOp { it.drawPoints(pointMode, pts, p) } }
    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) { val p = paint.snap(); val pts = points.copyOf(); polyOp { it.drawRawPoints(pointMode, pts, p) } }
    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {}
    override fun enableZ() {}
    override fun disableZ() {}

    private fun Path.copyPath(): Path = PolyComposePath().also { it.fillType = fillType; it.addPath(this, Offset.Zero) }
}

