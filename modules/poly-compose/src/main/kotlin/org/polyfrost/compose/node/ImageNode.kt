package org.polyfrost.compose.node

import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.polyfrost.compose.layout.px
import org.polyfrost.compose.render.RenderContext

class ImageNode(
    private val image: Image
) : PolyNode() {
    fun remeasure() {
        style.width = image.width.px
        style.height = image.height.px
    }

    override fun render(ctx: RenderContext) {
        val s = style
        if (s.background.alpha > 0) ctx.rect(x, y, width, height, s.background, s.radius)

        if (s.clipToBounds) {
            ctx.save()
            if (s.radius > 0f) ctx.clipRRect(x, y, width, height, s.radius) else ctx.clipRect(x, y, width, height)
        }
        ctx.image(image, x, y, width, height, Paint().apply {

        })
        if (s.clipToBounds) ctx.restore()

        if (s.borderWidth > 0f && s.borderColor.alpha > 0) ctx.rectStroke(x, y, width, height, s.borderColor, s.borderWidth, s.radius)

    }
}
