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
        style.height = PolySize.Px(lines.size * McFontQueue.measureTextHeight(scale))
    }

    override fun render(ctx: RenderContext) {
        val renderer = McFontQueue.renderer
        // read color.argb at draw time so chroma colours keep cycling every frame
        if (renderer == null) return
        val outline = outlineColor
        if (outline != null) {
            val plainText = COLOR_CODE.replace(text, "§r")
            val outlineArgb = outline.argb
            for (offX in -1..1) {
                for (offY in -1..1) {
                    if (offX == 0 && offY == 0) continue
                    renderer(ctx.canvas, plainText, x + offX * scale, y + offY * scale, outlineArgb, false, scale)
                }
            }
        }
        renderer(ctx.canvas, text, x, y, color.argb, shadow, scale)
    }

    private companion object {
        val COLOR_CODE = Regex("(?i)§[0-9a-f]")
    }
}
