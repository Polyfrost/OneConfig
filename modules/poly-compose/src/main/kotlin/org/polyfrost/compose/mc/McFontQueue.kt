package org.polyfrost.compose.mc

import org.polyfrost.compose.render.PolyGlyphPage

object McFontQueue {
    var measureWidth: ((text: String, scale: Float) -> Float)? = null
    var measureHeight: ((scale: Float) -> Float)? = null


    fun measureTextWidth(text: String, scale: Float): Float =
        measureWidth?.invoke(text, scale) ?: bundledWidth(text, scale)

    fun measureTextHeight(scale: Float): Float =
        measureHeight?.invoke(scale) ?: (9f * scale)

    fun measureBlockHeight(lines: Int, scale: Float): Float =
        lines * measureTextHeight(scale) - scale

    private fun bundledWidth(text: String, scale: Float): Float {
        val r = PolyGlyphPage.rasterizer ?: return text.length * 6f * scale
        var w = 0f
        var bold = false
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            if (cp == 0xA7 && i < text.length) {
                val code = text[i++].lowercaseChar()
                if (code == 'l') bold = true else if (code == 'r' || code in '0'..'9' || code in 'a'..'f') bold = false
                continue
            }
            val main = r.face(if (bold) "minecraft-bold" else "minecraft")
            w += if (main != null && main.glyph(cp) != 0) {
                main.advance(main.glyph(cp)) * 10f * scale
            } else {
                val uni = r.face("unifont") ?: continue
                uni.advance(uni.glyph(cp)) * 8f * scale
            }
        }
        return w
    }
}
