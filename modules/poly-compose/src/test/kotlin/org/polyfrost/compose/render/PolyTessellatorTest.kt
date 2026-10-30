package org.polyfrost.compose.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.backend.PolyCanvas
import org.polyfrost.compose.backend.PolyCompose
import org.polyfrost.compose.backend.PolyPaint

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

    private class Group(val items: List<Any>, val alpha: Float)

    private class Layered : PolyTessellator() {
        val ops = ArrayList<Any>()
        override fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {
            ops.add(Batch(texture, verts, colors, nearest))
        }
        override fun bitmapTexture(bitmap: PolyBitmap) = bitmap
        override fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) = Unit
        override fun live(draw: (PolyRenderer) -> Unit) = draw(this)
        override val layered get() = true
        override fun layerMark(): Any = ops.size
        override fun layer(mark: Any, alpha: Float) {
            val sub = ops.subList(mark as Int, ops.size)
            val g = Group(ArrayList(sub), alpha)
            sub.clear()
            ops.add(g)
        }
        var fades = 0
        override fun fade(mark: Any, alpha: Float): Boolean {
            fades++
            for (i in mark as Int until ops.size) {
                val b = ops[i] as? Batch ?: return false
                ops[i] = Batch(b.texture, b.verts, IntArray(b.colors.size) { mulAlpha(b.colors[it], alpha) }, b.nearest)
            }
            return true
        }
        fun done() = also { flush() }.ops
    }

    @Test
    fun nonOverlappingDrawsFadeWithoutLayer() {
        val r = Layered()
        r.saveLayerAlpha(0.5f)
        r.rect(0f, 0f, 4f, 4f, -1, 0f)
        r.rect(10f, 0f, 4f, 4f, -1, 0f)
        r.restore()
        val b = r.done().single() as Batch
        assertEquals(128, b.colors.maxOf { it ushr 24 })
    }

    @Test
    fun nonOverlappingDrawsAcrossFlushFadeInBackend() {
        val r = Layered()
        val bmp = PolyBitmap(300, 2, ByteArray(300 * 2 * 4) { -1 })
        r.saveLayerAlpha(0.5f)
        r.rect(0f, 0f, 4f, 4f, -1, 0f)
        r.image(bmp, 10f, 0f, 4f, 4f, 0f, 0f, 1f, 1f, -1)
        r.restore()
        val ops = r.done()
        assertEquals(1, r.fades)
        assertEquals(2, ops.size)
        assertTrue(ops.all { it is Batch && it.colors.maxOf { c -> c ushr 24 } == 128 })
    }

    @Test
    fun nearestImageUvsSitOffTexelTies() {
        val bmp = PolyBitmap(16, 16, ByteArray(16 * 16 * 4) { -1 })
        fun firstVertex(q: FilterQuality) = Recorder().run { image(bmp, 3f, 5f, 52f, 52f, 0f, 0f, 1f, 1f, -1, q); done() }.single().verts
        val linear = firstVertex(FilterQuality.Low)
        val nearest = firstVertex(FilterQuality.None)
        val texelU = abs(linear[10] - linear[2]) / 16f
        val du = nearest[2] - linear[2]
        assertTrue(du > 0f && du < texelU / 64f, "u bias $du for texel $texelU")
        assertTrue(nearest[3] - linear[3] > 0f)
        assertEquals(linear[0], nearest[0])
    }

    @Test
    fun layerTargetsGrowInBuckets() {
        assertEquals(128, PolyTessellator.layerTargetSize(0, 100))
        assertEquals(128, PolyTessellator.layerTargetSize(128, 110))
        assertEquals(128, PolyTessellator.layerTargetSize(128, 128))
        assertEquals(192, PolyTessellator.layerTargetSize(128, 130))
    }

    @Test
    fun singleDrawLayerMultipliesInPlace() {
        val r = Layered()
        r.rect(20f, 0f, 4f, 4f, -1, 0f)
        r.saveLayerAlpha(0.5f)
        r.rect(0f, 0f, 4f, 4f, -1, 0f)
        r.restore()
        val b = r.done().single() as Batch
        val ref = (Layered().apply { rect(0f, 0f, 4f, 4f, -1, 0f) }.done().single() as Batch).colors
        val alphas = b.colors.map { it ushr 24 }
        assertEquals(ref.size * 2, alphas.size)
        assertEquals(ref.map { it ushr 24 }, alphas.take(ref.size))
        assertEquals(ref.map { ((it ushr 24) * 0.5f).roundToInt() }, alphas.drop(ref.size))
    }

    @Test
    fun overlappingDrawsCompositeAsGroup() {
        val r = Layered()
        r.rect(20f, 0f, 4f, 4f, -1, 0f)
        r.saveLayerAlpha(0.5f)
        r.rect(0f, 0f, 4f, 4f, -1, 0f)
        r.rect(2f, 0f, 4f, 4f, -1, 0f)
        r.restore()
        r.rect(40f, 0f, 4f, 4f, -1, 0f)
        val ops = r.done()
        assertEquals(3, ops.size)
        val g = ops[1] as Group
        assertEquals(0.5f, g.alpha)
        val inner = g.items.single() as Batch
        assertEquals(255, inner.colors.maxOf { it ushr 24 })
        assertTrue((ops[0] as Batch).verts[0] >= 20f)
        assertTrue((ops[2] as Batch).verts[0] >= 40f)
    }

    @Test
    fun nestedLayersSplitAtFlush() {
        val r = Layered()
        r.rect(20f, 0f, 4f, 4f, -1, 0f)
        r.saveLayerAlpha(0.5f)
        r.rect(0f, 0f, 4f, 4f, -1, 0f)
        r.saveLayerAlpha(0.5f)
        r.rect(0f, 0f, 4f, 4f, -1, 0f)
        r.rect(1f, 0f, 4f, 4f, -1, 0f)
        r.restore()
        r.restore()
        val ops = r.done()
        assertEquals(2, ops.size)
        val outer = ops[1] as Group
        assertEquals(0.5f, outer.alpha)
        assertEquals(2, outer.items.size)
        assertEquals(255, (outer.items[0] as Batch).colors.maxOf { it ushr 24 })
        assertEquals(0.5f, (outer.items[1] as Group).alpha)
    }

    @Test
    fun deviceTransformTracksScaleAndRotation() {
        val r = Recorder()
        r.translate(3f, 4f)
        r.scale(2f, 3f)
        r.translate(1f, 1f)
        assertTrue(floatArrayOf(2f, 3f, 5f, 7f).contentEquals(r.deviceTransform()))
        r.rotate(90f)
        assertEquals(null, r.deviceTransform())
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

    @Test
    fun multiStopGradientStrokeKeepsMiddleStop() {
        val r = Recorder()
        val g = PolyGradient.Points(listOf(PolyColor(0xFFFFFFFF.toInt()), PolyColor(0x00FFFFFF), PolyColor(0xFFFFFFFF.toInt())), listOf(0f, 0.5f, 1f), 0f, 0f, 100f, 0f)
        r.rectStrokeGradient(0f, 0f, 100f, 20f, g, 2f, 4f)
        val b = r.done().single()
        val mid = b.colors.indices.filter { abs(b.verts[it * 4] - 50f) < 3f }.minOf { b.colors[it] ushr 24 }
        assertTrue(mid < 30, "middle alpha $mid")
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
    fun clipOutRectLeavesASoftEdgedHoleUntilRestored() {
        val b = Recorder().run {
            save(); scale(2f, 2f); clipOutRect(5f, 5f, 10f, 10f); rect(0f, 0f, 20f, 20f, -1, 0f); restore()
            rect(50f, 0f, 10f, 10f, -1, 0f)
            done()
        }
        b.forEach(::assertWinding)
        assertEquals(255, b.alphaAt(5.3f, 22.9f))
        assertEquals(255, b.alphaAt(20.7f, 9.4f), "just before the hole's soft edge")
        assertEquals(0, b.alphaAt(20.7f, 20.3f), "inside the hole")
        for (y in listOf(11.9f, 21.9f, 28.9f)) assertTrue(b.alphaAt(10.25f, y) in 60..68, "a quarter covered across the edge at $y")
        for (y in listOf(11.9f, 21.9f, 28.9f)) assertTrue(b.alphaAt(29.75f, y) in 60..68, "a quarter covered across the far edge at $y")
        assertEquals(255, b.alphaAt(55.3f, 5.7f), "restore keeps the hole from outliving its scope")
    }

    @Test
    fun polygonClipFollowsConvexOutlines() {
        val octagon = floatArrayOf(10f, 0f, 30f, 0f, 40f, 10f, 40f, 30f, 30f, 40f, 10f, 40f, 0f, 30f, 0f, 10f, 10f, 0f)
        fun draw(points: FloatArray, radius: Float = 0f) = Recorder().run { save(); clipPolygon(points); rect(0f, 0f, 40f, 40f, -1, radius); restore(); done() }
        val cut = draw(octagon)
        cut.forEach(::assertWinding)
        assertEquals(255, cut.alphaAt(20.3f, 21.7f))
        assertEquals(255, cut.alphaAt(12.3f, 1.9f))
        assertEquals(0, cut.alphaAt(2.3f, 2.9f), "outside the cut corner")
        assertEquals(0, draw(octagon, 4f).alphaAt(3.3f, 3.9f), "a rounded rect inside the outline's bounds is still cut")
        val wide = Recorder().run { save(); clipPolygon(octagon); rect(-10f, -10f, 60f, 60f, -1, 5f); restore(); done() }
        wide.forEach(::assertWinding)
        for ((y, alpha) in listOf(0.1f to 153, 0.25f to 191)) assertTrue(wide.alphaAt(20.3f, y) in alpha - 6..alpha + 6, "fading across the edge at $y")
        val notch = floatArrayOf(0f, 0f, 40f, 0f, 40f, 40f, 20f, 20f, 0f, 40f)
        assertEquals(255, draw(notch).alphaAt(20.3f, 35.9f), "concave outlines clip to their bounds")

        val r = Recorder()
        r.save(); r.clipPolygon(octagon)
        val key = r.stateKey()
        r.restore(); r.clipPolygon(floatArrayOf(20f, 0f, 40f, 20f, 20f, 40f, 0f, 20f))
        assertFalse(r.sameState(key), "cached layers must not outlive a change of shape within the same bounds")
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
    fun gradientPathsSampleTheRampAndBatchWithShapes() {
        PolyCompose.install()
        val r = Recorder()
        val canvas = PolyCanvas(r)
        val gradient = PolyPaint().apply { shader = LinearGradientShader(Offset.Zero, Offset(100f, 0f), listOf(Color.Red, Color.Blue)) }
        canvas.drawPath(Path().apply { moveTo(0f, 0f); lineTo(100f, 0f); lineTo(0f, 10f); close() }, gradient)
        canvas.drawPath(Path().apply { moveTo(10f, 20f); lineTo(90f, 20f) }, gradient.copy().apply { style = PaintingStyle.Stroke; strokeWidth = 2f })
        canvas.drawPath(Path().apply { moveTo(0f, 30f); lineTo(10f, 30f); lineTo(0f, 40f); close() }, PolyPaint())
        val b = r.done().single()
        val page = b.texture as PolyGlyphPage.Page
        val s = PolyGlyphPage.SIZE
        var shaded = 0
        for (i in b.colors.indices) {
            val texel = page.pixels[(b.verts[i * 4 + 3] * s).toInt() * s + (b.verts[i * 4 + 2] * s).toInt()]
            val argb = (texel and -0x1000000) or (texel and 0xFF shl 16) or (texel and 0xFF00) or (texel ushr 16 and 0xFF)
            if (b.verts[i * 4 + 1] > 25f) {
                assertEquals(-1, argb, "solid fills sample white")
            } else if (b.colors[i] ushr 24 == 255) {
                shaded++
                val expected = PolyGradient.lerpArgb(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), b.verts[i * 4] / 100f)
                for (shift in intArrayOf(0, 8, 16)) {
                    assertTrue(abs((expected ushr shift and 0xFF) - (argb ushr shift and 0xFF)) <= 3, "%08x at x=%f".format(argb, b.verts[i * 4]))
                }
            }
        }
        assertTrue(shaded > 0)
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
                val ix = 20f * (1f - cos(a)) + 0.45f
                val iy = 20f * (1f - sin(a)) + 0.45f
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
    fun skewedTextShearsAboutBaseline() {
        val face = object : PolyGlyphPage.Face {
            override fun glyph(codepoint: Int) = 1
            override fun advance(glyph: Int) = 1f
            override fun raster(glyph: Int, pixelSize: Float) = PolyGlyphPage.Raster(0, -4, 2, 4, ByteArray(8) { -1 })
        }
        val old = PolyGlyphPage.rasterizer
        PolyGlyphPage.rasterizer = object : PolyGlyphPage.Rasterizer { override fun face(name: String) = face }
        try {
            val b = Recorder().run { text("a", 10f, 20f, -1, PolyFont("skew", 4f), 0f, 0.5f); done() }.single()
            val corners = (0 until b.colors.size).map { b.verts[it * 4] to b.verts[it * 4 + 1] }.toSet()
            assertEquals(setOf(12f to 16f, 14f to 16f, 10f to 20f, 12f to 20f), corners)
        } finally {
            PolyGlyphPage.rasterizer = old
        }
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
    fun mcOutlineMergesGlyphsOfAdjacentLinesOnce() {
        // 16 outline cells tall, so the grown glyphs of the two lines share two rows
        val face = object : PolyGlyphPage.Face {
            override fun glyph(codepoint: Int) = if (codepoint == ' '.code) 2 else 1
            override fun advance(glyph: Int) = 0.2f
            override fun raster(glyph: Int, pixelSize: Float) =
                if (glyph == 2) null else PolyGlyphPage.Raster(0, -13, 2, 16, ByteArray(32) { -1 })
        }
        val old = PolyGlyphPage.rasterizer
        PolyGlyphPage.rasterizer = object : PolyGlyphPage.Rasterizer { override fun face(name: String) = face.takeIf { name == "minecraft" } }
        try {
            // at scale 2 a device pixel is one outline cell: the glyphs grow to 6x20 cells at (-2, -1) and (2, 17)
            val b = Recorder().run { mcTextOutline("a\n a", 10f, 20f, -1, 0x40000000, 2f); done() }
            b.forEach(::assertWinding)
            var covered = 0
            for (py in 0 until 80) for (px in 0 until 40) {
                val a = b.alphaAt(px + 0.37f, py + 0.41f)
                assertTrue(a == 0 || a == 0x40, "alpha $a at ($px, $py)")
                if (a > 0) covered++
            }
            assertEquals(2 * 6 * 20 - 2 * 2, covered)
            assertEquals(0x40, b.alphaAt(13.37f, 38.41f))
            assertEquals(0, b.alphaAt(11.37f, 40.41f))
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

    @Test
    fun pathHolesStayEmpty() {
        fun draw(evenOdd: Boolean, reverseHole: Boolean) = Recorder().run {
            val p = PolyPath()
            p.evenOdd = evenOdd
            p.moveTo(0f, 0f); p.lineTo(100f, 0f); p.lineTo(100f, 100f); p.lineTo(0f, 100f); p.close()
            if (reverseHole) { p.moveTo(30f, 30f); p.lineTo(30f, 70f); p.lineTo(70f, 70f); p.lineTo(70f, 30f) }
            else { p.moveTo(30f, 30f); p.lineTo(70f, 30f); p.lineTo(70f, 70f); p.lineTo(30f, 70f) }
            p.close()
            fillPath(p, -1)
            done()
        }
        for (b in listOf(draw(false, true), draw(true, false))) {
            b.forEach(::assertWinding)
            assertEquals(0, b.alphaAt(50.2f, 50.3f))
            assertEquals(255, b.alphaAt(10.2f, 10.3f))
            assertEquals(255, b.alphaAt(85.2f, 50.3f))
            assertEquals(255, b.alphaAt(50.2f, 20.3f))
        }
        assertEquals(255, draw(false, false).alphaAt(50.2f, 50.3f))
    }

    @Test
    fun rotationTurnsVerticesAroundThePivot() {
        val b = Recorder().run {
            translate(50f, 50f); rotate(90f); translate(-50f, -50f)
            rect(60f, 40f, 20f, 20f, -1, 0f)
            done()
        }
        assertEquals(255, b.alphaAt(50.2f, 70.3f))
        assertEquals(0, b.alphaAt(70.2f, 50.3f))
        val box = b.bounds()
        assertEquals(39.5f, box[0], 0.01f)
        assertEquals(59.5f, box[1], 0.01f)
    }

    @Test
    fun roundJoinRoundsTheOuterCorner() {
        val b = Recorder().run { strokePolyline(floatArrayOf(0f, 0f, 100f, 0f, 100f, 50f), false, -1, 10f, 0, PolyRenderer.JOIN_ROUND); done() }
        b.forEach(::assertWinding)
        assertEquals(0, b.alphaAt(104.6f, -4.6f))
        assertEquals(255, b.alphaAt(102.9f, -2.9f))
        assertEquals(255, b.alphaAt(97.1f, 2.9f))
        for (p in 0 until 40) assertTrue(b.alphaAt(90.37f + p * 0.37f, -4.9f + p * 0.3f) <= 256, "overlap at $p")
    }

    @Test
    fun dashedLineLeavesGaps() {
        PolyCompose.install()
        val r = Recorder()
        val paint = PolyPaint().apply {
            strokeWidth = 4f
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        PolyCanvas(r).drawLine(Offset(0f, 10f), Offset(100f, 10f), paint)
        val b = r.done()
        assertEquals(255, b.alphaAt(5.2f, 10.3f))
        assertEquals(0, b.alphaAt(15.2f, 10.3f))
        assertEquals(255, b.alphaAt(25.2f, 10.3f))
        assertEquals(0, b.alphaAt(95.2f, 10.3f))
    }

    @Test
    fun tintMaskMultipliesVertexColour() {
        val r = Recorder()
        r.saveLayerFade(PolyFade(false, floatArrayOf(0f, 100f), floatArrayOf(1f, 1f), intArrayOf(0xFF0000, 0x0000FF)))
        r.rect(0f, 0f, 100f, 10f, 0xFF808080.toInt(), 0f)
        r.restore()
        val b = r.done().single()
        for (i in b.colors.indices) {
            val x = b.verts[i * 4]
            if (x !in 0f..100f || b.colors[i] ushr 24 != 255) continue
            val c = b.colors[i]
            assertEquals(0, (c shr 8) and 0xFF)
            assertEquals((128 * (1f - x / 100f)).roundToInt().toFloat(), ((c shr 16) and 0xFF).toFloat(), 1.5f, "red at $x")
        }
    }

    private fun PolyPath.loop(vararg pts: Float) {
        moveTo(pts[0], pts[1])
        for (k in 2 until pts.size step 2) lineTo(pts[k], pts[k + 1])
        close()
    }

    @Test
    fun severalHolesBridgeWithoutCuttingEachOther() {
        fun draw(evenOdd: Boolean, holes: List<FloatArray>) = Recorder().run {
            val p = PolyPath()
            p.evenOdd = evenOdd
            p.loop(0f, 0f, 100f, 0f, 100f, 100f, 0f, 100f)
            for (h in holes) if (evenOdd) p.loop(h[0], h[1], h[2], h[1], h[2], h[3], h[0], h[3]) else p.loop(h[0], h[1], h[0], h[3], h[2], h[3], h[2], h[1])
            fillPath(p, -1)
            done()
        }
        val two = listOf(floatArrayOf(10f, 10f, 40f, 40f), floatArrayOf(60f, 60f, 90f, 90f))
        val three = two + listOf(floatArrayOf(60f, 10f, 90f, 40f))
        for (eo in listOf(false, true)) for (holes in listOf(two, three)) {
            val b = draw(eo, holes)
            for (h in holes) assertEquals(0, b.alphaAt((h[0] + h[2]) / 2f + 0.2f, (h[1] + h[3]) / 2f + 0.3f), "hole $eo ${holes.size}")
            val solid = mutableListOf(50.2f to 50.3f, 95.2f to 50.3f, 25.2f to 75.3f, 5.2f to 5.3f, 50.2f to 95.3f, 75.2f to 50.3f)
            if (holes.size == 2) solid += 75.2f to 25.3f
            for ((x, y) in solid) assertEquals(255, b.alphaAt(x, y), "solid at $x,$y $eo ${holes.size}")
            for (q in 0 until 200) assertTrue(b.alphaAt(q * 0.5f + 0.13f, q * 0.37f + 7.1f) <= 256, "overlap at $q")
        }
    }

    @Test
    fun overlappingContoursFillIndependently() {
        for (eo in listOf(false, true)) {
            val b = Recorder().run {
                val p = PolyPath()
                p.evenOdd = eo
                p.loop(0f, 0f, 40f, 0f, 40f, 40f, 0f, 40f)
                p.loop(20f, 20f, 35f, 70f, 5f, 70f)
                fillPath(p, -1)
                done()
            }
            assertEquals(255, b.alphaAt(20.2f, 60.3f))
            assertEquals(255, b.alphaAt(10.2f, 10.3f))
        }
    }

    @Test
    fun zeroLengthDashesDrawDots() {
        PolyCompose.install()
        fun draw(cap: StrokeCap) = Recorder().also { r ->
            val paint = PolyPaint().apply {
                strokeWidth = 4f
                strokeCap = cap
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0f, 10f), 0f)
            }
            PolyCanvas(r).drawLine(Offset(0f, 10f), Offset(100f, 10f), paint)
        }.done()
        val round = draw(StrokeCap.Round)
        for (x in listOf(0.2f, 10.2f, 50.2f, 90.2f)) assertEquals(255, round.alphaAt(x, 10.3f), "dot at $x")
        assertEquals(0, round.alphaAt(5.2f, 10.3f))
        assertEquals(0, round.alphaAt(11.9f, 11.8f))
        val square = draw(StrokeCap.Square)
        assertEquals(255, square.alphaAt(11.4f, 11.2f))
        assertTrue(draw(StrokeCap.Butt).isEmpty())
    }

    @Test
    fun roundJoinCapsAFullReversal() {
        val b = Recorder().run { strokePolyline(floatArrayOf(0f, 0f, 50f, 0f, 20f, 0f), false, -1, 10f, 0, PolyRenderer.JOIN_ROUND); done() }
        assertTrue(b.alphaAt(53.2f, 0.3f) > 0)
        assertEquals(0, b.alphaAt(56.2f, 0.3f))
    }
}
