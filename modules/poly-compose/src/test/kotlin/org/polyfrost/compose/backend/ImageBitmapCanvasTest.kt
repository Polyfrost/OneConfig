// Compose scenes are internal API, but are the only way to render a composition in tests
@file:OptIn(InternalComposeUiApi::class)

package org.polyfrost.compose.backend

import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
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
import org.polyfrost.compose.mc.McGlyph
import org.polyfrost.compose.mc.McGlyphs
import org.polyfrost.compose.render.FilterQuality
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyGlyphPage
import org.polyfrost.compose.render.PolyGradient
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyTessellator

class ImageBitmapCanvasTest {
    @Test
    fun downscaledMcGlyphsPreserveColorsAtTransparentEdges() {
        PolyCompose.install()
        val source = McGlyphs.source
        try {
            for (width in listOf(2, 258)) for (cropped in listOf(false, true)) {
                for ((ink, tint, alpha) in listOf(
                    Triple(-1, -1, 1f),
                    Triple(-1, 0x80FF8000.toInt(), 0.5f),
                    Triple(0x8040C080.toInt(), -1, 1f),
                )) {
                    val sheetWidth = if (cropped) width * 3 else width
                    val x0 = if (cropped) width else 0
                    val bitmap = PolyBitmap(sheetWidth, 2, ByteArray(sheetWidth * 2 * 4) { -1 })
                    for (y in 0..1) for (x in 0 until width) {
                        val color = if (x % 2 == 0) ink else 0x00FF00FF
                        val at = (y * sheetWidth + x0 + x) * 4
                        bitmap.pixels[at] = (color ushr 16).toByte()
                        bitmap.pixels[at + 1] = (color ushr 8).toByte()
                        bitmap.pixels[at + 2] = color.toByte()
                        bitmap.pixels[at + 3] = (color ushr 24).toByte()
                    }
                    val glyph = McGlyph(bitmap, x0.toFloat() / sheetWidth, 0f, (x0 + width).toFloat() / sheetWidth, 1f,
                        0f, width / 2f, 1f, width / 2f, 0.5f)
                    McGlyphs.source = { glyph }
                    val target = PolyImageBitmap(PolyBitmap(width / 2, 1, ByteArray(width / 2 * 4)))
                    val renderer = (Canvas(target) as PolyCanvas).renderer
                    renderer.saveLayerAlpha(alpha)
                    renderer.mcText("a", 0f, 0f, tint, false, 1f)
                    renderer.restore()
                    var expected = ((ink ushr 24) * 0.5f * (tint ushr 24) / 255f * alpha + 0.5f).toInt() shl 24
                    for (shift in listOf(0, 8, 16)) {
                        expected = expected or ((((ink ushr shift and 0xFF) * (tint ushr shift and 0xFF) / 255f) + 0.5f).toInt() shl shift)
                    }
                    val actual = target.bitmap
                    for (x in 0 until actual.width) assertNear(expected, actual.argb(x, 0), "x=$x, width=$width, cropped=$cropped, ink=$ink, tint=$tint, alpha=$alpha")
                }
            }
        } finally {
            McGlyphs.source = source
        }
    }

