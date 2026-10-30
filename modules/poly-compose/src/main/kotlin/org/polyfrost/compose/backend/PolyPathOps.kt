package org.polyfrost.compose.backend

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import kotlin.math.sign

// Fill rules stand in for polygon clipping. This is exact for Union and Xor, Intersect with a single convex contour, and
// Difference from a single convex contour or from a path enclosing the subtracted one, given inputs whose contours don't cross
// themselves or nest in the same direction. Otherwise, intersections are clipped to the second path's bounds, and differences
// also fill where the subtracted path lies outside the other.
internal fun pathOp(a: PolyComposePath, b: PolyComposePath, op: PathOperation): Pair<List<FloatArray>, PathFillType> {
    val ac = a.allContours()
    val bc = b.allContours()
    return when (op) {
        PathOperation.Union -> oriented(ac, 1f) + oriented(bc, 1f) to PathFillType.NonZero
        PathOperation.Xor -> (ac + bc).map { it.copyOf() } to PathFillType.EvenOdd
        PathOperation.Difference -> difference(ac, bc) to PathFillType.NonZero
        PathOperation.ReverseDifference -> difference(bc, ac) to PathFillType.NonZero
        else -> convex(bc)?.let { clip(ac, it) to a.fillType }
            ?: convex(ac)?.let { clip(bc, it) to b.fillType }
            ?: (clip(ac, bounds(bc).let { floatArrayOf(it.left, it.top, it.right, it.top, it.right, it.bottom, it.left, it.bottom) }) to a.fillType)
    }
}

// The subtracted contours wind against the others, so they cancel out where they overlap
private fun difference(a: List<FloatArray>, b: List<FloatArray>): List<FloatArray> {
    val box = bounds(a)
    val cut = convex(a)?.let { clip(b, it) } ?: b.filter { bounds(listOf(it)).overlaps(box) }
    return oriented(a, 1f) + oriented(cut, -1f)
}

private fun oriented(contours: List<FloatArray>, sign: Float): List<FloatArray> {
    val flip = contours.sumOf { area(it).toDouble() } * sign < 0.0
    return contours.map { c -> if (flip) FloatArray(c.size) { k -> c[c.size - 2 - (k and -2) + (k and 1)] } else c.copyOf() }
}

// Twice the signed area
private fun area(c: FloatArray): Float {
    var sum = 0f
    for (k in c.indices step 2) {
        val j = (k + 2) % c.size
        sum += c[k] * c[j + 1] - c[j] * c[k + 1]
    }
    return sum
}

private fun convex(contours: List<FloatArray>): FloatArray? {
    val c = contours.singleOrNull() ?: return null
    val n = c.size / 2
    if (n < 3) return null
    var turn = 0f
    var flips = 0
    var lastDx = 0f
    for (i in 0 until n) {
        val j = (i + 1) % n * 2
        val k = (i + 2) % n * 2
        val ax = c[j] - c[2 * i]
        val ay = c[j + 1] - c[2 * i + 1]
        val cross = ax * (c[k + 1] - c[j + 1]) - ay * (c[k] - c[j])
        if (cross * turn < 0f) return null
        if (cross != 0f) turn = cross
        // A contour like a pentagram turns one way throughout, but reverses horizontally more than twice
        if (ax * lastDx < 0f) flips++
        if (ax != 0f) lastDx = ax
    }
    return if (flips <= 2) c else null
}

// Sutherland–Hodgman, which keeps each subject contour's direction, so the result fills with the subject's fill rule
private fun clip(subjects: List<FloatArray>, clip: FloatArray): List<FloatArray> {
    val s = area(clip).sign
    if (s == 0f) return emptyList()
    return subjects.mapNotNull { subject ->
        var out = subject
        for (e in clip.indices step 2) {
            if (out.size < 6) return@mapNotNull null
            val ex = clip[e]
            val ey = clip[e + 1]
            val dx = clip[(e + 2) % clip.size] - ex
            val dy = clip[(e + 3) % clip.size] - ey
            val next = FloatArrayList()
            var px = out[out.size - 2]
            var py = out[out.size - 1]
            var pd = (dx * (py - ey) - dy * (px - ex)) * s
            for (k in out.indices step 2) {
                val cx = out[k]
                val cy = out[k + 1]
                val cd = (dx * (cy - ey) - dy * (cx - ex)) * s
                if ((cd >= 0f) != (pd >= 0f)) {
                    val t = pd / (pd - cd)
                    next.add(px + (cx - px) * t, py + (cy - py) * t)
                }
                if (cd >= 0f) next.add(cx, cy)
                px = cx
                py = cy
                pd = cd
            }
            out = next.toArray()
        }
        out.takeIf { it.size >= 6 }
    }
}
