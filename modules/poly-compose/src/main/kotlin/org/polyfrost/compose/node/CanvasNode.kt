package org.polyfrost.compose.node

import org.polyfrost.compose.render.RenderContext

class CanvasNode : PolyNode() {
    var onDraw: (RenderContext.(x: Float, y: Float, width: Float, height: Float) -> Unit)? = null

    override fun render(ctx: RenderContext) {
        onDraw?.invoke(ctx, x, y, width, height)
    }
}
