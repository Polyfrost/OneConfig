/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.hud.v1

import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyGradient
import org.polyfrost.compose.render.PolyPath
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class MergeAxis { X, Y }

/**
 * Fuses the backgrounds of HUDs which sit right next to each other into a single shape
 *
 * A column of HUDs then reads as one panel instead of a stack of separate cards
 *
 * Shared edges lose their corners
 *
 * Where two neighbours differ in size the step between them gets a concave fillet
 *
 * Only HUDs which share the same background colour and chroma and touch along an edge are fused
 *
 * Each outer corner keeps the radius of the HUD it belongs to and a step takes the smaller radius of the two HUDs
 * meeting there
 *
 * Everything else keeps drawing its own rounded rectangle
 */
internal object HudBackgroundMerge {
    /** Distance in gui units between two edges still counted as "touching" */
    private const val TOUCH_TOLERANCE = 2f

    /** How much two HUDs must overlap along a shared edge before they are fused */
    private const val MIN_OVERLAP = 2f

    private const val EPS = 0.01f

    /** Backstop for walking anchors set by hand while checking a link would not close a loop */
    private const val MAX_LINK_DEPTH = 16

    private const val PIN_X = 1

    private const val PIN_Y = 2

    private const val BLEND = 8f

    private const val MAX_BLEND_STEPS = 8

    private const val CLIP_MARGIN = 4f

    /**
     * A HUD in a fused shape held against the neighbour it touches on one [axis]
     *
     * [childPoint] on the child's box is the corner or edge midpoint that meets [parentPoint] on
     * the parent's box
     */
    class Link(
        val child: Hud,
        val parent: Hud,
        val childPoint: HudAnchor,
        val parentPoint: HudAnchor,
        val axis: MergeAxis,
    )

    class Group(
        val huds: List<Hud>,
        /** Spanning trees over the group's contacts so every HUD follows the one it is fused to */
        val links: List<Link>,
        private val bgColor: Int,
        val chroma: Boolean,
        private val chromaSpeed: Float,
        private val loops: List<Loop>,
        private val shading: Shading? = null,
    ) {
        private val path by lazy(LazyThreadSafetyMode.NONE) {
            PolyPath().also { pb -> for (loop in loops) emitLoop(pb, loop) }
        }

        private val fill by lazy(LazyThreadSafetyMode.NONE) { PolyColor(bgColor, chroma, chromaSpeed) }

        fun draw(ctx: RenderContext) {
            val shading = shading
            if (shading == null) ctx.fillPath(path, fill) else shading.draw(ctx, path)
        }
    }

    class Shading(
        private val alongY: Boolean,
        private val left: Float,
        private val top: Float,
        private val right: Float,
        private val bottom: Float,
        private val cuts: FloatArray,
        private val gradients: List<PolyGradient>,
    ) {
        fun draw(ctx: RenderContext, path: PolyPath) {
            val w = right - left
            val h = bottom - top
            if (gradients.size == 1) {
                ctx.renderer.fillPathGradient(path, left, top, w, h, gradients[0])
                return
            }
            val m = ctx.deviceTransform()
            val scale = if (m == null) 0f else if (alongY) m[0] else m[1]
            val offset = if (m == null) 0f else if (alongY) m[2] else m[3]
            val nudge = if (scale > 0f) 0.01f / scale else 0f
            fun snap(v: Float) = if (scale > 0f) ((v * scale + offset).roundToInt() - offset) / scale else v
            var start = (if (alongY) left else top) - CLIP_MARGIN
            for (k in gradients.indices) {
                val last = k == cuts.size
                val end = if (last) (if (alongY) right else bottom) + CLIP_MARGIN else snap(cuts[k])
                if (end <= start) continue
                val lo = if (k == 0) start else start + nudge
                val hi = if (last) end else end - nudge
                ctx.save()
                if (alongY) {
                    ctx.clipRect(lo, top - CLIP_MARGIN, hi - lo, h + CLIP_MARGIN * 2f)
                } else {
                    ctx.clipRect(left - CLIP_MARGIN, lo, w + CLIP_MARGIN * 2f, hi - lo)
                }
                ctx.renderer.fillPathGradient(path, left, top, w, h, gradients[k])
                ctx.restore()
                start = end
            }
        }
    }

