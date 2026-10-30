package org.polyfrost.compose.backend

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

// Measures only the first contour with a non-zero length, like Skia's PathMeasure
internal class PolyPathMeasure : PathMeasure {
    private var pts = FloatArray(0)
    private var count = 0
    private var dist = FloatArray(0)

    override var length = 0f
        private set

    override fun setPath(path: Path?, forceClosed: Boolean) {
        count = 0
        length = 0f
        val p = path as PolyComposePath? ?: return
        p.allContours().forEachIndexed { c, contour ->
            val n = contour.size / 2 + if (forceClosed || p.closedAt(c)) 1 else 0
            if (dist.size < n) dist = FloatArray(n)
            pts = contour
            for (i in 1 until n) dist[i] = dist[i - 1] + hypot(x(i) - x(i - 1), y(i) - y(i - 1))
            if (dist[n - 1] > 0f) {
                count = n
                length = dist[n - 1]
                return
            }
        }
    }

    override fun getSegment(startDistance: Float, stopDistance: Float, destination: Path, startWithMoveTo: Boolean): Boolean {
        val start = max(startDistance, 0f)
        val stop = min(stopDistance, length)
        if (!(start < stop)) return false
        val first = segmentEnd(start)
        val last = segmentEnd(stop)
        if (startWithMoveTo) at(first, start).let { destination.moveTo(it.x, it.y) }
        for (i in first until last) if (dist[i] > start) destination.lineTo(x(i), y(i))
        at(last, stop).let { destination.lineTo(it.x, it.y) }
        return true
    }

    override fun getPosition(distance: Float): Offset {
        if (count == 0) return Offset.Unspecified
        val d = distance.coerceIn(0f, length)
        return at(segmentEnd(d), d)
    }

    override fun getTangent(distance: Float): Offset {
        if (count == 0) return Offset.Unspecified
        val i = segmentEnd(distance.coerceIn(0f, length))
        val len = dist[i] - dist[i - 1]
        return Offset((x(i) - x(i - 1)) / len, (y(i) - y(i - 1)) / len)
    }

    // A closed contour's extra last point wraps around to its first
    private fun x(i: Int) = pts[2 * i % pts.size]
    private fun y(i: Int) = pts[(2 * i + 1) % pts.size]

    // Index of the end point of the non-degenerate segment containing d
    private fun segmentEnd(d: Float): Int {
        var i = 1
        while (i < count - 1 && (dist[i] < d || dist[i] == 0f)) i++
        return i
    }

    private fun at(i: Int, d: Float): Offset {
        val t = (d - dist[i - 1]) / (dist[i] - dist[i - 1])
        return Offset(x(i - 1) + (x(i) - x(i - 1)) * t, y(i - 1) + (y(i) - y(i - 1)) * t)
    }
}
