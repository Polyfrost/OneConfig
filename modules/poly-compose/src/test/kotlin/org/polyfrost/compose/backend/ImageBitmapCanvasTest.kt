// Compose scenes are internal API, but are the only way to render a composition in tests
@file:OptIn(InternalComposeUiApi::class)

package org.polyfrost.compose.backend

import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.render.FilterQuality
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyGlyphPage
import org.polyfrost.compose.render.PolyGradient
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyTessellator

class ImageBitmapCanvasTest {
    private class ImageLog : PolyTessellator() {
        val images = ArrayList<Pair<PolyBitmap, Int>>()
        override fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {}
        override fun bitmapTexture(bitmap: PolyBitmap) = bitmap
        override fun live(draw: (PolyRenderer) -> Unit) = draw(this)
        override fun image(bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: Int, filterQuality: FilterQuality) {
            images += bitmap to tint
        }
    }

    private fun PolyBitmap.argb(x: Int, y: Int): Int {
        val o = (y * width + x) * 4
        return ((pixels[o + 3].toInt() and 0xFF) shl 24) or ((pixels[o].toInt() and 0xFF) shl 16) or
            ((pixels[o + 1].toInt() and 0xFF) shl 8) or (pixels[o + 2].toInt() and 0xFF)
    }

    private fun drawInto(image: ImageBitmap, block: DrawScope.() -> Unit): PolyBitmap {
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat()), block)
        return (image as PolyImageBitmap).bitmap
    }

    @Test
    fun imagesAreSampledIntoBitmaps() {
        PolyCompose.install()
        // 300 pixels is too wide for the atlas, so that image is sampled from its own bytes
        for (w in intArrayOf(2, 300)) {
            val red = byteArrayOf(-1, 0, 0, -1)
            val blue = byteArrayOf(0, 0, -1, -1)
            val stripes = PolyBitmap(w, 1, ByteArray(w * 4) { i -> (if (i / 4 % 2 == 0) red else blue)[i % 4] })
            val out = drawInto(ImageBitmap(w + 2, 2)) { drawImage(stripes.asComposeImageBitmap(), Offset(1f, 0f)) }
            assertEquals(0, out.argb(0, 0))
            assertEquals(0xFFFF0000.toInt(), out.argb(1, 0))
            assertEquals(0xFF0000FF.toInt(), out.argb(2, 0))
            assertEquals(0xFF0000FF.toInt(), out.argb(w, 0))
            assertEquals(0, out.argb(w + 1, 0))
            assertEquals(0, out.argb(1, 1))
        }
    }

    private fun assertNear(expected: Int, actual: Int, at: String) {
        for (shift in intArrayOf(0, 8, 16, 24)) {
            val d = (actual ushr shift and 0xFF) - (expected ushr shift and 0xFF)
            assertTrue(d in -2..2, "%08x at %s, expected %08x".format(actual, at, expected))
        }
    }

    private fun redToBlue(t: Float) = PolyGradient.lerpArgb(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), t)

    @Test
    fun gradientsAreSampledIntoBitmaps() {
        PolyCompose.install()
        val out = drawInto(ImageBitmap(16, 2)) { drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Blue))) }
        for (x in 0 until 16) assertNear(redToBlue((x + 0.5f) / 16f), out.argb(x, 1), "$x")
    }

    @Test
    fun pathsAreFilledAndStrokedWithGradients() {
        PolyCompose.install()
        val triangle = Path().apply { moveTo(0f, 0f); lineTo(16f, 0f); lineTo(0f, 16f); close() }
        val fill = drawInto(ImageBitmap(16, 16)) { drawPath(triangle, Brush.horizontalGradient(listOf(Color.Red, Color.Blue))) }
        for (x in 1..10) assertNear(redToBlue((x + 0.5f) / 16f), fill.argb(x, 4), "($x, 4)")
        assertEquals(0, fill.argb(15, 15))

        val clamped = drawInto(ImageBitmap(16, 16)) { drawPath(triangle, Brush.horizontalGradient(listOf(Color.Red, Color.Blue), 4f, 8f)) }
        for (x in 1..3) assertEquals(0xFFFF0000.toInt(), clamped.argb(x, 4), "before the gradient at $x")
        for (x in 8..10) assertEquals(0xFF0000FF.toInt(), clamped.argb(x, 4), "past the gradient at $x")

        val line = Path().apply { moveTo(2f, 2f); lineTo(2f, 14f) }
        val stroke = drawInto(ImageBitmap(4, 16)) { drawPath(line, Brush.verticalGradient(listOf(Color.Red, Color.Blue)), style = Stroke(3f)) }
        for (y in 3..12) for (x in 1..2) assertNear(redToBlue((y + 0.5f) / 16f), stroke.argb(x, y), "($x, $y)")
        assertEquals(0, stroke.argb(1, 15))
    }

    @Test
    fun textIsSampledIntoBitmaps() {
        PolyCompose.install()
        val face = object : PolyGlyphPage.Face {
            override fun glyph(codepoint: Int) = 1
            override fun advance(glyph: Int) = 1f
            override fun raster(glyph: Int, pixelSize: Float) = PolyGlyphPage.Raster(0, -4, 2, 4, ByteArray(8) { -1 })
        }
        val old = PolyGlyphPage.rasterizer
        PolyGlyphPage.rasterizer = object : PolyGlyphPage.Rasterizer { override fun face(name: String) = face }
        try {
            val out = drawInto(ImageBitmap(6, 8)) { drawPoly { it.renderer.text("a", 1f, 6f, 0xFFFF0000.toInt(), PolyFont("bitmap", 4f)) } }
            for (y in 0 until 8) for (x in 0 until 6) {
                assertEquals(if (x in 1..2 && y in 2..5) 0xFFFF0000.toInt() else 0, out.argb(x, y), "($x, $y)")
            }
        } finally {
            PolyGlyphPage.rasterizer = old
        }
    }

    @Test
    fun redrawingClearsIntoFreshBitmap() {
        PolyCompose.install()
        val image = ImageBitmap(8, 8)
        val canvas = Canvas(image)
        val scope = CanvasDrawScope()
        scope.draw(Density(1f), LayoutDirection.Ltr, canvas, Size(8f, 8f)) { drawRect(Color.Blue, alpha = 0.5f) }
        val first = (image as PolyImageBitmap).bitmap
        assertEquals(0x800000FF.toInt(), first.argb(4, 4))

        scope.draw(Density(1f), LayoutDirection.Ltr, canvas, Size(8f, 8f)) {
            drawRect(Color.Black, blendMode = BlendMode.Clear)
            drawPath(Path().apply { moveTo(0f, 4f); lineTo(8f, 4f) }, Color.Red, style = Stroke(2f))
        }
        val second = image.bitmap
        assertNotSame(first, second, "textures are cached by bitmap identity")
        assertEquals(0x800000FF.toInt(), first.argb(4, 4))
        assertEquals(0xFFFF0000.toInt(), second.argb(4, 4))
        assertEquals(0, second.argb(4, 0) ushr 24)
    }

    @Test
    fun imageVectorIsRasterizedOnceAndTinted() {
        PolyCompose.install()
        val square = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 12f, viewportHeight = 12f)
            .path(fill = SolidColor(Color.Black)) { moveTo(2f, 2f); lineTo(10f, 2f); lineTo(10f, 10f); lineTo(2f, 10f); close() }
            .build()
        var tint by mutableStateOf(Color.Red)
        val rec = FrameRecomposer(Dispatchers.Unconfined) {}
        val scope = SingleComposeSceneRenderingScope {}
        val scene = CanvasLayersComposeScene(frameRecomposer = rec, density = Density(1f), size = IntSize(100, 100))
        scene.setContent { Image(square, null, colorFilter = ColorFilter.tint(tint)) }
        val frames = List(2) {
            if (it == 1) {
                tint = Color.Blue
                Snapshot.sendApplyNotifications()
            }
            ImageLog().also { r -> with(scope) { scene.render(rec, PolyCanvas(r), System.nanoTime()) } }.images
        }
        scene.close()
        rec.close()

        val (bitmap, red) = frames[0].single()
        assertEquals(0xFFFF0000.toInt(), red)
        assertEquals(-1, bitmap.argb(12, 12), "one-color vectors are cached as a white coverage mask")
        assertEquals(0, bitmap.argb(1, 1) ushr 24)
        val (again, blue) = frames[1].single()
        assertEquals(0xFF0000FF.toInt(), blue)
        assertSame(bitmap, again, "an unchanged vector must not be rasterized again")
    }

    @Test
    fun trimmedImageVectorDrawsOnlyItsSegment() {
        PolyCompose.install()
        val outline = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 12f, viewportHeight = 12f)
            .addPath(addPathNodes("M2,2 L10,2 L10,10 L2,10 Z"), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, trimPathEnd = 0.5f)
            .build()
        val rec = FrameRecomposer(Dispatchers.Unconfined) {}
        val scope = SingleComposeSceneRenderingScope {}
        val scene = CanvasLayersComposeScene(frameRecomposer = rec, density = Density(1f), size = IntSize(100, 100))
        scene.setContent { Image(outline, null) }
        val images = ImageLog().also { r -> with(scope) { scene.render(rec, PolyCanvas(r), System.nanoTime()) } }.images
        scene.close()
        rec.close()

        val bitmap = images.single().first
        assertEquals(0xFF, bitmap.argb(12, 4) ushr 24, "top edge is in the first half")
        assertEquals(0xFF, bitmap.argb(20, 12) ushr 24, "right edge is in the first half")
        assertEquals(0, bitmap.argb(12, 20) ushr 24, "bottom edge is trimmed")
        assertEquals(0, bitmap.argb(4, 12) ushr 24, "left edge is trimmed")
    }
}