    fun computeGroups(huds: List<Hud>, refWidth: Float, refHeight: Float): List<Group> {
        val items = ArrayList<Item>(huds.size)
        for (hud in huds) {
            if (!canMerge(hud)) continue
            items.add(
                Item(
                    hud, hud.x, hud.y, hud.renderedW, hud.renderedH, hud.bgRadius * hud.effectiveScale,
                    hud.layoutX(refWidth), hud.layoutY(refHeight),
                )
            )
        }
        if (items.size < 2) return emptyList()

        val parent = IntArray(items.size) { it }
        val neighbours = HashMap<Int, MutableList<Contact>>()
        for (i in items.indices) {
            for (j in i + 1 until items.size) {
                val a = items[i]
                val b = items[j]
                if (!a.canFuse(b)) continue
                val pins = contactPins(a, b)
                if (pins == 0) continue
                union(parent, i, j)
                neighbours.getOrPut(i) { ArrayList(2) }.add(Contact(j, pins))
                neighbours.getOrPut(j) { ArrayList(2) }.add(Contact(i, pins))
            }
        }

        val components = LinkedHashMap<Int, MutableList<Int>>()
        for (i in items.indices) components.getOrPut(find(parent, i)) { ArrayList() }.add(i)

        val out = ArrayList<Group>(components.size)
        for (component in components.values) {
            if (component.size < 2) continue
            val members = component.map { items[it] }
            val rects = snapEdges(members.map { floatArrayOf(it.x, it.y, it.w, it.h) })
            val loops = trace(rects, FloatArray(members.size) { members[it].radius })
            if (loops.isEmpty()) continue
            val first = members[0]
            out.add(
                Group(
                    members.map { it.hud },
                    axisLinks(items, component, neighbours, MergeAxis.X) +
                        axisLinks(items, component, neighbours, MergeAxis.Y),
                    first.hud.bgColor, first.hud.bgChroma, first.hud.bgChromaSpeed, loops,
                    shading(rects, IntArray(members.size) { members[it].hud.bgColor }),
                )
            )
        }
        return out
    }

    @OptIn(ExperimentalOneConfigUiApi::class)
    fun canMerge(hud: Hud): Boolean =
        hud !is LegacyHudMarker && hud.canMergeBackground() &&
            hud.showBackground && hud.mergeBackground && hud.hasBackground() && !hud.borderEnabled &&
            (!hud.hidden || hud.keepBgWhenHidden) &&
            hud.renderedW > 0f && hud.renderedH > 0f

    fun sameStyle(a: Hud, b: Hud): Boolean =
        a.bgColor == b.bgColor && a.bgChroma == b.bgChroma &&
            (!a.bgChroma || abs(a.bgChromaSpeed - b.bgChromaSpeed) <= EPS)

    fun canFuse(a: Hud, b: Hud): Boolean =
        sameStyle(a, b) || (a.mergeAcrossColours && b.mergeAcrossColours && !a.bgChroma && !b.bgChroma)

    private class Contact(val other: Int, val pins: Int)

