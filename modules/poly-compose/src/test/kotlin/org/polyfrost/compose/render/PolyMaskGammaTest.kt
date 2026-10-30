package org.polyfrost.compose.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PolyMaskGammaTest {
    private fun at(t: ByteArray, i: Int) = t[i].toInt() and 0xFF

    @Test
    fun matchesSkiaCorrectingLut() {
        assertEquals(listOf(0, 36, 73, 109, 146, 182, 219, 255), (0 until 8).map { PolyMaskGamma.scale255(it) })
        val white = PolyMaskGamma.table(255, 0.5f)
        val black = PolyMaskGamma.table(0, 0.5f)
        assertEquals(listOf(0, 188, 255), listOf(at(white, 0), at(white, 128), at(white, 255)))
        assertEquals(listOf(0, 91, 255), listOf(at(black, 0), at(black, 128), at(black, 255)))
    }
}
