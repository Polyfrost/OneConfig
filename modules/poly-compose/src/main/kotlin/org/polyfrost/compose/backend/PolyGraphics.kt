package org.polyfrost.compose.backend

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.StampedPathEffectStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.platform.PlatformBlurFilter
import androidx.compose.ui.graphics.platform.PlatformColorFilter
import androidx.compose.ui.graphics.platform.PlatformGraphics
import androidx.compose.ui.graphics.platform.PlatformRenderEffect
import androidx.compose.ui.graphics.platform.PlatformShader
import androidx.compose.ui.graphics.toArgb
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.PointGradient
import org.polyfrost.compose.ui.graphics.RadialGradient
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import org.polyfrost.compose.ui.graphics.ImageBitmap as PolyBitmap

object PolyGraphics : PlatformGraphics {
    override fun createCanvas(image: ImageBitmap): Canvas = error("drawing into ImageBitmap is not supported")
    override fun createPaint(): Paint = PolyPaint()
    override fun isBlendModeSupported(blendMode: BlendMode) = blendMode == BlendMode.SrcOver
    override fun isTileModeSupported(tileMode: TileMode) = tileMode == TileMode.Clamp
    override fun createPath(): Path = PolyComposePath()
    override fun createPathIterator(path: Path, conicEvaluation: PathIterator.ConicEvaluation, tolerance: Float): PathIterator =
        error("PathIterator is not supported")
    override fun createPathMeasure(): PathMeasure = error("PathMeasure is not supported")

    override fun createImageBitmap(width: Int, height: Int, config: ImageBitmapConfig, hasAlpha: Boolean, colorSpace: ColorSpace): ImageBitmap =
        PolyImageBitmap(PolyBitmap(width, height, ByteArray(width * height * 4)))

    override fun decodeImageBitmap(bytes: ByteArray): ImageBitmap =
        PolyImageBitmap(PolyBitmap.decode(bytes) ?: error("cannot decode image (${bytes.size} bytes)"))

    override fun createLinearGradientShader(from: Offset, to: Offset, colors: List<Color>, colorStops: List<Float>?, tileMode: TileMode): Shader =
        Shader(GradientShader(PointGradient(colors.map(::polyColor), colorStops, from.x, from.y, to.x, to.y), null, 0f))

    override fun createRadialGradientShader(center: Offset, radius: Float, colors: List<Color>, colorStops: List<Float>?, tileMode: TileMode): Shader =
        Shader(GradientShader(RadialGradient(colors.map(::polyColor), colorStops), center, radius))

    override fun createSweepGradientShader(center: Offset, colors: List<Color>, colorStops: List<Float>?): Shader =
        Shader(GradientShader(PointGradient(listOf(polyColor(colors.first())), null, 0f, 0f, 1f, 0f), null, 0f))

    override fun createImageShader(image: ImageBitmap, tileModeX: TileMode, tileModeY: TileMode): Shader = Shader(ImageShader(image))
    override fun createCompositeShader(destination: Shader, source: Shader, blendMode: BlendMode): Shader = source
    override fun transformShader(shader: Shader, matrix: Matrix): Shader = shader

    override fun createBlurRenderEffect(renderEffect: RenderEffect?, radiusX: Float, radiusY: Float, edgeTreatment: TileMode): PlatformRenderEffect = NoEffect
    override fun createOffsetRenderEffect(renderEffect: RenderEffect?, offset: Offset): PlatformRenderEffect = NoEffect
    override fun createBlurFilter(radius: Float): PlatformBlurFilter = BlurFilter(radius)
    override fun setBlurFilter(paint: Paint, blur: PlatformBlurFilter?) { (paint as? PolyPaint)?.blur = (blur as? BlurFilter)?.radius ?: 0f }

    override fun createTintColorFilter(color: Color, blendMode: BlendMode): PlatformColorFilter = TintFilter(color, blendMode)
    override fun createColorMatrixColorFilter(colorMatrix: ColorMatrix): PlatformColorFilter = MatrixFilter(colorMatrix)
    override fun createLightingColorFilter(multiply: Color, add: Color): PlatformColorFilter = PlatformColorFilter()
    override fun colorMatrixFromFilter(filter: PlatformColorFilter): ColorMatrix = (filter as? MatrixFilter)?.matrix ?: ColorMatrix()

