package org.polyfrost.compose.node

import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.RenderContext

class McTextNode : PolyNode() {
    var text: String = ""
    var color: PolyColor = PolyColor.WHITE
    var shadow: Boolean = true
    var scale: Float = 1f
    var outlineColor: PolyColor? = null
    var textProvider: (() -> String)? = null
    private var measuredFonts = -1

    override fun measureContent() {
        val t = textProvider?.invoke() ?: text
        if (t == text && measuredFonts == FontManager.revision) return
        text = t
        remeasure()
    }

    fun remeasure() {
        measuredFonts = FontManager.revision
        val lines = text.lines()
        style.width = PolySize.Px(lines.maxOf {  McFontQueue.measureTextWidth(it, scale) })
        style.height = PolySize.Px(McFontQueue.measureBlockHeight(lines.size, scale))
    }

    override fun render(ctx: RenderContext) {
        val c = color
        val outline = outlineColor
        if (c.chroma && outline == null) {
            ctx.mcTextLive(text, x, y, { c.argb }, shadow, scale)
        } else if (c.chroma || outline?.chroma == true) {
            val t = text; val px = x; val py = y; val sh = shadow; val sc = scale
            ctx.live { draw(this, t, px, py, c, outline, sh, sc) }
        } else {
            draw(ctx, text, x, y, c, outline, shadow, scale)
        }
    }

    private fun draw(ctx: RenderContext, text: String, x: Float, y: Float, color: PolyColor, outline: PolyColor?, shadow: Boolean, scale: Float) {
        if (outline != null) ctx.mcTextOutline(text, x, y, color.argb, outline.argb, scale)
        else ctx.mcText(text, x, y, color.argb, shadow, scale)
    }
}
