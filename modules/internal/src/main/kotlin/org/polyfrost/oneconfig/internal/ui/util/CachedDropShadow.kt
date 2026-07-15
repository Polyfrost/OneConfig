package org.polyfrost.oneconfig.internal.ui.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter
import kotlin.math.ceil

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

    private var mask: ImageBitmap? = null
    private var maskSize: Size = Size.Unspecified
    private var maskDensity = 0f
    private var maskLayoutDirection = LayoutDirection.Ltr

    private var tint: ColorFilter? = null
    private var tintColor: Color = Color.Unspecified

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
        mask = null
        maskSize = Size.Unspecified
    }

    override fun ContentDrawScope.draw() {
        drawShadow()
        drawContent()
    }

    private fun DrawScope.drawShadow() {
        if (size.width <= 0f || size.height <= 0f) return

        val radiusPx = shadow.radius.toPx()
        val spreadPx = shadow.spread.toPx()

        if (
            mask == null ||
            maskSize != size ||
            maskDensity != density ||
            maskLayoutDirection != layoutDirection
        ) {
            mask = buildMask(size, radiusPx, spreadPx)
            maskSize = size
            maskDensity = density
            maskLayoutDirection = layoutDirection
        }
        val bitmap = mask ?: return

        val color = shadow.color
        var filter = tint
        if (filter == null || tintColor != color) {
            filter = ColorFilter.tint(color)
            tint = filter
            tintColor = color
        }

        val inset = -(radiusPx + spreadPx)
        translate(shadow.offset.x.toPx(), shadow.offset.y.toPx()) {
            drawImage(
                image = bitmap,
                topLeft = Offset(inset, inset),
                alpha = shadow.alpha,
                colorFilter = filter,
                blendMode = shadow.blendMode,
            )
        }
    }

    private fun DrawScope.buildMask(size: Size, radiusPx: Float, spreadPx: Float): ImageBitmap? {
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
        return bitmap
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
        tint = null
        tintColor = Color.Unspecified
    }

    private companion object {
        const val BLUR_SIGMA_SCALE = 0.57735f
    }
}