    private fun axisLinks(
        items: List<Item>,
        component: List<Int>,
        neighbours: Map<Int, List<Contact>>,
        axis: MergeAxis,
    ): List<Link> {
        val pin = if (axis == MergeAxis.X) PIN_X else PIN_Y
        val out = ArrayList<Link>(component.size - 1)
        val index = IdentityHashMap<Hud, Int>(component.size * 2)
        for (i in component) index[items[i].hud] = i
        val parentOf = HashMap<Int, Int>(component.size * 2)

        // whether following [from] up the links being built here and then hand-set anchors ever
        // arrives back at [target] which is the loop a link must never close
        fun reaches(from: Int, target: Hud): Boolean {
            var hud: Hud? = items[from].hud
            var depth = 0
            while (hud != null && depth < component.size + MAX_LINK_DEPTH) {
                if (hud === target) return true
                val idx = index[hud]
                val linked = if (idx == null) null else parentOf[idx]
                hud = if (linked != null) items[linked].hud else hud.anchorParent
                depth++
            }
            return false
        }

        val pivot = HashMap<Int, Float>(component.size * 2)
        val seen = HashSet<Int>(component.size * 2)
        for (i in component) {
            if (!items[i].hud.isAnchored) continue
            seen.add(i)
            pivot[i] = fraction(items[i].hud.effectiveSelfAnchorPoint, axis)
        }

        while (true) {
            var bestParent = -1
            var bestChild = -1
            var bestPinned = false
            for (cur in seen) {
                for (contact in neighbours[cur].orEmpty()) {
                    val n = contact.other
                    if (n in seen) continue
                    val pinned = contact.pins and pin != 0
                    if (bestChild != -1 && (bestPinned || !pinned)) continue
                    if (reaches(cur, items[n].hud)) continue
                    bestParent = cur
                    bestChild = n
                    bestPinned = pinned
                }
            }
            if (bestChild == -1) {
                val next = component.firstOrNull { it !in seen } ?: break
                seen.add(next)
                pivot[next] = fraction(items[next].hud.effectiveGrowthAnchor, axis)
                continue
            }
            seen.add(bestChild)
            parentOf[bestChild] = bestParent
            val child = items[bestChild]
            val (childPoint, parentPoint) = contactPoints(
                child, items[bestParent], axis,
                fraction(child.hud.effectiveGrowthAnchor, axis), pivot.getValue(bestParent),
            )
            pivot[bestChild] = fraction(childPoint, axis)
            out.add(Link(child.hud, items[bestParent].hud, childPoint, parentPoint, axis))
        }
        return out
    }

    /**
     * The pair of box points where [child] meets [parent]
     *
     * The edges that touch decide one axis and the edges that line up along the shared edge decide
     * the other
     *
     * So a HUD stacked flush under another links its top-left to the parent's bottom-left
     */
    private fun contactPoints(
        child: Item,
        parent: Item,
        axis: MergeAxis,
        childPivot: Float,
        parentPivot: Float,
    ): Pair<HudAnchor, HudAnchor> {
        var childFx = Float.NaN
        var parentFx = Float.NaN
        var childFy = Float.NaN
        var parentFy = Float.NaN

        if (abs(child.y - parent.bottom) <= TOUCH_TOLERANCE) {
            childFy = 0f
            parentFy = 1f
        } else if (abs(child.bottom - parent.y) <= TOUCH_TOLERANCE) {
            childFy = 1f
            parentFy = 0f
        }
        if (abs(child.x - parent.right) <= TOUCH_TOLERANCE) {
            childFx = 0f
            parentFx = 1f
        } else if (abs(child.right - parent.x) <= TOUCH_TOLERANCE) {
            childFx = 1f
            parentFx = 0f
        }

        if (childFx.isNaN()) {
            if (axis == MergeAxis.X) {
                childFx = crossSide(child.x, child.right, parent.x, parent.right, childPivot, parentPivot)
                parentFx = parentPivot
            } else {
                // when stacked line the link up with whichever side edges are flush else their middles
                val side = alignedSide(child.x, child.right, parent.x, parent.right)
                childFx = side
                parentFx = side
            }
        }
        if (childFy.isNaN()) {
            if (axis == MergeAxis.Y) {
                childFy = crossSide(child.y, child.bottom, parent.y, parent.bottom, childPivot, parentPivot)
                parentFy = parentPivot
            } else {
                val side = alignedSide(child.y, child.bottom, parent.y, parent.bottom)
                childFy = side
                parentFy = side
            }
        }
        return anchorAt(childFx, childFy) to anchorAt(parentFx, parentFy)
    }

