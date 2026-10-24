package org.polyfrost.compose.node

import androidx.compose.runtime.State
import org.polyfrost.compose.ui.graphics.FilterQuality
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.RenderContext

class ImageNode(
    var image: ImageBitmap
) : PolyNode() {
    internal var autoWidth = false
    internal var autoHeight = false

    var tint: PolyColor? = null
    var filterQuality = FilterQuality.Low
    var animatedTint: State<PolyColor>? = null

    override fun animating() = super.animating() || animatedTint.let { it != null && it.value != tint }

    fun remeasure() {
        if (style.width == PolySize.Wrap || autoWidth) { style.width = PolySize.Px(image.width.toFloat()); autoWidth = true }
        if (style.height == PolySize.Wrap || autoHeight) { style.height = PolySize.Px(image.height.toFloat()); autoHeight = true }
    }

    override fun render(ctx: RenderContext) {
        val s = style
        val bg = s.drawnBackground
        if (bg.alpha > 0) ctx.rect(x, y, width, height, bg, s.radius)

        if (s.clipToBounds) {
            ctx.save()
            if (s.radius > 0f) ctx.clipRRect(x, y, width, height, s.radius) else ctx.clipRect(x, y, width, height)
        }
        val tint = animatedTint?.value ?: tint
        if (tint != null) ctx.image(image, x, y, width, height, 0f, 0f, 1f, 1f, tint, filterQuality) else ctx.image(image, x, y, width, height, filterQuality)
        if (s.clipToBounds) ctx.restore()

        val border = s.drawnBorderColor
        if (s.borderWidth > 0f && border.alpha > 0) ctx.rectStroke(x, y, width, height, border, s.borderWidth, s.radius)

    }
}