    override fun createCornerPathEffect(radius: Float): PathEffect = NoPathEffect
    override fun createDashPathEffect(intervals: FloatArray, phase: Float): PathEffect = DashEffect(intervals, phase)
    override fun createChainPathEffect(outer: PathEffect, inner: PathEffect): PathEffect = outer
    override fun createStampedPathEffect(shape: Path, advance: Float, phase: Float, style: StampedPathEffectStyle): PathEffect = NoPathEffect

    internal fun polyColor(c: Color) = PolyColor(c.toArgb())
}

internal class GradientShader(val brush: Brush, val center: Offset?, val radius: Float) : PlatformShader {
    fun brushFor(x: Float, y: Float, w: Float, h: Float): Brush {
        val c = center ?: return brush
        val g = brush as RadialGradient
        val m = min(w, h)
        if (w <= 0f || h <= 0f || m <= 0f) return g
        return g.copy(centerX = (c.x - x) / w, centerY = (c.y - y) / h, radius = radius / m)
    }
}

internal class ImageShader(val image: ImageBitmap) : PlatformShader
internal class BlurFilter(val radius: Float) : PlatformBlurFilter()
internal class TintFilter(val color: Color, val blendMode: BlendMode) : PlatformColorFilter()
internal class MatrixFilter(val matrix: ColorMatrix) : PlatformColorFilter()
internal class DashEffect(val intervals: FloatArray, val phase: Float) : PathEffect
internal object NoPathEffect : PathEffect
internal object NoEffect : PlatformRenderEffect

class PolyImageBitmap(val bitmap: PolyBitmap) : ImageBitmap {
    override val width get() = bitmap.width
    override val height get() = bitmap.height
    override val colorSpace: ColorSpace get() = ColorSpaces.Srgb
    override val hasAlpha get() = true
    override val config get() = ImageBitmapConfig.Argb8888

    override fun readPixels(buffer: IntArray, startX: Int, startY: Int, width: Int, height: Int, bufferOffset: Int, stride: Int) {
        val px = bitmap.pixels
        for (y in 0 until height) for (x in 0 until width) {
            val o = ((startY + y) * bitmap.width + startX + x) * 4
            buffer[bufferOffset + y * stride + x] = ((px[o + 3].toInt() and 0xFF) shl 24) or ((px[o].toInt() and 0xFF) shl 16) or
                ((px[o + 1].toInt() and 0xFF) shl 8) or (px[o + 2].toInt() and 0xFF)
        }
    }

    override fun prepareToDraw() {}
}

fun PolyBitmap.asComposeImageBitmap(): ImageBitmap = PolyImageBitmap(this)

class PolyPaint : Paint {
    override var alpha = 1f
    override var isAntiAlias = true
    override var color = Color.Black
    override var blendMode = BlendMode.SrcOver
    override var style = PaintingStyle.Fill
    override var strokeWidth = 0f
    override var strokeCap = StrokeCap.Butt
    override var strokeJoin = StrokeJoin.Miter
    override var strokeMiterLimit = 4f
    override var filterQuality = FilterQuality.Low
    override var shader: Shader? = null
    override var colorFilter: ColorFilter? = null
    override var pathEffect: PathEffect? = null
    var blur = 0f

    fun copy(): PolyPaint = PolyPaint().also {
        it.alpha = alpha; it.isAntiAlias = isAntiAlias; it.color = color; it.blendMode = blendMode; it.style = style
        it.strokeWidth = strokeWidth; it.strokeCap = strokeCap; it.strokeJoin = strokeJoin; it.strokeMiterLimit = strokeMiterLimit
        it.filterQuality = filterQuality; it.shader = shader; it.colorFilter = colorFilter; it.pathEffect = pathEffect; it.blur = blur
    }

    fun argb(): Int {
        val tint = colorFilter?.let { tintOf(it) }
        val c = tint ?: color
        val a = (c.alpha * alpha * (if (tint != null) color.alpha else 1f)).coerceIn(0f, 1f)
        return c.copy(alpha = a).toArgb()
    }

    private fun tintOf(filter: ColorFilter): Color? = (filterPlatform(filter) as? TintFilter)?.color

    companion object {
        private val platformField = runCatching {
            ColorFilter::class.java.declaredFields.first { PlatformColorFilter::class.java.isAssignableFrom(it.type) }.apply { isAccessible = true }
        }.getOrNull()

        internal fun filterPlatform(filter: ColorFilter): Any? = platformField?.get(filter)
    }
}

