package org.polyfrost.compose.backend

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.math.sqrt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PolyPathMeasureTest {
    init { PolyCompose.install() }

    private val square = Path().apply { addRect(Rect(0f, 0f, 10f, 10f)) }

    private fun measure(path: Path, forceClosed: Boolean = false) = PathMeasure().apply { setPath(path, forceClosed) }

    @Test
    fun lengthIncludesClosingSegment() {
        assertEquals(40f, measure(square).length)
        val polyline = Path().apply { moveTo(0f, 0f); lineTo(3f, 4f); lineTo(3f, 10f) }
        assertEquals(11f, measure(polyline).length)
        assertEquals(11f + sqrt(109f), measure(polyline, forceClosed = true).length, 1e-4f)
    }

    @Test
    fun measuresFirstNonEmptyContourOnly() {
        val path = Path().apply { moveTo(5f, 5f); lineTo(5f, 5f); moveTo(0f, 0f); lineTo(4f, 0f); moveTo(0f, 0f); lineTo(0f, 100f) }
        val m = measure(path)
        assertEquals(4f, m.length)
        assertEquals(Offset(2f, 0f), m.getPosition(2f))
        assertEquals(Offset(1f, 0f), m.getTangent(0f))
        assertEquals(Offset.Unspecified, measure(Path()).getPosition(0f))
    }

    @Test
    fun segmentKeepsCornersInRange() {
        val dst = Path()
        assertTrue(measure(square).getSegment(5f, 15f, dst))
        val m = measure(dst)
        assertEquals(10f, m.length)
        // addRect defaults to counter-clockwise, which runs down the left edge first
        assertEquals(Offset(0f, 5f), m.getPosition(0f))
        assertEquals(Offset(0f, 10f), m.getPosition(5f))
        assertEquals(Offset(5f, 10f), m.getPosition(10f))
        assertFalse(measure(square).getSegment(15f, 15f, dst))
    }

    @Test
    fun wrappedTrimMatchesCompose() {
        // Same calls PathComponent makes for trimPathStart = 0.875, trimPathEnd = 0.125
        val m = measure(square)
        val out = Path()
        val tmp = Path()
        assertTrue(m.getSegment(35f, m.length, tmp))
        out.addPath(tmp)
        tmp.reset()
        assertTrue(m.getSegment(0f, 5f, tmp))
        out.addPath(tmp)
        assertEquals(Rect(0f, 0f, 5f, 5f), out.getBounds())
        val first = measure(out)
        assertEquals(5f, first.length)
        assertEquals(Offset(5f, 0f), first.getPosition(0f))
        assertEquals(Offset(-1f, 0f), first.getTangent(0f))
    }
}
