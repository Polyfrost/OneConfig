package org.polyfrost.compose.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

class PolyGlyphPageTest {
    private class CountingFace(private val raster: (glyph: Int) -> PolyGlyphPage.Raster?) : PolyGlyphPage.Face {
        var rasterized = 0

        override fun glyph(codepoint: Int) = codepoint
        override fun advance(glyph: Int) = 1f
        override fun raster(glyph: Int, pixelSize: Float): PolyGlyphPage.Raster? {
            rasterized++
            return raster(glyph)
        }
    }

    private fun raster(w: Int, h: Int) = PolyGlyphPage.Raster(0, 0, w, h, ByteArray(w * h) { -1 })

    private fun rows(g: PolyGlyphPage.Glyph): IntRange {
        val y = (g.v0 * PolyGlyphPage.SIZE).roundToInt()
        return y until y + g.h
    }

    @Test
    fun glyphPagesDontUseUpBitmapPages() {
        val face = CountingFace { raster(1000, 1000) }
        val atlas = PolyGlyphPage.atlas(face, 4000)
        repeat(5) { PolyGlyphPage.glyph(atlas, face, 4000, it) }
        val bitmap = PolyBitmap(200, 200, ByteArray(200 * 200 * 4))
        val g = PolyGlyphPage.bitmap(bitmap)
        assertTrue(g != null && g.page === PolyGlyphPage.pages.last())
    }

    @Test
    fun blankGlyphsAreRasterizedOnce() {
        val face = CountingFace { null }
        repeat(3) { assertNull(PolyGlyphPage.glyph(PolyGlyphPage.atlas(face, 64), face, 64, 1)) }
        repeat(3) { assertNull(PolyGlyphPage.glyph(PolyGlyphPage.atlas(face, 64, 3), face, 64, 1, lum = 3)) }
        assertEquals(1, face.rasterized)
    }

    @Test
    fun blankPrerasterizedGlyphsAreRasterizedOnce() {
        val face = CountingFace { null }
        repeat(3) { PolyGlyphPage.prerasterize(face, 64, 1, 0) }
        assertNull(PolyGlyphPage.glyph(PolyGlyphPage.atlas(face, 64), face, 64, 1))
        assertEquals(1, face.rasterized)
    }

    @Test
    fun dirtyRowsCoverOnlyNewGlyphs() {
        val face = CountingFace { raster(8, it) }
        val atlas = PolyGlyphPage.atlas(face, 64)
        val page = PolyGlyphPage.glyph(atlas, face, 64, 12)!!.page
        val uploaded = page.version
        assertTrue(page.dirtyRows(uploaded).isEmpty())

        val next = PolyGlyphPage.glyph(atlas, face, 64, 16)!!
        assertSame(page, next.page)
        assertEquals(rows(next), page.dirtyRows(uploaded))
        assertTrue(page.dirtyRows(page.version).isEmpty())
        assertTrue((PolyGlyphPage.WHITE_UV * PolyGlyphPage.SIZE).toInt() in page.dirtyRows(0))
    }

    @Test
    fun dirtyRowsSpanShelvesForEachUploader() {
        val face = CountingFace { raster(PolyGlyphPage.SIZE / 2, it) }
        val atlas = PolyGlyphPage.atlas(face, 64)
        val page = PolyGlyphPage.pages.last()
        val a = page.version
        val first = PolyGlyphPage.glyph(atlas, face, 64, 10)!!
        val b = page.version
        val wrapped = PolyGlyphPage.glyph(atlas, face, 64, 20)!!
        assertSame(page, first.page)
        assertSame(page, wrapped.page)
        assertTrue(rows(wrapped).first > rows(first).last)
        assertEquals(rows(wrapped), page.dirtyRows(b))
        assertEquals(rows(first).first..rows(wrapped).last, page.dirtyRows(a))
    }
}
