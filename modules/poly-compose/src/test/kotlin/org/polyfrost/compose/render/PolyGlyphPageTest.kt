package org.polyfrost.compose.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PolyGlyphPageTest {
    private class CountingFace(private val raster: PolyGlyphPage.Raster?) : PolyGlyphPage.Face {
        var rasterized = 0

        override fun glyph(codepoint: Int) = 1
        override fun advance(glyph: Int) = 1f
        override fun raster(glyph: Int, pixelSize: Float): PolyGlyphPage.Raster? {
            rasterized++
            return raster
        }
    }

    @Test
    fun blankGlyphsAreRasterizedOnce() {
        val face = CountingFace(null)
        repeat(3) { assertNull(PolyGlyphPage.glyph(PolyGlyphPage.atlas(face, 64), face, 64, 1)) }
        repeat(3) { assertNull(PolyGlyphPage.glyph(PolyGlyphPage.atlas(face, 64, 3), face, 64, 1, lum = 3)) }
        assertEquals(1, face.rasterized)
    }

    @Test
    fun blankPrerasterizedGlyphsAreRasterizedOnce() {
        val face = CountingFace(null)
        repeat(3) { PolyGlyphPage.prerasterize(face, 64, 1, 0) }
        assertNull(PolyGlyphPage.glyph(PolyGlyphPage.atlas(face, 64), face, 64, 1))
        assertEquals(1, face.rasterized)
    }
}