class PolyComposePath : Path {
    internal val contours = ArrayList<FloatArray>()
    private var cur = FloatArrayList()
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var closed = ArrayList<Boolean>()

    internal var roundRect: RoundRect? = null
    internal var plainRect: Rect? = null

    override var fillType = PathFillType.NonZero
    override val isConvex: Boolean get() = roundRect != null || plainRect != null
    override val isEmpty: Boolean get() = contours.isEmpty() && cur.size == 0

    private fun shapeChanged() { roundRect = null; plainRect = null }

    private fun flush(close: Boolean) {
        if (cur.size >= 4) { contours.add(cur.toArray()); closed.add(close) }
        cur = FloatArrayList()
    }

    override fun moveTo(x: Float, y: Float) {
        shapeChanged()
        flush(false)
        cur.add(x, y)
        startX = x; startY = y; lastX = x; lastY = y
    }

    override fun relativeMoveTo(dx: Float, dy: Float) = moveTo(lastX + dx, lastY + dy)

    override fun lineTo(x: Float, y: Float) {
        shapeChanged()
        if (cur.size == 0) cur.add(lastX, lastY)
        cur.add(x, y)
        lastX = x; lastY = y
    }

    override fun relativeLineTo(dx: Float, dy: Float) = lineTo(lastX + dx, lastY + dy)

    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        val x0 = lastX; val y0 = lastY
        val n = segments(abs(x1 - x0) + abs(y1 - y0) + abs(x2 - x1) + abs(y2 - y1))
        for (i in 1..n) {
            val t = i / n.toFloat(); val u = 1f - t
            lineTo(u * u * x0 + 2 * u * t * x1 + t * t * x2, u * u * y0 + 2 * u * t * y1 + t * t * y2)
        }
    }

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) =
        quadraticBezierTo(lastX + dx1, lastY + dy1, lastX + dx2, lastY + dy2)

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val x0 = lastX; val y0 = lastY
        val n = segments(abs(x1 - x0) + abs(y1 - y0) + abs(x2 - x1) + abs(y2 - y1) + abs(x3 - x2) + abs(y3 - y2))
        for (i in 1..n) {
            val t = i / n.toFloat(); val u = 1f - t
            val a = u * u * u; val b = 3 * u * u * t; val c = 3 * u * t * t; val d = t * t * t
            lineTo(a * x0 + b * x1 + c * x2 + d * x3, a * y0 + b * y1 + c * y2 + d * y3)
        }
    }

    override fun relativeCubicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float) =
        cubicTo(lastX + dx1, lastY + dy1, lastX + dx2, lastY + dy2, lastX + dx3, lastY + dy3)

    override fun arcTo(rect: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float, forceMoveTo: Boolean) {
        val start = Math.toRadians(startAngleDegrees.toDouble())
        val sweep = Math.toRadians(sweepAngleDegrees.toDouble())
        val cx = rect.center.x; val cy = rect.center.y
        val rx = rect.width / 2f; val ry = rect.height / 2f
        val n = max(2, segments(abs(sweep).toFloat() * max(rx, ry)))
        for (i in 0..n) {
            val a = start + sweep * i / n
            val x = cx + rx * cos(a).toFloat(); val y = cy + ry * sin(a).toFloat()
            if (i == 0 && (forceMoveTo || cur.size == 0)) moveTo(x, y) else lineTo(x, y)
        }
    }

    override fun addRect(rect: Rect, direction: Path.Direction) {
        val empty = isEmpty
        moveTo(rect.left, rect.top); lineTo(rect.right, rect.top); lineTo(rect.right, rect.bottom); lineTo(rect.left, rect.bottom); close()
        if (empty) plainRect = rect
    }

    override fun addOval(oval: Rect, direction: Path.Direction) {
        val empty = isEmpty
        arcTo(oval, 0f, 359.999f, true); close()
        if (empty && oval.width == oval.height) roundRect = RoundRect(oval, androidx.compose.ui.geometry.CornerRadius(oval.width / 2f))
    }

    override fun addRoundRect(roundRect: RoundRect, direction: Path.Direction) {
        val empty = isEmpty
        val r = roundRect
        fun corner(cx: Float, cy: Float, rx: Float, ry: Float, startDeg: Float) {
            if (rx <= 0f || ry <= 0f) { lineTo(cx + rx * cos(Math.toRadians(startDeg + 45.0)).toFloat() * 1.4142135f, cy + ry * sin(Math.toRadians(startDeg + 45.0)).toFloat() * 1.4142135f); return }
            val n = segments(PI.toFloat() / 2f * max(rx, ry))
            for (i in 0..n) {
                val a = Math.toRadians(startDeg + 90.0 * i / n)
                lineTo(cx + rx * cos(a).toFloat(), cy + ry * sin(a).toFloat())
            }
        }
        moveTo(r.left + r.topLeftCornerRadius.x, r.top)
        corner(r.right - r.topRightCornerRadius.x, r.top + r.topRightCornerRadius.y, r.topRightCornerRadius.x, r.topRightCornerRadius.y, 270f)
        corner(r.right - r.bottomRightCornerRadius.x, r.bottom - r.bottomRightCornerRadius.y, r.bottomRightCornerRadius.x, r.bottomRightCornerRadius.y, 0f)
        corner(r.left + r.bottomLeftCornerRadius.x, r.bottom - r.bottomLeftCornerRadius.y, r.bottomLeftCornerRadius.x, r.bottomLeftCornerRadius.y, 90f)
        corner(r.left + r.topLeftCornerRadius.x, r.top + r.topLeftCornerRadius.y, r.topLeftCornerRadius.x, r.topLeftCornerRadius.y, 180f)
        close()
        if (empty) this.roundRect = r
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) =
        addArc(oval, Math.toDegrees(startAngleRadians.toDouble()).toFloat(), Math.toDegrees(sweepAngleRadians.toDouble()).toFloat())

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) = arcTo(oval, startAngleDegrees, sweepAngleDegrees, true)

    override fun addPath(path: Path, offset: Offset) {
        val other = path as PolyComposePath
        val empty = isEmpty
        flush(false)
        other.allContours().forEachIndexed { i, pts ->
            contours.add(FloatArray(pts.size) { k -> pts[k] + if (k % 2 == 0) offset.x else offset.y })
            closed.add(other.closedAt(i))
        }
        if (empty && offset == Offset.Zero) { roundRect = other.roundRect; plainRect = other.plainRect } else shapeChanged()
    }

    override fun close() {
        if (cur.size >= 2) { lastX = startX; lastY = startY }
        flush(true)
    }

    override fun reset() {
        contours.clear(); closed = ArrayList(); cur = FloatArrayList()
        lastX = 0f; lastY = 0f; startX = 0f; startY = 0f
        shapeChanged()
    }

    override fun rewind() = reset()

    override fun translate(offset: Offset) {
        for (c in contours) for (k in c.indices) c[k] += if (k % 2 == 0) offset.x else offset.y
        cur.offset(offset.x, offset.y)
        roundRect = roundRect?.let { it.copy(left = it.left + offset.x, top = it.top + offset.y, right = it.right + offset.x, bottom = it.bottom + offset.y) }
        plainRect = plainRect?.translate(offset)
    }

    override fun getBounds(): Rect {
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        for (c in allContours()) for (k in c.indices step 2) {
            l = min(l, c[k]); r = max(r, c[k]); t = min(t, c[k + 1]); b = max(b, c[k + 1])
        }
        return if (l > r) Rect.Zero else Rect(l, t, r, b)
    }

    override fun op(path1: Path, path2: Path, operation: PathOperation): Boolean {
        reset()
        addPath(path1, Offset.Zero)
        return true
    }

    @Deprecated("") override fun addRect(rect: Rect) = addRect(rect, Path.Direction.CounterClockwise)
    @Deprecated("") override fun addOval(oval: Rect) = addOval(oval, Path.Direction.CounterClockwise)
    @Deprecated("") override fun addRoundRect(roundRect: RoundRect) = addRoundRect(roundRect, Path.Direction.CounterClockwise)

    internal fun allContours(): List<FloatArray> = if (cur.size >= 4) contours + cur.toArray() else contours
    internal fun closedAt(i: Int): Boolean = if (i < closed.size) closed[i] else false

    private fun segments(length: Float) = ceil(sqrt(max(length, 1f)) * 1.5f).toInt().coerceIn(1, 64)
}

internal class FloatArrayList {
    private var a = FloatArray(16)
    var size = 0
        private set

    fun add(x: Float, y: Float) {
        if (size + 2 > a.size) a = a.copyOf(a.size * 2)
        a[size++] = x; a[size++] = y
    }

    fun offset(dx: Float, dy: Float) { for (k in 0 until size) a[k] += if (k % 2 == 0) dx else dy }
    fun toArray(): FloatArray = a.copyOf(size)
}
