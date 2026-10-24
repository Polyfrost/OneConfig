package org.polyfrost.oneconfig.api.notifications.v1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.layout.offsetX
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyGlyphPage
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyTessellator
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.dropShadow
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.graphics.RectangleShape
import org.polyfrost.compose.ui.graphics.shadow.Shadow
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import kotlin.math.roundToInt

class PolyTessellatorTest {
    private class Batch(val texture: Any, val verts: FloatArray, val colors: IntArray)

    private class Recorder : PolyTessellator() {
        val batches = ArrayList<Batch>()
        override fun submit(texture: Any, premultiplied: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {
            batches.add(Batch(texture, verts, colors))
        }
        override fun bitmapTexture(bitmap: ImageBitmap) = bitmap
        override fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) = Unit
        var lives = 0
        override fun live(draw: (PolyRenderer) -> Unit) { lives++; draw(this) }
        val rectYs = ArrayList<Float>()
        override fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) { rectYs += y; super.rect(x, y, w, h, color, radius) }
        fun done() = also { flush() }.batches
    }

    @Test
    fun scrollSkipsChildrenOutsideViewport() {
        val scroll = org.polyfrost.compose.foundation.ScrollState()
        val list = org.polyfrost.compose.node.PolyNode()
        list.style.layoutType = org.polyfrost.compose.layout.LayoutType.Column
        org.polyfrost.compose.ui.Modifier.size(100.dp, 100.dp).verticalScroll(scroll).applyTo(list.style)
        repeat(4) { i ->
            val item = org.polyfrost.compose.node.PolyNode()
            var m = org.polyfrost.compose.ui.Modifier.size(100.dp, 100.dp).background(PolyColor(-1))
            if (i == 2) m = m.dropShadow(RectangleShape, Shadow(radius = 8.dp, color = PolyColor(0x80000000.toInt()), offset = DpOffset(0.dp, 4.dp)))
            m.applyTo(item.style)
            item.parent = list
            list.children += item
        }
        org.polyfrost.compose.layout.PolyLayoutEngine.layout(list, 100f, 100f)
        scroll.scrollTo(100f)
        org.polyfrost.compose.layout.PolyLayoutEngine.layout(list, 100f, 100f)
        val r = Recorder().also { list.render(org.polyfrost.compose.render.RenderContext(it)) }
        assertEquals(listOf(0f, 100f), r.rectYs)
    }

    @Test
    fun animatedAlphaDrawsLiveOnlyInFlight() {
        val alpha = androidx.compose.runtime.mutableFloatStateOf(0.3f)
        val node = org.polyfrost.compose.node.PolyNode()
        org.polyfrost.compose.ui.Modifier.background(PolyColor(-1)).alpha(alpha, 0.6f).applyTo(node.style)
        node.width = 10f; node.height = 10f
        fun draw() = Recorder().also { node.renderFaded(org.polyfrost.compose.render.RenderContext(it)) }
        val flying = draw()
        assertEquals(1, flying.lives)
        assertTrue(flying.done().first().colors[0] ushr 24 < 128)
        alpha.floatValue = 0.6f
        val settled = draw()
        assertEquals(0, settled.lives)
        assertTrue(settled.done().first().colors[0] ushr 24 > 128)
    }

    @Test
    fun animatedOffsetDrawsLiveOnlyInFlight() {
        val x = androidx.compose.runtime.mutableFloatStateOf(3f)
        val node = org.polyfrost.compose.node.PolyNode()
        org.polyfrost.compose.ui.Modifier.background(PolyColor(-1)).offsetX(x, 24f).applyTo(node.style)
        node.x = 24f; node.width = 10f; node.height = 10f
        fun draw() = Recorder().also { node.renderFaded(org.polyfrost.compose.render.RenderContext(it)) }
        fun Batch.minX() = (0 until colors.size).minOf { verts[it * 4] }
        val flying = draw()
        assertEquals(1, flying.lives)
        assertEquals(2.5f, flying.done().first().minX(), 0.01f)
        x.floatValue = 24f
        val settled = draw()
        assertEquals(0, settled.lives)
        assertEquals(23.5f, settled.done().first().minX(), 0.01f)
    }

    @Test
    fun animatedBackgroundDrawsLiveOnlyInFlight() {
        val target = PolyColor(0xFF0000FF.toInt())
        val color = androidx.compose.runtime.mutableStateOf(PolyColor(0xFFFF0000.toInt()))
        val node = org.polyfrost.compose.node.PolyNode()
        node.style.background = target
        node.style.animatedBackground = color
        node.width = 10f; node.height = 10f
        fun draw() = Recorder().also { node.renderFaded(org.polyfrost.compose.render.RenderContext(it)) }
        val flying = draw()
        assertEquals(1, flying.lives)
        assertEquals(0xFFFF0000.toInt(), flying.done().first().colors[0])
        color.value = PolyColor(0xFF0000FF.toInt())
        val settled = draw()
        assertEquals(0, settled.lives)
        assertEquals(0xFF0000FF.toInt(), settled.done().first().colors[0])
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

        val corner = draw(10f, 0f, 0f, 20f, 20f)
        assertWinding(corner)
        assertTrue(corner.colors.size > draw(0f, 0f, 0f, 20f, 20f).colors.size, "corner rect not cut")
        for (i in corner.colors.indices) {
            val dx = 20f - corner.verts[i * 4]; val dy = 20f - corner.verts[i * 4 + 1]
            if (dx > 0f && dy > 0f && corner.colors[i] ushr 24 != 0) assertTrue(kotlin.math.hypot(dx, dy) <= 20.6f, "vertex past the corner")
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
            if (dx > 0f && dy > 0f && b.colors[i] ushr 24 != 0) assertTrue(kotlin.math.hypot(dx, dy) <= 4.6f, "($x, $y) past the outer corner")
        }
    }

    @Test
    fun fadeMaskCutsAndFades() {
        val r = Recorder()
        r.translate(0f, 10f)
        r.saveLayerFade(org.polyfrost.compose.render.PolyFade(true, floatArrayOf(5f, 15f), floatArrayOf(1f, 0f)))
        r.rect(0f, 0f, 10f, 20f, -1, 0f)
        r.rect(0f, 0f, 10f, 4f, -1, 0f)
        r.restore()
        val b = r.done().single()
        assertWinding(b)
        for (i in 0 until b.colors.size) {
            val x = b.verts[i * 4]
            val y = b.verts[i * 4 + 1]
            if (x !in 0f..10f || y !in 10f..30f) continue
            val a = (b.colors[i] ushr 24) / 255f
            val expected = ((25f - y) / 10f).coerceIn(0f, 1f)
            assertEquals(expected, a, 0.01f, "alpha at ($x, $y)")
        }
        val ys = (0 until b.colors.size).map { b.verts[it * 4 + 1] }.toSet()
        assertTrue(15f in ys && 25f in ys, "cut at both stops")
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
            r.text(text, 0f, 64f, -1, org.polyfrost.compose.render.PolyFont("cjk", 16f))
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
        r.rectGradient(0f, 0f, 100f, 10f, Brush.horizontalGradient(0xFFFF0000.toInt(), 0xFF0000FF.toInt()), 0f)
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
    fun shadowIsNineSlicesOfABlurredMask() {
        val r = Recorder()
        r.shadow(10f, 10f, 40f, 60f, 4f, Shadow(radius = 6.dp, color = PolyColor(0x80000000.toInt())))
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
        val small = Recorder().apply { shadow(0f, 0f, 4f, 4f, 2f, Shadow(radius = 6.dp, color = PolyColor(0x80000000.toInt()), offset = DpOffset(0.dp, 4.dp))) }.done().single()
        assertEquals(4 * 4, small.colors.size)
    }

    @Test
    fun imageCarriesUvAndTint() {
        val r = Recorder()
        val bmp = ImageBitmap(4, 4, ByteArray(64) { -1 })
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
        val big = ImageBitmap(300, 2, ByteArray(300 * 2 * 4))
        assertEquals(big, Recorder().apply { image(big, 0f, 0f, 8f, 8f, 0f, 0f, 1f, 1f, -1) }.done().single().texture)
    }
}