    @Test
    fun downscaledMcGlyphsDoNotSampleNeighboringCells() {
        PolyCompose.install()
        val source = McGlyphs.source
        try {
            for (size in listOf(8, 258)) {
                val pixels = ByteArray(size * size * 4)
                for (y in 2 until size - 2) for (x in 2 until size - 2) {
                    pixels.fill(-1, (y * size + x) * 4, (y * size + x + 1) * 4)
                }
                val isolated = PolyBitmap(size, size, pixels)
                val sheetSize = size * 3
                val sheet = PolyBitmap(sheetSize, sheetSize, ByteArray(sheetSize * sheetSize * 4) { -1 })
                for (y in 0 until size) {
                    pixels.copyInto(sheet.pixels, ((y + size) * sheetSize + size) * 4, y * size * 4, (y + 1) * size * 4)
                }
                fun render(bitmap: PolyBitmap, u0: Float, u1: Float): PolyBitmap {
                    McGlyphs.source = { McGlyph(bitmap, u0, u0, u1, u1, 0f, size.toFloat(), size.toFloat(), size.toFloat(), 1f) }
                    val targetSize = (size * 0.66f).toInt() + 2
                    val target = PolyImageBitmap(PolyBitmap(targetSize, targetSize, ByteArray(targetSize * targetSize * 4)))
                    val renderer = (Canvas(target) as PolyCanvas).renderer
                    renderer.translate(0.49f, 0.49f)
                    renderer.scale(0.66f, 0.66f)
                    renderer.mcText("a", 0f, 0f, -1, false, 1f)
                    return target.bitmap
                }
                val expected = render(isolated, 0f, 1f)
                val actual = render(sheet, 1f / 3f, 2f / 3f)
                assertEquals(0, actual.argb(0, 2) ushr 24, "left neighbor at size $size")
                assertEquals(0, actual.argb(2, 0) ushr 24, "top neighbor at size $size")
                assertTrue((3 until expected.pixels.size step 4).any { (expected.pixels[it].toInt() and 0xFF) in 1..254 }, "linear coverage at size $size")
                for (i in actual.pixels.indices) {
                    val delta = (actual.pixels[i].toInt() and 0xFF) - (expected.pixels[i].toInt() and 0xFF)
                    assertTrue(delta in -1..1, "channel ${i % 4} at (${i / 4 % actual.width}, ${i / 4 / actual.width}), size $size")
                }
            }
        } finally {
            McGlyphs.source = source
        }
    }

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
    fun perCornerRoundRectsKeepEachRadius() {
        PolyCompose.install()
        val shape = RoundRect(0f, 0f, 20f, 20f, bottomRightCornerRadius = CornerRadius(10f))
        val out = drawInto(ImageBitmap(20, 20)) { drawPath(Path().apply { addRoundRect(shape) }, Color.Red) }
        assertEquals(0xFFFF0000.toInt(), out.argb(0, 0))
        assertEquals(0, out.argb(19, 19) ushr 24)
    }

    @Test
    fun oppositelyWoundShapesCutHoles() {
        PolyCompose.install()
        val ring = Path().apply {
            addRect(Rect(0f, 0f, 20f, 20f))
            addRect(Rect(5f, 5f, 15f, 15f), Path.Direction.Clockwise)
        }
        val out = drawInto(ImageBitmap(20, 20)) { drawPath(ring, Color.Red) }
        assertEquals(0xFFFF0000.toInt(), out.argb(2, 10))
        assertEquals(0, out.argb(10, 10) ushr 24)
    }

    @Test
    fun tintReplacesImageColors() {
        PolyCompose.install()
        val red = PolyBitmap(2, 2, ByteArray(16) { i -> if (i % 4 == 0 || i % 4 == 3) -1 else 0 })
        val out = drawInto(ImageBitmap(2, 2)) { drawImage(red.asComposeImageBitmap(), colorFilter = ColorFilter.tint(Color.Blue)) }
        assertEquals(0xFF0000FF.toInt(), out.argb(0, 0))
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
    fun differenceClipLeavesTheRectangleOut() {
        PolyCompose.install()
        // How Compose draws a border whose stroke is wider than twice its corner radius
        val out = drawInto(ImageBitmap(12, 12)) {
            drawRect(Color.Red)
            clipRect(3f, 3f, 9f, 9f, ClipOp.Difference) { drawRoundRect(Color.Blue, cornerRadius = CornerRadius(1f)) }
        }
        for (x in 1 until 11) assertEquals(if (x in 3..8) 0xFFFF0000.toInt() else 0xFF0000FF.toInt(), out.argb(x, 6), "($x, 6)")
        assertEquals(0xFF0000FF.toInt(), out.argb(6, 2))
        assertEquals(0xFFFF0000.toInt(), out.argb(6, 3))
    }

    @Test
    fun clipPathKeepsEachCornersRadius() {
        PolyCompose.install()
        val corner = Path().apply { addRoundRect(RoundRect(0f, 0f, 20f, 20f, topLeftCornerRadius = CornerRadius(8f))) }
        val out = drawInto(ImageBitmap(20, 20)) { clipPath(corner) { drawRect(Color.Red) } }
        assertEquals(0, out.argb(1, 1) ushr 24, "outside the rounded corner")
        assertEquals(0xFFFF0000.toInt(), out.argb(18, 1))
        assertEquals(0xFFFF0000.toInt(), out.argb(18, 18))
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
    fun canvasesOnOneImageKeepEachOthersDrawings() {
        PolyCompose.install()
        val image = ImageBitmap(3, 1)
        val first = Canvas(image)
        val paint = Paint()
        first.drawRect(0f, 0f, 1f, 1f, paint.apply { color = Color.Red })
        val second = Canvas(image)
        second.drawRect(1f, 0f, 2f, 1f, paint.apply { color = Color.Blue })
        first.drawRect(2f, 0f, 3f, 1f, paint.apply { color = Color.Green })
        val out = (image as PolyImageBitmap).bitmap
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), 0xFF00FF00.toInt()), List(3) { out.argb(it, 0) })
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
