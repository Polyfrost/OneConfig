package org.polyfrost.oneconfig.api.notifications.v1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SvgMaskTest {
    @Test
    fun rasterisesStrokesAndFills() {
        val bmp = Notifications.icon("/svgmask/test.svg", 40)!!
        assertEquals(40, bmp.width)
        assertEquals(20, bmp.height)
        fun px(x: Int, y: Int) = IntArray(4) { bmp.pixels[(y * bmp.width + x) * 4 + it].toInt() and 0xFF }.toList()

        assertEquals(listOf(255, 0, 0, 255), px(10, 10))
        assertEquals(255, px(1, 10)[3])
        assertEquals(0, px(10, 5)[3])
        assertEquals(listOf(255, 255, 255, 255), px(30, 10))
        assertEquals(0, px(22, 10)[3])
        assertEquals(255, px(24, 10)[3])
        assertEquals(0, px(23, 10)[3])
        assertTrue(px(10, 7)[3] < 10 && px(10, 8)[3] > 245)
    }

    @Test
    fun rasterisesCircles() {
        val bmp = SvgMask.rasterize("""<svg viewBox="0 0 10 10"><circle cx="5" cy="5" r="3" fill="white"/></svg>""", 10)!!
        fun a(x: Int, y: Int) = bmp.pixels[(y * bmp.width + x) * 4 + 3].toInt() and 0xFF
        assertEquals(255, a(5, 5))
        assertEquals(0, a(0, 0))
        assertEquals(0, a(9, 5))
    }
}
