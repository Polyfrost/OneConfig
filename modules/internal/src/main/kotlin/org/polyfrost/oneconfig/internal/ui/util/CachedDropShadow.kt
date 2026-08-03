package org.polyfrost.oneconfig.internal.ui.util

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.alphaMultiplier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.RefCnt
import kotlin.math.ceil
import org.jetbrains.skia.BlendMode as SkBlendMode
import org.jetbrains.skia.ColorFilter as SkColorFilter
import org.jetbrains.skia.Paint as SkPaint
import org.jetbrains.skia.Rect as SkRect

@Stable
fun Modifier.cachedDropShadow(shape: Shape, shadow: Shadow): Modifier {
    require(shadow.brush == null) {
        "cachedDropShadow does not support gradient brushes, use Modifier.dropShadow instead"
    }
    return this then CachedDropShadowElement(shape, shadow)
}

private data class CachedDropShadowElement(
    val shape: Shape,
    val shadow: Shadow,
) : ModifierNodeElement<CachedDropShadowNode>() {

    override fun create() = CachedDropShadowNode(shape, shadow)

    override fun update(node: CachedDropShadowNode) {
        node.update(shape, shadow)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "cachedDropShadow"
        properties["shape"] = shape
        properties["shadow"] = shadow
    }
}

private class CachedDropShadowNode(
    private var shape: Shape,
    private var shadow: Shadow,
) : Modifier.Node(), DrawModifierNode {

    private var maskImage: Image? = null
    private var maskWidth = 0
    private var maskHeight = 0
    private var maskSrc: SkRect = SkRect.makeWH(0f, 0f)
    private var maskSize: Size = Size.Unspecified
    private var maskDensity = 0f
    private var maskLayoutDirection = LayoutDirection.Ltr

    private var tint: SkColorFilter? = null
    private var tintColor: Color = Color.Unspecified

    private var blitPaint: SkPaint? = null

    fun update(shape: Shape, shadow: Shadow) {
        if (this.shape == shape && this.shadow == shadow) return
        val geometryChanged =
            this.shape != shape ||
                this.shadow.radius != shadow.radius ||
                this.shadow.spread != shadow.spread
        this.shape = shape
        this.shadow = shadow
        if (geometryChanged) invalidateMask()
        invalidateDraw()
    }

    private fun invalidateMask() {
        maskImage?.let { retire(it) }
        maskImage = null
        maskSize = Size.Unspecified
    }

    override fun ContentDrawScope.draw() {
        drawShadow()
        drawContent()
    }

    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    @OptIn(InternalComposeApi::class)
    private fun DrawScope.drawShadow() {
        if (size.width <= 0f || size.height <= 0f) return

        val radiusPx = shadow.radius.toPx()
        val spreadPx = shadow.spread.toPx()

        if (
            maskImage == null ||
            maskSize != size ||
            maskDensity != density ||
            maskLayoutDirection != layoutDirection
        ) {
            maskImage?.let { retire(it) }
            maskImage = buildMask(size, radiusPx, spreadPx)
            maskSize = size
            maskDensity = density
            maskLayoutDirection = layoutDirection
        }
        val image = maskImage ?: return

        val color = shadow.color
        var filter = tint
        if (filter == null || tintColor != color) {
            tint?.let { retire(it) }
            filter = SkColorFilter.makeBlend(color.toArgb(), SkBlendMode.SRC_IN)
            tint = filter
            tintColor = color
        }

        val inset = -(radiusPx + spreadPx)
        val left = inset + shadow.offset.x.toPx()
        val top = inset + shadow.offset.y.toPx()
        val dst = SkRect.makeXYWH(left, top, maskWidth.toFloat(), maskHeight.toFloat())

        val paint = blitPaint ?: SkPaint().also { blitPaint = it }
        drawIntoCanvas { canvas ->
            paint.colorFilter = filter
            paint.blendMode = shadow.blendMode.toSkia()
            paint.setAlphaf((shadow.alpha * canvas.alphaMultiplier).coerceIn(0f, 1f))
            canvas.skiaCanvas.drawImageRect(image, maskSrc, dst, SAMPLING, paint, true)
        }
    }

    private fun DrawScope.buildMask(size: Size, radiusPx: Float, spreadPx: Float): Image? {
        val outset = radiusPx * 2f + spreadPx * 2f
        val shadowWidth = size.width + outset
        val shadowHeight = size.height + outset
        val width = ceil(shadowWidth).toInt()
        val height = ceil(shadowHeight).toInt()
        if (width <= 0 || height <= 0) return null

        var path: Path? = null
        var cornerRadius = CornerRadius.Zero
        when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Generic -> path = outline.path
            is Outline.Rounded -> {
                val roundRect = outline.roundRect
                if (roundRect.isSimple) {
                    cornerRadius = roundRect.topLeftCornerRadius
                } else {
                    path = Path().apply { addRoundRect(roundRect) }
                }
            }
            is Outline.Rectangle -> {} // square corners, no path
        }

        val bitmap = ImageBitmap(width, height, ImageBitmapConfig.Alpha8)
        val canvas = Canvas(bitmap)
        val paint = shadowPaint(radiusPx)

        if (path != null) {
            if (spreadPx > 0f) {
                canvas.translate(radiusPx + spreadPx, radiusPx + spreadPx)
                canvas.drawPath(path, paint)
                canvas.drawPath(
                    path,
                    shadowPaint(radiusPx).apply {
                        style = PaintingStyle.Stroke
                        strokeWidth = spreadPx * 2f
                    },
                )
            } else {
                canvas.translate(radiusPx, radiusPx)
                canvas.drawPath(path, paint)
            }
        } else {
            canvas.drawRoundRect(
                radiusPx,
                radiusPx,
                shadowWidth - radiusPx,
                shadowHeight - radiusPx,
                cornerRadius.x,
                cornerRadius.y,
                paint,
            )
        }
        maskWidth = width
        maskHeight = height
        maskSrc = SkRect.makeWH(width.toFloat(), height.toFloat())
        return Image.makeFromBitmap(bitmap.asSkiaBitmap())
    }

    private fun shadowPaint(radiusPx: Float) = Paint().apply {
        color = Color.Black
        blendMode = BlendMode.SrcOver
        style = PaintingStyle.Fill
        if (radiusPx > 0f) {
            asFrameworkPaint().maskFilter =
                MaskFilter.makeBlur(FilterBlurMode.NORMAL, BLUR_SIGMA_SCALE * radiusPx + 0.5f)
        }
    }

    override fun onDetach() {
        invalidateMask()
        tint?.let { retire(it) }
        tint = null
        tintColor = Color.Unspecified
        blitPaint?.let { runCatching { it.close() } }
        blitPaint = null
    }

    private companion object {
        const val BLUR_SIGMA_SCALE = 0.57735f

        val SAMPLING: SamplingMode = FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE)

        const val RETIRE_CLOSE_DELAY_MS = 1000L
        private val retired = ArrayDeque<Pair<Long, RefCnt>>()

        fun retire(resource: RefCnt) {
            val now = System.currentTimeMillis()
            synchronized(retired) {
                retired.addLast(now to resource)
                while (retired.isNotEmpty() && now - retired.first().first >= RETIRE_CLOSE_DELAY_MS) {
                    runCatching { retired.removeFirst().second.close() }
                }
            }
        }

        fun BlendMode.toSkia(): SkBlendMode = when (this) {
            BlendMode.Clear -> SkBlendMode.CLEAR
            BlendMode.Src -> SkBlendMode.SRC
            BlendMode.Dst -> SkBlendMode.DST
            BlendMode.SrcOver -> SkBlendMode.SRC_OVER
            BlendMode.DstOver -> SkBlendMode.DST_OVER
            BlendMode.SrcIn -> SkBlendMode.SRC_IN
            BlendMode.DstIn -> SkBlendMode.DST_IN
            BlendMode.SrcOut -> SkBlendMode.SRC_OUT
            BlendMode.DstOut -> SkBlendMode.DST_OUT
            BlendMode.SrcAtop -> SkBlendMode.SRC_ATOP
            BlendMode.DstAtop -> SkBlendMode.DST_ATOP
            BlendMode.Xor -> SkBlendMode.XOR
            BlendMode.Plus -> SkBlendMode.PLUS
            BlendMode.Modulate -> SkBlendMode.MODULATE
            BlendMode.Screen -> SkBlendMode.SCREEN
            BlendMode.Overlay -> SkBlendMode.OVERLAY
            BlendMode.Darken -> SkBlendMode.DARKEN
            BlendMode.Lighten -> SkBlendMode.LIGHTEN
            BlendMode.ColorDodge -> SkBlendMode.COLOR_DODGE
            BlendMode.ColorBurn -> SkBlendMode.COLOR_BURN
            BlendMode.Hardlight -> SkBlendMode.HARD_LIGHT
            BlendMode.Softlight -> SkBlendMode.SOFT_LIGHT
            BlendMode.Difference -> SkBlendMode.DIFFERENCE
            BlendMode.Exclusion -> SkBlendMode.EXCLUSION
            BlendMode.Multiply -> SkBlendMode.MULTIPLY
            BlendMode.Hue -> SkBlendMode.HUE
            BlendMode.Saturation -> SkBlendMode.SATURATION
            BlendMode.Color -> SkBlendMode.COLOR
            BlendMode.Luminosity -> SkBlendMode.LUMINOSITY
            else -> SkBlendMode.SRC_OVER
        }
    }
}
