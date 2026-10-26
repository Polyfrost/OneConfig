package org.polyfrost.compose.backend

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.min
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

private fun Paint.fadeMask(): PolyFade? = (shader?.platformShader as? GradientShader)?.fade(alpha)

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

    override fun rotate(degrees: Float) {}
    override fun skew(sx: Float, sy: Float) {}

    override fun concat(matrix: Matrix) {
        val v = matrix.values
        r.translate(v[Matrix.TranslateX], v[Matrix.TranslateY])
        r.scale(v[Matrix.ScaleX], v[Matrix.ScaleY])
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

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) =
        r.line(p1.x, p1.y, p2.x, p2.y, (paint as PolyPaint).argb(), paint.strokeWidth.coerceAtLeast(1f))

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) =
        shape(left, top, right - left, bottom - top, 0f, paint as PolyPaint)

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

    override fun drawPath(path: Path, paint: Paint) {
        val p = path as PolyComposePath
        val pp = paint as PolyPaint
        p.roundRect?.let { return shape(it.left, it.top, it.width, it.height, it.topLeftCornerRadius.x, pp) }
        p.plainRect?.let { return shape(it.left, it.top, it.width, it.height, 0f, pp) }
        val color = pp.argb()
        if (pp.style == PaintingStyle.Stroke) {
            val sw = pp.strokeWidth.coerceAtLeast(1f)
            p.allContours().forEachIndexed { i, c -> polyline(c, p.closedAt(i), color, sw) }
            return
        }
        for (c in p.allContours()) {
            if (c.size < 6) continue
            val poly = PolyPath()
            poly.moveTo(c[0], c[1])
            for (k in 2 until c.size step 2) poly.lineTo(c[k], c[k + 1])
            poly.close()
            r.fillPath(poly, color)
        }
    }

    private fun polyline(c: FloatArray, closed: Boolean, color: Int, width: Float) {
        val n = c.size / 2
        for (i in 0 until (if (closed) n else n - 1)) {
            val j = (i + 1) % n
            r.line(c[i * 2], c[i * 2 + 1], c[j * 2], c[j * 2 + 1], color, width)
        }
    }

    private fun shape(x: Float, y: Float, w: Float, h: Float, radius: Float, paint: PolyPaint) {
        if (w <= 0f || h <= 0f || paint.blendMode == BlendMode.DstIn) return
        if (paint.blur > 0f) {
            r.shadow(x, y, w, h, radius, PolyShadow(PolyColor(paint.argb()), paint.blur, 0f, 0f, 0f))
            return
        }
        if (paint.style == PaintingStyle.Stroke) {
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
        val matrix = paint.colorFilter?.let { (PolyPaint.filterPlatform(it) as? MatrixFilter)?.matrix }
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
            PointMode.Lines -> for (k in 0 until points.size - 3 step 4) r.line(points[k], points[k + 1], points[k + 2], points[k + 3], c, sw)
            else -> for (k in 0 until points.size - 3 step 2) r.line(points[k], points[k + 1], points[k + 2], points[k + 3], c, sw)
        }
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {}
    override fun enableZ() {}
    override fun disableZ() {}
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
    override fun rotate(degrees: Float) {}
    override fun skew(sx: Float, sy: Float) {}
    override fun concat(matrix: Matrix) { val m = Matrix(matrix.values.copyOf()); polyOp { it.concat(m) } }
    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) = polyOp { it.clipRect(left, top, right, bottom, clipOp) }
    override fun clipPath(path: Path, clipOp: ClipOp) { val p = path.copyPath(); polyOp { it.clipPath(p, clipOp) } }
    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) { val p = paint.snap(); polyOp { it.drawLine(p1, p2, p) } }
    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        if (paint.blendMode == BlendMode.DstIn) { paint.fadeMask()?.let { ops += MaskOp(it) }; return }
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

    private fun Path.copyPath(): Path = PolyComposePath().also { it.addPath(this, Offset.Zero) }
}