    private fun alignedSide(aMin: Float, aMax: Float, bMin: Float, bMax: Float): Float = when {
        abs(aMin - bMin) <= TOUCH_TOLERANCE -> 0f
        abs(aMax - bMax) <= TOUCH_TOLERANCE -> 1f
        else -> 0.5f
    }

    private fun crossSide(
        cMin: Float,
        cMax: Float,
        pMin: Float,
        pMax: Float,
        childPivot: Float,
        parentPivot: Float,
    ): Float {
        val minFlush = abs(cMin - pMin) <= TOUCH_TOLERANCE
        val maxFlush = abs(cMax - pMax) <= TOUCH_TOLERANCE
        return when {
            minFlush && parentPivot == 0f -> 0f
            maxFlush && parentPivot == 1f -> 1f
            minFlush -> 0f
            maxFlush -> 1f
            else -> childPivot
        }
    }

    private fun fraction(anchor: HudAnchor, axis: MergeAxis): Float = if (axis == MergeAxis.X) {
        when (anchor) {
            HudAnchor.TopLeft, HudAnchor.Left, HudAnchor.BottomLeft -> 0f
            HudAnchor.TopRight, HudAnchor.Right, HudAnchor.BottomRight -> 1f
            else -> 0.5f
        }
    } else {
        when (anchor) {
            HudAnchor.TopLeft, HudAnchor.Top, HudAnchor.TopRight -> 0f
            HudAnchor.BottomLeft, HudAnchor.Bottom, HudAnchor.BottomRight -> 1f
            else -> 0.5f
        }
    }

    private fun anchorAt(fx: Float, fy: Float): HudAnchor = when (fy) {
        0f -> when (fx) {
            0f -> HudAnchor.TopLeft
            0.5f -> HudAnchor.Top
            else -> HudAnchor.TopRight
        }
        0.5f -> when (fx) {
            0f -> HudAnchor.Left
            0.5f -> HudAnchor.Center
            else -> HudAnchor.Right
        }
        else -> when (fx) {
            0f -> HudAnchor.BottomLeft
            0.5f -> HudAnchor.Bottom
            else -> HudAnchor.BottomRight
        }
    }

    private class Item(
        val hud: Hud,
        var x: Float,
        var y: Float,
        var w: Float,
        var h: Float,
        val radius: Float,
        val layoutX: Float,
        val layoutY: Float,
    ) {
        val right get() = x + w
        val bottom get() = y + h

        val layoutRight get() = layoutX + w
        val layoutBottom get() = layoutY + h

        fun canFuse(other: Item): Boolean = canFuse(hud, other.hud)
    }

    private fun contactPins(a: Item, b: Item): Int {
        val ay = a.layoutY
        val by = b.layoutY
        val ax = a.layoutX
        val bx = b.layoutX
        val aBottom = a.layoutBottom
        val bBottom = b.layoutBottom
        val aRight = a.layoutRight
        val bRight = b.layoutRight
        val sharesRow = overlap(ay, aBottom, by, bBottom) >= MIN_OVERLAP
        val sharesColumn = overlap(ax, aRight, bx, bRight) >= MIN_OVERLAP
        val stacked = abs(aBottom - by) <= TOUCH_TOLERANCE || abs(bBottom - ay) <= TOUCH_TOLERANCE
        val sideBySide = abs(aRight - bx) <= TOUCH_TOLERANCE || abs(bRight - ax) <= TOUCH_TOLERANCE
        val stackedOnTop = stacked && sharesColumn
        val rowNeighbours = sideBySide && sharesRow
        when {
            stackedOnTop -> if (nests(ax, aRight, bx, bRight)) return PIN_Y
            rowNeighbours -> if (nests(ay, aBottom, by, bBottom)) return PIN_X
        }
        if (!stackedOnTop && !rowNeighbours && !(stacked && sideBySide)) return 0
        if (!a.hud.mergeDiagonally || !b.hud.mergeDiagonally) return 0
        return PIN_X or PIN_Y
    }

