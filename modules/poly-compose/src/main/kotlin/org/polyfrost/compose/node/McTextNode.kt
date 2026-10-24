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
    var outlineColor: PolyColor? = null

    fun remeasure() {
        val lines = text.lines()
        style.width = PolySize.Px(lines.maxOf {  McFontQueue.measureTextWidth(it, scale) })
        style.height = PolySize.Px(McFontQueue.measureBlockHeight(lines.size, scale))
    }

    override fun render(ctx: RenderContext) {
        // read color.argb at draw time so chroma colours keep cycling every frame
        val outline = outlineColor
        if (outline != null) ctx.mcTextOutline(text, x, y, color.argb, outline.argb, scale)
        else ctx.mcText(text, x, y, color.argb, shadow, scale)
    }
}
