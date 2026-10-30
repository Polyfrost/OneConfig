// Compose scenes are internal API, but are the only way to render a composition in tests
@file:OptIn(InternalComposeUiApi::class)

package org.polyfrost.compose.backend

import androidx.compose.foundation.Image
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
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
import org.junit.jupiter.api.Test
import org.polyfrost.compose.render.FilterQuality
import org.polyfrost.compose.render.PolyBitmap
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
