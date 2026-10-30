// Compose scenes are internal API, but are the only way to render a composition in tests
@file:OptIn(InternalComposeUiApi::class)

package org.polyfrost.compose.backend

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PolyPathOpsTest {
    init { PolyCompose.install() }

    private fun rect(l: Float, t: Float, r: Float, b: Float) = Path().apply { addRect(Rect(l, t, r, b)) }

    // Wound the other way from addRect
    private fun reversedRect(l: Float, t: Float, r: Float, b: Float) = Path().apply { moveTo(l, t); lineTo(l, b); lineTo(r, b); lineTo(r, t); close() }

    private fun op(a: Path, b: Path, op: PathOperation) = Path().apply { assertTrue(op(a, b, op)) }

    private fun Path.hits(vararg xy: Float): List<Boolean> = PathHitTester(this).let { t -> xy.indices.step(2).map { Offset(xy[it], xy[it + 1]) in t } }

    private fun ImageBitmap.alpha(x: Int, y: Int) = (this as PolyImageBitmap).bitmap.let { it.pixels[(y * it.width + x) * 4 + 3].toInt() and 0xFF }

    @Test
    fun differenceOfInnerRectLeavesRing() {
        val ring = op(rect(0f, 0f, 20f, 20f), reversedRect(5f, 5f, 15f, 15f), PathOperation.Difference)
        assertEquals(listOf(true, true, false, false), ring.hits(2f, 2f, 17f, 10f, 10f, 10f, 25f, 10f))

        val image = ImageBitmap(20, 20)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(20f, 20f)) { drawPath(ring, Color.Red) }
        assertEquals(255, image.alpha(2, 10))
        assertEquals(0, image.alpha(10, 10))
    }

    @Test
    fun opsMayWriteIntoTheirInput() {
        val path = rect(0f, 0f, 20f, 20f)
        assertTrue(path.op(path, rect(5f, 5f, 15f, 15f), PathOperation.Difference))
        assertEquals(listOf(true, false), path.hits(2f, 2f, 10f, 10f))
        val inner = rect(5f, 5f, 15f, 15f)
        assertTrue(inner.op(rect(0f, 0f, 20f, 20f), inner, PathOperation.ReverseDifference))
        assertEquals(listOf(false, false), inner.hits(2f, 2f, 10f, 10f))
    }

    @Test
    fun combinesOverlappingRects() {
        val a = rect(0f, 0f, 10f, 10f)
        val b = reversedRect(5f, 0f, 15f, 10f)
        val points = floatArrayOf(2f, 5f, 7f, 5f, 12f, 5f, 20f, 5f)
        assertEquals(listOf(true, true, true, false), op(a, b, PathOperation.Union).hits(*points))
        assertEquals(listOf(true, false, true, false), op(a, b, PathOperation.Xor).hits(*points))
        assertEquals(listOf(false, true, false, false), op(a, b, PathOperation.Intersect).hits(*points))
        assertEquals(listOf(true, false, false, false), op(a, b, PathOperation.Difference).hits(*points))
        assertEquals(listOf(false, false, true, false), op(a, b, PathOperation.ReverseDifference).hits(*points))
    }

    @Test
    fun intersectClipsAgainstConvexContour() {
        val triangle = Path().apply { moveTo(0f, 0f); lineTo(20f, 0f); lineTo(0f, 20f); close() }
        val clipped = op(rect(5f, 5f, 30f, 30f), triangle, PathOperation.Intersect)
        assertEquals(listOf(true, false, false), clipped.hits(7f, 7f, 12f, 12f, 2f, 2f))
    }

    @Test
    fun nonUniformRoundedBorderRenders() {
        val rec = FrameRecomposer(Dispatchers.Unconfined) {}
        val scene = CanvasLayersComposeScene(frameRecomposer = rec, density = Density(1f), size = IntSize(40, 40))
        scene.setContent { Box(Modifier.size(40.dp).border(4.dp, Color.Red, RoundedCornerShape(topStart = 12.dp))) }
        val image = ImageBitmap(40, 40)
        with(SingleComposeSceneRenderingScope {}) { scene.render(rec, Canvas(image), System.nanoTime()) }
        scene.close()
        rec.close()

        assertEquals(255, image.alpha(20, 1), "top edge")
        assertEquals(255, image.alpha(38, 38), "square corner")
        assertEquals(0, image.alpha(20, 20), "inside")
        assertEquals(0, image.alpha(1, 1), "outside the rounded corner")
        assertFalse(image.alpha(5, 5) == 0, "along the rounded corner")
    }
}
