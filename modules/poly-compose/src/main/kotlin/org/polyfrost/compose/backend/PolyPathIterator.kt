package org.polyfrost.compose.backend

import androidx.compose.ui.graphics.CloseSegment
import androidx.compose.ui.graphics.DoneSegment
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathSegment

// Paths are stored flattened, so every contour is a move followed by lines. Like Skia, a closed contour
// yields its closing line before the close, which hit testing relies on as it ignores close segments.
internal class PolyPathIterator(
    override val path: Path,
    override val conicEvaluation: PathIterator.ConicEvaluation,
    override val tolerance: Float,
) : PathIterator {
    private val source = path as PolyComposePath
    private val contours = source.allContours()
    private var contour = 0
    private var step = 0
    private val scratch = FloatArray(8)

    override fun calculateSize(includeConvertedConics: Boolean): Int = contours.indices.sumOf(::steps)

    override fun hasNext(): Boolean = contour < contours.size

    override fun next(outPoints: FloatArray, offset: Int): PathSegment.Type {
        check(outPoints.size - offset >= 8) { "The points array must contain at least 8 floats" }
        if (!hasNext()) return PathSegment.Type.Done
        val c = contours[contour]
        val n = c.size / 2
        val type = when {
            step == 0 -> {
                outPoints[offset] = c[0]
                outPoints[offset + 1] = c[1]
                PathSegment.Type.Move
            }
            step < n || step < steps(contour) - 1 -> {
                c.copyInto(outPoints, offset, 2 * step - 2, 2 * step)
                val end = 2 * (step % n)
                outPoints[offset + 2] = c[end]
                outPoints[offset + 3] = c[end + 1]
                PathSegment.Type.Line
            }
            else -> PathSegment.Type.Close
        }
        if (++step == steps(contour)) {
            contour++
            step = 0
        }
        return type
    }

    override fun next(): PathSegment = when (val type = next(scratch)) {
        PathSegment.Type.Done -> DoneSegment
        PathSegment.Type.Close -> CloseSegment
        PathSegment.Type.Move -> PathSegment(type, scratch.copyOf(2), 0f)
        else -> PathSegment(type, scratch.copyOf(4), 0f)
    }

    private fun steps(i: Int): Int {
        val c = contours[i]
        if (!source.closedAt(i)) return c.size / 2
        val open = c[0] != c[c.size - 2] || c[1] != c[c.size - 1]
        return c.size / 2 + if (open) 2 else 1
    }
}
