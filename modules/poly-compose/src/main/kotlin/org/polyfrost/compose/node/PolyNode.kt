package org.polyfrost.compose.node

import org.polyfrost.compose.layout.PolyLayoutStyle
import org.polyfrost.compose.render.CapturingRenderer
import org.polyfrost.compose.render.RenderContext

open class PolyNode {
    val children = mutableListOf<PolyNode>()
    val style = PolyLayoutStyle()
    var parent: PolyNode? = null
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f

    private var decoration: Decoration? = null

    open fun measureContent() {}

    open fun render(ctx: RenderContext) {
        val s = style
        decorate(ctx)

        if (s.clipToBounds) {
            ctx.save()
            if (s.radius > 0f) ctx.clipRRect(x, y, width, height, s.radius) else ctx.clipRect(x, y, width, height)
        }
        children.forEach { child ->
            val fade = child.style.fade
            if (fade == null) {
                child.render(ctx)
            } else {
                ctx.live {
                    val a = fade()
                    if (a > 0f) {
                        saveLayerAlpha(a)
                        child.render(this)
                        restore()
                    }
                }
            }
        }
        if (s.clipToBounds) ctx.restore()
    }

    private fun decorate(ctx: RenderContext) {
        val s = style
        val bg = s.background
        val border = s.borderColor
        val stroke = s.borderWidth
        if (bg.chroma || stroke > 0f && border.chroma) {
            val px = x; val py = y; val w = width; val h = height; val r = s.radius
            ctx.live { drawDecoration(this, px, py, w, h, r, bg.argb, if (stroke > 0f) border.argb else 0, stroke) }
            return
        }
        val bgArgb = bg.argb
        val borderArgb = if (stroke > 0f) border.argb else 0
        if (bgArgb ushr 24 == 0 && borderArgb ushr 24 == 0) return
        val cr = if (ctx.cacheNodes) ctx.renderer as? CapturingRenderer else null
        if (cr == null) return drawDecoration(ctx, x, y, width, height, s.radius, bgArgb, borderArgb, stroke)
        val d = decoration
        if (d != null && d.x == x && d.y == y && d.w == width && d.h == height && d.radius == s.radius && d.bg == bgArgb &&
            d.border == borderArgb && d.stroke == stroke && cr.sameState(d.key) && cr.valid(d.captured)
        ) {
            cr.append(d.captured)
            return
        }
        val key = cr.stateKey()
        val mark = cr.mark()
        drawDecoration(ctx, x, y, width, height, s.radius, bgArgb, borderArgb, stroke)
        decoration = Decoration(x, y, width, height, s.radius, bgArgb, borderArgb, stroke, key, cr.since(mark))
    }

    private class Decoration(
        val x: Float, val y: Float, val w: Float, val h: Float, val radius: Float,
        val bg: Int, val border: Int, val stroke: Float, val key: Any, val captured: Any,
    )
}

private fun drawDecoration(ctx: RenderContext, x: Float, y: Float, w: Float, h: Float, radius: Float, bg: Int, border: Int, stroke: Float) {
    if (bg ushr 24 != 0) ctx.renderer.rect(x, y, w, h, bg, radius)
    if (stroke > 0f && border ushr 24 != 0) ctx.renderer.rectStroke(x, y, w, h, border, stroke, radius)
}
