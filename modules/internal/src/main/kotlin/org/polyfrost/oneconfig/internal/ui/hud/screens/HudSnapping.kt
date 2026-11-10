package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val HUD_SNAP_SCREEN_PADDING = 4f
internal const val HUD_GRID_SIZE = 8f

private const val ALIGN_EPSILON = 0.01f
private const val GAP_EPSILON = 0.05f

internal class HudSnapLine(
    val position: Float,
    val spanStart: Float,
    val spanEnd: Float,
    val start: Boolean = true,
    val center: Boolean = true,
    val end: Boolean = true,
)

internal class HudGapRef(val size: Float, val before: Rect, val after: Rect)

internal class HudSnapGuide(val vertical: Boolean, val position: Float, val start: Float, val end: Float)

internal class HudGapMarker(val from: Offset, val to: Offset, val size: Float)

internal class HudSnapResult(
    val guides: List<HudSnapGuide>,
    val gaps: List<HudGapMarker>,
    val sideGaps: List<HudMeasureLine>,
) {
    companion object {
        val NONE = HudSnapResult(emptyList(), emptyList(), emptyList())
    }
}

private fun Rect.lo(h: Boolean) = if (h) left else top
private fun Rect.hi(h: Boolean) = if (h) right else bottom
private fun Rect.crossLo(h: Boolean) = if (h) top else left
private fun Rect.crossHi(h: Boolean) = if (h) bottom else right

private fun crossOverlaps(r: Rect, h: Boolean, lo: Float, hi: Float) = r.crossLo(h) < hi && lo < r.crossHi(h)

private fun nearestBefore(obstacles: List<Rect>, h: Boolean, edge: Float, crossLo: Float, crossHi: Float): Rect? {
    var best: Rect? = null
    for (o in obstacles) {
        if (o.hi(h) > edge || !crossOverlaps(o, h, crossLo, crossHi)) continue
        if (best == null || o.hi(h) > best.hi(h)) best = o
    }
    return best
}

private fun nearestAfter(obstacles: List<Rect>, h: Boolean, edge: Float, crossLo: Float, crossHi: Float): Rect? {
    var best: Rect? = null
    for (o in obstacles) {
        if (o.lo(h) < edge || !crossOverlaps(o, h, crossLo, crossHi)) continue
        if (best == null || o.lo(h) < best.lo(h)) best = o
    }
    return best
}

private fun gapMarker(before: Rect, after: Rect, h: Boolean): HudGapMarker {
    val cross = (maxOf(before.crossLo(h), after.crossLo(h)) + minOf(before.crossHi(h), after.crossHi(h))) / 2f
    val from = before.hi(h)
    val to = after.lo(h)
    return if (h) HudGapMarker(Offset(from, cross), Offset(to, cross), to - from)
    else HudGapMarker(Offset(cross, from), Offset(cross, to), to - from)
}

