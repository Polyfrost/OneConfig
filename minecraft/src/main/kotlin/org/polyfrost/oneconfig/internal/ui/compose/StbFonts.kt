package org.polyfrost.oneconfig.internal.ui.compose

import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype.stbtt_FindGlyphIndex
import org.lwjgl.stb.STBTruetype.stbtt_FreeSDF
import org.lwjgl.stb.STBTruetype.stbtt_GetFontOffsetForIndex
import org.lwjgl.stb.STBTruetype.stbtt_GetFontVMetrics
import org.lwjgl.stb.STBTruetype.stbtt_GetGlyphBitmapBox
import org.lwjgl.stb.STBTruetype.stbtt_GetGlyphHMetrics
import org.lwjgl.stb.STBTruetype.stbtt_GetGlyphSDF
import org.lwjgl.stb.STBTruetype.stbtt_InitFont
import org.lwjgl.stb.STBTruetype.stbtt_MakeGlyphBitmap
import org.lwjgl.stb.STBTruetype.stbtt_ScaleForMappingEmToPixels
import org.lwjgl.system.MemoryUtil
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyGlyphPage
import kotlin.math.roundToInt

internal object StbFonts : PolyFont.Engine, PolyGlyphPage.Rasterizer {
    class Face(bytes: ByteArray) : PolyGlyphPage.Face {
        private val data = MemoryUtil.memAlloc(bytes.size).put(bytes).flip()
        val info: STBTTFontinfo = STBTTFontinfo.malloc()
        val emScale: Float
        private val unitMetrics: FloatArray
        private val glyphIds = HashMap<Int, Int>()
        private val advances = HashMap<Int, Int>()

        init {
            check(stbtt_InitFont(info, data, stbtt_GetFontOffsetForIndex(data, 0))) { "stb_truetype can't read font" }
            emScale = stbtt_ScaleForMappingEmToPixels(info, 1f)
            val asc = IntArray(1); val desc = IntArray(1); val gap = IntArray(1)
            stbtt_GetFontVMetrics(info, asc, desc, gap)
            val post = table(bytes, "post")
            val ulPos = if (post < 0) Float.NaN else -s16(bytes, post + 8) * emScale
            val ulThick = if (post < 0) Float.NaN else s16(bytes, post + 10) * emScale
            val os2 = table(bytes, "OS/2")
            val soThick = if (os2 < 0) Float.NaN else s16(bytes, os2 + 26) * emScale
            val soPos = if (os2 < 0) Float.NaN else -s16(bytes, os2 + 28) * emScale
            unitMetrics = floatArrayOf(-asc[0] * emScale, -desc[0] * emScale, gap[0] * emScale, ulPos, ulThick, soPos, soThick)
        }

        override fun glyph(codepoint: Int): Int = glyphIds.getOrPut(codepoint) { stbtt_FindGlyphIndex(info, codepoint) }

        override fun advance(glyph: Int): Float = advanceUnits(glyph) * emScale

        fun advanceUnits(glyph: Int): Int = advances.getOrPut(glyph) {
            val adv = IntArray(1); val lsb = IntArray(1)
            stbtt_GetGlyphHMetrics(info, glyph, adv, lsb)
            adv[0]
        }

        fun metrics(size: Float) = PolyFont.Metrics(
            unitMetrics[0] * size, unitMetrics[1] * size, unitMetrics[2] * size,
            if (unitMetrics[3].isNaN()) size * 0.08f else unitMetrics[3] * size,
            if (unitMetrics[4].isNaN()) size * 0.06f else unitMetrics[4] * size,
            if (unitMetrics[5].isNaN() || unitMetrics[6] <= 0f) unitMetrics[0] * size / 3f else unitMetrics[5] * size,
            if (unitMetrics[6].isNaN() || unitMetrics[6] <= 0f) (if (unitMetrics[4].isNaN()) size * 0.06f else unitMetrics[4] * size) else unitMetrics[6] * size,
        )

        override fun raster(glyph: Int, pixelSize: Float): PolyGlyphPage.Raster? {
            val scale = stbtt_ScaleForMappingEmToPixels(info, pixelSize)
            return if (DILATE > 0f) rasterDilated(glyph, scale, pixelSize) else rasterExact(glyph, scale)
        }

        private fun rasterExact(id: Int, scale: Float): PolyGlyphPage.Raster? {
            val x0 = IntArray(1); val y0 = IntArray(1); val x1 = IntArray(1); val y1 = IntArray(1)
            stbtt_GetGlyphBitmapBox(info, id, scale, scale, x0, y0, x1, y1)
            val w = x1[0] - x0[0]
            val h = y1[0] - y0[0]
            if (w <= 0 || h <= 0) return null
            val buf = MemoryUtil.memAlloc(w * h)
            try {
                stbtt_MakeGlyphBitmap(info, buf, w, h, w, scale, scale, id)
                return PolyGlyphPage.Raster(x0[0], y0[0], w, h, ByteArray(w * h).also { buf.get(it) })
            } finally {
                MemoryUtil.memFree(buf)
            }
        }

        private fun rasterDilated(id: Int, scale: Float, pixelSize: Float): PolyGlyphPage.Raster? {
            val w = IntArray(1); val h = IntArray(1); val xo = IntArray(1); val yo = IntArray(1)
            val sdf = stbtt_GetGlyphSDF(info, scale, id, SDF_PAD, SDF_EDGE.toByte(), SDF_SCALE, w, h, xo, yo) ?: return null
            try {
                val bias = 0.5f + DILATE * pixelSize
                val out = ByteArray(w[0] * h[0]) { i ->
                    val dist = ((sdf.get(i).toInt() and 0xFF) - SDF_EDGE) / SDF_SCALE
                    ((dist + bias).coerceIn(0f, 1f) * 255f).roundToInt().toByte()
                }
                return PolyGlyphPage.Raster(xo[0], yo[0], w[0], h[0], out)
            } finally {
                stbtt_FreeSDF(sdf)
            }
        }
    }

    private val faces = HashMap<String, Face?>()

    override fun face(name: String): Face? = faces.getOrPut(name) { FontManager.fontData(name)?.let(::Face) }

    override fun measureTextWidth(font: PolyFont, text: String): Float {
        val face = face(font.name) ?: return 0f
        var units = 0L
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            units += face.advanceUnits(face.glyph(cp))
        }
        return units * face.emScale * font.size
    }

    override fun metrics(font: PolyFont) = face(font.name)?.metrics(font.size) ?: PolyFont.Metrics(0f, 0f, 0f, 0f, 0f)

    private val DILATE = if (System.getProperty("os.name").startsWith("Mac")) java.lang.Float.parseFloat(System.getProperty("oneconfig.hud.textDilate", "0.0035")) else 0f
    private const val SDF_PAD = 2
    private const val SDF_EDGE = 128
    private const val SDF_SCALE = 32f

    private fun u16(b: ByteArray, o: Int) = (b[o].toInt() and 0xFF shl 8) or (b[o + 1].toInt() and 0xFF)
    private fun s16(b: ByteArray, o: Int) = u16(b, o).toShort().toInt()
    private fun u32(b: ByteArray, o: Int) = (u16(b, o) shl 16) or u16(b, o + 2)

    private fun table(b: ByteArray, tag: String): Int {
        for (i in 0 until u16(b, 4)) {
            val r = 12 + i * 16
            if (String(b, r, 4, Charsets.ISO_8859_1) == tag) return u32(b, r + 8)
        }
        return -1
    }
}
