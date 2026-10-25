package org.polyfrost.compose.node

import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.RenderContext

class TextNode : PolyNode() {
    var text: String = ""
    var color: PolyColor = PolyColor.WHITE
    var fontSize: Float = 12f
    var shadow: Boolean = false
    var shadowColor: PolyColor = PolyColor(0x80000000.toInt())
    var shadowOffset: Float = 1f

    var fontName: String? = null

    private fun font() = fontName?.let { PolyFont(it, fontSize) } ?: PolyFont.default(fontSize)

    fun remeasure() {
        val font = font()
        style.width = PolySize.Px(font.measureTextWidth(text))
        style.height = PolySize.Px(font.metrics.let { it.descent - it.ascent })
    }

    override fun render(ctx: RenderContext) {
        val font = font()
        val baseline = y - font.metrics.ascent
        if (shadow) ctx.text(text, x + shadowOffset, baseline + shadowOffset, shadowColor, font)
        ctx.text(text, x, baseline, color, font)
    }
}