internal class HudSnapContext(
    val screen: Rect,
    val obstacles: List<Rect>,
) {
    private val xLines = buildLines(true)
    private val yLines = buildLines(false)
    private val rowGaps = buildGaps(true)
    private val columnGaps = buildGaps(false)

    private fun lines(h: Boolean) = if (h) xLines else yLines
    private fun gaps(h: Boolean) = if (h) rowGaps else columnGaps

    private fun buildLines(h: Boolean): List<HudSnapLine> {
        val extent = screen.hi(h)
        val crossExtent = screen.crossHi(h)
        val lines = ArrayList<HudSnapLine>(obstacles.size * 3 + 5)
        lines.add(HudSnapLine(0f, 0f, crossExtent))
        lines.add(HudSnapLine(extent / 2f, 0f, crossExtent))
        lines.add(HudSnapLine(extent, 0f, crossExtent))
        lines.add(HudSnapLine(HUD_SNAP_SCREEN_PADDING, 0f, crossExtent, center = false, end = false))
        lines.add(HudSnapLine(extent - HUD_SNAP_SCREEN_PADDING, 0f, crossExtent, start = false, center = false))
        for (o in obstacles) {
            val lo = o.crossLo(h)
            val hi = o.crossHi(h)
            lines.add(HudSnapLine(o.lo(h), lo, hi))
            lines.add(HudSnapLine((o.lo(h) + o.hi(h)) / 2f, lo, hi))
            lines.add(HudSnapLine(o.hi(h), lo, hi))
        }
        return lines
    }

    private fun buildGaps(h: Boolean): List<HudGapRef> {
        val gaps = ArrayList<HudGapRef>()
        for (i in obstacles.indices) {
            val a = obstacles[i]
            val edge = a.hi(h)
            var b: Rect? = null
            var bStart = Float.POSITIVE_INFINITY
            for (j in obstacles.indices) {
                if (i == j) continue
                val o = obstacles[j]
                if (o.hi(h) <= edge || !crossOverlaps(o, h, a.crossLo(h), a.crossHi(h))) continue
                val start = maxOf(o.lo(h), edge)
                if (start < bStart) {
                    bStart = start
                    b = o
                }
            }
            if (b == null) continue
            val size = bStart - edge
            if (size > GAP_EPSILON) gaps.add(HudGapRef(size, a, b))
        }
        return gaps
    }

    private fun snapAxis(raw: Float, size: Float, crossLo: Float, crossHi: Float, h: Boolean, threshold: Float): Float? {
        var best: Float? = null
        var bestDist = threshold
        fun consider(target: Float) {
            val d = abs(target - raw)
            if (d < bestDist) {
                bestDist = d
                best = target
            }
        }
        for (line in lines(h)) {
            if (line.start) consider(line.position)
            if (line.center) consider(line.position - size / 2f)
            if (line.end) consider(line.position - size)
        }
        val before = nearestBefore(obstacles, h, raw + threshold, crossLo, crossHi)
        val after = nearestAfter(obstacles, h, raw + size - threshold, crossLo, crossHi)
        for (gap in gaps(h)) {
            if (before != null) consider(before.hi(h) + gap.size)
            if (after != null) consider(after.lo(h) - gap.size - size)
        }
        if (before != null && after != null) {
            val free = after.lo(h) - before.hi(h) - size
            if (free >= 0f) consider(before.hi(h) + free / 2f)
        }
        return best
    }

    fun snapX(rawX: Float, rawY: Float, width: Float, height: Float, threshold: Float): Float? =
        snapAxis(rawX, width, rawY, rawY + height, true, threshold)

    fun snapY(rawX: Float, rawY: Float, width: Float, height: Float, threshold: Float): Float? =
        snapAxis(rawY, height, rawX, rawX + width, false, threshold)

    fun feedback(rect: Rect, snapping: Boolean): HudSnapResult {
        val sideGaps = hudSideGaps(rect, obstacles, screen)
        if (!snapping) return HudSnapResult(emptyList(), emptyList(), sideGaps)
        val guides = ArrayList<HudSnapGuide>()
        collectGuides(rect, true, guides)
        collectGuides(rect, false, guides)
        val gaps = ArrayList<HudGapMarker>()
        collectGaps(rect, true, gaps)
        collectGaps(rect, false, gaps)
        return HudSnapResult(guides, gaps, sideGaps)
    }

    private fun collectGuides(rect: Rect, h: Boolean, out: MutableList<HudSnapGuide>) {
        val lo = rect.lo(h)
        val hi = rect.hi(h)
        val mid = (lo + hi) / 2f
        val first = out.size
        for (line in lines(h)) {
            val p = line.position
            val matched = (line.start && abs(p - lo) < ALIGN_EPSILON) ||
                (line.center && abs(p - mid) < ALIGN_EPSILON) ||
                (line.end && abs(p - hi) < ALIGN_EPSILON)
            if (!matched) continue
            var merged = false
            for (i in first until out.size) {
                val g = out[i]
                if (abs(g.position - p) >= ALIGN_EPSILON) continue
                out[i] = HudSnapGuide(h, g.position, minOf(g.start, line.spanStart), maxOf(g.end, line.spanEnd))
                merged = true
                break
            }
            if (!merged) {
                out.add(
                    HudSnapGuide(
                        h, p,
                        minOf(rect.crossLo(h), line.spanStart),
                        maxOf(rect.crossHi(h), line.spanEnd),
                    )
                )
            }
        }
    }

    private fun collectGaps(rect: Rect, h: Boolean, out: MutableList<HudGapMarker>) {
        val crossLo = rect.crossLo(h)
        val crossHi = rect.crossHi(h)
        val before = nearestBefore(obstacles, h, rect.lo(h) + ALIGN_EPSILON, crossLo, crossHi)
        val after = nearestAfter(obstacles, h, rect.hi(h) - ALIGN_EPSILON, crossLo, crossHi)
        val gapBefore = before?.let { rect.lo(h) - it.hi(h) }?.takeIf { it > GAP_EPSILON }
        val gapAfter = after?.let { it.lo(h) - rect.hi(h) }?.takeIf { it > GAP_EPSILON }
        val start = out.size
        var showBefore = false
        var showAfter = false
        if (gapBefore != null && gapAfter != null && abs(gapBefore - gapAfter) < GAP_EPSILON) {
            showBefore = true
            showAfter = true
        }
        for (ref in gaps(h)) {
            val matchesBefore = gapBefore != null && abs(ref.size - gapBefore) < GAP_EPSILON
            val matchesAfter = gapAfter != null && abs(ref.size - gapAfter) < GAP_EPSILON
            if (!matchesBefore && !matchesAfter) continue
            if (matchesBefore) showBefore = true
            if (matchesAfter) showAfter = true
            out.add(gapMarker(ref.before, ref.after, h))
        }
        if (showBefore && before != null) out.add(start, gapMarker(before, rect, h))
        if (showAfter && after != null) out.add(start, gapMarker(rect, after, h))
    }
}

