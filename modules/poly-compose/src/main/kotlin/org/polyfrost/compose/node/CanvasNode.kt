package org.polyfrost.compose.node

import org.polyfrost.compose.render.RenderContext

class CanvasNode : PolyNode() {
    var onDraw: (RenderContext.(x: Float, y: Float, width: Float, height: Float) -> Unit)? = null

    var live = false

    override fun render(ctx: RenderContext) {
        val draw = onDraw ?: return
        if (!live) return draw(ctx, x, y, width, height)
        val x = x; val y = y; val w = width; val h = height
        ctx.live { draw(this, x, y, w, h) }
    }
}
