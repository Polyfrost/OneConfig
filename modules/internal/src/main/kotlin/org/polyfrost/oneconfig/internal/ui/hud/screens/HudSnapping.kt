package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudAnchor
import org.polyfrost.oneconfig.api.hud.v1.HudSnapEdge
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val HUD_SNAP_SCREEN_PADDING = 4f
internal const val HUD_GRID_SIZE = 8f
internal const val HUD_MERGE_TOUCH = 2f

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

internal val HudAnchor.edgeX: HudSnapEdge get() = when (this) {
    HudAnchor.Auto -> HudSnapEdge.None
    HudAnchor.TopLeft, HudAnchor.Left, HudAnchor.BottomLeft -> HudSnapEdge.Start
    HudAnchor.Top, HudAnchor.Center, HudAnchor.Bottom -> HudSnapEdge.Center
    HudAnchor.TopRight, HudAnchor.Right, HudAnchor.BottomRight -> HudSnapEdge.End
}

internal val HudAnchor.edgeY: HudSnapEdge get() = when (this) {
    HudAnchor.Auto -> HudSnapEdge.None
    HudAnchor.TopLeft, HudAnchor.Top, HudAnchor.TopRight -> HudSnapEdge.Start
    HudAnchor.Left, HudAnchor.Center, HudAnchor.Right -> HudSnapEdge.Center
    HudAnchor.BottomLeft, HudAnchor.Bottom, HudAnchor.BottomRight -> HudSnapEdge.End
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
    private val alignRects: List<Rect> = obstacles,
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
        val lines = ArrayList<HudSnapLine>(alignRects.size * 3 + 5)
        lines.add(HudSnapLine(0f, 0f, crossExtent))
        lines.add(HudSnapLine(extent / 2f, 0f, crossExtent))
        lines.add(HudSnapLine(extent, 0f, crossExtent))
        lines.add(HudSnapLine(HUD_SNAP_SCREEN_PADDING, 0f, crossExtent, center = false, end = false))
        lines.add(HudSnapLine(extent - HUD_SNAP_SCREEN_PADDING, 0f, crossExtent, start = false, center = false))
        for (o in alignRects) {
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
            if (size > HUD_MERGE_TOUCH) gaps.add(HudGapRef(size, a, b))
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

    /** Which part of [rect] sits on a guide along one axis, favouring [prefer] and then the centre when several do */
    fun edge(rect: Rect, h: Boolean, prefer: HudSnapEdge): HudSnapEdge {
        val lo = rect.lo(h)
        val hi = rect.hi(h)
        val mid = (lo + hi) / 2f
        var start = false
        var center = false
        var end = false
        for (line in lines(h)) {
            val p = line.position
            if (line.start && abs(p - lo) < ALIGN_EPSILON) start = true
            if (line.center && abs(p - mid) < ALIGN_EPSILON) center = true
            if (line.end && abs(p - hi) < ALIGN_EPSILON) end = true
        }
        return when {
            prefer == HudSnapEdge.Start && start -> HudSnapEdge.Start
            prefer == HudSnapEdge.Center && center -> HudSnapEdge.Center
            prefer == HudSnapEdge.End && end -> HudSnapEdge.End
            center -> HudSnapEdge.Center
            start -> HudSnapEdge.Start
            end -> HudSnapEdge.End
            else -> HudSnapEdge.None
        }
    }

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
        val gapBefore = before?.let { rect.lo(h) - it.hi(h) }?.takeIf { it > HUD_MERGE_TOUCH }
        val gapAfter = after?.let { it.lo(h) - rect.hi(h) }?.takeIf { it > HUD_MERGE_TOUCH }
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
    private var snapped = false
    private var delta = Offset.Zero

    var startEdges: Map<Hud, Pair<HudSnapEdge, HudSnapEdge>> = emptyMap()

    var result by mutableStateOf(HudSnapResult.NONE)
        private set

    private fun union(rects: List<Rect>) = rects.reduceOrNull { a, b ->
        Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
    }

    fun startsWith(dragged: List<Rect>): Boolean = context != null && union(dragged) == startRect

    fun begin(
        dragged: List<Rect>,
        grabbed: Rect?,
        shapes: List<List<Rect>>,
        screenWidth: Float,
        screenHeight: Float,
    ) {
        val group = union(dragged)
        startRect = group
        grabbedRect = grabbed?.takeIf { group != null && it != group }
        val outlines = shapes.mapNotNull(::union)
        val members = shapes.flatMap { if (it.size > 1) it else emptyList() }
        context = HudSnapContext(Rect(0f, 0f, screenWidth, screenHeight), outlines, outlines + members)

        result = HudSnapResult.NONE
        snapped = false
        delta = Offset.Zero
    }

    fun move(rawDx: Float, rawDy: Float, threshold: Float, snapping: Boolean, gridEnabled: Boolean): Offset {
        val ctx = context
        val start = startRect
        if (ctx == null || start == null) return Offset(rawDx, rawDy)
        snapped = snapping
        if (!snapping) {
            delta = Offset(rawDx, rawDy)
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
        delta = Offset(dx, dy)
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

    /**
     * The edges a HUD that began the drag at [startRect] with [start] edges ends up with, decided from where the
     * drag left it: an axis on a guide takes that guide's edge, an axis back where it began keeps its start edge
     * and any other axis has none. [prefer] breaks ties when several edges sit on guides
     *
     * Measured from where the drag aimed the HUD because HUDs sit on whole pixels and can land half a pixel
     * off the guide
     */
    fun edges(startRect: Rect, prefer: HudAnchor, start: Pair<HudSnapEdge, HudSnapEdge>): Pair<HudSnapEdge, HudSnapEdge> {
        val ctx = context
        val aimed = startRect.translate(delta)
        fun axis(h: Boolean, offset: Float, prefer: HudSnapEdge, start: HudSnapEdge): HudSnapEdge {
            val guide = if (ctx != null && snapped) {
                ctx.edge(aimed, h, start.takeUnless { it == HudSnapEdge.None } ?: prefer)
            } else HudSnapEdge.None
            return when {
                guide != HudSnapEdge.None -> guide
                offset == 0f -> start
                else -> HudSnapEdge.None
            }
        }
        return axis(true, delta.x, prefer.edgeX, start.first) to axis(false, delta.y, prefer.edgeY, start.second)
    }

    fun end() {
        snapped = false
        delta = Offset.Zero
        startEdges = emptyMap()
        context = null
        startRect = null
        grabbedRect = null
        result = HudSnapResult.NONE
    }
}