    private fun nests(aMin: Float, aMax: Float, bMin: Float, bMax: Float): Boolean =
        (aMin >= bMin - TOUCH_TOLERANCE && aMax <= bMax + TOUCH_TOLERANCE) ||
            (bMin >= aMin - TOUCH_TOLERANCE && bMax <= aMax + TOUCH_TOLERANCE)

    private fun overlap(aMin: Float, aMax: Float, bMin: Float, bMax: Float): Float =
        min(aMax, bMax) - maxOf(aMin, bMin)

    private fun union(parent: IntArray, a: Int, b: Int) {
        val ra = find(parent, a)
        val rb = find(parent, b)
        if (ra != rb) parent[rb] = ra
    }

    private fun find(parent: IntArray, i: Int): Int {
        var n = i
        while (parent[n] != n) {
            parent[n] = parent[parent[n]]
            n = parent[n]
        }
        return n
    }

    /** One closed outline of a fused shape as x, y pairs with the corner radius at each point */
    class Loop(val points: FloatArray, val radii: FloatArray)

    /** Outlines the union of [rects] given as x, y, width, height where each rect rounds its own corners by [radii] */
    fun outlines(rects: List<FloatArray>, radii: FloatArray): List<Loop> = trace(snapEdges(rects), radii)

    private fun snapEdges(rects: List<FloatArray>): List<FloatArray> {
        val xs = snapMap(rects.flatMap { listOf(it[0], it[0] + it[2]) })
        val ys = snapMap(rects.flatMap { listOf(it[1], it[1] + it[3]) })
        return rects.map {
            val left = xs.getValue(it[0])
            val right = xs.getValue(it[0] + it[2])
            val top = ys.getValue(it[1])
            val bottom = ys.getValue(it[1] + it[3])
            floatArrayOf(left, top, right - left, bottom - top)
        }
    }

    private fun snapMap(values: List<Float>): Map<Float, Float> {
        val sorted = values.distinct().sorted()
        val out = HashMap<Float, Float>(sorted.size)
        var i = 0
        while (i < sorted.size) {
            var j = i + 1
            while (j < sorted.size && sorted[j] - sorted[j - 1] <= TOUCH_TOLERANCE) j++
            val cluster = sorted.subList(i, j)
            val mean = cluster.sum() / cluster.size
            for (v in cluster) out[v] = mean
            i = j
        }
        return out
    }

