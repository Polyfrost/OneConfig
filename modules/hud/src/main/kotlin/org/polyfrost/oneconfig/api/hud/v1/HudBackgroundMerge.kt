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

/**
 * Fuses the backgrounds of HUDs which sit right next to each other into a single shape, so that a
 * column of HUDs reads as one panel instead of a stack of separate cards: shared edges lose their
 * corners, and where two neighbours differ in size the step between them gets a concave fillet.
 *
 * Only HUDs which share the same background style (colour, chroma and radius) and touch along an
 * edge are fused; everything else keeps drawing its own rounded rectangle.
 */
internal object HudBackgroundMerge {
    /** Distance between two edges, in gui units, still counted as "touching". */
    private const val TOUCH_TOLERANCE = 2f

    /** How much two HUDs must overlap along a shared edge before they are fused. */
    private const val MIN_OVERLAP = 2f

    private const val EPS = 0.01f

    class Group(
        val huds: List<Hud>,
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

    fun computeGroups(huds: List<Hud>): List<Group> {
        val items = ArrayList<Item>(huds.size)
        for (hud in huds) {
            if (hud is LegacyHudMarker) continue
            if (!hud.showBackground || !hud.mergeBackground || !hud.hasBackground() || hud.hidden) continue
            val w = hud.renderedW
            val h = hud.renderedH
            if (w <= 0f || h <= 0f) continue
            items.add(Item(hud, hud.x, hud.y, w, h, hud.bgRadius * hud.effectiveScale))
        }
        if (items.size < 2) return emptyList()

        val parent = IntArray(items.size) { it }
        for (i in items.indices) {
            for (j in i + 1 until items.size) {
                val a = items[i]
                val b = items[j]
                if (!a.sameStyle(b) || !touching(a, b)) continue
                union(parent, i, j)
            }
        }

        val components = LinkedHashMap<Int, MutableList<Item>>()
        for (i in items.indices) components.getOrPut(find(parent, i)) { ArrayList() }.add(items[i])

        val out = ArrayList<Group>(components.size)
        for (component in components.values) {
            if (component.size < 2) continue
            val loops = outlines(component.map { floatArrayOf(it.x, it.y, it.w, it.h) })
            if (loops.isEmpty()) continue
            val first = component[0]
            out.add(Group(component.map { it.hud }, first.hud.bgColor, first.hud.bgChroma, first.hud.bgChromaSpeed, first.radius, loops))
        }
        return out
    }

    private class Item(val hud: Hud, var x: Float, var y: Float, var w: Float, var h: Float, val radius: Float) {
        val right get() = x + w
        val bottom get() = y + h

        fun sameStyle(other: Item): Boolean =
            hud.bgColor == other.hud.bgColor && hud.bgChroma == other.hud.bgChroma &&
                (!hud.bgChroma || abs(hud.bgChromaSpeed - other.hud.bgChromaSpeed) <= EPS) &&
                abs(radius - other.radius) <= EPS
    }

    private fun touching(a: Item, b: Item): Boolean {
        val sharesRow = overlap(a.y, a.bottom, b.y, b.bottom) >= MIN_OVERLAP
        val sharesColumn = overlap(a.x, a.right, b.x, b.right) >= MIN_OVERLAP
        val stacked = abs(a.bottom - b.y) <= TOUCH_TOLERANCE || abs(b.bottom - a.y) <= TOUCH_TOLERANCE
        val sideBySide = abs(a.right - b.x) <= TOUCH_TOLERANCE || abs(b.right - a.x) <= TOUCH_TOLERANCE
        val stackedOnTop = stacked && sharesColumn
        val rowNeighbours = sideBySide && sharesRow
        val squareOn = when {
            stackedOnTop -> nests(a.x, a.right, b.x, b.right)
            rowNeighbours -> nests(a.y, a.bottom, b.y, b.bottom)
            else -> false
        }
        if (squareOn) return true
        if (!stackedOnTop && !rowNeighbours && !(stacked && sideBySide)) return false
        return a.hud.mergeDiagonally && b.hud.mergeDiagonally
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

        // directed clockwise in screen space (y down), so every loop keeps the filled area on its right
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
        }
        return key
    }
}
