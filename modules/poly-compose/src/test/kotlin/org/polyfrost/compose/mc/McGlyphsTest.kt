package org.polyfrost.compose.mc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyTessellator

class McGlyphsTest {
    private class Recorder : PolyTessellator() {
        val xs = ArrayList<Float>()
        var allNearest = true
        override fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {
            allNearest = allNearest && nearest
            for (q in 0 until colors.size / 4) xs += verts[q * 16]
        }
        override fun bitmapTexture(bitmap: PolyBitmap) = bitmap
        override fun live(draw: (PolyRenderer) -> Unit) = draw(this)
        fun done() = also { flush() }
    }

    @Test
    fun resourceGlyphsDrawWhereTheyMeasure() {
        val bitmap = PolyBitmap(4, 2, ByteArray(32) { -1 })
        val a = McGlyph(bitmap, 0f, 0f, 0.5f, 1f, 1f, 2f, 2f, 3f, 1f)
        val space = McGlyph(null, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 4f, 1f)
        val old = McGlyphs.source
        McGlyphs.source = { cp -> if (cp == 'a'.code) a else space.takeIf { cp == ' '.code } }
        try {
            // plain a, then bold a, space and a, each bold glyph one offset wider
            assertEquals((3f + 4f + 5f + 4f) * 2f, McGlyphs.width("a§la a", 2f))
            val r = Recorder().apply { mcText("a§la a", 10f, 20f, -1, false, 2f) }.done()
            assertTrue(r.allNearest)
            assertEquals(listOf(10f, 16f, 18f, 34f, 36f), r.xs.sorted())
        } finally {
            McGlyphs.source = old
        }
    }
}
