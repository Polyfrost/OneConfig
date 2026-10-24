package org.polyfrost.compose.node

import org.polyfrost.compose.layout.PolyLayoutStyle
import org.polyfrost.compose.render.RenderContext
import kotlin.math.abs
import kotlin.math.max

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
        val bg = s.drawnBackground
        if (bg.alpha > 0) ctx.rect(x, y, width, height, bg, s.radius)
        s.gradient?.let { ctx.rectGradient(x, y, width, height, it, s.radius) }
        val bottom = s.borderColorBottom
        if (s.borderWidth > 0f) {
            val border = s.drawnBorderColor
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
        val cull = style.scroll != null && style.clipToBounds
        for (c in children) if (!c.style.overlay && !(cull && c.outside(this))) c.renderFaded(ctx)
    }

    private fun outside(p: PolyNode): Boolean {
        val s = style
        if (animating() || s.scaleX != 1f || s.scaleY != 1f || s.translateX != 0f || s.translateY != 0f || s.drawnOffsetX != 0f) return false
        var pad = 0f
        for (sh in s.shadows) pad = max(pad, sh.spread.value + max(abs(sh.offset.x.value), abs(sh.offset.y.value)) + sh.radius.value * 2f + 1f)
        return x - pad >= p.x + p.width || x + width + pad <= p.x || y - pad >= p.y + p.height || y + height + pad <= p.y
    }

    fun renderFaded(ctx: RenderContext) {
        if (animating()) return ctx.live { renderFaded(this, style.animatedAlpha?.value ?: style.alpha) }
        renderFaded(ctx, style.alpha)
    }

    protected open fun animating() = style.animating

    private fun renderFaded(ctx: RenderContext, a: Float) {
        val s = style
        if (a <= 0f) return
        val dx = s.drawnOffsetX
        val transformed = s.scaleX != 1f || s.scaleY != 1f || s.translateX != 0f || s.translateY != 0f || dx != 0f
        val fade = s.fade?.invoke(this)
        if (a >= 1f && !transformed && fade == null) return render(ctx)
        if (a < 1f) ctx.saveLayerAlpha(a) else ctx.save()
        if (transformed) {
            val cx = x + width / 2f
            val cy = y + height / 2f
            ctx.translate(cx + s.translateX + dx, cy + s.translateY)
            ctx.scale(s.scaleX, s.scaleY)
            ctx.translate(-cx, -cy)
        }
        if (fade != null) {
            val o = if (fade.vertical) y else x
            ctx.saveLayerFade(org.polyfrost.compose.render.PolyFade(fade.vertical, FloatArray(fade.stops.size) { fade.stops[it] + o }, fade.alphas))
        }
        render(ctx)
        if (fade != null) ctx.restore()
        ctx.restore()
    }
}
