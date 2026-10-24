package org.polyfrost.compose.node

import org.polyfrost.compose.render.RenderContext

class RootNode : PolyNode() {
    var layoutRequested = false

    var density = 1f

    override fun render(ctx: RenderContext) {
        super.render(ctx)
        overlays(this, ctx)
    }

    private fun overlays(node: PolyNode, ctx: RenderContext) {
        for (c in node.children) {
            if (c.style.overlay) c.renderFaded(ctx)
            overlays(c, ctx)
        }
    }
}

fun PolyNode.requestLayout() {
    var n: PolyNode = this
    while (true) n = n.parent ?: break
    (n as? RootNode)?.layoutRequested = true
}
