package org.polyfrost.compose.mc

import kotlin.math.roundToInt
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyGlyphPage

/**
 * A glyph of the game's active font, drawn from the [u0]..[v1] region of [bitmap] (null for blank glyphs) and tinted by the text color.
 * Metrics are in unscaled text units: [top] is measured from the line top, and [offset] is both the bold and the shadow offset.
 */
class McGlyph(
    val bitmap: PolyBitmap?,
    val u0: Float, val v0: Float, val u1: Float, val v1: Float,
    val top: Float, val width: Float, val height: Float,
    val advance: Float, val offset: Float,
) {
    private val pixelX = bitmap?.let { (u0 * it.width).roundToInt() } ?: 0
    private val pixelY = bitmap?.let { (v0 * it.height).roundToInt() } ?: 0
    internal val pixelWidth = bitmap?.let { (u1 * it.width).roundToInt() - pixelX } ?: 0
    internal val pixelHeight = bitmap?.let { (v1 * it.height).roundToInt() - pixelY } ?: 0

    internal val linearBitmap: PolyBitmap? by lazy {
        val b = bitmap ?: return@lazy null
        val w = pixelWidth
        val h = pixelHeight
        val pixels = ByteArray(w * h * 4)
        for (row in 0 until h) {
            val start = ((pixelY + row) * b.width + pixelX) * 4
            b.pixels.copyInto(pixels, row * w * 4, start, start + w * 4)
        }
        for (i in pixels.indices step 4) {
            val alpha = pixels[i + 3].toInt() and 0xFF
            for (channel in 0..2) {
                pixels[i + channel] = (((pixels[i + channel].toInt() and 0xFF) * alpha + 127) / 255).toByte()
            }
        }
        PolyBitmap(w, h, pixels)
    }
}

object McGlyphs {
    /**
     * Glyphs of the game's active font for Minecraft text drawn by the tessellator; codepoints it returns null for fall back to the
     * bundled faces. Call [FontManager.invalidateMetrics] once the glyphs it returns change.
     */
    @JvmStatic
    @Volatile
    var source: ((codepoint: Int) -> McGlyph?)? = null

    /** Width of [text], honoring § formatting codes, as the tessellator draws it. */
    @JvmStatic
    fun width(text: String, scale: Float): Float {
        val source = source
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
            val g = source?.invoke(cp)
            w += if (g == null) bundledAdvance(cp, bold) else if (bold) g.advance + g.offset else g.advance
        }
        return w * scale
    }

    private fun bundledAdvance(cp: Int, bold: Boolean): Float {
        val r = PolyGlyphPage.rasterizer ?: return 0f
        val main = r.face(if (bold) "minecraft-bold" else "minecraft")
        if (main != null && main.glyph(cp) != 0) return main.advance(main.glyph(cp)) * 10f
        val uni = r.face("unifont") ?: return 0f
        return uni.advance(uni.glyph(cp)) * 8f
    }
}
