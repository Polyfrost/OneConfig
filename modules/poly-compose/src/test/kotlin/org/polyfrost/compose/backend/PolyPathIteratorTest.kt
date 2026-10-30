package org.polyfrost.compose.backend

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathHitTester
import androidx.compose.ui.graphics.PathSegment
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PolyPathIteratorTest {
    init { PolyCompose.install() }

    @Test
    fun yieldsClosingLineBeforeClose() {
        val path = Path().apply { moveTo(0f, 0f); lineTo(4f, 0f); lineTo(0f, 3f); close(); moveTo(9f, 9f); lineTo(9f, 10f) }
        val segments = path.iterator().asSequence().toList()
        assertEquals(
            listOf(PathSegment.Type.Move, PathSegment.Type.Line, PathSegment.Type.Line, PathSegment.Type.Line, PathSegment.Type.Close, PathSegment.Type.Move, PathSegment.Type.Line),
            segments.map { it.type },
        )
        assertArrayEquals(floatArrayOf(0f, 3f, 0f, 0f), segments[3].points)
        assertEquals(segments.size, path.iterator().calculateSize())

        val it = path.iterator()
        val points = FloatArray(10)
        repeat(segments.size) { _ -> it.next(points, 2) }
        assertArrayEquals(floatArrayOf(9f, 9f, 9f, 10f), points.copyOfRange(2, 6))
        assertEquals(PathSegment.Type.Done, it.next(points, 2))
    }

    @Test
    fun hitTestsRectAndTriangle() {
        val rect = PathHitTester(Path().apply { addRect(Rect(10f, 10f, 30f, 20f)) })
        assertTrue(Offset(15f, 15f) in rect)
        assertFalse(Offset(35f, 15f) in rect)
        assertFalse(Offset(15f, 25f) in rect)

        val triangle = PathHitTester(Path().apply { moveTo(0f, 0f); lineTo(20f, 0f); lineTo(0f, 20f); close() })
        assertTrue(Offset(5f, 5f) in triangle)
        assertFalse(Offset(15f, 15f) in triangle)
    }
}
