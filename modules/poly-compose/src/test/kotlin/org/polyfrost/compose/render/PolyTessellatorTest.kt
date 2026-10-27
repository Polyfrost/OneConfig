package org.polyfrost.compose.render

import kotlin.math.abs
import kotlin.math.hypot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

class PolyTessellatorTest {
    private class Batch(val texture: Any, val verts: FloatArray, val colors: IntArray, val nearest: Boolean = false)

    private class Recorder : PolyTessellator() {
        val batches = ArrayList<Batch>()
        override fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {
            batches.add(Batch(texture, verts, colors, nearest))
        }
        override fun bitmapTexture(bitmap: PolyBitmap) = bitmap
        override fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) = Unit
        var lives = 0
        override fun live(draw: (PolyRenderer) -> Unit) { lives++; draw(this) }
        val rectYs = ArrayList<Float>()
        override fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) { rectYs += y; super.rect(x, y, w, h, color, radius) }
        fun done() = also { flush() }.batches
    }

    @Test
    fun outlineGlyphGrowsCoverage() {
        val face = object : PolyGlyphPage.Face {
            override fun glyph(codepoint: Int) = 1
            override fun advance(glyph: Int) = 1f
            override fun raster(glyph: Int, pixelSize: Float) = PolyGlyphPage.Raster(2, 3, 1, 1, byteArrayOf(-1))
        }
        fun alphas(round: Boolean): List<Int> {
            val g = PolyGlyphPage.glyph(PolyGlyphPage.atlas(face, 4, 1f, round), face, 4, 1, 1f, round)!!
            assertEquals(listOf(1, 2, 3, 3), listOf(g.offX, g.offY, g.w, g.h))
            val x0 = (g.u0 * PolyGlyphPage.SIZE).roundToInt()
            val y0 = (g.v0 * PolyGlyphPage.SIZE).roundToInt()
            return (0 until 9).map { g.page.pixels[(y0 + it / 3) * PolyGlyphPage.SIZE + x0 + it % 3] ushr 24 }
        }
        assertEquals(List(9) { 255 }, alphas(false))
        val round = alphas(true)
        assertEquals(listOf(255, 255, 255, 255, 255), listOf(round[1], round[3], round[4], round[5], round[7]))
        assertTrue(round[0] in 100..200)
    }

    @Test
    fun fadeMaskFadesVertexAlpha() {
        val r = Recorder()
        r.saveLayerFade(PolyFade(true, floatArrayOf(10f, 20f), floatArrayOf(1f, 0f)))
        r.rect(0f, 0f, 10f, 30f, -1, 0f)
        r.restore()
        val b = r.done().single()
        val ys = HashSet<Float>()
        for (i in b.colors.indices) {
            val x = b.verts[i * 4]
            val y = b.verts[i * 4 + 1]
            if (x < 0f || x > 10f || y < 0f || y > 30f) continue
            ys += y
            val expected = (255f * (1f - ((y - 10f) / 10f).coerceIn(0f, 1f))).roundToInt()
            assertTrue(abs((b.colors[i] ushr 24) - expected) <= 1, "alpha at y=$y")
        }
        assertTrue(10f in ys && 20f in ys)
    }

    private fun assertWinding(b: Batch) {
        for (q in 0 until b.colors.size / 4) {
            var a = 0f
            for (i in 0 until 4) {
                val p = (q * 4 + i) * 4; val n = (q * 4 + (i + 1) % 4) * 4
                a += b.verts[p] * b.verts[n + 1] - b.verts[n] * b.verts[p + 1]
            }
            assertTrue(a <= 1e-3f, "quad $q wound the wrong way")
        }
    }

    @Test
    fun convexFillPairsTriangles() {
        fun Batch.solidArea(): Float {
            var sum = 0f
            for (q in 0 until colors.size / 4) {
                if ((0 until 4).any { colors[q * 4 + it] ushr 24 != 255 }) continue
                var a = 0f
                for (i in 0 until 4) {
                    val p = (q * 4 + i) * 4; val n = (q * 4 + (i + 1) % 4) * 4
                    a += verts[p] * verts[n + 1] - verts[n] * verts[p + 1]
                }
                sum += -a / 2f
            }
            return sum
        }
        val rect = Recorder().run { rect(0f, 0f, 100f, 50f, -1, 0f); done().single() }
        assertEquals(20, rect.colors.size)
        assertWinding(rect)
        assertEquals(99f * 49f, rect.solidArea(), 0.01f)
        val round = Recorder().run { rect(0f, 0f, 100f, 50f, -1, 10f); done().single() }
        assertWinding(round)
        val r = 9.5f
        assertEquals(99f * 49f - (4f - Math.PI.toFloat()) * r * r, round.solidArea(), 99f * 49f * 0.005f)
    }

    @Test
    fun circleFillNeverOverlapsItself() {
        val b = Recorder().run { rect(0f, 0f, 38f, 38f, 0x80FFFFFF.toInt(), 19f); done().single() }
        fun side(ax: Float, ay: Float, bx: Float, by: Float, px: Float, py: Float) = (bx - ax) * (py - ay) - (by - ay) * (px - ax)
        fun inTri(i: Int, j: Int, k: Int, px: Float, py: Float): Boolean {
            val v = b.verts
            val d0 = side(v[i * 4], v[i * 4 + 1], v[j * 4], v[j * 4 + 1], px, py)
            val d1 = side(v[j * 4], v[j * 4 + 1], v[k * 4], v[k * 4 + 1], px, py)
            val d2 = side(v[k * 4], v[k * 4 + 1], v[i * 4], v[i * 4 + 1], px, py)
            return (d0 > 0f && d1 > 0f && d2 > 0f) || (d0 < 0f && d1 < 0f && d2 < 0f)
        }
        var y = 0.13f
        while (y < 38f) {
            var x = 0.29f
            while (x < 38f) {
                var hits = 0
                for (q in 0 until b.colors.size / 4) {
                    val o = q * 4
                    if (inTri(o, o + 1, o + 2, x, y)) hits++
                    if (inTri(o + 2, o + 3, o, x, y)) hits++
                }
                assertTrue(hits <= 1, "($x, $y) covered $hits times")
                x += 0.5f
            }
            y += 0.5f
        }
    }

    @Test
    fun roundedClipCutsOnlyCorners() {
        fun draw(radius: Float, x: Float, y: Float, w: Float, h: Float) = Recorder().run {
            scale(2f, 2f)
            clipRRect(0f, 0f, 100f, 50f, radius)
            rect(x, y, w, h, -1, 0f)
            done().single()
        }
        val clear = draw(10f, 2f, 20f, 96f, 10f)
        val plain = draw(0f, 2f, 20f, 96f, 10f)
        assertEquals(plain.verts.toList(), clear.verts.toList())
        assertEquals(plain.colors.toList(), clear.colors.toList())

        val shaped = Recorder().run { scale(2f, 2f); clipRRect(0f, 0f, 100f, 50f, 10f); rect(0f, 0f, 100f, 50f, -1, 10f); done().single() }
        val free = Recorder().run { scale(2f, 2f); clipRect(0f, 0f, 100f, 50f); rect(0f, 0f, 100f, 50f, -1, 10f); done().single() }
        assertEquals(free.verts.toList(), shaped.verts.toList())
        val covering = Recorder().run { scale(2f, 2f); clipRRect(0f, 0f, 100f, 50f, 10f); rect(-5f, -5f, 110f, 60f, -1, 4f); done().single() }
        assertEquals(free.verts.toList(), covering.verts.toList())
        val footer = Recorder().run { clipRRect(0f, 0f, 100f, 50f, 10f); rect(0f, 30f, 100f, 20f, -1, 0f); done() }
        footer.forEach(::assertWinding)
        assertEquals(255, footer.alphaAt(50.2f, 40.2f))
        assertEquals(255, footer.alphaAt(4.2f, 40.2f))
        assertEquals(0, footer.alphaAt(1.2f, 48.8f), "outside the rounded corner")
        assertEquals(0, footer.alphaAt(50.2f, 28.2f), "above the rect")
        for (p in 0 until 100) assertTrue(footer.alphaAt(p + 0.37f, 30f + p * 0.19f) <= 256, "overlap at $p")
        assertTrue(footer.sumOf { it.colors.size } < Recorder().run { clipRRect(0f, 0f, 100f, 50f, 10f); clipRect(-1f, 29f, 102f, 30f); rect(-1f, 0f, 102f, 60f, -1, 0f); done() }.sumOf { it.colors.size } + 64)

        val corner = draw(10f, 0f, 0f, 20f, 20f)
        assertWinding(corner)
        assertTrue(corner.colors.size > draw(0f, 0f, 0f, 20f, 20f).colors.size, "corner rect not cut")
        for (i in corner.colors.indices) {
            val dx = 20f - corner.verts[i * 4]; val dy = 20f - corner.verts[i * 4 + 1]
            if (dx > 0f && dy > 0f && corner.colors[i] ushr 24 != 0) assertTrue(hypot(dx, dy) <= 20.6f, "vertex past the corner")
        }
    }

    @Test
    fun scissorBoxCutOnCpu() {
        fun draw(third: Boolean) = Recorder().run {
            save(); clipRect(10f, 10f, 20f, 20f); rect(0f, 0f, 40f, 40f, -1, 0f); restore()
            rect(50f, 50f, 10f, 10f, -1, 0f)
            if (third) { save(); clipRect(0f, 0f, 5f, 5f); rect(50f, 50f, 10f, 10f, -1, 0f); restore() }
            done()
        }
        val b = draw(true).single()
        assertWinding(b)
        assertEquals(draw(false).single().colors.size, b.colors.size)
        for (i in b.colors.indices) {
            val x = b.verts[i * 4]; val y = b.verts[i * 4 + 1]
            assertTrue((x in 10f..30f && y in 10f..30f) || (x >= 49f && y >= 49f), "vertex ($x, $y) outside its scissor box")
        }
    }

    @Test
    fun nestedRoundedClipsIntersect() {
        val r = Recorder()
        r.save()
        r.clipRRect(0f, 0f, 10f, 10f, 4f)
        r.save()
        r.clipRRect(5f, 5f, 20f, 20f, 4f)
        r.rect(0f, 0f, 30f, 30f, -1, 0f)
        r.restore()
        r.restore()
        val b = r.done().single()
        assertWinding(b)
        for (i in 0 until b.colors.size) {
            val x = b.verts[i * 4]; val y = b.verts[i * 4 + 1]
            assertTrue(x in 4.4f..10.6f && y in 4.4f..10.6f, "vertex ($x, $y) outside the intersection")
            val dx = x - 6f; val dy = y - 6f
            if (dx > 0f && dy > 0f && b.colors[i] ushr 24 != 0) assertTrue(hypot(dx, dy) <= 4.6f, "($x, $y) past the outer corner")
        }
    }

    @Test
    fun glyphsOverflowIntoANewPage() {
        val face = object : PolyGlyphPage.Face {
            override fun glyph(codepoint: Int) = codepoint
            override fun advance(glyph: Int) = 1f
            override fun raster(glyph: Int, pixelSize: Float) = PolyGlyphPage.Raster(0, -64, 64, 64, ByteArray(64 * 64) { -1 })
        }
        val old = PolyGlyphPage.rasterizer
        PolyGlyphPage.rasterizer = object : PolyGlyphPage.Rasterizer { override fun face(name: String) = face }
        try {
            val text = String(IntArray(300) { 0x4E00 + it }, 0, 300)
            val r = Recorder()
            r.text(text, 0f, 64f, -1, PolyFont("cjk", 16f))
            val batches = r.done()
            assertEquals(300, batches.sumOf { it.colors.size / 4 })
            val pages = batches.map { it.texture }.distinct()
            assertTrue(pages.size >= 2, "one page held ${batches.size} batches")
            assertTrue(pages.all { it is PolyGlyphPage.Page })
        } finally {
            PolyGlyphPage.rasterizer = old
        }
    }

    @Test
    fun shapesBatchOnTheGlyphPage() {
        val r = Recorder()
        r.scale(2f, 2f)
        r.rect(0f, 0f, 10f, 10f, -1, 3f)
        r.rectStroke(0f, 0f, 10f, 10f, -1, 1f, 3f)
        val b = r.done().single()
        assertEquals(PolyGlyphPage.pages[0], b.texture)
        assertWinding(b)
        val xs = b.verts.filterIndexed { i, _ -> i % 4 == 0 }
        assertTrue(xs.max() in 20.4f..21.6f, "max x ${xs.max()}")
    }

    @Test
    fun linearGradientMapsPositionToTheRamp() {
        val r = Recorder()
        r.rectGradient(0f, 0f, 100f, 10f, PolyGradient.horizontal(0xFFFF0000.toInt(), 0xFF0000FF.toInt()), 0f)
        val b = r.done().single()
        val page = b.texture as PolyGlyphPage.Page
        val s = PolyGlyphPage.SIZE
        val left = b.verts[2] * s - (0.5f + 255f * b.verts[0] / 100f)
        assertEquals(left.roundToInt().toFloat(), left, 1e-2f)
        fun texel(u: Float, v: Float) = page.pixels[(v * s).toInt() * s + (u * s).toInt()]
        assertWinding(b)
        for (v in 0 until b.colors.size) {
            val x = b.verts[v * 4]; val u = b.verts[v * 4 + 2]
            assertEquals((left + 0.5f + 255f * x / 100f) / s, u, 1e-4f)
        }
        val lo = b.colors.indices.minBy { b.verts[it * 4] }
        val hi = b.colors.indices.maxBy { b.verts[it * 4] }
        assertEquals(0xFF0000FF.toInt(), texel(b.verts[lo * 4 + 2], b.verts[lo * 4 + 3]))
        assertEquals(0xFFFF0000.toInt(), texel(b.verts[hi * 4 + 2], b.verts[hi * 4 + 3]))
    }

    @Test
    fun strokeGradientFollowsGradientDirectionUnderTransform() {
        val r = Recorder()
        r.translate(30f, 5f)
        r.scale(2f, 2f)
        r.rectStrokeGradient(0f, 0f, 100f, 10f, PolyGradient.horizontal(0xFFFF0000.toInt(), 0xFF0000FF.toInt()), 1f, 0f)
        val b = r.done().single()
        val solid = b.colors.indices.filter { b.colors[it] ushr 24 == 255 }
        val lo = solid.minBy { b.verts[it * 4] }
        val hi = solid.maxBy { b.verts[it * 4] }
        fun red(c: Int) = (c shr 16) and 0xFF
        fun blue(c: Int) = c and 0xFF
        assertTrue(red(b.colors[lo]) > 240 && blue(b.colors[lo]) < 15, "left %08x".format(b.colors[lo]))
        assertTrue(blue(b.colors[hi]) > 240 && red(b.colors[hi]) < 15, "right %08x".format(b.colors[hi]))
    }

    @Test
    fun shadowIsNineSlicesOfABlurredMask() {
        val r = Recorder()
        r.shadow(10f, 10f, 40f, 60f, 4f, PolyShadow(PolyColor(0x80000000.toInt()), 6f, 0f, 0f, 0f))
        val b = r.done().single()
        assertEquals(9 * 4, b.colors.size)
        assertTrue(b.colors.all { it == 0x80000000.toInt() })
        val page = b.texture as PolyGlyphPage.Page
        val s = PolyGlyphPage.SIZE
        fun a(q: Int, v: Int) = page.pixels[(b.verts[(q * 4 + v) * 4 + 3] * s).toInt() * s + (b.verts[(q * 4 + v) * 4 + 2] * s).toInt()] ushr 24
        assertTrue(a(4, 0) > 250, "centre ${a(4, 0)}")
        assertTrue(a(0, 0) < 3, "corner ${a(0, 0)}")
        val xs = b.verts.filterIndexed { i, _ -> i % 4 == 0 }
        assertEquals(30f, (xs.min() + xs.max()) / 2f, 1e-3f)
        assertTrue(xs.min() < 10f - 6f)
        val small = Recorder().apply { shadow(0f, 0f, 4f, 4f, 2f, PolyShadow(PolyColor(0x80000000.toInt()), 6f, 0f, 4f, 0f)) }.done().single()
        assertEquals(4 * 4, small.colors.size)
    }

    @Test
    fun bigShadowMaskIsDownsampledButKeepsItsExtent() {
        val r = Recorder()
        r.shadow(0f, 0f, 400f, 300f, 20f, PolyShadow(PolyColor(0x80000000.toInt()), 50f, 0f, 0f, 0f))
        val b = r.done().single()
        assertEquals(9 * 4, b.colors.size)
        val page = b.texture as PolyGlyphPage.Page
        val s = PolyGlyphPage.SIZE
        fun a(q: Int, v: Int) = page.pixels[(b.verts[(q * 4 + v) * 4 + 3] * s).toInt() * s + (b.verts[(q * 4 + v) * 4 + 2] * s).toInt()] ushr 24
        assertTrue(a(4, 0) > 250, "centre ${a(4, 0)}")
        assertTrue(a(0, 0) < 3, "corner ${a(0, 0)}")
        val xs = b.verts.filterIndexed { i, _ -> i % 4 == 0 }
        assertEquals(200f, (xs.min() + xs.max()) / 2f, 1e-3f)
        assertTrue(xs.min() in -100f..-80f, "extent ${xs.min()}")
        val us = b.verts.filterIndexed { i, _ -> i % 4 == 2 }
        assertTrue((us.max() - us.min()) * s < 128f, "mask ${(us.max() - us.min()) * s} texels wide")
    }

    @Test
    fun shadowWithHoleSkipsOnlyTheInsideOfTheHole() {
        val sh = PolyShadow(PolyColor(0x80000000.toInt()), 50f, 0f, 0f, 0f)
        val full = Recorder().apply { shadow(-4f, 9f, 408f, 308f, 24f, sh) }.done().single()
        val holed = Recorder().apply { shadowWithHole(-4f, 9f, 408f, 308f, 24f, sh, 0f, 0f, 400f, 300f, 20f) }.done().single()
        fun hits(b: Batch, x: Float, y: Float): Int {
            var n = 0
            for (q in 0 until b.colors.size / 4) {
                val xs = (0 until 4).map { b.verts[(q * 4 + it) * 4] }
                val ys = (0 until 4).map { b.verts[(q * 4 + it) * 4 + 1] }
                if (x > xs.min() && x < xs.max() && y > ys.min() && y < ys.max()) n++
            }
            return n
        }
        for (y in -100..420 step 7) for (x in -100..520 step 7) {
            val px = x + 0.37f; val py = y + 0.41f
            val expected = hits(full, px, py)
            val inside = (0..4).any {
                val a = it * Math.PI.toFloat() / 8f
                val ix = 20f * (1f - kotlin.math.cos(a)) + 0.45f
                val iy = 20f * (1f - kotlin.math.sin(a)) + 0.45f
                px in ix..400f - ix && py in iy..300f - iy
            }
            assertEquals(if (inside) 0 else expected, hits(holed, px, py), "at $px,$py")
        }
    }

    @Test
    fun imageCutByRoundClipKeepsFullCoverageAlongItsEdges() {
        val bmp = PolyBitmap(4, 4, ByteArray(64) { -1 })
        val b = Recorder().apply { save(); clipRRect(0f, 0f, 1000f, 300f, 20f); image(bmp, 0f, 0f, 1000f, 300f); restore() }.done()
        fun alphaAt(px: Float, py: Float): Float {
            for (batch in b) for (q in 0 until batch.colors.size / 4) for (t in arrayOf(intArrayOf(0, 1, 2), intArrayOf(0, 2, 3))) {
                val i = t.map { q * 4 + it }
                val x = i.map { batch.verts[it * 4] }; val y = i.map { batch.verts[it * 4 + 1] }
                val d = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2])
                if (abs(d) < 1e-9f) continue
                val l0 = ((y[1] - y[2]) * (px - x[2]) + (x[2] - x[1]) * (py - y[2])) / d
                val l1 = ((y[2] - y[0]) * (px - x[2]) + (x[0] - x[2]) * (py - y[2])) / d
                val l2 = 1f - l0 - l1
                if (l0 < 0f || l1 < 0f || l2 < 0f) continue
                return (l0 * (batch.colors[i[0]] ushr 24) + l1 * (batch.colors[i[1]] ushr 24) + l2 * (batch.colors[i[2]] ushr 24)) / 255f
            }
            return 0f
        }
        for (x in listOf(100f, 500f, 900f, 970f)) assertEquals(1f, alphaAt(x + 0.5f, 0.5f), 0.02f, "top at $x")
        for (y in listOf(50f, 150f, 250f)) assertEquals(1f, alphaAt(999.5f, y + 0.5f), 0.02f, "right at $y")
    }

    @Test
    fun imageCarriesUvAndTint() {
        val r = Recorder()
        val bmp = PolyBitmap(4, 4, ByteArray(64) { -1 })
        r.saveLayerAlpha(0.5f)
        r.image(bmp, 0f, 0f, 8f, 8f, 0.25f, 0f, 0.75f, 1f, 0xFF00FF00.toInt())
        r.restore()
        r.rect(0f, 0f, 2f, 2f, -1, 0f)
        val b = r.done().single()
        assertTrue(b.texture is PolyGlyphPage.Page)
        val us = (0 until 4).map { b.verts[it * 4 + 2] * PolyGlyphPage.SIZE }.toSet()
        assertEquals(2, us.size)
        assertEquals(2f, us.max() - us.min(), 1e-3f)
        assertTrue(b.colors.take(4).all { it == 0x8000FF00.toInt() })
        val big = PolyBitmap(300, 2, ByteArray(300 * 2 * 4))
        assertEquals(big, Recorder().apply { image(big, 0f, 0f, 8f, 8f, 0f, 0f, 1f, 1f, -1) }.done().single().texture)
    }

    private fun List<Batch>.alphaAt(px: Float, py: Float): Int {
        var sum = 0f
        for (b in this) for (q in 0 until b.colors.size / 4) for (tri in arrayOf(intArrayOf(0, 1, 2), intArrayOf(0, 2, 3))) {
            val i = IntArray(3) { q * 4 + tri[it] }
            val x = FloatArray(3) { b.verts[i[it] * 4] }; val y = FloatArray(3) { b.verts[i[it] * 4 + 1] }
            val d = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2])
            if (abs(d) < 1e-6f) continue
            val l0 = ((y[1] - y[2]) * (px - x[2]) + (x[2] - x[1]) * (py - y[2])) / d
            val l1 = ((y[2] - y[0]) * (px - x[2]) + (x[0] - x[2]) * (py - y[2])) / d
            val l2 = 1f - l0 - l1
            if (l0 < 0f || l1 < 0f || l2 < 0f) continue
            sum += l0 * (b.colors[i[0]] ushr 24) + l1 * (b.colors[i[1]] ushr 24) + l2 * (b.colors[i[2]] ushr 24)
        }
        return sum.roundToInt()
    }

    @Test
    fun strokeOnItsOwnClipEdgeMatchesTheClippedRing() {
        fun draw(grow: Float) = Recorder().apply {
            scale(2f, 2f)
            save(); clipRRect(10f - grow, 10f - grow, 100f + grow * 2f, 60f + grow * 2f, 8f + grow)
            rectStroke(10f, 10f, 100f, 60f, -1, 1.5f, 8f)
            restore()
        }.done()
        val fast = draw(0f)
        val ref = draw(0.002f)
        assertTrue(fast.sumOf { it.colors.size } < ref.sumOf { it.colors.size })
        for (y in 16..24) for (x in 16..240) {
            val px = x + 0.37f; val py = y + 0.41f
            assertEquals(ref.alphaAt(px, py).toFloat(), fast.alphaAt(px, py).toFloat(), 3f, "at $px,$py")
        }
        for (y in 18..140 step 7) for (x in 18..24) assertEquals(ref.alphaAt(x + 0.37f, y + 0.41f).toFloat(), fast.alphaAt(x + 0.37f, y + 0.41f).toFloat(), 3f, "at $x,$y")
    }

    @Test
    fun imageOnAFractionalClipEdgeKeepsTheOuterRampLikeAFill() {
        val bmp = PolyBitmap(4, 4, ByteArray(64) { -1 })
        fun draw(image: Boolean) = Recorder().apply {
            save(); clipRRect(10.7f, 10.7f, 99.6f, 59.6f, 8f)
            if (image) image(bmp, 10.7f, 10.7f, 99.6f, 59.6f) else rect(10.7f, 10.7f, 99.6f, 59.6f, -1, 8f)
            restore()
        }.done()
        val img = draw(true)
        val fill = draw(false)
        for ((x, y) in listOf(10.5f to 40.37f, 110.5f to 40.37f, 60.37f to 10.5f, 60.37f to 70.5f, 9.5f to 40.37f, 111.5f to 40.37f, 60.37f to 9.5f, 60.37f to 71.5f)) {
            assertEquals(fill.alphaAt(x, y).toFloat(), img.alphaAt(x, y).toFloat(), 3f, "at $x,$y")
        }
        for ((x, y) in listOf(10.5f to 40.37f, 110.5f to 40.37f, 60.37f to 10.5f, 60.37f to 70.5f)) assertTrue(img.alphaAt(x, y) > 40, "at $x,$y")
    }

    @Test
    fun rectWithHoleLeavesARoundedHole() {
        val b = Recorder().run { rectWithHole(0f, 0f, 100f, 100f, 30f, 30f, 40f, 40f, 8f, -1); done() }
        b.forEach(::assertWinding)
        assertEquals(255, b.alphaAt(10.5f, 10.5f))
        assertEquals(255, b.alphaAt(31.2f, 31.2f), "rounded corner of the hole")
        assertEquals(255, b.alphaAt(68.8f, 68.8f), "rounded corner of the hole")
        assertEquals(0, b.alphaAt(50.2f, 50.2f))
        assertEquals(0, b.alphaAt(45.2f, 31.2f))
        for (p in 0 until 100) assertTrue(b.alphaAt(p + 0.37f, 20f + p * 0.61f) <= 256, "overlap at $p")
        val past = Recorder().run { rectWithHole(0f, 0f, 100f, 100f, 80f, 40f, 40f, 20f, 8f, -1); done() }
        assertEquals(0, past.alphaAt(95.2f, 50.2f))
        assertEquals(255, past.alphaAt(50.2f, 50.2f))
        assertTrue(past.all { b -> (0 until b.colors.size).all { b.verts[it * 4] <= 100.01f } }, "drawn past the clip")
    }

    @Test
    fun mcOutlineCoversEachPixelOnce() {
        val face = object : PolyGlyphPage.Face {
            override fun glyph(codepoint: Int) = 1
            override fun advance(glyph: Int) = 0.2f
            override fun raster(glyph: Int, pixelSize: Float) = PolyGlyphPage.Raster(0, -4, 2, 4, ByteArray(8) { -1 })
        }
        val old = PolyGlyphPage.rasterizer
        PolyGlyphPage.rasterizer = object : PolyGlyphPage.Rasterizer { override fun face(name: String) = face.takeIf { name == "minecraft" } }
        try {
            val b = Recorder().run { mcTextOutline("ab", 10f, 20f, -1, 0x40000000, 1f); done() }
            b.forEach(::assertWinding)
            var covered = 0
            for (py in 0 until 40) for (px in 0 until 40) {
                val a = b.alphaAt(px + 0.5f, py + 0.5f)
                assertTrue(a == 0 || a == 0x40, "alpha $a at ($px, $py)")
                if (a > 0) covered++
            }
            assertEquals(5 * 4, covered)
            assertEquals(0x40, b.alphaAt(9.5f, 24.5f))
            assertEquals(0, b.alphaAt(14.5f, 24.5f))
        } finally {
            PolyGlyphPage.rasterizer = old
        }
    }

    @Test
    fun nearestImagesBatchApart() {
        val bmp = PolyBitmap(4, 4, ByteArray(64) { -1 })
        val b = Recorder().run {
            image(bmp, 0f, 0f, 8f, 8f, FilterQuality.None)
            image(bmp, 8f, 0f, 8f, 8f)
            done()
        }
        assertEquals(listOf(true, false), b.map { it.nearest })
        assertEquals(b[0].texture, b[1].texture)
    }

    private fun Batch.solidArea(): Float {
        var sum = 0f
        for (q in 0 until colors.size / 4) {
            if ((0 until 4).any { colors[q * 4 + it] ushr 24 != 255 }) continue
            var a = 0f
            for (i in 0 until 4) {
                val p = (q * 4 + i) * 4; val n = (q * 4 + (i + 1) % 4) * 4
                a += verts[p] * verts[n + 1] - verts[n] * verts[p + 1]
            }
            sum += -a / 2f
        }
        return sum
    }

    private fun List<Batch>.bounds(): FloatArray {
        val out = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (b in this) for (v in 0 until b.verts.size / 4) {
            val x = b.verts[v * 4]; val y = b.verts[v * 4 + 1]
            out[0] = minOf(out[0], x); out[1] = minOf(out[1], y); out[2] = maxOf(out[2], x); out[3] = maxOf(out[3], y)
        }
        return out
    }

    @Test
    fun polylineStrokesMiterAndCap() {
        val l = Recorder().run { strokePolyline(floatArrayOf(0f, 0f, 100f, 0f, 100f, 50f), false, -1, 10f, 0); done().single() }
        assertWinding(l)
        assertTrue(l.solidArea() in 1300f..1500f, "area ${l.solidArea()}")
        assertEquals(105.5f, listOf(l).bounds()[2], 0.01f)
        assertEquals(-5.5f, listOf(l).bounds()[1], 0.01f)

        fun capStart(cap: Int) = Recorder().run { strokePolyline(floatArrayOf(10f, 10f, 50f, 10f), false, -1, 10f, cap); done() }.bounds()[0]
        assertEquals(9.5f, capStart(0), 0.01f)
        assertEquals(4.5f, capStart(1), 0.2f)
        assertEquals(4.5f, capStart(2), 0.01f)
    }

    @Test
    fun closedPolylineStrokesARing() {
        val ring = Recorder().run { strokePolyline(floatArrayOf(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f), true, -1, 10f, 0); done().single() }
        assertWinding(ring)
        assertTrue(ring.solidArea() in 3600f..4000f, "area ${ring.solidArea()}")
        assertEquals(-5.5f, listOf(ring).bounds()[0], 0.01f)
    }
}