internal class HudDragSnapper {
    private var context: HudSnapContext? = null
    private var startRect: Rect? = null
    private var grabbedRect: Rect? = null

    var result by mutableStateOf(HudSnapResult.NONE)
        private set

    private fun union(rects: List<Rect>) = rects.reduceOrNull { a, b ->
        Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
    }

    fun startsWith(dragged: List<Rect>): Boolean = context != null && union(dragged) == startRect

    fun begin(dragged: List<Rect>, grabbed: Rect?, others: List<Rect>, screenWidth: Float, screenHeight: Float) {
        val group = union(dragged)
        startRect = group
        grabbedRect = grabbed?.takeIf { group != null && it != group }
        context = HudSnapContext(Rect(0f, 0f, screenWidth, screenHeight), others)
        result = HudSnapResult.NONE
    }

    fun move(rawDx: Float, rawDy: Float, threshold: Float, snapping: Boolean, gridEnabled: Boolean): Offset {
        val ctx = context
        val start = startRect
        if (ctx == null || start == null) return Offset(rawDx, rawDy)
        if (!snapping) {
            result = ctx.feedback(start.translate(rawDx, rawDy), false)
            return Offset(rawDx, rawDy)
        }
        val grabbed = grabbedRect
        val dx = snapDelta(rawDx, true, gridEnabled, start, grabbed) { r, raw ->
            ctx.snapX(raw, r.top + rawDy, r.width, r.height, threshold)
        }
        val dy = snapDelta(rawDy, false, gridEnabled, start, grabbed) { r, raw ->
            ctx.snapY(r.left + rawDx, raw, r.width, r.height, threshold)
        }
        val group = start.translate(dx, dy)
        val feedback = ctx.feedback(group, true)
        result = if (grabbed == null) {
            feedback
        } else {
            val own = ctx.feedback(grabbed.translate(dx, dy), true)
            HudSnapResult(feedback.guides + own.guides, feedback.gaps + own.gaps, feedback.sideGaps)
        }
        return Offset(dx, dy)
    }

    private inline fun snapDelta(
        rawDelta: Float,
        h: Boolean,
        gridEnabled: Boolean,
        group: Rect,
        grabbed: Rect?,
        snapAxis: (Rect, Float) -> Float?,
    ): Float {
        var best: Float? = null
        var bestDist = Float.POSITIVE_INFINITY
        for (r in listOfNotNull(group, grabbed)) {
            val raw = r.lo(h) + rawDelta
            val snapped = snapAxis(r, raw) ?: continue
            val d = abs(snapped - raw)
            if (d < bestDist) {
                bestDist = d
                best = snapped - r.lo(h)
            }
        }
        best?.let { return it }
        val origin = group.lo(h)
        if (gridEnabled) return ((origin + rawDelta) / HUD_GRID_SIZE).roundToInt() * HUD_GRID_SIZE - origin
        return rawDelta
    }

    fun end() {
        context = null
        startRect = null
        grabbedRect = null
        result = HudSnapResult.NONE
    }
}
