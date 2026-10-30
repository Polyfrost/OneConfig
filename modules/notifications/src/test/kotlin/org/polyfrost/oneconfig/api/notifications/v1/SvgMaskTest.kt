package org.polyfrost.oneconfig.api.notifications.v1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

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

    @Test
    fun parsesArcsWithCompactFlags() {
        for (d in listOf("M0 0 A5 5 0 0 1 10 0", "M0 0a5 5 0 0110 0")) {
            val pts = SvgMask.parsePath(d).single()
            assertEquals(10f, pts[pts.size - 2], 1e-3f, d)
            assertEquals(0f, pts[pts.size - 1], 1e-3f, d)
            val ys = pts.filterIndexed { i, _ -> i % 2 == 1 }
            assertEquals(5f, ys.maxOf { abs(it) }, 0.05f, d)
        }
    }

    @Test
    fun fitsViewBoxIntoRootViewport() {
        fun raster(par: String, content: String): (Int, Int) -> Int {
            val bmp = SvgMask.rasterize("""<svg width="200" height="100" viewBox="0 0 10 10" preserveAspectRatio="$par">$content</svg>""", 40)!!
            assertEquals(40, bmp.width)
            assertEquals(20, bmp.height)
            return { x, y -> bmp.pixels[(y * bmp.width + x) * 4 + 3].toInt() and 0xFF }
        }
        val full = """<rect width="10" height="10" fill="white"/>"""

        val meet = raster("xMidYMid meet", full)
        assertEquals(255, meet(20, 10))
        assertEquals(255, meet(11, 1))
        assertEquals(0, meet(8, 10))
        assertEquals(0, meet(31, 10))

        val none = raster("none", full)
        assertEquals(255, none(1, 10))
        assertEquals(255, none(38, 18))

        val slice = raster("xMidYMid slice", """<rect width="10" height="5" fill="white"/>""")
        assertEquals(255, slice(1, 8))
        assertEquals(0, slice(1, 11))
    }

    @Test
    fun rasterisesLines() {
        val bmp = SvgMask.rasterize("""<svg viewBox="0 0 10 10" fill="none" stroke="white" stroke-width="2"><line x1="0" y1="5" x2="10" y2="5"/></svg>""", 10)!!
        fun a(x: Int, y: Int) = bmp.pixels[(y * bmp.width + x) * 4 + 3].toInt() and 0xFF
        assertTrue(a(5, 5) > 200)
        assertEquals(0, a(5, 1))
    }

    @Test
    fun appliesElementTransform() {
        val bmp = SvgMask.rasterize("""<svg viewBox="0 0 20 20"><rect x="8" y="0" width="4" height="20" transform="rotate(90 10 10)" fill="white"/></svg>""", 20)!!
        fun a(x: Int, y: Int) = bmp.pixels[(y * bmp.width + x) * 4 + 3].toInt() and 0xFF
        assertEquals(255, a(2, 10))
        assertEquals(0, a(10, 2))
    }

    @Test
    fun groupsPassTransformAndStyleToUse() {
        val svg = """<svg viewBox="0 0 20 20"><defs><rect id="r" width="4" height="4"/></defs><g transform="translate(10 0)" style="fill:#00ff00"><use href="#r" x="2" y="2"/></g></svg>"""
        val bmp = SvgMask.rasterize(svg, 20)!!
        fun px(x: Int, y: Int) = IntArray(4) { bmp.pixels[(y * bmp.width + x) * 4 + it].toInt() and 0xFF }.toList()
        assertEquals(listOf(0, 255, 0, 255), px(13, 3))
        assertEquals(0, px(3, 3)[3])
    }

    @Test
    fun buttCapsStopAtEndpoints() {
        val bmp = SvgMask.rasterize("""<svg viewBox="0 0 10 10"><line x1="2" y1="5" x2="8" y2="5" stroke="white" stroke-width="2"/></svg>""", 10)!!
        fun a(x: Int, y: Int) = bmp.pixels[(y * bmp.width + x) * 4 + 3].toInt() and 0xFF
        assertEquals(255, a(2, 4))
        assertEquals(0, a(1, 4))
        assertEquals(0, a(8, 4))
    }

    @Test
    fun gradientHrefSharesStopsOnly() {
        val svg = """<svg viewBox="0 0 10 10"><defs><linearGradient id="s" gradientTransform="scale(0.1)"><stop stop-color="#ff0000"/><stop offset="1" stop-color="#0000ff"/></linearGradient>""" +
            """<linearGradient id="g" href="#s"/></defs><rect width="10" height="10" fill="url(#g)"/></svg>"""
        val bmp = SvgMask.rasterize(svg, 10)!!
        val right = bmp.pixels[(5 * 10 + 9) * 4 + 2].toInt() and 0xFF
        val left = bmp.pixels[(5 * 10) * 4].toInt() and 0xFF
        assertTrue(left > 220 && right > 220)
    }

    @Test
    fun tintKeepsPaintAlpha() {
        val svg = """<svg viewBox="0 0 10 10"><rect width="10" height="10" fill="url(#g)"/><defs><linearGradient id="g"><stop stop-color="white" stop-opacity="0.25"/></linearGradient></defs></svg>"""
        val bmp = SvgMask.rasterize(svg, 10, -1)!!
        assertTrue(abs((bmp.pixels[5 * 10 * 4 + 5 * 4 + 3].toInt() and 0xFF) - 64) <= 1)
    }

    @Test
    fun dashesStrokes() {
        val bmp = SvgMask.rasterize("""<svg viewBox="0 0 20 4"><path d="M0 2H20" stroke="white" stroke-width="2" stroke-dasharray="4 4"/></svg>""", 20)!!
        fun a(x: Int) = bmp.pixels[(2 * bmp.width + x) * 4 + 3].toInt() and 0xFF
        assertTrue(a(2) > 200 && a(10) > 200 && a(18) > 200)
        assertEquals(0, a(6))
        assertEquals(0, a(14))
    }
}
