package org.polyfrost.compose.node

import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.RenderContext

class McTextNode : PolyNode() {
    var text: String = ""
    var color: PolyColor = PolyColor.WHITE
    var shadow: Boolean = true
    var scale: Float = 1f

    fun remeasure() {
        style.width = PolySize.Px(McFontQueue.measureTextWidth(text, scale))
        style.height = PolySize.Px(McFontQueue.measureTextHeight(scale))
    }

    override fun render(ctx: RenderContext) {
        val renderer = McFontQueue.renderer
        // Read color.argb at draw time so chroma colours keep cycling every frame.
        if (renderer != null) renderer(ctx.canvas, text, x, y, color.argb, shadow, scale)
    }
}