    private fun trace(rects: List<FloatArray>, radii: FloatArray): List<Loop> {
        val xs = rects.flatMap { listOf(it[0], it[0] + it[2]) }.distinct().sorted()
        val ys = rects.flatMap { listOf(it[1], it[1] + it[3]) }.distinct().sorted()
        val nx = xs.size - 1
        val ny = ys.size - 1
        if (nx <= 0 || ny <= 0) return emptyList()

        val covered = Array(nx) { BooleanArray(ny) }
        for (i in 0 until nx) {
            val cx = (xs[i] + xs[i + 1]) / 2f
            for (j in 0 until ny) {
                val cy = (ys[j] + ys[j + 1]) / 2f
                covered[i][j] = rects.any { cx > it[0] && cx < it[0] + it[2] && cy > it[1] && cy < it[1] + it[3] }
            }
        }

        fun cov(i: Int, j: Int) = i in 0 until nx && j in 0 until ny && covered[i][j]

        // overlapping HUDs can share a cell so the HUD whose own corner is at the point wins
        fun ownerRadius(i: Int, j: Int, x: Float, y: Float): Float {
            if (!cov(i, j)) return 0f
            val cx = (xs[i] + xs[i + 1]) / 2f
            val cy = (ys[j] + ys[j + 1]) / 2f
            var fallback = -1
            for (k in rects.indices) {
                val r = rects[k]
                if (cx <= r[0] || cx >= r[0] + r[2] || cy <= r[1] || cy >= r[1] + r[3]) continue
                val cornerX = abs(x - r[0]) <= EPS || abs(x - (r[0] + r[2])) <= EPS
                val cornerY = abs(y - r[1]) <= EPS || abs(y - (r[1] + r[3])) <= EPS
                if (cornerX && cornerY) return radii[k]
                if (fallback < 0) fallback = k
            }
            return if (fallback < 0) 0f else radii[fallback]
        }

        fun cornerRadius(px: Int, py: Int, cx: Int, cy: Int, bx: Int, by: Int): Float {
            // the cell between the incoming edge reversed and the outgoing edge is inside a convex corner
            // and is the empty notch of a concave one
            val i = if (bx - cx - (cx - px) > 0) cx else cx - 1
            val j = if (by - cy - (cy - py) > 0) cy else cy - 1
            val x = xs[cx]
            val y = ys[cy]
            if (cov(i, j)) return ownerRadius(i, j, x, y)
            val otherI = if (i == cx) cx - 1 else cx
            val otherJ = if (j == cy) cy - 1 else cy
            return minOf(ownerRadius(otherI, j, x, y), ownerRadius(i, otherJ, x, y))
        }

        // directed clockwise in screen space with y down so every loop keeps the filled area on its right
        val next = HashMap<Int, MutableList<Int>>()
        fun node(i: Int, j: Int) = i * (ny + 1) + j
        fun edge(i0: Int, j0: Int, i1: Int, j1: Int) {
            next.getOrPut(node(i0, j0)) { ArrayList(2) }.add(node(i1, j1))
        }
        for (i in 0 until nx) {
            for (j in 0 until ny) {
                if (!covered[i][j]) continue
                if (!cov(i, j - 1)) edge(i, j, i + 1, j)
                if (!cov(i + 1, j)) edge(i + 1, j, i + 1, j + 1)
                if (!cov(i, j + 1)) edge(i + 1, j + 1, i, j + 1)
                if (!cov(i - 1, j)) edge(i, j + 1, i, j)
            }
        }

        val loops = ArrayList<Loop>()
        while (next.isNotEmpty()) {
            val start = next.keys.first()
            val points = ArrayList<Int>()
            var current = start
            var closed = false
            while (true) {
                val outgoing = next[current] ?: break
                val to = outgoing.removeAt(outgoing.size - 1)
                if (outgoing.isEmpty()) next.remove(current)
                points.add(current)
                current = to
                if (current == start) {
                    closed = true
                    break
                }
            }
            if (!closed || points.size < 4) continue
            val coords = ArrayList<Float>(points.size * 2)
            val corners = ArrayList<Float>(points.size)
            for (k in points.indices) {
                val prev = points[(k - 1 + points.size) % points.size]
                val cur = points[k]
                val nxt = points[(k + 1) % points.size]
                val px = prev / (ny + 1)
                val py = prev % (ny + 1)
                val cx = cur / (ny + 1)
                val cy = cur % (ny + 1)
                val bx = nxt / (ny + 1)
                val by = nxt % (ny + 1)
                if ((px == cx && cx == bx) || (py == cy && cy == by)) continue
                coords.add(xs[cx])
                coords.add(ys[cy])
                corners.add(cornerRadius(px, py, cx, cy, bx, by))
            }
            loops.add(Loop(coords.toFloatArray(), corners.toFloatArray()))
        }
        return loops
    }

    private class Profile(val stops: FloatArray, val colors: IntArray) {
        fun sample(t: Float): Int {
            if (t <= stops[0]) return colors[0]
            for (i in 1 until stops.size) {
                if (t > stops[i]) continue
                val a = stops[i - 1]
                val b = stops[i]
                return if (b > a) PolyGradient.lerpArgb(colors[i - 1], colors[i], (t - a) / (b - a)) else colors[i]
            }
            return colors[colors.size - 1]
        }

        override fun equals(other: Any?): Boolean =
            other is Profile && stops.contentEquals(other.stops) && colors.contentEquals(other.colors)

        override fun hashCode(): Int = stops.contentHashCode() * 31 + colors.contentHashCode()
    }

