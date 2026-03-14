package org.polyfrost.compose.node

import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.render.RenderContext

class McTextNode : PolyNode() {
    var text: String = ""
    var color: Int = 0xFFFFFFFF.toInt()
    var shadow: Boolean = true
    var scale: Float = 1f

    fun remeasure() {
        style.width = PolySize.Px(McFontQueue.measureTextWidth(text, scale))
        style.height = PolySize.Px(McFontQueue.measureTextHeight(scale))
    }

    override fun render(ctx: RenderContext) {
        val renderer = McFontQueue.renderer
        if (renderer != null) renderer(ctx.canvas, text, x, y, color, shadow, scale)
    }
}
