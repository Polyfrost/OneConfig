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

import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PathBuilder
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.RenderContext
import kotlin.math.abs
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
 * Only HUDs which share the same background style (colour chroma and radius) and touch along an
 * edge are fused
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
        private val chroma: Boolean,
        private val chromaSpeed: Float,
        private val radius: Float,
        private val loops: List<FloatArray>,
    ) {
        fun draw(ctx: RenderContext) {
            val color = PolyColor(bgColor, chroma, chromaSpeed)
            if (color.alpha == 0) return
            val paint = ctx.paint
            paint.color = color.argb
            paint.mode = PaintMode.FILL
            PathBuilder().use { pb ->
                for (loop in loops) emitLoop(pb, loop, radius)
                pb.snapshot().use { ctx.canvas.drawPath(it, paint) }
            }
        }
    }

    fun computeGroups(huds: List<Hud>, refWidth: Float, refHeight: Float): List<Group> {
        val items = ArrayList<Item>(huds.size)
        for (hud in huds) {
            if (hud is LegacyHudMarker) continue
            if (!hud.canMergeBackground()) continue
            if (!hud.showBackground || !hud.mergeBackground || !hud.hasBackground()) continue
            if (hud.hidden && !hud.keepBgWhenHidden) continue
            val w = hud.renderedW
            val h = hud.renderedH
            if (w <= 0f || h <= 0f) continue
            items.add(
                Item(
                    hud, hud.x, hud.y, w, h, hud.bgRadius * hud.effectiveScale,
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
                if (!a.sameStyle(b)) continue
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
            val loops = outlines(members.map { floatArrayOf(it.x, it.y, it.w, it.h) })
            if (loops.isEmpty()) continue
            val first = members[0]
            out.add(
                Group(
                    members.map { it.hud },
                    axisLinks(items, component, neighbours, MergeAxis.X) +
                        axisLinks(items, component, neighbours, MergeAxis.Y),
                    first.hud.bgColor, first.hud.bgChroma, first.hud.bgChromaSpeed, first.radius, loops,
                )
            )
        }
        return out
    }

    private class Contact(val other: Int, val pins: Int)

    private fun axisLinks(
        items: List<Item>,
        component: List<Int>,
        neighbours: Map<Int, List<Contact>>,
        axis: MergeAxis,
    ): List<Link> {
        val pin = if (axis == MergeAxis.X) PIN_X else PIN_Y
        val out = ArrayList<Link>(component.size - 1)
        val index = java.util.IdentityHashMap<Hud, Int>(component.size * 2)
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

        val seen = HashSet<Int>(component.size * 2)
        for (i in component) if (items[i].hud.isAnchored) seen.add(i)

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
                continue
            }
            seen.add(bestChild)
            parentOf[bestChild] = bestParent
            val (childPoint, parentPoint) = contactPoints(items[bestChild], items[bestParent])
            out.add(Link(items[bestChild].hud, items[bestParent].hud, childPoint, parentPoint, axis))
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
    private fun contactPoints(child: Item, parent: Item): Pair<HudAnchor, HudAnchor> {
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
            // when stacked line the link up with whichever side edges are flush else their middles
            val side = alignedSide(child.x, child.right, parent.x, parent.right)
            childFx = side
            parentFx = side
        }
        if (childFy.isNaN()) {
            val side = alignedSide(child.y, child.bottom, parent.y, parent.bottom)
            childFy = side
            parentFy = side
        }
        return anchorAt(childFx, childFy) to anchorAt(parentFx, parentFy)
    }

    private fun alignedSide(aMin: Float, aMax: Float, bMin: Float, bMax: Float): Float = when {
        abs(aMin - bMin) <= TOUCH_TOLERANCE -> 0f
        abs(aMax - bMax) <= TOUCH_TOLERANCE -> 1f
        else -> 0.5f
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

        fun sameStyle(other: Item): Boolean =
            hud.bgColor == other.hud.bgColor && hud.bgChroma == other.hud.bgChroma &&
                (!hud.bgChroma || abs(hud.bgChromaSpeed - other.hud.bgChromaSpeed) <= EPS) &&
                abs(radius - other.radius) <= EPS
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

    fun outlines(rects: List<FloatArray>): List<FloatArray> = trace(snapEdges(rects))

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

    private fun trace(rects: List<FloatArray>): List<FloatArray> {
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

        val loops = ArrayList<FloatArray>()
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
            loops.add(toCoordinates(points, xs, ys, ny))
        }
        return loops
    }

    private fun toCoordinates(points: List<Int>, xs: List<Float>, ys: List<Float>, ny: Int): FloatArray {
        val out = ArrayList<Float>(points.size * 2)
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
            out.add(xs[cx])
            out.add(ys[cy])
        }
        return out.toFloatArray()
    }

    private fun emitLoop(pb: PathBuilder, loop: FloatArray, radius: Float) {
        val n = loop.size / 2
        if (n < 3) return
        fun px(k: Int) = loop[(k % n) * 2]
        fun py(k: Int) = loop[(k % n) * 2 + 1]

        pb.moveTo((px(0) + px(1)) / 2f, (py(0) + py(1)) / 2f)
        for (k in 1..n) {
            val cx = px(k)
            val cy = py(k)
            val prevLen = hypot(cx - px(k - 1), cy - py(k - 1))
            val nextLen = hypot(px(k + 1) - cx, py(k + 1) - cy)
            val r = minOf(radius, prevLen / 2f, nextLen / 2f)
            val mx = (cx + px(k + 1)) / 2f
            val my = (cy + py(k + 1)) / 2f
            if (r <= EPS) {
                pb.lineTo(cx, cy)
                pb.lineTo(mx, my)
            } else {
                pb.tangentArcTo(cx, cy, mx, my, r)
            }
        }
        pb.closePath()
    }

    fun layoutKey(huds: List<Hud>): Int {
        var key = huds.size
        for (hud in huds) {
            key = key * 31 + (hud.x * 4f).roundToInt()
            key = key * 31 + (hud.y * 4f).roundToInt()
            key = key * 31 + (hud.renderedW * 4f).roundToInt()
            key = key * 31 + (hud.renderedH * 4f).roundToInt()
            key = key * 31 + hud.bgColor
            key = key * 31 + (hud.bgRadius * hud.effectiveScale * 4f).roundToInt()
            key = key * 31 + (if (hud.showBackground) 1 else 0)
            key = key * 31 + (if (hud.mergeBackground) 1 else 0)
            key = key * 31 + (if (hud.mergeDiagonally) 1 else 0)
            key = key * 31 + (if (hud.hidden) 1 else 0)
            key = key * 31 + (if (hud.keepBgWhenHidden) 1 else 0)
        }
        return key
    }
}