    private fun shading(rects: List<FloatArray>, colors: IntArray): Shading? {
        if (colors.all { it == colors[0] }) return null
        val xs = rects.flatMap { listOf(it[0], it[0] + it[2]) }.distinct().sorted()
        val ys = rects.flatMap { listOf(it[1], it[1] + it[3]) }.distinct().sorted()
        val nx = xs.size - 1
        val ny = ys.size - 1
        if (nx <= 0 || ny <= 0) return null
        val owner = Array(nx) { i ->
            val cx = (xs[i] + xs[i + 1]) / 2f
            IntArray(ny) { j ->
                val cy = (ys[j] + ys[j + 1]) / 2f
                rects.indexOfFirst { cx > it[0] && cx < it[0] + it[2] && cy > it[1] && cy < it[1] + it[3] }
            }
        }
        val transposed = Array(ny) { j -> IntArray(nx) { i -> owner[i][j] } }
        val byColumns = plan(xs, ys, owner, colors)
        val byRows = plan(ys, xs, transposed, colors)
        val alongY = byRows.size >= byColumns.size
        val best = if (alongY) byColumns else byRows
        val left = xs[0]
        val top = ys[0]
        val right = xs[nx]
        val bottom = ys[ny]
        val cuts = FloatArray(best.size - 1) { best[it].first }
        val gradients = best.map { (_, profile) ->
            PolyGradient.Points(
                profile.colors.map { PolyColor(it) }, profile.stops.asList(),
                left, top, if (alongY) left else right, if (alongY) bottom else top,
            )
        }
        return Shading(alongY, left, top, right, bottom, cuts, gradients)
    }

    private fun plan(split: List<Float>, along: List<Float>, owner: Array<IntArray>, colors: IntArray): List<Pair<Float, Profile>> {
        val ns = split.size - 1
        val na = along.size - 1
        val grid = Array(ns) { IntArray(na) { colors[0] } }
        for (b in 0 until na) {
            for (a in 0 until ns) {
                var best = -1
                var dist = Int.MAX_VALUE
                for (o in 0 until ns) {
                    val k = owner[o][b]
                    if (k >= 0 && abs(o - a) < dist) {
                        dist = abs(o - a)
                        best = k
                    }
                }
                if (best >= 0) grid[a][b] = colors[best]
            }
        }

        val lo = along[0]
        val span = along[na] - lo
        val stripEnd = ArrayList<Float>(ns)
        val stripProfile = ArrayList<Profile>(ns)
        for (a in 0 until ns) {
            val profile = profile(along, grid[a], lo, span)
            if (stripProfile.isNotEmpty() && stripProfile[stripProfile.size - 1] == profile) {
                stripEnd[stripEnd.size - 1] = split[a + 1]
            } else {
                stripProfile.add(profile)
                stripEnd.add(split[a + 1])
            }
        }

        val out = ArrayList<Pair<Float, Profile>>()
        var prevEnd = split[0]
        for (s in stripProfile.indices) {
            val p = stripProfile[s]
            if (s == stripProfile.size - 1) {
                out.add(Float.NaN to p)
                break
            }
            val q = stripProfile[s + 1]
            val seam = stripEnd[s]
            val before = min(BLEND, (seam - (if (s == 0) split[0] else stripEnd[s - 1])) / 2f)
            val after = min(BLEND, (stripEnd[s + 1] - seam) / 2f)
            if (seam - before - prevEnd > EPS) out.add(seam - before to p)
            val steps = ceil((before + after) / 2f).toInt().coerceIn(2, MAX_BLEND_STEPS)
            val step = (before + after) / steps
            for (m in 0 until steps) {
                var end = seam - before + step * (m + 1)
                if (m < steps - 1 && step >= 2f && abs(end - seam) < 1f) end = if (end < seam) seam - 1f else seam + 1f
                out.add(end to mix(p, q, (m + 0.5f) / steps))
            }
            prevEnd = seam + after
        }
        return out
    }

