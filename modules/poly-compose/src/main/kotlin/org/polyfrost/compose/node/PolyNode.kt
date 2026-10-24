package org.polyfrost.compose.node

import org.polyfrost.compose.layout.PolyLayoutStyle
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.ui.draw.ContentDrawScope

open class PolyNode {
    val children = mutableListOf<PolyNode>()
    val style = PolyLayoutStyle()
    var parent: PolyNode? = null
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f

    var contentWidth = 0f
    var contentHeight = 0f

    open fun measureContent(maxWidth: Float, maxHeight: Float) {}

    open fun render(ctx: RenderContext) {
        val s = style
        for (sh in s.shadows) ctx.shadow(x, y, width, height, s.radius, sh)
        val bg = s.background
        if (bg.alpha > 0) ctx.rect(x, y, width, height, bg, s.radius)
        s.gradient?.let { ctx.rectGradient(x, y, width, height, it, s.radius) }
        val bottom = s.borderColorBottom
        if (s.borderWidth > 0f) {
            val border = s.borderColor
            if (bottom != null) ctx.rectStrokeGradient(x, y, width, height, border, bottom, s.borderWidth, s.radius)
            else if (border.alpha > 0) ctx.rectStroke(x, y, width, height, border, s.borderWidth, s.radius)
        }

        if (s.clipToBounds) {
            ctx.save()
            if (s.radius > 0f) ctx.clipRRect(x, y, width, height, s.radius) else ctx.clipRect(x, y, width, height)
        }
        renderChildren(ctx)
        if (s.clipToBounds) ctx.restore()
    }

    protected fun renderChildren(ctx: RenderContext) {
        for (c in children) c.renderFaded(ctx)
    }

    fun renderFaded(ctx: RenderContext) {
        val s = style
        val a = s.alpha
        if (a <= 0f) return
        val transformed = s.scaleX != 1f || s.scaleY != 1f || s.translateX != 0f || s.translateY != 0f
        if (a >= 1f && !transformed) return draw(ctx)
        if (a < 1f) ctx.saveLayerAlpha(a) else ctx.save()
        if (transformed) {
            val cx = x + width / 2f
            val cy = y + height / 2f
            ctx.translate(cx + s.translateX, cy + s.translateY)
            ctx.scale(s.scaleX, s.scaleY)
            ctx.translate(-cx, -cy)
        }
        draw(ctx)
        ctx.restore()
    }

    private fun draw(ctx: RenderContext) {
        val over = style.drawWithContent ?: return render(ctx)
        ContentDrawScope(ctx, this).over()
    }
}
