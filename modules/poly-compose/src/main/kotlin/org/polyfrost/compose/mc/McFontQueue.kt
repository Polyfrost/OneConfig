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

    private val advances = FloatArray(0x590) { Float.NaN }
    @Volatile private var advancesFor: PolyGlyphPage.Rasterizer? = null

    private fun bundledWidth(text: String, scale: Float): Float {
        val r = PolyGlyphPage.rasterizer ?: return text.length * 6f * scale
        if ('§' !in text) plainWidth(r, text, scale)?.let { return it }
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

    private fun plainWidth(r: PolyGlyphPage.Rasterizer, text: String, scale: Float): Float? {
        if (advancesFor !== r) {
            advances.fill(Float.NaN)
            advancesFor = r
        }
        var w = 0f
        for (ch in text) {
            val code = ch.code
            if (code >= 0x590) return null
            var a = advances[code]
            if (a.isNaN()) {
                val main = r.face("minecraft")
                a = if (main != null && main.glyph(code) != 0) main.advance(main.glyph(code)) * 10f
                else r.face("unifont")?.let { it.advance(it.glyph(code)) * 8f } ?: return null
                advances[code] = a
            }
            w += a * scale
        }
        return w
    }
}