    private fun profile(edges: List<Float>, cells: IntArray, lo: Float, span: Float): Profile {
        val runStart = ArrayList<Float>()
        val runColor = ArrayList<Int>()
        for (b in cells.indices) {
            if (b == 0 || cells[b] != cells[b - 1]) {
                runStart.add(edges[b])
                runColor.add(cells[b])
            }
        }
        val stops = ArrayList<Float>(runStart.size * 2 + 2)
        val out = ArrayList<Int>(runStart.size * 2 + 2)
        stops.add(0f)
        out.add(runColor[0])
        for (r in 1 until runStart.size) {
            val seam = runStart[r]
            val next = if (r + 1 < runStart.size) runStart[r + 1] else edges[edges.size - 1]
            val before = min(BLEND, (seam - runStart[r - 1]) / 2f)
            val after = min(BLEND, (next - seam) / 2f)
            stops.add((seam - before - lo) / span)
            out.add(runColor[r - 1])
            stops.add((seam + after - lo) / span)
            out.add(runColor[r])
        }
        stops.add(1f)
        out.add(runColor[runColor.size - 1])
        return Profile(stops.toFloatArray(), out.toIntArray())
    }

    private fun mix(p: Profile, q: Profile, t: Float): Profile {
        val stops = (p.stops.asList() + q.stops.asList()).distinct().sorted()
        return Profile(
            stops.toFloatArray(),
            IntArray(stops.size) { PolyGradient.lerpArgb(p.sample(stops[it]), q.sample(stops[it]), t) },
        )
    }

    private fun emitLoop(pb: PolyPath, loop: Loop) {
        val points = loop.points
        val n = points.size / 2
        if (n < 3) return
        fun px(k: Int) = points[(k % n) * 2]
        fun py(k: Int) = points[(k % n) * 2 + 1]

        pb.moveTo((px(0) + px(1)) / 2f, (py(0) + py(1)) / 2f)
        for (k in 1..n) {
            val cx = px(k)
            val cy = py(k)
            val prevLen = hypot(cx - px(k - 1), cy - py(k - 1))
            val nextLen = hypot(px(k + 1) - cx, py(k + 1) - cy)
            val r = minOf(loop.radii[k % n], prevLen / 2f, nextLen / 2f)
            val mx = (cx + px(k + 1)) / 2f
            val my = (cy + py(k + 1)) / 2f
            if (r <= EPS) {
                pb.lineTo(cx, cy)
                pb.lineTo(mx, my)
            } else {
                pb.tangentArcTo(cx, cy, mx, my, r)
            }
        }
        pb.close()
    }

    @OptIn(ExperimentalOneConfigUiApi::class)
    fun layoutKey(huds: List<Hud>): Int {
        var key = huds.size
        for (i in huds.indices) {
            val hud = huds[i]
            key = key * 31 + (hud.x * 4f).roundToInt()
            key = key * 31 + (hud.y * 4f).roundToInt()
            key = key * 31 + (hud.renderedW * 4f).roundToInt()
            key = key * 31 + (hud.renderedH * 4f).roundToInt()
            key = key * 31 + hud.bgColor
            key = key * 31 + (hud.bgRadius * hud.effectiveScale * 4f).roundToInt()
            key = key * 31 + (if (hud.showBackground) 1 else 0)
            key = key * 31 + (if (hud.mergeBackground) 1 else 0)
            key = key * 31 + (if (hud.mergeDiagonally) 1 else 0)
            key = key * 31 + (if (hud.mergeAcrossColours) 1 else 0)
            key = key * 31 + (if (hud.bgChroma) 1 else 0)
            key = key * 31 + (if (hud.hidden) 1 else 0)
            key = key * 31 + (if (hud.keepBgWhenHidden) 1 else 0)
            key = key * 31 + (if (hud.borderEnabled) 1 else 0)
            key = key * 31 + hud.effectiveGrowthAnchor.ordinal
            key = key * 31 + hud.effectiveSelfAnchorPoint.ordinal
        }
        return key
    }
}
