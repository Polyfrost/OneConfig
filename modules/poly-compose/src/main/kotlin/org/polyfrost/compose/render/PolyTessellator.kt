package org.polyfrost.compose.render

import java.util.Arrays
import java.util.Collections
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.tan
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import org.polyfrost.compose.mc.McGlyph
import org.polyfrost.compose.mc.McGlyphs

abstract class PolyTessellator : PolyRenderer {
    protected class State(
        @JvmField var sx: Float, @JvmField var sy: Float, @JvmField var tx: Float, @JvmField var ty: Float,
        @JvmField var alpha: Float, @JvmField var clip: FloatArray?, @JvmField var rclip: RClip?, @JvmField var mask: Mask? = null,
        @JvmField var glyphScale: Float = 1f, @JvmField var rot: FloatArray? = null, @JvmField var hole: RClip? = null,
    ) {
        fun copy() = State(sx, sy, tx, ty, alpha, clip, rclip, mask, glyphScale, rot, hole)
    }

    protected class Mask(val fade: PolyFade, val parent: Mask?)

    protected class RClip(
        val edge: FloatArray, val inner: FloatArray, val outer: FloatArray, val parent: RClip?, val safe: FloatArray, val box: FloatArray,
        val rrect: Boolean = true,
    ) {
        val invLen = FloatArray(edge.size / 2) { e ->
            val n = edge.size / 2
            1f / max(hypot(edge[(e + 1) % n * 2] - edge[e * 2], edge[(e + 1) % n * 2 + 1] - edge[e * 2 + 1]), 1e-4f)
        }
    }

    protected var state = State(1f, 1f, 0f, 0f, 1f, null, null)
    private val stack = ArrayDeque<State>()

    private class Layer(val depth: Int, val alpha: Float, var start: Int, val box: Int) {
        var mark: Any? = null
    }

    private val layers = ArrayList<Layer>()
    private var boxes = FloatArray(64)
    private var boxCount = 0

    private var verts = FloatArray(4096)
    private var colors = IntArray(1024)
    private var count = 0
    private var batchTexture: Any = PolyGlyphPage.pages[0]
    private var batchPremul = false
    private var batchNearest = false
    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = -Float.MAX_VALUE
    private var maxY = -Float.MAX_VALUE

    protected abstract fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray)

    protected abstract fun bitmapTexture(bitmap: PolyBitmap): Any

    protected open val cpuOnly: Boolean get() = false

    protected open val layered: Boolean get() = false

    protected open fun layerMark(): Any = Unit

    protected open fun layer(mark: Any, alpha: Float) {}

    protected open fun fade(mark: Any, alpha: Float): Boolean = false

    protected fun beginDraw() {
        if (boxCount * 4 == boxes.size) boxes = boxes.copyOf(boxes.size * 2)
        val o = boxCount++ * 4
        boxes[o] = Float.MAX_VALUE; boxes[o + 1] = Float.MAX_VALUE; boxes[o + 2] = -Float.MAX_VALUE; boxes[o + 3] = -Float.MAX_VALUE
    }

    protected fun touch(x0: Float, y0: Float, x1: Float, y1: Float) {
        if (boxCount == 0) return
        val o = boxCount * 4 - 4
        if (x0 < boxes[o]) boxes[o] = x0
        if (y0 < boxes[o + 1]) boxes[o + 1] = y0
        if (x1 > boxes[o + 2]) boxes[o + 2] = x1
        if (y1 > boxes[o + 3]) boxes[o + 3] = y1
    }

    protected fun touchAll() = touch(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)

    protected val drawBoxMark: Int get() = boxCount

    protected fun drawBoxesSince(mark: Int): FloatArray = boxes.copyOfRange(min(mark, boxCount) * 4, boxCount * 4)

    protected fun appendDrawBoxes(b: FloatArray) {
        while (boxes.size < boxCount * 4 + b.size) boxes = boxes.copyOf(boxes.size * 2)
        System.arraycopy(b, 0, boxes, boxCount * 4, b.size)
        boxCount += b.size / 4
    }

    private fun overlapping(from: Int): Boolean {
        if (boxCount - from > MAX_FADE_DRAWS) return true
        for (i in from until boxCount) {
            val a = i * 4
            if (boxes[a] > boxes[a + 2]) continue
            for (j in i + 1 until boxCount) {
                val b = j * 4
                if (boxes[b] > boxes[b + 2]) continue
                if (boxes[a] < boxes[b + 2] && boxes[b] < boxes[a + 2] && boxes[a + 1] < boxes[b + 3] && boxes[b + 1] < boxes[a + 3]) return true
            }
        }
        return false
    }

    private fun collapseBoxes(from: Int) {
        if (boxCount <= from) return
        val o = from * 4
        for (i in from + 1 until boxCount) {
            val k = i * 4
            boxes[o] = min(boxes[o], boxes[k]); boxes[o + 1] = min(boxes[o + 1], boxes[k + 1])
            boxes[o + 2] = max(boxes[o + 2], boxes[k + 2]); boxes[o + 3] = max(boxes[o + 3], boxes[k + 3])
        }
        boxCount = from + 1
    }

    override fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) {
        beginDraw()
        if (w <= 0f || h <= 0f) return
        clippedFill(x, y, w, h, radius) { fillPolygonPx(it, it.size / 2, color, convex = true) }
    }

    override fun rectWithHole(x: Float, y: Float, w: Float, h: Float, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float, color: Int) {
        beginDraw()
        if (w <= 0f || h <= 0f) return
        if (holeW <= 0f || holeH <= 0f || holeX >= x + w || holeY >= y + h || holeX + holeW <= x || holeY + holeH <= y) return rect(x, y, w, h, color, 0f)
        val c = mulAlpha(color, state.alpha)
        if (c ushr 24 == 0) return
        val m = 2f / scaleAvg()
        val l = min(x, holeX - m); val t = min(y, holeY - m)
        val r = max(x + w, holeX + holeW + m); val b = max(y + h, holeY + holeH + m)
        val clipped = l < x || t < y || r > x + w || b > y + h
        if (clipped) { save(); clipRect(x, y, w, h) }
        val outer = floatArrayOf(l, t, r, t, r, b, l, b).also(::transformInPlace)
        val hole = rrectPoints(holeX, holeY, holeW, holeH, holeRadius)
        val n = hole.size / 2
        val per = n / 4
        val dir = if (signedArea(outer, 4) > 0f) 1f else -1f
        val oIn = FloatArray(8); val oOut = FloatArray(8)
        val hIn = FloatArray(n * 2); val hOut = FloatArray(n * 2)
        offsetPolygon(outer, 4, dir, oIn, oOut)
        offsetPolygon(hole, n, -dir, hIn, hOut)
        begin(whitePage(), false)
        val wuv = PolyGlyphPage.WHITE_UV
        val clear = c and 0x00FFFFFF
        fun q(a: FloatArray, i: Int, ca: Int, b: FloatArray, j: Int, cb: Int, d: FloatArray, k: Int, cd: Int, e: FloatArray, o: Int, ce: Int) = quadVerts(
            a[i * 2], a[i * 2 + 1], wuv, wuv, ca, b[j * 2], b[j * 2 + 1], wuv, wuv, cb,
            d[k * 2], d[k * 2 + 1], wuv, wuv, cd, e[o * 2], e[o * 2 + 1], wuv, wuv, ce,
        )
        for (k in 0 until 4) {
            val base = k * per
            val next = (k + 1) % 4
            for (i in 0 until per - 1) q(oIn, k, c, hIn, base + i, c, hIn, base + i + 1, c, hIn, base + i + 1, c)
            q(oIn, k, c, hIn, base + per - 1, c, hIn, next * per, c, oIn, next, c)
            q(oIn, k, c, oOut, k, clear, oOut, next, clear, oIn, next, c)
        }
        for (i in 0 until n) {
            val j = (i + 1) % n
            q(hIn, i, c, hOut, i, clear, hOut, j, clear, hIn, j, c)
        }
        if (clipped) restore()
    }

    override fun rectGradient(x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, radius: Float) {
        beginDraw()
        shaded(gradient, x, y, w, h) { clippedFill(x, y, w, h, radius) { fillPolygonPx(it, it.size / 2, -1, convex = true) } }
    }

    // Set while drawing with a gradient, mapping each vertex to where the gradient's texture holds its color
    private var shading: ((Float, Float, FloatArray) -> Unit)? = null

    private fun shaded(gradient: PolyGradient, x: Float, y: Float, w: Float, h: Float, draw: () -> Unit) {
        if (w <= 0f || h <= 0f) return
        val s = state
        val tex: PolyBitmap
        val uv: (Float, Float, FloatArray) -> Unit
        if (gradient.linear) {
            tex = ramp(gradient)
            uv = { fx, fy, out -> out[0] = (0.5f + 255f * gradient.position((fx - s.tx) / s.sx, (fy - s.ty) / s.sy, x, y, w, h)) / 256f; out[1] = 0.5f }
        } else {
            tex = boxRaster(gradient, w / h)
            uv = { fx, fy, out -> out[0] = ((fx - s.tx) / s.sx - x) / w; out[1] = ((fy - s.ty) / s.sy - y) / h }
        }
        if (cpuOnly) return
        // The atlas packs other textures next to the ramp, so a ramp read past its ends needs a texture of its own to clamp
        val g = if (gradient.linear && !inRamp(gradient, x, y, w, h)) null else PolyGlyphPage.bitmap(tex)
        begin(g?.page ?: bitmapTexture(tex), false)
        shading = if (g == null) uv else { fx, fy, out -> uv(fx, fy, out); out[0] = g.u(out[0]); out[1] = g.v(out[1]) }
        try {
            draw()
        } finally {
            shading = null
        }
    }

    // Linear positions are affine, so the box's corners bound them; the ramp's one-texel border covers that far past either end
    private fun inRamp(g: PolyGradient, x: Float, y: Float, w: Float, h: Float): Boolean {
        for (i in 0 until 4) {
            val t = g.position(if (i and 1 == 0) x else x + w, if (i < 2) y else y + h, x, y, w, h)
            if (t < -1f / 255f || t > 256f / 255f) return false
        }
        return true
    }

    override fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: Int, strokeWidth: Float, radius: Float) =
        stroke(x, y, w, h, color, strokeWidth, radius, shade = null)

    override fun rectStrokeGradient(x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, strokeWidth: Float, radius: Float) {
        val sx = state.sx; val sy = state.sy; val tx = state.tx; val ty = state.ty
        stroke(x, y, w, h, gradient.sample(0f), strokeWidth, radius, dense = !gradient.affine) { px, py -> gradient.colorAt((px - tx) / sx, (py - ty) / sy, x, y, w, h) }
    }

    private fun stroke(x: Float, y: Float, w: Float, h: Float, color: Int, strokeWidth: Float, radius: Float, dense: Boolean = false, shade: ((Float, Float) -> Int)?) {
        beginDraw()
        if (w <= 0f || h <= 0f || strokeWidth <= 0f) return
        val q = strokeWidth / 4f
        val rc = state.rclip
        if (rc != null && state.rot == null && radius >= q && strokeWidth * scaleAvg() >= 2f && state.sx > 0f && state.sy > 0f) {
            val b = boxPx(x, y, w, h, radius)
            val k = rc.box
            var same = true
            for (i in 0 until 6) if (abs(b[i] - k[i]) > 1e-3f) same = false
            if (same) return stroke(x + q, y + q, w - q * 2f, h - q * 2f, color, strokeWidth / 2f, radius - q, dense, shade)
        }
        val swPx = strokeWidth * scaleAvg()
        val alphaMul = if (swPx < 1f) swPx else 1f
        val half = (if (swPx < 1f) 1f / scaleAvg() else strokeWidth) / 2f
        val segs = cornerSegments(radius + half)
        var outer = rrectPoints(x - half, y - half, w + half * 2f, h + half * 2f, if (radius > 0f) radius + half else 0f, segs)
        var inner = rrectPoints(x + half, y + half, w - half * 2f, h - half * 2f, max(0f, radius - half), segs)
        if (dense) densify(outer, inner).let { outer = it.first; inner = it.second }
        unclipped(x - half, y - half, w + half * 2f, h + half * 2f, if (radius > 0f) radius + half else 0f) {
            strokeRing(outer, inner, outer.size / 2, mulAlpha(color, alphaMul), shade?.let { f -> { px: Float, py: Float -> mulAlpha(f(px, py), alphaMul) } }, band = half * 2f * scaleAvg() > 1.001f)
        }
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float) {
        beginDraw()
        val dx = x2 - x1
        val dy = y2 - y1
        val len = hypot(dx, dy)
        if (len <= 0f) return
        val swPx = strokeWidth * scaleAvg()
        val alphaMul = if (swPx < 1f) swPx else 1f
        val half = (if (swPx < 1f) 1f / scaleAvg() else strokeWidth) / 2f
        val nx = -dy / len * half
        val ny = dx / len * half
        val pts = floatArrayOf(x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny)
        transformInPlace(pts)
        fillPolygonPx(pts, 4, mulAlpha(color, alphaMul), convex = true)
    }

    override fun fillPathGradient(path: PolyPath, x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient) =
        shaded(gradient, x, y, w, h) { fillPath(path, -1) }

    override fun fillPath(path: PolyPath, color: Int) {
        beginDraw()
        val loops = flatten(path)
        for (loop in loops) transformInPlace(loop)
        if (loops.size == 1) return fillPolygonPx(loops[0], loops[0].size / 2, color, convex = false)
        val n = loops.size
        val area = FloatArray(n) { signedArea(loops[it], loops[it].size / 2) }
        val parent = IntArray(n) { -1 }
        val outside = IntArray(n)
        val eo = path.evenOdd
        fun own(i: Int) = if (eo || area[i] > 0f) 1 else -1
        for (i in 0 until n) for (j in 0 until n) {
            if (i == j || abs(area[j]) <= abs(area[i]) || !inLoop(loops[j], loops[i])) continue
            outside[i] += own(j)
            if (parent[i] < 0 || abs(area[j]) < abs(area[parent[i]])) parent[i] = j
        }
        fun filled(w: Int) = if (eo) w % 2 != 0 else w != 0
        val outer = BooleanArray(n) { !filled(outside[it]) && filled(outside[it] + own(it)) }
        val holes = arrayOfNulls<ArrayList<Int>>(n)
        for (i in 0 until n) {
            if (outer[i] || !filled(outside[i]) || filled(outside[i] + own(i))) continue
            var o = parent[i]
            while (o >= 0 && !outer[o]) o = parent[o]
            if (o >= 0) (holes[o] ?: ArrayList<Int>().also { holes[o] = it }).add(i)
        }
        for (i in 0 until n) {
            if (!outer[i]) continue
            val h = holes[i]
            if (h == null) fillPolygonPx(loops[i], loops[i].size / 2, color, convex = false)
            else fillWithHoles(loops, area, i, h, color)
        }
    }

    private fun fillWithHoles(loops: List<FloatArray>, area: FloatArray, o: Int, holes: List<Int>, color: Int) {
        val c = mulAlpha(color, state.alpha)
        if (c ushr 24 == 0) return
        val rings = ArrayList<FloatArray>(holes.size * 2 + 2)
        fun ring(p: FloatArray, dir: Float): FloatArray {
            val inner = FloatArray(p.size); val outer = FloatArray(p.size)
            offsetPolygon(p, p.size / 2, dir, inner, outer)
            rings += inner; rings += outer
            return inner
        }
        val cw = area[o] > 0f
        var merged = ring(loops[o], if (cw) 1f else -1f).let { if (cw) it else reversed(it) }
        for (h in holes.sortedBy { minX(loops[it]) }) {
            val inner = ring(loops[h], if (area[h] > 0f) -1f else 1f)
            merged = bridge(merged, if (area[h] > 0f) reversed(inner) else inner)
        }
        begin(whitePage(), false)
        val w = PolyGlyphPage.WHITE_UV
        val m = merged
        for (t in earClip(m, m.size / 2)) {
            val a = t[0] * 2; val b = t[1] * 2; val d = t[2] * 2
            quadVerts(m[a], m[a + 1], w, w, c, m[b], m[b + 1], w, w, c, m[d], m[d + 1], w, w, c, m[d], m[d + 1], w, w, c)
        }
        val clear = c and 0x00FFFFFF
        for (k in 0 until rings.size step 2) {
            val inner = rings[k]; val outer = rings[k + 1]
            val n = inner.size / 2
            for (i in 0 until n) {
                val a = i * 2; val b = (i + 1) % n * 2
                quadVerts(inner[a], inner[a + 1], w, w, c, outer[a], outer[a + 1], w, w, clear, outer[b], outer[b + 1], w, w, clear, inner[b], inner[b + 1], w, w, c)
            }
        }
    }

    private fun minX(p: FloatArray): Float {
        var m = Float.MAX_VALUE
        for (i in p.indices step 2) m = min(m, p[i])
        return m
    }

    private fun reversed(p: FloatArray): FloatArray {
        val n = p.size / 2
        return FloatArray(p.size) { if (it % 2 == 0) p[(n - 1 - it / 2) * 2] else p[(n - 1 - it / 2) * 2 + 1] }
    }

    private fun inLoop(outer: FloatArray, p: FloatArray): Boolean {
        for (i in p.indices step 2) if (!pointInLoop(outer, p[i], p[i + 1])) return false
        return true
    }

    private fun pointInLoop(p: FloatArray, x: Float, y: Float): Boolean {
        var inside = false
        val n = p.size / 2
        var j = n - 1
        for (i in 0 until n) {
            val yi = p[i * 2 + 1]; val yj = p[j * 2 + 1]
            if ((yi > y) != (yj > y) && x < (p[j * 2] - p[i * 2]) * (y - yi) / (yj - yi) + p[i * 2]) inside = !inside
            j = i
        }
        return inside
    }

    private fun bridge(outer: FloatArray, hole: FloatArray): FloatArray {
        val no = outer.size / 2
        val nh = hole.size / 2
        var hi = 0
        for (i in 1 until nh) if (hole[i * 2] < hole[hi * 2] || (hole[i * 2] == hole[hi * 2] && hole[i * 2 + 1] < hole[hi * 2 + 1])) hi = i
        val hx = hole[hi * 2]; val hy = hole[hi * 2 + 1]
        fun x(i: Int) = outer[i * 2]
        fun y(i: Int) = outer[i * 2 + 1]
        fun prev(i: Int) = (i + no - 1) % no
        fun next(i: Int) = (i + 1) % no
        fun ar(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float) = (by - ay) * (cx - bx) - (bx - ax) * (cy - by)
        fun ar(a: Int, b: Int, c: Int) = ar(x(a), y(a), x(b), y(b), x(c), y(c))
        var qx = -Float.MAX_VALUE
        var m = -1
        var touching = false
        for (i in 0 until no) {
            val j = next(i)
            if (hy <= y(i) && hy >= y(j) && y(j) != y(i)) {
                val ix = x(i) + (hy - y(i)) * (x(j) - x(i)) / (y(j) - y(i))
                if (ix <= hx && ix > qx) {
                    qx = ix
                    m = if (x(i) < x(j)) i else j
                    if (ix == hx) { touching = true; break }
                }
            }
        }
        if (m < 0) {
            var d = Float.MAX_VALUE
            for (i in 0 until no) {
                val dd = hypot(x(i) - hx, y(i) - hy)
                if (dd < d) { d = dd; m = i }
            }
        } else if (!touching) {
            val stop = m
            val mx = x(m); val my = y(m)
            var tanMin = Float.MAX_VALUE
            var p = m
            do {
                val px = x(p); val py = y(p)
                val ax = if (hy < my) hx else qx
                val cx = if (hy < my) qx else hx
                val inTri = (cx - px) * (hy - py) >= (ax - px) * (hy - py) && (ax - px) * (my - py) >= (mx - px) * (hy - py) && (mx - px) * (hy - py) >= (cx - px) * (my - py)
                if (hx >= px && px >= mx && hx != px && inTri) {
                    val tan = abs(hy - py) / (hx - px)
                    val a = prev(p); val b = next(p)
                    val local = if (ar(a, p, b) < 0f) ar(px, py, hx, hy, x(b), y(b)) >= 0f && ar(px, py, x(a), y(a), hx, hy) >= 0f
                    else ar(px, py, hx, hy, x(a), y(a)) < 0f || ar(px, py, x(b), y(b), hx, hy) < 0f
                    val sector = ar(prev(m), m, prev(p)) < 0f && ar(next(p), m, next(m)) < 0f
                    if (local && (tan < tanMin || (tan == tanMin && (px > x(m) || (px == x(m) && sector))))) { m = p; tanMin = tan }
                }
                p = next(p)
            } while (p != stop)
        }
        val out = FloatArray((no + nh + 2) * 2)
        var k = 0
        fun put(a: FloatArray, i: Int) { out[k++] = a[i * 2]; out[k++] = a[i * 2 + 1] }
        for (i in 0..m) put(outer, i)
        for (s in 0..nh) put(hole, (hi + s) % nh)
        for (i in m until no) put(outer, i)
        return out
    }

    override fun strokePolylineGradient(
        points: FloatArray, closed: Boolean, x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, strokeWidth: Float, cap: Int, join: Int,
    ) = shaded(gradient, x, y, w, h) { strokePolyline(points, closed, -1, strokeWidth, cap, join) }

    override fun strokePolyline(points: FloatArray, closed: Boolean, color: Int, strokeWidth: Float, cap: Int, join: Int) {
        beginDraw()
        if (points.size < 4 || strokeWidth <= 0f) return
        val p = dedupe(points)
        if (p.size == 2) {
            if (cap != PolyRenderer.CAP_BUTT) rect(p[0] - strokeWidth / 2f, p[1] - strokeWidth / 2f, strokeWidth, strokeWidth, color, if (cap == PolyRenderer.CAP_ROUND) strokeWidth / 2f else 0f)
            return
        }
        val loop = (closed || hypot(points[0] - points[points.size - 2], points[1] - points[points.size - 1]) <= 1e-3f) && p.size >= 6
        transformInPlace(p)
        val n = p.size / 2
        if (n < 2) return
        val swPx = strokeWidth * scaleAvg()
        val c = mulAlpha(color, if (swPx < 1f) swPx else 1f)
        val half = max(swPx, 1f) / 2f
        if (!loop && cap == PolyRenderer.CAP_SQUARE) {
            fun extend(i: Int, from: Int) {
                val dx = p[i * 2] - p[from * 2]; val dy = p[i * 2 + 1] - p[from * 2 + 1]
                val l = hypot(dx, dy)
                p[i * 2] += dx / l * half; p[i * 2 + 1] += dy / l * half
            }
            extend(0, 1)
            extend(n - 1, n - 2)
        }
        val arcSegs = max(4, min(32, ceil(sqrt(half) * 3.2f).toInt()))
        val per = if (join == PolyRenderer.JOIN_ROUND) arcSegs + 1 else 1
        val left = FloatArray(n * per * 2)
        val right = FloatArray(n * per * 2)
        var lo = 0
        var ro = 0
        for (i in 0 until n) {
            val prev = if (loop || i > 0) (i + n - 1) % n else -1
            val next = if (loop || i < n - 1) (i + 1) % n else -1
            var mx = 0f; var my = 0f
            var d0x = 0f; var d0y = 0f; var d1x = 0f; var d1y = 0f
            if (prev >= 0) {
                val dx = p[i * 2] - p[prev * 2]; val dy = p[i * 2 + 1] - p[prev * 2 + 1]
                val l = hypot(dx, dy)
                d0x = dx / l; d0y = dy / l
                mx -= d0y; my += d0x
            }
            if (next >= 0) {
                val dx = p[next * 2] - p[i * 2]; val dy = p[next * 2 + 1] - p[i * 2 + 1]
                val l = hypot(dx, dy)
                d1x = dx / l; d1y = dy / l
                mx -= d1y; my += d1x
            }
            if (prev >= 0 && next >= 0) { mx /= 2f; my /= 2f }
            val d2 = mx * mx + my * my
            val k = if (d2 > 1e-6f) min(1f / d2, 4f) * half else half
            val px = p[i * 2]; val py = p[i * 2 + 1]
            val turn = if (join != PolyRenderer.JOIN_ROUND || prev < 0 || next < 0) 0f else atan2(d0x * d1y - d0y * d1x, d0x * d1x + d0y * d1y)
            if (abs(turn) < 1e-3f) {
                left[lo++] = px + mx * k; left[lo++] = py + my * k
                right[ro++] = px - mx * k; right[ro++] = py - my * k
                continue
            }
            val segs = max(1, ceil(arcSegs * abs(turn) / Math.PI.toFloat()).toInt())
            val side = if (turn > 0f) -1f else 1f
            val a0 = atan2(d0x * side, -d0y * side)
            val ix = px - mx * k * side; val iy = py - my * k * side
            for (s in 0..segs) {
                val a = a0 + turn * s / segs
                val ax = px + cos(a) * half; val ay = py + sin(a) * half
                if (turn > 0f) {
                    left[lo++] = ix; left[lo++] = iy; right[ro++] = ax; right[ro++] = ay
                } else {
                    left[lo++] = ax; left[lo++] = ay; right[ro++] = ix; right[ro++] = iy
                }
            }
        }
        val m = lo / 2
        val l = if (lo == left.size) left else left.copyOf(lo)
        val r = if (ro == right.size) right else right.copyOf(ro)
        if (loop) {
            val leftOuter = abs(signedArea(l, m)) >= abs(signedArea(r, m))
            strokeRing(if (leftOuter) l else r, if (leftOuter) r else l, m, c, band = half * 2f > 1.001f)
            return
        }
        val segs = if (cap == PolyRenderer.CAP_ROUND) arcSegs else 0
        val ce = (segs - 1).coerceAtLeast(0)
        val out = FloatArray((m * 2 + ce * 2) * 2)
        var o = 0
        fun cap(i: Int, from: FloatArray, at: Int) {
            val cx = p[i * 2]; val cy = p[i * 2 + 1]
            val a0 = atan2(from[at * 2 + 1] - cy, from[at * 2] - cx)
            for (s in 1 until segs) {
                val a = a0 - Math.PI.toFloat() * s / segs
                out[o++] = cx + cos(a) * half; out[o++] = cy + sin(a) * half
            }
        }
        for (i in 0 until m) { out[o++] = l[i * 2]; out[o++] = l[i * 2 + 1] }
        cap(n - 1, l, m - 1)
        for (i in m - 1 downTo 0) { out[o++] = r[i * 2]; out[o++] = r[i * 2 + 1] }
        cap(0, r, 0)
        val total = o / 2
        fun ri(k: Int) = m + ce + (m - 1 - k)
        val quads = IntArray((m - 1 + ce * 2) * 4)
        var q = 0
        fun quad(a: Int, b: Int, d: Int, e: Int) { quads[q++] = a; quads[q++] = b; quads[q++] = d; quads[q++] = e }
        for (k in 0 until m - 1) quad(k, k + 1, ri(k + 1), ri(k))
        for (j in 0 until ce) quad(m - 1, m + j, m + j + 1, m + j + 1)
        for (j in 0 until ce) quad(ri(0), ri(0) + 1 + j, (ri(0) + 2 + j) % total, (ri(0) + 2 + j) % total)
        fillPolygonPx(out, total, c, convex = false, quads = quads)
    }

    override fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont) = glyphs(text, x, y, color, font, 0f, false, true)

    override fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont, letterSpacing: Float) = glyphs(text, x, y, color, font, 0f, false, true, letterSpacing)

    override fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont, letterSpacing: Float, skewX: Float) {
        if (skewX == 0f || state.sy == 0f) return text(text, x, y, color, font, letterSpacing)
        shearX = skewX * state.sx / state.sy
        shearBase = y * state.sy + state.ty
        glyphs(text, x, y, color, font, 0f, false, true, letterSpacing)
        shearX = 0f
    }

    override fun textOutline(text: String, x: Float, y: Float, color: Int, font: PolyFont, width: Float) =
        glyphs(text, x, y, color, font, width * scaleAvg(), true, true)

    private fun glyphs(text: String, x: Float, y: Float, color: Int, font: PolyFont, outlinePx: Float, round: Boolean, subpixel: Boolean, letterSpacing: Float = 0f) {
        beginDraw()
        val face = PolyGlyphPage.rasterizer?.face(font.name) ?: return
        val c = mulAlpha(color, state.alpha)
        if (c ushr 24 == 0) return
        val k = state.glyphScale
        val outline = outlinePx / k
        val bucket = (font.size * scaleAvg() / k * 4f).roundToInt()
        val lum = if (outline > 0f) -1 else PolyMaskGamma.bucket(color)
        val atlas = if (cpuOnly) null else PolyGlyphPage.atlas(face, bucket, outline, round, lum)
        var fallback: PolyGlyphPage.Face? = null
        var fallbackAtlas: HashMap<Int, PolyGlyphPage.Glyph?>? = null
        fun glyph(f: PolyGlyphPage.Face, id: Int, phase: Int): PolyGlyphPage.Glyph? {
            val a = if (f === face) atlas else fallbackAtlas
            if (a != null) return PolyGlyphPage.glyph(a, f, bucket, id, outline, round, phase, lum)
            if (outline <= 0f) PolyGlyphPage.prerasterize(f, bucket, id, phase)
            return null
        }
        var penX = x
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            var f = face
            var id = face.glyph(cp)
            if (id == 0 && !Character.isWhitespace(cp)) {
                if (fallback == null) {
                    fallback = PolyGlyphPage.rasterizer?.face(FALLBACK_FONT)
                    if (fallback != null && !cpuOnly) fallbackAtlas = PolyGlyphPage.atlas(fallback, bucket, outline, round, lum)
                }
                val fid = fallback?.glyph(cp) ?: 0
                if (fid != 0) { f = fallback!!; id = fid }
            }
            val sx = penX * state.sx + state.tx
            if (k != 1f) {
                val phase = if (subpixel) ((sx - floor(sx)) * PolyGlyphPage.SUBPIXELS).toInt() else 0
                val g = glyph(f, id, phase)
                if (g != null) {
                    val px = sx + (g.offX - phase.toFloat() / PolyGlyphPage.SUBPIXELS) * k
                    val py = y * state.sy + state.ty + g.offY * k
                    begin(g.page, false)
                    glyphQuad(px, py, px + g.w * k, py + g.h * k, g.u0, g.v0, g.u1, g.v1, c)
                }
                penX += f.advance(id) * font.size + letterSpacing
                continue
            }
            var base = if (subpixel) floor(sx).toInt() else sx.roundToInt()
            var phase = if (subpixel) ((sx - base) * PolyGlyphPage.SUBPIXELS).roundToInt() else 0
            if (phase == PolyGlyphPage.SUBPIXELS) { base++; phase = 0 }
            val g = glyph(f, id, phase)
            if (g != null) {
                val px = base + g.offX
                val py = (y * state.sy + state.ty).roundToInt() + g.offY
                begin(g.page, false)
                glyphQuad(px.toFloat(), py.toFloat(), (px + g.w).toFloat(), (py + g.h).toFloat(), g.u0, g.v0, g.u1, g.v1, c)
            }
            penX += f.advance(id) * font.size + letterSpacing
        }
    }

    override fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) {
        if (cpuOnly) return
        val lines = text.lines()
        if (shadow) {
            beginDraw()
            lines.forEachIndexed { index, line -> mcLine(line, x + scale, y + index * 9f * scale + scale, color, scale, true) }
        }
        beginDraw()
        lines.forEachIndexed { index, line -> mcLine(line, x, y + index * 9f * scale, color, scale, false) }
    }

    override fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float) {
        beginDraw()
        if (cpuOnly) return
        val c = mulAlpha(outlineColor, state.alpha)
        val rects = if (c ushr 24 == 0) null else PolyGlyphPage.rasterizer?.let { r -> mcOutlines.getOrPut(text to FontManager.revision) { mcOutline(r, text) } }
        if (rects != null && rects.isNotEmpty()) {
            begin(whitePage(), false)
            val s = state
            val wuv = PolyGlyphPage.WHITE_UV
            var i = 0
            while (i < rects.size) {
                val ax = ((x + rects[i] * scale) * s.sx + s.tx).roundToInt().toFloat()
                val ay = ((y + rects[i + 1] * scale) * s.sy + s.ty).roundToInt().toFloat()
                val bx = ((x + rects[i + 2] * scale) * s.sx + s.tx).roundToInt().toFloat()
                val by = ((y + rects[i + 3] * scale) * s.sy + s.ty).roundToInt().toFloat()
                if (ax != bx && ay != by) quadPx(min(ax, bx), min(ay, by), max(ax, bx), max(ay, by), wuv, wuv, wuv, wuv, c)
                i += 4
            }
        }
        mcText(text, x, y, color, false, scale)
    }

    // device-space horizontal shear: x += shearX * (shearBase - y)
    private var shearX = 0f
    private var shearBase = 0f

    private fun glyphQuad(x0: Float, y0: Float, x1: Float, y1: Float, u0: Float, v0: Float, u1: Float, v1: Float, c: Int) {
        if (shearX == 0f) return quadPx(x0, y0, x1, y1, u0, v0, u1, v1, c)
        val s0 = shearX * (shearBase - y0)
        val s1 = shearX * (shearBase - y1)
        vertex(x0 + s0, y0, u0, v0, c)
        vertex(x0 + s1, y1, u0, v1, c)
        vertex(x1 + s1, y1, u1, v1, c)
        vertex(x1 + s0, y0, u1, v0, c)
    }

    private fun mcEffect(x0: Float, y0: Float, x1: Float, y1: Float, color: Int) {
        val c = mulAlpha(color, state.alpha)
        if (c ushr 24 == 0) return
        begin(whitePage(), false)
        val s = state
        val w = PolyGlyphPage.WHITE_UV
        quadPx(x0 * s.sx + s.tx, y0 * s.sy + s.ty, x1 * s.sx + s.tx, y1 * s.sy + s.ty, w, w, w, w, c)
    }

    private fun mcLine(line: String, x: Float, top: Float, color: Int, scale: Float, shadow: Boolean) {
        val baseline = top + 7f * scale
        var c = color
        var bold = false
        var italic = false
        var under = false
        var strike = false
        var pen = x
        var i = 0
        while (i < line.length) {
            val cp = line.codePointAt(i)
            i += Character.charCount(cp)
            if (cp == 0xA7 && i < line.length) {
                val code = MC_CODES.indexOf(line[i].lowercaseChar())
                i++
                if (code in 0..15 || code == 17) { bold = false; italic = false; under = false; strike = false }
                when {
                    code in 0..15 -> c = (color and -0x1000000) or MC_COLORS[code]
                    code == 16 -> bold = true
                    code == 17 -> c = color
                    code == 18 -> strike = true
                    code == 19 -> under = true
                    code == 20 -> italic = true
                }
                continue
            }
            val drawn = if (shadow) (c and -0x1000000) or ((c and 0xFCFCFC) shr 2) else c
            val g = McGlyphs.source?.invoke(cp)
            // the shadow line is already one unit off, but vanilla offsets each glyph's shadow by its own offset
            val o = if (shadow && g != null) (g.offset - 1f) * scale else 0f
            shearX = if (italic) 0.25f * state.sx / state.sy else 0f
            shearBase = (top + o + 4f * scale) * state.sy + state.ty
            val start = pen
            if (g != null) {
                mcGlyph(g, pen + o, top + o, scale, drawn)
                if (bold) mcGlyph(g, pen + o + g.offset * scale, top + o, scale, drawn)
                pen += (g.advance + if (bold) g.offset else 0f) * scale
            } else {
                val main = PolyGlyphPage.rasterizer?.face(if (bold) "minecraft-bold" else "minecraft")
                val s = String(Character.toChars(cp))
                pen += if (main != null && main.glyph(cp) != 0) {
                    val font = PolyFont(if (bold) "minecraft-bold" else "minecraft", 10f * scale)
                    glyphs(s, pen, baseline, drawn, font, 0f, false, false)
                    main.advance(main.glyph(cp)) * font.size
                } else {
                    val font = PolyFont("unifont", 8f * scale)
                    val uni = PolyGlyphPage.rasterizer?.face("unifont") ?: continue
                    glyphs(s, pen, baseline, drawn, font, 0f, false, false)
                    uni.advance(uni.glyph(cp)) * font.size
                }
            }
            if (under) mcEffect(start + o - scale, top + o + 8f * scale, pen + o, top + o + 9f * scale, drawn)
            if (strike) mcEffect(start + o - scale, top + o + 3.5f * scale, pen + o, top + o + 4.5f * scale, drawn)
        }
        shearX = 0f
    }

    private fun mcGlyph(g: McGlyph, x: Float, top: Float, scale: Float, color: Int) {
        val b = g.bitmap ?: return
        val c = mulAlpha(color, state.alpha)
        if (c ushr 24 == 0) return
        val p = PolyGlyphPage.bitmap(b)
        begin(p?.page ?: bitmapTexture(b), false, nearest = true)
        val s = state
        val x0 = x * s.sx + s.tx
        val y0 = (top + g.top * scale) * s.sy + s.ty
        val tu = (p?.let { it.u1 - it.u0 } ?: 1f) / b.width * NEAREST_TIE_BIAS
        val tv = (p?.let { it.v1 - it.v0 } ?: 1f) / b.height * NEAREST_TIE_BIAS
        glyphQuad(
            x0, y0, x0 + g.width * scale * s.sx, y0 + g.height * scale * s.sy,
            (p?.u(g.u0) ?: g.u0) + tu, (p?.v(g.v0) ?: g.v0) + tv, (p?.u(g.u1) ?: g.u1) + tu, (p?.v(g.v1) ?: g.v1) + tv, c,
        )
    }

    override fun image(bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: Int, filterQuality: FilterQuality) {
        beginDraw()
        val c = mulAlpha(tint, state.alpha)
        if (c ushr 24 == 0 || cpuOnly) return
        val g = PolyGlyphPage.bitmap(bitmap)
        begin(g?.page ?: bitmapTexture(bitmap), false, filterQuality == FilterQuality.None)
        val x0 = x * state.sx + state.tx
        val y0 = y * state.sy + state.ty
        val x1 = x0 + w * state.sx
        val y1 = y0 + h * state.sy
        val tu = if (filterQuality == FilterQuality.None) ((g?.let { abs(it.u(1f) - it.u(0f)) } ?: 1f) / bitmap.width * NEAREST_TIE_BIAS) else 0f
        val tv = if (filterQuality == FilterQuality.None) ((g?.let { abs(it.v(1f) - it.v(0f)) } ?: 1f) / bitmap.height * NEAREST_TIE_BIAS) else 0f
        val ua = (g?.u(u0) ?: u0) + tu; val va = (g?.v(v0) ?: v0) + tv
        val ub = (g?.u(u1) ?: u1) + tu; val vb = (g?.v(v1) ?: v1) + tv
        val rc = state.rclip
        if (rc != null && state.rot == null && state.sx > 0f && state.sy > 0f && x1 > x0 && y1 > y0) {
            val k = rc.box
            val e = 1e-3f
            val covers = x0 <= k[0] + e && y0 <= k[1] + e && x1 >= k[2] - e && y1 >= k[3] - e
            val sf = rc.safe
            val safe = (x0 >= sf[0] && y0 >= sf[1] && x1 <= sf[2] && y1 <= sf[3]) || (x0 >= sf[4] && y0 >= sf[5] && x1 <= sf[6] && y1 <= sf[7])
            if (covers || !safe) {
                val uLo = min(ua, ub); val uHi = max(ua, ub); val vLo = min(va, vb); val vHi = max(va, vb)
                val uv: (Float, Float, FloatArray) -> Unit = { fx, fy, out ->
                    out[0] = (ua + (fx - x0) / (x1 - x0) * (ub - ua)).coerceIn(uLo, uHi)
                    out[1] = (va + (fy - y0) / (y1 - y0) * (vb - va)).coerceIn(vLo, vHi)
                }
                clippedFill(x, y, w, h, 0f) { fillPolygonPx(it, it.size / 2, tint, convex = true, uv = uv, keepBatch = true) }
                return
            }
        }
        quadPx(x0, y0, x1, y1, ua, va, ub, vb, c)
    }

    override fun shadow(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: PolyShadow) =
        shadowWithHole(x, y, w, h, radius, shadow, 0f, 0f, 0f, 0f, 0f)

    override fun shadowWithHole(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: PolyShadow, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float) {
        beginDraw()
        val s = scaleAvg()
        val color = shadow.color.argb
        val spread = shadow.spread
        val blur = shadow.blur
        if (color ushr 24 == 0 || s <= 0f) return
        val bx = x - spread + shadow.offsetX
        val by = y - spread + shadow.offsetY
        val bw = w + spread * 2f
        val bh = h + spread * 2f
        val rad = if (radius > 0f) max(0f, radius + spread) else 0f
        val sigmaQ = if (blur > 0f) ((blur * s * 0.57735f + 0.5f) * 2f).roundToInt() else 0
        if (sigmaQ == 0 || bw <= 0f || bh <= 0f) return rect(bx, by, bw, bh, color, rad)
        val rPx = min(rad * s, min(bw, bh) * s / 2f).roundToInt()
        val m = shadowMask(rPx, sigmaQ)
        val e = m.extent / s
        val x0 = bx - e
        val y0 = by - e
        val x3 = bx + bw + e
        val y3 = by + bh + e
        val kx = min(m.slice, (x3 - x0) * s / 2f)
        val ky = min(m.slice, (y3 - y0) * s / 2f)
        val mw = m.width
        val xs = floatArrayOf(x0, x0 + kx / s, x3 - kx / s, x3)
        val ys = floatArrayOf(y0, y0 + ky / s, y3 - ky / s, y3)
        val us = floatArrayOf(0f, kx / mw, 1f - kx / mw, 1f)
        val vs = floatArrayOf(0f, ky / mw, 1f - ky / mw, 1f)
        val holes = if (holeW > 0f && holeH > 0f) {
            val p = 0.45f / s
            val hr = min(holeRadius, min(holeW, holeH) / 2f)
            FloatArray(4 * HOLE_STEPS) {
                val a = (it / 4) * (Math.PI.toFloat() / 2f) / (HOLE_STEPS - 1)
                val ix = hr * (1f - cos(a)) + p
                val iy = hr * (1f - sin(a)) + p
                when (it % 4) { 0 -> holeX + ix; 1 -> holeY + iy; 2 -> holeX + holeW - ix; else -> holeY + holeH - iy }
            }
        } else null
        for (j in 0 until 3) for (i in 0 until 3) {
            if (xs[i + 1] <= xs[i] || ys[j + 1] <= ys[j]) continue
            if (holes == null) image(m.bitmap, xs[i], ys[j], xs[i + 1] - xs[i], ys[j + 1] - ys[j], us[i], vs[j], us[i + 1], vs[j + 1], color)
            else shadowPiece(m.bitmap, xs[i], ys[j], xs[i + 1], ys[j + 1], xs[i], ys[j], xs[i + 1], ys[j + 1], us[i], vs[j], us[i + 1], vs[j + 1], color, holes, 0)
        }
    }

    private fun shadowPiece(
        bmp: PolyBitmap, l: Float, t: Float, r: Float, b: Float,
        cx0: Float, cy0: Float, cx1: Float, cy1: Float, u0: Float, v0: Float, u1: Float, v1: Float,
        color: Int, holes: FloatArray, k: Int,
    ) {
        if (r <= l || b <= t) return
        if (k == holes.size) {
            fun u(x: Float) = u0 + (x - cx0) / (cx1 - cx0) * (u1 - u0)
            fun v(y: Float) = v0 + (y - cy0) / (cy1 - cy0) * (v1 - v0)
            image(bmp, l, t, r - l, b - t, u(l), v(t), u(r), v(b), color)
            return
        }
        val hl = holes[k]; val ht = holes[k + 1]; val hr = holes[k + 2]; val hb = holes[k + 3]
        val n = k + 4
        if (hr <= l || hl >= r || hb <= t || ht >= b) return shadowPiece(bmp, l, t, r, b, cx0, cy0, cx1, cy1, u0, v0, u1, v1, color, holes, n)
        val mt = max(t, ht); val mb = min(b, hb)
        shadowPiece(bmp, l, t, r, mt, cx0, cy0, cx1, cy1, u0, v0, u1, v1, color, holes, n)
        shadowPiece(bmp, l, mb, r, b, cx0, cy0, cx1, cy1, u0, v0, u1, v1, color, holes, n)
        shadowPiece(bmp, l, mt, max(l, hl), mb, cx0, cy0, cx1, cy1, u0, v0, u1, v1, color, holes, n)
        shadowPiece(bmp, min(r, hr), mt, r, mb, cx0, cy0, cx1, cy1, u0, v0, u1, v1, color, holes, n)
    }

    private var keyScratch = FloatArray(32)
    private var keySize = 0

    protected fun keyAdd(v: Float) {
        if (keySize == keyScratch.size) keyScratch = keyScratch.copyOf(keySize * 2)
        keyScratch[keySize++] = v
    }

    protected open fun writeStateKey() {
        val s = state
        keyAdd(s.sx); keyAdd(s.sy); keyAdd(s.tx); keyAdd(s.ty); keyAdd(s.alpha); keyAdd(s.glyphScale)
        s.rot?.let { keyAdd(-2f); for (v in it) keyAdd(v) }
        val clip = s.clip
        if (clip != null) for (v in clip) keyAdd(v) else keyAdd(-1f)
        var c = s.rclip
        while (c != null) { for (v in c.box) keyAdd(v); if (!c.rrect) for (v in c.edge) keyAdd(v); c = c.parent }
        var h = s.hole
        while (h != null) { keyAdd(-3f); for (v in h.edge) keyAdd(v); h = h.parent }
        var m = s.mask
        while (m != null) {
            keyAdd(if (m.fade.vertical) 1f else 0f)
            for (v in m.fade.stops) keyAdd(v)
            for (v in m.fade.alphas) keyAdd(v)
            m.fade.colors?.let { for (v in it) keyAdd(v.toFloat()) }
            m = m.parent
        }
    }

    fun stateKey(): Any {
        keySize = 0
        writeStateKey()
        return StateKey(keyScratch.copyOf(keySize))
    }

    fun sameState(key: Any): Boolean {
        if (key !is StateKey) return false
        keySize = 0
        writeStateKey()
        return Arrays.equals(key.values, 0, key.values.size, keyScratch, 0, keySize)
    }

    private class StateKey(val values: FloatArray) {
        override fun equals(other: Any?): Boolean = other is StateKey && values.contentEquals(other.values)
        override fun hashCode(): Int = values.contentHashCode()
    }

    fun saveState(): Any = state.copy()

    fun restoreState(saved: Any) { state = (saved as State).copy() }

    override fun save() {
        stack.addLast(state)
        state = state.copy()
    }

    override fun restore() {
        val l = layers.lastOrNull()
        if (l != null && l.depth == stack.size) endLayer(l)
        state = stack.removeLast()
    }

    override fun saveLayerAlpha(alpha: Float) {
        save()
        if (alpha >= 1f) return
        if (!layered || alpha <= 0f) {
            state.alpha *= alpha
            return
        }
        layers.add(Layer(stack.size, state.alpha * alpha, count, boxCount))
        state.alpha = 1f
    }

    private fun endLayer(l: Layer) {
        val mark = l.mark
        if (!overlapping(l.box) && (mark == null || fade(mark, l.alpha))) {
            layers.removeAt(layers.lastIndex)
            for (i in (if (mark == null) l.start else 0) until count) colors[i] = if (batchPremul) scaleArgb(colors[i], l.alpha) else mulAlpha(colors[i], l.alpha)
            return
        }
        flush()
        layers.removeAt(layers.lastIndex)
        layer(l.mark!!, l.alpha)
        collapseBoxes(l.box)
    }

    override fun deviceTransform(): FloatArray? = if (state.rot != null) null else floatArrayOf(state.sx, state.sy, state.tx, state.ty)

    override fun saveLayerFade(fade: PolyFade) {
        save()
        var vertical = fade.vertical
        var s = if (vertical) state.sy else state.sx
        var t = if (vertical) state.ty else state.tx
        val m = state.rot
        if (m != null) {
            val ax = if (vertical) m[1] else m[0]
            val ay = if (vertical) m[3] else m[2]
            val tx = state.tx; val ty = state.ty
            vertical = abs(ay) >= abs(ax)
            s *= if (vertical) ay else ax
            t = if (vertical) m[2] * tx + m[3] * ty + m[5] else m[0] * tx + m[1] * ty + m[4]
        }
        val stops = FloatArray(fade.stops.size) { fade.stops[it] * s + t }
        val px = if (s >= 0f) PolyFade(vertical, stops, fade.alphas, fade.colors)
        else PolyFade(vertical, stops.reversedArray(), fade.alphas.reversedArray(), fade.colors?.reversedArray())
        state.mask = Mask(px, state.mask)
    }

    override fun translate(dx: Float, dy: Float) {
        state.tx += dx * state.sx
        state.ty += dy * state.sy
    }

    override fun scale(sx: Float, sy: Float) {
        state.sx *= sx
        state.sy *= sy
    }

    override fun rotate(degrees: Float) {
        val s = state
        if (degrees % 360f == 0f || s.sx == 0f || s.sy == 0f) return
        val rad = Math.toRadians(degrees.toDouble())
        val cs = cos(rad).toFloat()
        val sn = sin(rad).toFloat()
        val lb = -sn * s.sx / s.sy
        val lc = sn * s.sy / s.sx
        val le = s.tx - cs * s.tx - lb * s.ty
        val lf = s.ty - lc * s.tx - cs * s.ty
        val m = s.rot
        s.rot = if (m == null) floatArrayOf(cs, lb, lc, cs, le, lf)
        else floatArrayOf(
            m[0] * cs + m[1] * lc, m[0] * lb + m[1] * cs, m[2] * cs + m[3] * lc, m[2] * lb + m[3] * cs,
            m[0] * le + m[1] * lf + m[4], m[2] * le + m[3] * lf + m[5],
        )
    }

    override fun animatedScale(sx: Float, sy: Float) {
        scale(sx, sy)
        state.glyphScale *= (sx + sy) / 2f
    }

    override fun clipRect(x: Float, y: Float, w: Float, h: Float) {
        var x0 = x * state.sx + state.tx
        var y0 = y * state.sy + state.ty
        var x1 = x0 + w * state.sx
        var y1 = y0 + h * state.sy
        val m = state.rot
        if (m != null) {
            val px = floatArrayOf(x0, x1, x1, x0)
            val py = floatArrayOf(y0, y0, y1, y1)
            x0 = Float.MAX_VALUE; y0 = Float.MAX_VALUE; x1 = -Float.MAX_VALUE; y1 = -Float.MAX_VALUE
            for (i in 0 until 4) {
                val rx = m[0] * px[i] + m[1] * py[i] + m[4]
                val ry = m[2] * px[i] + m[3] * py[i] + m[5]
                x0 = min(x0, rx); y0 = min(y0, ry); x1 = max(x1, rx); y1 = max(y1, ry)
            }
        }
        val prev = state.clip
        val next = if (prev == null) floatArrayOf(floor(x0), floor(y0), ceil(x1), ceil(y1))
        else floatArrayOf(max(floor(x0), prev[0]), max(floor(y0), prev[1]), min(ceil(x1), prev[2]), min(ceil(y1), prev[3]))
        state.clip = next
    }

    override fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float) {
        clipRect(x, y, w, h)
        if (radius <= 0f || state.rot != null) return
        val p = rrectPoints(x, y, w, h, radius)
        orient(p)
        val n = p.size / 2
        val inner = FloatArray(p.size); val outer = FloatArray(p.size)
        offsetPolygon(p, n, 1f, inner, outer)
        val box = boxPx(x, y, w, h, radius)
        val l = box[0]; val t = box[1]; val rt = box[2]; val b = box[3]
        val rx = box[4] + 1f; val ry = box[5] + 1f
        val safe = floatArrayOf(l + rx, t + 1f, rt - rx, b - 1f, l + 1f, t + ry, rt - 1f, b - ry)
        state.rclip = RClip(p, inner, outer, state.rclip, safe, box)
    }

    override fun clipPolygon(points: FloatArray) {
        super.clipPolygon(points)
        if (state.rot != null) return
        val p = FloatArray(points.size)
        var k = 0
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        for (i in points.indices step 2) {
            val x = points[i] * state.sx + state.tx
            val y = points[i + 1] * state.sy + state.ty
            // Coverage is measured from each edge, which repeated points don't give a direction
            if (k > 0 && abs(x - p[k - 2]) < 0.01f && abs(y - p[k - 1]) < 0.01f) continue
            p[k++] = x; p[k++] = y
            l = min(l, x); t = min(t, y); r = max(r, x); b = max(b, y)
        }
        if (k > 4 && abs(p[0] - p[k - 2]) < 0.01f && abs(p[1] - p[k - 1]) < 0.01f) k -= 2
        val n = k / 2
        if (n < 3) return
        val poly = p.copyOf(k)
        orient(poly)
        for (i in 0 until n) {
            val a = i * 2; val c = (i + 1) % n * 2; val d = (i + 2) % n * 2
            // Concave outlines stay clipped to their bounds
            if ((poly[c] - poly[a]) * (poly[d + 1] - poly[c + 1]) - (poly[c + 1] - poly[a + 1]) * (poly[d] - poly[c]) < -0.01f) return
        }
        val inner = FloatArray(k); val outer = FloatArray(k)
        offsetPolygon(poly, n, 1f, inner, outer)
        state.rclip = RClip(poly, inner, outer, state.rclip, NO_SAFE, floatArrayOf(l, t, r, b, 0f, 0f), rrect = false)
    }

    override fun clipOutRect(x: Float, y: Float, w: Float, h: Float) {
        // Narrower holes would need their soft edges to overlap; drawing over them misses less than a pixel
        if (w * abs(state.sx) < 1f || h * abs(state.sy) < 1f) return
        val p = floatArrayOf(x, y, x + w, y, x + w, y + h, x, y + h)
        transformInPlace(p)
        state.rot?.let { m ->
            for (i in p.indices step 2) {
                val px = p[i]
                p[i] = m[0] * px + m[1] * p[i + 1] + m[4]
                p[i + 1] = m[2] * px + m[3] * p[i + 1] + m[5]
            }
        }
        orient(p)
        val inner = FloatArray(8); val outer = FloatArray(8)
        offsetPolygon(p, 4, 1f, inner, outer)
        state.hole = RClip(p, inner, outer, state.hole, NONE, NONE)
    }

    // Winds a clip polygon so edgeSide is positive inside it
    private fun orient(p: FloatArray) {
        if (signedArea(p, p.size / 2) >= 0f) return
        for (i in 0 until p.size / 4) {
            val j = p.size / 2 - 1 - i
            val tx = p[i * 2]; val ty = p[i * 2 + 1]
            p[i * 2] = p[j * 2]; p[i * 2 + 1] = p[j * 2 + 1]
            p[j * 2] = tx; p[j * 2 + 1] = ty
        }
    }

    private fun boxPx(x: Float, y: Float, w: Float, h: Float, radius: Float): FloatArray {
        val r = max(0f, min(radius, min(w, h) / 2f))
        val ax = x * state.sx + state.tx; val bx = (x + w) * state.sx + state.tx
        val ay = y * state.sy + state.ty; val by = (y + h) * state.sy + state.ty
        return floatArrayOf(min(ax, bx), min(ay, by), max(ax, bx), max(ay, by), r * abs(state.sx), r * abs(state.sy))
    }

    private inline fun clippedFill(x: Float, y: Float, w: Float, h: Float, radius: Float, fill: (FloatArray) -> Unit) {
        val saved = state.rclip
        if (saved == null || state.rot != null || state.sx <= 0f || state.sy <= 0f) {
            val r = rrectPoints(x, y, w, h, radius)
            return unclipped(x, y, w, h, radius) { fill(r) }
        }
        val b = boxPx(x, y, w, h, radius)
        val k = saved.box
        val e = 1e-3f
        val covers = b[0] <= k[0] + e && b[1] <= k[1] + e && b[2] >= k[2] - e && b[3] >= k[3] - e && b[4] <= k[4] + e && b[5] <= k[5] + e
        val sf = saved.safe
        val safe = (b[0] >= sf[0] && b[1] >= sf[1] && b[2] <= sf[2] && b[3] <= sf[3]) || (b[0] >= sf[4] && b[1] >= sf[5] && b[2] <= sf[6] && b[3] <= sf[7])
        if (!covers && (safe || b[4] > 0.5f || b[5] > 0.5f)) {
            val r = rrectPoints(x, y, w, h, radius)
            return unclipped(x, y, w, h, radius) { fill(r) }
        }
        val poly = if (covers) saved.edge else clipToBox(saved.edge, b[0], b[1], b[2], b[3])
        if (poly.size < 6) return
        state.rclip = saved.parent
        try {
            unclipped((k[0] - state.tx) / state.sx, (k[1] - state.ty) / state.sy, (k[2] - k[0]) / state.sx, (k[3] - k[1]) / state.sy, k[4] / state.sx) { fill(poly) }
        } finally {
            state.rclip = saved
        }
    }

    private fun clipToBox(p: FloatArray, l: Float, t: Float, r: Float, b: Float): FloatArray {
        var cur = p
        for (side in 0 until 4) {
            val n = cur.size / 2
            if (n == 0) break
            val bound = when (side) { 0 -> l; 1 -> t; 2 -> r; else -> b }
            val axis = side % 2
            val keepAbove = side < 2
            fun inside(i: Int) = if (keepAbove) cur[i * 2 + axis] >= bound else cur[i * 2 + axis] <= bound
            val out = FloatArray((n + 1) * 2)
            var k = 0
            for (i in 0 until n) {
                val j = (i + 1) % n
                val ii = inside(i); val ij = inside(j)
                if (ii) { out[k++] = cur[i * 2]; out[k++] = cur[i * 2 + 1] }
                if (ii != ij) {
                    val a0 = cur[i * 2 + axis]; val a1 = cur[j * 2 + axis]
                    val f = (bound - a0) / (a1 - a0)
                    val x = cur[i * 2] + (cur[j * 2] - cur[i * 2]) * f
                    val y = cur[i * 2 + 1] + (cur[j * 2 + 1] - cur[i * 2 + 1]) * f
                    if (axis == 0) { out[k++] = bound; out[k++] = y } else { out[k++] = x; out[k++] = bound }
                }
            }
            cur = out.copyOf(k)
        }
        return cur
    }

    private inline fun unclipped(x: Float, y: Float, w: Float, h: Float, radius: Float, draw: () -> Unit) {
        val saved = state.rclip ?: return draw()
        if (state.rot != null) return draw()
        val b = boxPx(x, y, w, h, radius)
        var c: RClip? = saved
        while (c != null) {
            val k = c.box
            val e = 1e-3f
            // A polygon's box doesn't say whether the drawing fits in it
            if (!c.rrect || b[0] < k[0] - e || b[1] < k[1] - e || b[2] > k[2] + e || b[3] > k[3] + e || b[4] < k[4] - e || b[5] < k[5] - e) break
            c = c.parent
        }
        state.rclip = c
        try {
            draw()
        } finally {
            state.rclip = saved
        }
    }

    // Geometry drawn with a gradient keeps sampling the gradient's texture
    private fun whitePage(): Any = if (shading != null) batchTexture else batchTexture as? PolyGlyphPage.Page ?: PolyGlyphPage.pages[0]

    protected fun begin(texture: Any, premultiplied: Boolean, nearest: Boolean = false) {
        if (count > 0 && (texture !== batchTexture || premultiplied != batchPremul || nearest != batchNearest)) flush()
        batchTexture = texture
        batchPremul = premultiplied
        batchNearest = nearest
    }

    protected fun quadPx(x0: Float, y0: Float, x1: Float, y1: Float, u0: Float, v0: Float, u1: Float, v1: Float, color: Int) {
        vertex(x0, y0, u0, v0, color)
        vertex(x0, y1, u0, v1, color)
        vertex(x1, y1, u1, v1, color)
        vertex(x1, y0, u1, v0, color)
    }

    protected fun resetTessellator() {
        state = State(1f, 1f, 0f, 0f, 1f, null, null)
        stack.clear()
        layers.clear()
        boxCount = 0
        count = 0
        qn = 0
        batchTexture = PolyGlyphPage.pages[0]
        batchPremul = false
        batchNearest = false
        minX = Float.MAX_VALUE; minY = Float.MAX_VALUE; maxX = -Float.MAX_VALUE; maxY = -Float.MAX_VALUE
    }

    protected fun flush() {
        for (l in layers) {
            if (l.mark != null) continue
            if (l.start > 0) submitPrefix(l.start)
            l.mark = layerMark()
        }
        if (count == 0) return
        submit(batchTexture, batchPremul, batchNearest, verts.copyOf(count * 4), colors.copyOf(count), floatArrayOf(minX, minY, maxX, maxY))
        count = 0
        minX = Float.MAX_VALUE; minY = Float.MAX_VALUE; maxX = -Float.MAX_VALUE; maxY = -Float.MAX_VALUE
    }

    private fun submitPrefix(n: Int) {
        val v = verts.copyOf(n * 4)
        submit(batchTexture, batchPremul, batchNearest, v, colors.copyOf(n), boundsOf(v))
        System.arraycopy(verts, n * 4, verts, 0, (count - n) * 4)
        System.arraycopy(colors, n, colors, 0, count - n)
        count -= n
        for (l in layers) l.start -= n
        val b = boundsOf(verts.copyOf(count * 4))
        minX = b[0]; minY = b[1]; maxX = b[2]; maxY = b[3]
    }

    private fun boundsOf(v: FloatArray): FloatArray {
        val b = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        for (i in 0 until v.size / 4) {
            b[0] = min(b[0], v[i * 4]); b[1] = min(b[1], v[i * 4 + 1])
            b[2] = max(b[2], v[i * 4]); b[3] = max(b[3], v[i * 4 + 1])
        }
        return b
    }

    private fun scaleArgb(color: Int, alpha: Float): Int {
        var out = 0
        var shift = 0
        while (shift < 32) {
            out = out or ((((color ushr shift) and 0xFF) * alpha).roundToInt().coerceIn(0, 255) shl shift)
            shift += 8
        }
        return out
    }

    protected fun mulAlpha(color: Int, alpha: Float): Int {
        if (alpha >= 1f) return color
        val a = ((color ushr 24) * alpha).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (color and 0x00FFFFFF)
    }

    private fun scaleAvg() = (state.sx + state.sy) / 2f

    private fun cornerSegments(radius: Float): Int {
        val r = radius * scaleAvg()
        return if (r <= 0.5f) 0 else min(16, max(2, ceil(sqrt(r) * 1.6f).toInt()))
    }

    private fun rrectPoints(x: Float, y: Float, w: Float, h: Float, radius: Float, segs: Int = cornerSegments(radius)): FloatArray {
        val r = max(0f, min(radius, min(w, h) / 2f))
        val out: FloatArray
        if (segs == 0) {
            out = floatArrayOf(x, y, x + w, y, x + w, y + h, x, y + h)
        } else {
            out = FloatArray(4 * (segs + 1) * 2)
            val trig = cornerTrig(segs)
            val per = (segs + 1) * 2
            for (corner in 0 until 4) {
                val cx = if (corner == 0 || corner == 3) x + r else x + w - r
                val cy = if (corner < 2) y + r else y + h - r
                var k = corner * per
                while (k < (corner + 1) * per) {
                    out[k] = cx + trig[k] * r
                    out[k + 1] = cy + trig[k + 1] * r
                    k += 2
                }
            }
        }
        transformInPlace(out)
        return out
    }

    private fun transformInPlace(p: FloatArray) {
        val s = state
        var i = 0
        while (i < p.size) {
            p[i] = p[i] * s.sx + s.tx
            p[i + 1] = p[i + 1] * s.sy + s.ty
            i += 2
        }
    }

    private val uvTmp = FloatArray(2)

    private fun fillPolygonPx(p: FloatArray, n: Int, color: Int, convex: Boolean, uv: ((Float, Float, FloatArray) -> Unit)? = null, keepBatch: Boolean = false, quads: IntArray? = null) {
        if (n < 3) return
        val c = mulAlpha(color, state.alpha)
        if (c ushr 24 == 0) return
        val clockwise = signedArea(p, n) > 0f
        val inner = FloatArray(n * 2)
        val outer = FloatArray(n * 2)
        offsetPolygon(p, n, if (clockwise) 1f else -1f, inner, outer)
        if (!keepBatch) begin(whitePage(), false)
        val iuv = uvs(inner, n, uv)
        val ouv = uvs(outer, n, uv)
        fun tri(a: Int, b: Int, d: Int) = quadVerts(
            inner[a * 2], inner[a * 2 + 1], iuv[a * 2], iuv[a * 2 + 1], c, inner[b * 2], inner[b * 2 + 1], iuv[b * 2], iuv[b * 2 + 1], c,
            inner[d * 2], inner[d * 2 + 1], iuv[d * 2], iuv[d * 2 + 1], c, inner[d * 2], inner[d * 2 + 1], iuv[d * 2], iuv[d * 2 + 1], c,
        )
        if (convex) {
            var i = 1
            while (i + 2 < n) {
                quadVerts(
                    inner[0], inner[1], iuv[0], iuv[1], c, inner[i * 2], inner[i * 2 + 1], iuv[i * 2], iuv[i * 2 + 1], c,
                    inner[i * 2 + 2], inner[i * 2 + 3], iuv[i * 2 + 2], iuv[i * 2 + 3], c, inner[i * 2 + 4], inner[i * 2 + 5], iuv[i * 2 + 4], iuv[i * 2 + 5], c,
                )
                i += 2
            }
            if (i + 1 < n) tri(0, i, i + 1)
        } else if (quads != null) {
            var q = 0
            while (q < quads.size) {
                val a = quads[q] * 2; val b = quads[q + 1] * 2; val d = quads[q + 2] * 2; val e = quads[q + 3] * 2
                quadVerts(
                    inner[a], inner[a + 1], iuv[a], iuv[a + 1], c, inner[b], inner[b + 1], iuv[b], iuv[b + 1], c,
                    inner[d], inner[d + 1], iuv[d], iuv[d + 1], c, inner[e], inner[e + 1], iuv[e], iuv[e + 1], c,
                )
                q += 4
            }
        } else {
            for (t in earClip(inner, n)) tri(t[0], t[1], t[2])
        }
        val clear = c and 0x00FFFFFF
        for (i in 0 until n) {
            val j = (i + 1) % n
            quadVerts(
                inner[i * 2], inner[i * 2 + 1], iuv[i * 2], iuv[i * 2 + 1], c, outer[i * 2], outer[i * 2 + 1], ouv[i * 2], ouv[i * 2 + 1], clear,
                outer[j * 2], outer[j * 2 + 1], ouv[j * 2], ouv[j * 2 + 1], clear, inner[j * 2], inner[j * 2 + 1], iuv[j * 2], iuv[j * 2 + 1], c,
            )
        }
    }

    private fun uvs(p: FloatArray, n: Int, uv: ((Float, Float, FloatArray) -> Unit)?): FloatArray {
        val out = FloatArray(n * 2)
        if (uv == null) { out.fill(PolyGlyphPage.WHITE_UV); return out }
        for (i in 0 until n) {
            uv(p[i * 2], p[i * 2 + 1], uvTmp)
            out[i * 2] = uvTmp[0]; out[i * 2 + 1] = uvTmp[1]
        }
        return out
    }

    private fun strokeRing(outerPts: FloatArray, innerPts: FloatArray, n: Int, color: Int, shade: ((Float, Float) -> Int)? = null, band: Boolean = true) {
        val c = mulAlpha(color, state.alpha)
        if ((shade == null && c ushr 24 == 0) || n < 3) return
        val dir = if (signedArea(outerPts, n) > 0f) 1f else -1f
        val oIn = FloatArray(n * 2); val oOut = FloatArray(n * 2)
        val iIn = FloatArray(n * 2); val iOut = FloatArray(n * 2)
        offsetPolygon(outerPts, n, dir, oIn, oOut)
        offsetPolygon(innerPts, n, -dir, iIn, iOut)
        begin(whitePage(), false)
        val w = PolyGlyphPage.WHITE_UV
        fun at(p: FloatArray, i: Int) = if (shade == null) c else mulAlpha(shade(p[i * 2], p[i * 2 + 1]), state.alpha)
        fun clear(p: FloatArray, i: Int) = at(p, i) and 0x00FFFFFF
        val oc = IntArray(n); val ic = IntArray(n); val ocl = IntArray(n); val icl = IntArray(n)
        for (i in 0 until n) {
            oc[i] = at(oIn, i); ic[i] = at(iIn, i)
            ocl[i] = clear(oOut, i); icl[i] = clear(iOut, i)
        }
        for (i in 0 until n) {
            val j = (i + 1) % n
            val a = i * 2; val b = j * 2
            if (band) quadVerts(oIn[a], oIn[a + 1], w, w, oc[i], iIn[a], iIn[a + 1], w, w, ic[i], iIn[b], iIn[b + 1], w, w, ic[j], oIn[b], oIn[b + 1], w, w, oc[j])
            quadVerts(oIn[a], oIn[a + 1], w, w, oc[i], oOut[a], oOut[a + 1], w, w, ocl[i], oOut[b], oOut[b + 1], w, w, ocl[j], oIn[b], oIn[b + 1], w, w, oc[j])
            quadVerts(iIn[a], iIn[a + 1], w, w, ic[i], iOut[a], iOut[a + 1], w, w, icl[i], iOut[b], iOut[b + 1], w, w, icl[j], iIn[b], iIn[b + 1], w, w, ic[j])
        }
    }

    private fun densify(outer: FloatArray, inner: FloatArray, step: Float = 4f): Pair<FloatArray, FloatArray> {
        val n = outer.size / 2
        val ks = IntArray(n) { i ->
            val j = (i + 1) % n
            max(1, ceil(hypot(outer[j * 2] - outer[i * 2], outer[j * 2 + 1] - outer[i * 2 + 1]) / step).toInt())
        }
        val o = FloatArray(ks.sum() * 2)
        val i2 = FloatArray(o.size)
        var p = 0
        for (i in 0 until n) {
            val j = (i + 1) % n
            val k = ks[i]
            for (t in 0 until k) {
                val f = t.toFloat() / k
                o[p] = outer[i * 2] + (outer[j * 2] - outer[i * 2]) * f; o[p + 1] = outer[i * 2 + 1] + (outer[j * 2 + 1] - outer[i * 2 + 1]) * f
                i2[p] = inner[i * 2] + (inner[j * 2] - inner[i * 2]) * f; i2[p + 1] = inner[i * 2 + 1] + (inner[j * 2 + 1] - inner[i * 2 + 1]) * f
                p += 2
            }
        }
        return o to i2
    }

    private fun signedArea(p: FloatArray, n: Int): Float {
        var a = 0f
        for (i in 0 until n) {
            val j = (i + 1) % n
            a += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1]
        }
        return a / 2f
    }

    private fun offsetPolygon(p: FloatArray, n: Int, dir: Float, inner: FloatArray, outer: FloatArray) {
        fun same(a: Int, b: Int) = abs(p[a * 2] - p[b * 2]) < 1e-4f && abs(p[a * 2 + 1] - p[b * 2 + 1]) < 1e-4f
        for (i in 0 until n) {
            var prev = (i + n - 1) % n
            var next = (i + 1) % n
            for (s in 2 until n) { if (!same(prev, i)) break; prev = (i + n - s) % n }
            for (s in 2 until n) { if (!same(next, i)) break; next = (i + s) % n }
            var e0x = p[i * 2] - p[prev * 2]; var e0y = p[i * 2 + 1] - p[prev * 2 + 1]
            var e1x = p[next * 2] - p[i * 2]; var e1y = p[next * 2 + 1] - p[i * 2 + 1]
            val l0 = hypot(e0x, e0y).takeIf { it > 1e-4f } ?: 1f
            val l1 = hypot(e1x, e1y).takeIf { it > 1e-4f } ?: 1f
            e0x /= l0; e0y /= l0; e1x /= l1; e1y /= l1
            val n0x = -e0y * dir; val n0y = e0x * dir
            val n1x = -e1y * dir; val n1y = e1x * dir
            var mx = (n0x + n1x) / 2f
            var my = (n0y + n1y) / 2f
            val d2 = mx * mx + my * my
            if (d2 > 1e-6f) {
                val scale = min(1f / d2, 4f)
                mx *= scale; my *= scale
            }
            val x = p[i * 2]; val y = p[i * 2 + 1]
            inner[i * 2] = x + mx * 0.5f; inner[i * 2 + 1] = y + my * 0.5f
            outer[i * 2] = x - mx * 0.5f; outer[i * 2 + 1] = y - my * 0.5f
        }
    }

    private fun earClip(p: FloatArray, n: Int): List<IntArray> {
        val idx = ArrayList<Int>(n).apply { for (i in 0 until n) add(i) }
        val ccw = signedArea(p, n) < 0f
        val out = ArrayList<IntArray>(n)
        var guard = n * n
        while (idx.size > 3 && guard-- > 0) {
            var clipped = false
            for (k in idx.indices) {
                val a = idx[(k + idx.size - 1) % idx.size]
                val b = idx[k]
                val c = idx[(k + 1) % idx.size]
                val cross = (p[b * 2] - p[a * 2]) * (p[c * 2 + 1] - p[a * 2 + 1]) - (p[b * 2 + 1] - p[a * 2 + 1]) * (p[c * 2] - p[a * 2])
                if (if (ccw) cross >= 0f else cross <= 0f) continue
                var inside = false
                for (o in idx) {
                    if (o == a || o == b || o == c) continue
                    val ox = p[o * 2]; val oy = p[o * 2 + 1]
                    if ((ox == p[a * 2] && oy == p[a * 2 + 1]) || (ox == p[b * 2] && oy == p[b * 2 + 1]) || (ox == p[c * 2] && oy == p[c * 2 + 1])) continue
                    if (pointInTri(p[o * 2], p[o * 2 + 1], p, a, b, c)) { inside = true; break }
                }
                if (inside) continue
                out.add(intArrayOf(a, b, c))
                idx.removeAt(k)
                clipped = true
                break
            }
            if (!clipped) break
        }
        if (idx.size == 3) out.add(intArrayOf(idx[0], idx[1], idx[2]))
        return out
    }

    private fun pointInTri(px: Float, py: Float, p: FloatArray, a: Int, b: Int, c: Int): Boolean {
        fun side(x1: Float, y1: Float, x2: Float, y2: Float) = (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2)
        val d1 = side(p[a * 2], p[a * 2 + 1], p[b * 2], p[b * 2 + 1])
        val d2 = side(p[b * 2], p[b * 2 + 1], p[c * 2], p[c * 2 + 1])
        val d3 = side(p[c * 2], p[c * 2 + 1], p[a * 2], p[a * 2 + 1])
        val neg = d1 < 0f || d2 < 0f || d3 < 0f
        val pos = d1 > 0f || d2 > 0f || d3 > 0f
        return !(neg && pos)
    }

    private fun flatten(path: PolyPath): List<FloatArray> {
        val loops = ArrayList<FloatArray>()
        var cur = ArrayList<Float>()
        var lastX = 0f
        var lastY = 0f
        fun add(x: Float, y: Float) { cur.add(x); cur.add(y); lastX = x; lastY = y }
        path.visit(object : PolyPath.Visitor {
            override fun moveTo(x: Float, y: Float) {
                if (cur.size >= 6) loops.add(cur.toFloatArray())
                cur = ArrayList()
                add(x, y)
            }

            override fun lineTo(x: Float, y: Float) = add(x, y)

            override fun tangentArcTo(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float) {
                val ax = lastX - x1; val ay = lastY - y1
                val bx = x2 - x1; val by = y2 - y1
                val la = hypot(ax, ay); val lb = hypot(bx, by)
                if (la < 1e-4f || lb < 1e-4f || radius <= 0f) { add(x1, y1); return }
                val ux = ax / la; val uy = ay / la
                val vx = bx / lb; val vy = by / lb
                val cosT = (ux * vx + uy * vy).coerceIn(-1f, 1f)
                val theta = acos(cosT)
                if (theta < 1e-4f || theta > Math.PI.toFloat() - 1e-4f) { add(x1, y1); return }
                val dist = radius / tan(theta / 2f)
                val t0x = x1 + ux * dist; val t0y = y1 + uy * dist
                val t1x = x1 + vx * dist; val t1y = y1 + vy * dist
                val bisx = ux + vx; val bisy = uy + vy
                val bl = hypot(bisx, bisy)
                val cd = radius / sin(theta / 2f)
                val ccx = x1 + bisx / bl * cd; val ccy = y1 + bisy / bl * cd
                val a0 = atan2(t0y - ccy, t0x - ccx)
                var a1 = atan2(t1y - ccy, t1x - ccx)
                var sweep = a1 - a0
                while (sweep > Math.PI) sweep -= (2 * Math.PI).toFloat()
                while (sweep < -Math.PI) sweep += (2 * Math.PI).toFloat()
                a1 = a0 + sweep
                add(t0x, t0y)
                val segs = max(2, cornerSegments(radius))
                for (i in 1..segs) {
                    val a = a0 + (a1 - a0) * i / segs
                    add(ccx + cos(a) * radius, ccy + sin(a) * radius)
                }
            }

            override fun close() {
                if (cur.size >= 6) loops.add(cur.toFloatArray())
                cur = ArrayList()
            }
        })
        if (cur.size >= 6) loops.add(cur.toFloatArray())
        return loops.map { dedupe(it) }
    }

    private fun dedupe(p: FloatArray): FloatArray {
        val out = FloatArray(p.size)
        var n = 0
        var i = 0
        while (i < p.size) {
            val x = p[i]; val y = p[i + 1]
            if (n < 2 || hypot(out[n - 2] - x, out[n - 1] - y) > 1e-3f) { out[n++] = x; out[n++] = y }
            i += 2
        }
        if (n >= 4 && hypot(out[0] - out[n - 2], out[1] - out[n - 1]) <= 1e-3f) n -= 2
        return if (n == out.size) out else out.copyOf(n)
    }

    private fun quadVerts(
        x0: Float, y0: Float, u0: Float, v0: Float, c0: Int, x1: Float, y1: Float, u1: Float, v1: Float, c1: Int,
        x2: Float, y2: Float, u2: Float, v2: Float, c2: Int, x3: Float, y3: Float, u3: Float, v3: Float, c3: Int,
    ) {
        val area = (x0 * y1 - x1 * y0) + (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3)
        if (area > 0f) {
            vertex(x0, y0, u0, v0, c0)
            vertex(x3, y3, u3, v3, c3)
            vertex(x2, y2, u2, v2, c2)
            vertex(x1, y1, u1, v1, c1)
            return
        }
        vertex(x0, y0, u0, v0, c0)
        vertex(x1, y1, u1, v1, c1)
        vertex(x2, y2, u2, v2, c2)
        vertex(x3, y3, u3, v3, c3)
    }

    private val qx = FloatArray(4); private val qy = FloatArray(4); private val qu = FloatArray(4); private val qv = FloatArray(4)
    private val qc = IntArray(4)
    private var qn = 0

    private fun vertex(x: Float, y: Float, u: Float, v: Float, color: Int) {
        val m = state.rot
        if (m == null) { qx[qn] = x; qy[qn] = y } else { qx[qn] = m[0] * x + m[1] * y + m[4]; qy[qn] = m[2] * x + m[3] * y + m[5] }
        val sh = shading
        if (sh == null) { qu[qn] = u; qv[qn] = v } else { sh(x, y, uvTmp); qu[qn] = uvTmp[0]; qv[qn] = uvTmp[1] }
        qc[qn] = color
        if (++qn < 4) return
        qn = 0
        val box = state.clip
        var crosses = 0
        if (box != null) {
            val x0 = min(min(qx[0], qx[1]), min(qx[2], qx[3])); val x1 = max(max(qx[0], qx[1]), max(qx[2], qx[3]))
            val y0 = min(min(qy[0], qy[1]), min(qy[2], qy[3])); val y1 = max(max(qy[0], qy[1]), max(qy[2], qy[3]))
            if (x1 <= box[0] || x0 >= box[2] || y1 <= box[1] || y0 >= box[3]) return
            if (x0 < box[0]) crosses = 1
            if (x1 > box[2]) crosses = crosses or 2
            if (y0 < box[1]) crosses = crosses or 4
            if (y1 > box[3]) crosses = crosses or 8
            if (crosses != 0 && clearOutside(box)) crosses = 0
        }
        var rclip = state.rclip
        while (rclip != null && quadInside(rclip)) rclip = rclip.parent
        var mask = state.mask
        while (mask != null && maskOpaque(mask.fade)) mask = mask.parent
        val hole = state.hole
        if (crosses == 0 && rclip == null && mask == null && hole == null) {
            for (i in 0 until 4) emit(qx[i], qy[i], qu[i], qv[i], qc[i])
        } else {
            val quad = Poly()
            for (i in 0 until 4) quad.add(qx[i], qy[i], qu[i], qv[i], qc[i])
            var polys = listOf(quad)
            if (crosses != 0) polys = boxCut(quad, box!!, crosses)
            if (rclip != null && polys.isNotEmpty()) polys = clipQuad(polys, rclip, ::cut)
            if (hole != null && polys.isNotEmpty()) polys = clipQuad(polys, hole, ::cutHole)
            while (mask != null) {
                val m = mask.fade
                polys = polys.flatMap { faded(it, m) }
                mask = mask.parent
            }
            for (p in polys) emitFan(p)
        }
    }

    private fun maskOpaque(fade: PolyFade): Boolean {
        val pos = if (fade.vertical) qy else qx
        val span = fade.span(pos[0])
        return fade.opaque(span) && (1 until 4).all { fade.span(pos[it]) == span }
    }

    private fun faded(p: Poly, fade: PolyFade): List<Poly> {
        val out = ArrayList<Poly>()
        var rest = p
        for (stop in fade.stops) {
            if (rest.size < 3) break
            val before = Poly()
            rest = splitAxis(rest, fade.vertical, stop, before)
            if (before.size >= 3) out.add(before)
        }
        if (rest.size >= 3) out.add(rest)
        val tint = fade.colors != null
        for (piece in out) for (i in 0 until piece.size) {
            val pos = if (fade.vertical) piece.y[i] else piece.x[i]
            val c = coverColor(piece.c[i], fade.alphaAt(pos), batchPremul)
            piece.c[i] = if (tint) modulate(c, fade.colorAt(pos)) else c
        }
        return out
    }

    private fun modulate(color: Int, rgb: Int): Int {
        var out = color and -0x1000000
        var shift = 0
        while (shift < 24) {
            out = out or ((((color ushr shift) and 0xFF) * ((rgb ushr shift) and 0xFF) + 127) / 255 shl shift)
            shift += 8
        }
        return out
    }

    private fun clearOutside(box: FloatArray): Boolean {
        val e = 0.5f + 1e-3f
        for (i in 0 until 4) {
            val x = qx[i]; val y = qy[i]
            if (x >= box[0] && x <= box[2] && y >= box[1] && y <= box[3]) continue
            if (x < box[0] - e || x > box[2] + e || y < box[1] - e || y > box[3] + e) return false
            if (if (batchPremul) qc[i] != 0 else qc[i] ushr 24 != 0) return false
        }
        return true
    }

    private fun boxCut(p: Poly, box: FloatArray, edges: Int): List<Poly> {
        var rest = p
        if (edges and 1 != 0) rest = splitAxis(rest, false, box[0], null)
        if (edges and 2 != 0 && rest.size >= 3) rest = splitAxis(rest, false, box[2], null, -1f)
        if (edges and 4 != 0 && rest.size >= 3) rest = splitAxis(rest, true, box[1], null)
        if (edges and 8 != 0 && rest.size >= 3) rest = splitAxis(rest, true, box[3], null, -1f)
        return if (rest.size >= 3) listOf(rest) else emptyList()
    }

    private fun splitAxis(p: Poly, vertical: Boolean, at: Float, before: Poly?, sign: Float = 1f): Poly {
        val after = Poly()
        for (i in 0 until p.size) {
            val j = (i + 1) % p.size
            val di = ((if (vertical) p.y[i] else p.x[i]) - at) * sign
            val dj = ((if (vertical) p.y[j] else p.x[j]) - at) * sign
            if (di >= 0f) after.addFrom(p, i) else before?.addFrom(p, i)
            if ((di >= 0f) != (dj >= 0f)) {
                val t = di / (di - dj)
                val x = p.x[i] + (p.x[j] - p.x[i]) * t; val y = p.y[i] + (p.y[j] - p.y[i]) * t
                val u = p.u[i] + (p.u[j] - p.u[i]) * t; val v = p.v[i] + (p.v[j] - p.v[i]) * t
                val c = PolyGradient.lerpArgb(p.c[i], p.c[j], t)
                after.add(x, y, u, v, c)
                before?.add(x, y, u, v, c)
            }
        }
        return after
    }

    private fun emit(x: Float, y: Float, u: Float, v: Float, color: Int) {
        if (count == colors.size) {
            colors = colors.copyOf(count * 2)
            verts = verts.copyOf(count * 8)
        }
        val o = count * 4
        verts[o] = x; verts[o + 1] = y; verts[o + 2] = u; verts[o + 3] = v
        colors[count++] = color
        if (boxCount > 0) touch(x, y, x, y)
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
    }

    private fun edgeSide(clip: FloatArray, e: Int, x: Float, y: Float): Float {
        val n = clip.size / 2
        val ax = clip[e * 2]; val ay = clip[e * 2 + 1]
        val bx = clip[(e + 1) % n * 2]; val by = clip[(e + 1) % n * 2 + 1]
        return (bx - ax) * (y - ay) - (by - ay) * (x - ax)
    }

    private fun quadInside(clip: RClip): Boolean {
        for (i in 0 until 4) if (!inside(clip, qx[i], qy[i])) return false
        return true
    }

    private fun inside(clip: RClip, x: Float, y: Float) = inSafe(clip, x, y) || insideConvex(clip.inner, x, y)

    private fun inSafe(clip: RClip, x: Float, y: Float): Boolean {
        val s = clip.safe
        return (x >= s[0] && x <= s[2] && y >= s[1] && y <= s[3]) || (x >= s[4] && x <= s[6] && y >= s[5] && y <= s[7])
    }

    private fun insideConvex(clip: FloatArray, x: Float, y: Float): Boolean {
        for (e in 0 until clip.size / 2) if (edgeSide(clip, e, x, y) < 0f) return false
        return true
    }

    private class Poly {
        var x = FloatArray(8); var y = FloatArray(8); var u = FloatArray(8); var v = FloatArray(8)
        var c = IntArray(8)
        var size = 0

        fun add(px: Float, py: Float, pu: Float, pv: Float, pc: Int) {
            if (size == c.size) {
                val n = size * 2
                x = x.copyOf(n); y = y.copyOf(n); u = u.copyOf(n); v = v.copyOf(n); c = c.copyOf(n)
            }
            x[size] = px; y[size] = py; u[size] = pu; v[size] = pv; c[size] = pc
            size++
        }

        fun addFrom(p: Poly, i: Int) = add(p.x[i], p.y[i], p.u[i], p.v[i], p.c[i])
    }

    private fun split(p: Poly, clip: FloatArray, e: Int, outside: Poly?): Poly {
        var allIn = true
        var allOut = true
        for (i in 0 until p.size) {
            if (edgeSide(clip, e, p.x[i], p.y[i]) >= 0f) allOut = false else allIn = false
        }
        if (allIn) return p
        if (allOut) {
            if (outside != null) for (i in 0 until p.size) outside.addFrom(p, i)
            return Poly()
        }
        val inside = Poly()
        for (i in 0 until p.size) {
            val j = (i + 1) % p.size
            val di = edgeSide(clip, e, p.x[i], p.y[i])
            val dj = edgeSide(clip, e, p.x[j], p.y[j])
            if (di >= 0f) inside.addFrom(p, i) else outside?.addFrom(p, i)
            if ((di >= 0f) != (dj >= 0f)) {
                val t = di / (di - dj)
                val x = p.x[i] + (p.x[j] - p.x[i]) * t; val y = p.y[i] + (p.y[j] - p.y[i]) * t
                val u = p.u[i] + (p.u[j] - p.u[i]) * t; val v = p.v[i] + (p.v[j] - p.v[i]) * t
                val c = PolyGradient.lerpArgb(p.c[i], p.c[j], t)
                inside.add(x, y, u, v, c)
                outside?.add(x, y, u, v, c)
            }
        }
        return inside
    }

    private inline fun clipQuad(quad: List<Poly>, clip: RClip, cut: (Poly, RClip, MutableList<Poly>) -> Unit): List<Poly> {
        var polys = quad
        var c: RClip? = clip
        while (c != null && polys.isNotEmpty()) {
            val next = ArrayList<Poly>()
            for (p in polys) cut(p, c, next)
            polys = next
            c = c.parent
        }
        return polys
    }

    // Keeps what lies outside the hole, fading out across the pixel straddling its edge
    private fun cutHole(p: Poly, hole: RClip, out: MutableList<Poly>) {
        var rest = p
        for (e in 0 until 4) {
            val outside = Poly()
            rest = split(rest, hole.outer, e, outside)
            if (outside.size >= 3) out.add(outside)
            if (rest.size < 3) return
        }
        for (e in 0 until 4) {
            rest = band(rest, hole.inner, e, corners = true) { b ->
                for (i in 0 until b.size) b.c[i] = coverColor(b.c[i], 1f - coverage(hole, b.x[i], b.y[i]), batchPremul)
                out.add(b)
            }
            if (rest.size < 3) return
        }
    }

    // Splits off what lies past inner edge e; with corners, also where later edges' fades start, so each piece's fade stays linear
    private inline fun band(rest: Poly, inner: FloatArray, e: Int, corners: Boolean, add: (Poly) -> Unit): Poly {
        var past = Poly()
        val inside = split(rest, inner, e, past)
        if (corners && past.size >= 3) {
            var corner = Poly()
            for (f in e + 1 until inner.size / 2) {
                past = split(past, inner, f, corner)
                if (corner.size > 0) {
                    if (corner.size >= 3) add(corner)
                    corner = Poly()
                }
                if (past.size < 3) break
            }
        }
        if (past.size >= 3) add(past)
        return inside
    }

    private fun cut(p: Poly, clip: RClip, out: MutableList<Poly>) {
        for (i in 0 until p.size) if (!inside(clip, p.x[i], p.y[i])) return cutCorners(p, clip, out)
        out.add(p)
    }

    private fun cutCorners(p: Poly, clip: RClip, out: MutableList<Poly>) {
        val k = clip.box
        val cols = ArrayList<Poly>(3)
        var rest = p
        for (x in floatArrayOf(k[0] + k[4], k[2] - k[4])) {
            if (!crosses(rest.x, rest.size, x)) continue
            val before = Poly()
            rest = splitAxis(rest, false, x, before)
            if (before.size >= 3) cols.add(before)
        }
        if (rest.size >= 3) cols.add(rest)
        for (col in cols) {
            var r = col
            for (y in floatArrayOf(k[1] + k[5], k[3] - k[5])) {
                if (!crosses(r.y, r.size, y)) continue
                val before = Poly()
                r = splitAxis(r, true, y, before)
                if (before.size >= 3) cutInside(before, clip, out)
            }
            if (r.size >= 3) cutInside(r, clip, out)
        }
    }

    private fun crosses(v: FloatArray, n: Int, at: Float): Boolean {
        var lo = false
        var hi = false
        for (i in 0 until n) {
            if (v[i] < at - 1f) lo = true
            if (v[i] > at + 1f) hi = true
        }
        return lo && hi
    }

    private fun cutInside(p: Poly, clip: RClip, out: MutableList<Poly>) {
        for (i in 0 until p.size) if (!inside(clip, p.x[i], p.y[i])) return cutPiece(p, clip, out)
        out.add(p)
    }

    private fun cutPiece(p: Poly, clip: RClip, out: MutableList<Poly>) {
        var rest = p
        for (e in 0 until clip.outer.size / 2) {
            if (rest.size < 3) return
            rest = split(rest, clip.outer, e, null)
        }
        for (e in 0 until clip.inner.size / 2) {
            if (rest.size < 3) return
            // cutCorners already keeps a rounded rect's straight sides apart from its corners
            rest = band(rest, clip.inner, e, corners = !clip.rrect) { out.add(covered(it, clip)) }
        }
        if (rest.size >= 3) out.add(covered(rest, clip))
    }

    private fun covered(p: Poly, clip: RClip): Poly {
        for (i in 0 until p.size) if (!inSafe(clip, p.x[i], p.y[i])) p.c[i] = coverColor(p.c[i], coverage(clip, p.x[i], p.y[i]), batchPremul)
        return p
    }

    private fun emitFan(p: Poly) {
        if (p.size < 3) return
        var i = 1
        while (i + 1 < p.size) {
            val k = if (i + 2 < p.size) i + 2 else i + 1
            emit(p.x[0], p.y[0], p.u[0], p.v[0], p.c[0])
            emit(p.x[i], p.y[i], p.u[i], p.v[i], p.c[i])
            emit(p.x[i + 1], p.y[i + 1], p.u[i + 1], p.v[i + 1], p.c[i + 1])
            emit(p.x[k], p.y[k], p.u[k], p.v[k], p.c[k])
            i += 2
        }
    }

    private fun coverage(clip: RClip, x: Float, y: Float): Float {
        var d = Float.MAX_VALUE
        for (e in 0 until clip.invLen.size) d = min(d, edgeSide(clip.edge, e, x, y) * clip.invLen[e])
        return (d + 0.5f).coerceIn(0f, 1f)
    }

    private fun coverColor(color: Int, cov: Float, premul: Boolean): Int {
        if (cov >= 1f) return color
        if (!premul) return mulAlpha(color, cov)
        var out = 0
        var shift = 0
        while (shift <= 24) {
            out = out or ((((color ushr shift) and 0xFF) * cov).roundToInt().coerceIn(0, 255) shl shift)
            shift += 8
        }
        return out
    }

    private class ShadowMask(val bitmap: PolyBitmap, val slice: Float, val extent: Float, val width: Float)

    companion object {
        private const val MAX_FADE_DRAWS = 16

        private const val NEAREST_TIE_BIAS = 1f / 256f

        // Holes are only cut along their edges, so they need no safe areas or box
        private val NONE = FloatArray(0)

        // Inverted, so no point is known to be inside a polygon without testing its edges
        private val NO_SAFE = floatArrayOf(0f, 0f, -1f, -1f, 0f, 0f, -1f, -1f)

        fun layerTargetSize(current: Int, needed: Int): Int = if (current >= needed) current else (needed + 63) / 64 * 64
        const val FALLBACK_FONT = "unifont"

        private val cornerTrigs = arrayOfNulls<FloatArray>(17)

        private fun cornerTrig(segs: Int): FloatArray = cornerTrigs[segs] ?: FloatArray(4 * (segs + 1) * 2).also { t ->
            var k = 0
            for (corner in 0 until 4) {
                val start = Math.PI.toFloat() * (1f + corner * 0.5f)
                for (i in 0..segs) {
                    val a = start + (Math.PI.toFloat() / 2f) * i / segs
                    t[k++] = cos(a)
                    t[k++] = sin(a)
                }
            }
            cornerTrigs[segs] = t
        }

        private fun <K, V> lru(max: Int): MutableMap<K, V> = Collections.synchronizedMap(object : LinkedHashMap<K, V>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > max
        })

        private val ramps = lru<Pair<List<PolyColor>, List<Float>?>, PolyBitmap>(64)
        private val boxRasters = lru<Pair<PolyGradient, Int>, PolyBitmap>(32)
        private val masks = lru<Long, ShadowMask>(32)
        private val mcOutlines = lru<Pair<String, Int>, FloatArray>(256)
        private val mcRasters = HashMap<Pair<PolyGlyphPage.Face, Int>, PolyGlyphPage.Raster?>()

        private const val MC_GRID = 2

        // nearest-samples the glyph's alpha at outline grid resolution, positioned like a face raster on the baseline
        private fun mcRaster(g: McGlyph): PolyGlyphPage.Raster? {
            val b = g.bitmap ?: return null
            val w = (g.width * MC_GRID).roundToInt()
            val h = (g.height * MC_GRID).roundToInt()
            if (w <= 0 || h <= 0) return null
            val coverage = ByteArray(w * h) { k ->
                val sx = ((g.u0 + (g.u1 - g.u0) * (k % w + 0.5f) / w) * b.width).toInt().coerceIn(0, b.width - 1)
                val sy = ((g.v0 + (g.v1 - g.v0) * (k / w + 0.5f) / h) * b.height).toInt().coerceIn(0, b.height - 1)
                b.pixels[(sy * b.width + sx) * 4 + 3]
            }
            return PolyGlyphPage.Raster(0, ((g.top - 7f) * MC_GRID).roundToInt(), w, h, coverage)
        }

        private fun mcOutline(r: PolyGlyphPage.Rasterizer, text: String): FloatArray {
            val placed = ArrayList<PolyGlyphPage.Raster>()
            val at = ArrayList<Int>()
            val slant = ArrayList<Int>()
            val effects = ArrayList<Int>()
            text.lines().forEachIndexed { index, line ->
                val lineTop = index * 9
                val baseline = (lineTop + 7) * MC_GRID
                var pen = 0f
                var bold = false
                var italic = false
                var under = false
                var strike = false
                var i = 0
                while (i < line.length) {
                    val cp = line.codePointAt(i)
                    i += Character.charCount(cp)
                    if (cp == 0xA7 && i < line.length) {
                        val code = MC_CODES.indexOf(line[i++].lowercaseChar())
                        when (code) {
                            in 0..15, 17 -> { bold = false; italic = false; under = false; strike = false }
                            16 -> bold = true
                            18 -> strike = true
                            19 -> under = true
                            20 -> italic = true
                        }
                        continue
                    }
                    fun place(g: PolyGlyphPage.Raster?, x: Float) {
                        if (g == null || g.w <= 0 || g.h <= 0) return
                        placed += g
                        at += (x * MC_GRID).roundToInt() + g.offX
                        at += baseline + g.offY
                        slant += if (italic) lineTop * MC_GRID else Int.MIN_VALUE
                    }
                    val start = pen
                    val rg = McGlyphs.source?.invoke(cp)
                    if (rg != null) {
                        val g = mcRaster(rg)
                        place(g, pen)
                        if (bold) place(g, pen + rg.offset)
                        pen += rg.advance + if (bold) rg.offset else 0f
                    } else {
                        val main = r.face(if (bold) "minecraft-bold" else "minecraft")
                        val face: PolyGlyphPage.Face
                        val em: Float
                        if (main != null && main.glyph(cp) != 0) { face = main; em = 10f } else { face = r.face("unifont") ?: continue; em = 8f }
                        val id = face.glyph(cp)
                        place(mcRasters.getOrPut(face to id) { face.raster(id, em * MC_GRID) }, pen)
                        pen += face.advance(id) * em
                    }
                    fun effect(top: Float) {
                        effects += ((start - 1f) * MC_GRID).roundToInt(); effects += ((lineTop + top) * MC_GRID).roundToInt()
                        effects += (pen * MC_GRID).roundToInt(); effects += ((lineTop + top + 1f) * MC_GRID).roundToInt()
                    }
                    if (under) effect(8f)
                    if (strike) effect(3.5f)
                }
            }
            if (placed.isEmpty() && effects.isEmpty()) return FloatArray(0)
            val grow = MC_GRID
            var x0 = Int.MAX_VALUE; var y0 = Int.MAX_VALUE; var x1 = Int.MIN_VALUE; var y1 = Int.MIN_VALUE
            for (k in placed.indices) {
                val pad = if (slant[k] == Int.MIN_VALUE) 0 else 3 * MC_GRID
                x0 = min(x0, at[k * 2] - pad); y0 = min(y0, at[k * 2 + 1])
                x1 = max(x1, at[k * 2] + placed[k].w + pad); y1 = max(y1, at[k * 2 + 1] + placed[k].h)
            }
            for (k in effects.indices step 4) {
                x0 = min(x0, effects[k]); y0 = min(y0, effects[k + 1]); x1 = max(x1, effects[k + 2]); y1 = max(y1, effects[k + 3])
            }
            x0 -= grow; y0 -= grow; x1 += grow; y1 += grow
            val w = x1 - x0
            val h = y1 - y0
            val ink = BooleanArray(w * h)
            for (k in placed.indices) {
                val g = placed[k]
                val gx = at[k * 2] - x0
                val gy = at[k * 2 + 1] - y0
                val lt = slant[k]
                for (py in 0 until g.h) {
                    val dx = if (lt == Int.MIN_VALUE) 0 else ((1f - 0.25f * ((at[k * 2 + 1] + py + 0.5f - lt) / MC_GRID)) * MC_GRID).roundToInt()
                    for (px in 0 until g.w) {
                        if ((g.coverage[py * g.w + px].toInt() and 0xFF) >= 128) ink[(gy + py) * w + (gx + px + dx).coerceIn(0, w - 1)] = true
                    }
                }
            }
            for (k in effects.indices step 4) {
                for (py in effects[k + 1] until effects[k + 3]) for (px in effects[k] until effects[k + 2]) ink[(py - y0) * w + px - x0] = true
            }
            val wide = BooleanArray(w * h)
            for (py in 0 until h) for (px in 0 until w) {
                if (!ink[py * w + px]) continue
                for (d in max(0, px - grow)..min(w - 1, px + grow)) wide[py * w + d] = true
            }
            val grown = BooleanArray(w * h)
            for (py in 0 until h) for (px in 0 until w) {
                if (!wide[py * w + px]) continue
                for (d in max(0, py - grow)..min(h - 1, py + grow)) grown[d * w + px] = true
            }
            val out = ArrayList<Float>()
            var open = HashMap<Long, Int>()
            for (py in 0..h) {
                val next = HashMap<Long, Int>()
                if (py < h) {
                    var px = 0
                    while (px < w) {
                        if (!grown[py * w + px]) { px++; continue }
                        val start = px
                        while (px < w && grown[py * w + px]) px++
                        val key = (start.toLong() shl 32) or px.toLong()
                        next[key] = open.remove(key) ?: py
                    }
                }
                for ((key, top) in open) {
                    out += ((key ushr 32).toInt() + x0).toFloat() / MC_GRID
                    out += (top + y0).toFloat() / MC_GRID
                    out += (key.toInt() + x0).toFloat() / MC_GRID
                    out += (py + y0).toFloat() / MC_GRID
                }
                open = next
            }
            return out.toFloatArray()
        }

        private fun bitmapOf(w: Int, h: Int, argb: (Int, Int) -> Int): PolyBitmap {
            val px = ByteArray(w * h * 4)
            for (y in 0 until h) for (x in 0 until w) {
                val c = argb(x, y)
                val o = (y * w + x) * 4
                px[o] = (c shr 16).toByte(); px[o + 1] = (c shr 8).toByte(); px[o + 2] = c.toByte(); px[o + 3] = (c ushr 24).toByte()
            }
            return PolyBitmap(w, h, px)
        }

        private fun ramp(g: PolyGradient): PolyBitmap = ramps.getOrPut(g.colors to g.stops) { bitmapOf(256, 1) { x, _ -> g.sample(x / 255f) } }

        private fun boxRaster(g: PolyGradient, aspect: Float): PolyBitmap {
            val q = (aspect * 16f).roundToInt().coerceIn(1, 1024)
            return boxRasters.getOrPut(g to q) {
                val a = q / 16f
                val w = if (a >= 1f) 64 else max(8, (64 * a).roundToInt())
                val h = if (a >= 1f) max(8, (64 / a).roundToInt()) else 64
                bitmapOf(w, h) { x, y -> g.colorAt((x + 0.5f) / w * a, (y + 0.5f) / h, 0f, 0f, a, 1f) }
            }
        }

        private const val MAX_MASK_SIGMA = 6f
        private const val HOLE_STEPS = 5

        private fun shadowMask(rPx: Int, sigmaQ: Int): ShadowMask {
            val d = max(1, ceil(sigmaQ / 2f / MAX_MASK_SIGMA).toInt())
            val rT = (rPx.toFloat() / d).roundToInt()
            val qT = max(1, (sigmaQ.toFloat() / d).roundToInt())
            return masks.getOrPut((rT.toLong() shl 32) or (qT.toLong() shl 16) or d.toLong()) { shadowMask(rT, qT, d) }
        }

        private fun shadowMask(rT: Int, sigmaQ: Int, d: Int): ShadowMask {
            val sigma = sigmaQ / 2f
            val e = ceil(sigma * 3f).toInt()
            val core = 2 * (rT + e) + 1
            val m = core + 2 * e
            val cov = FloatArray(m * m)
            val lo = e.toFloat(); val hi = (e + core).toFloat(); val r = rT.toFloat()
            for (y in 0 until m) for (x in 0 until m) {
                val px = x + 0.5f; val py = y + 0.5f
                val qx = max(max(lo + r - px, px - (hi - r)), 0f)
                val qy = max(max(lo + r - py, py - (hi - r)), 0f)
                val inX = min(px - lo, hi - px); val inY = min(py - lo, hi - py)
                val dist = if (qx > 0f && qy > 0f) hypot(qx, qy) - r else -min(inX, inY)
                cov[y * m + x] = (0.5f - dist).coerceIn(0f, 1f)
            }
            val k = FloatArray(e * 2 + 1) { exp(-((it - e) * (it - e)) / (2f * sigma * sigma)) }
            val sum = k.sum()
            for (i in k.indices) k[i] /= sum
            val tmp = FloatArray(m * m)
            for (y in 0 until m) for (x in 0 until m) {
                var a = 0f
                for (i in k.indices) { val sx = x + i - e; if (sx in 0 until m) a += cov[y * m + sx] * k[i] }
                tmp[y * m + x] = a
            }
            val bmp = bitmapOf(m, m) { x, y ->
                var a = 0f
                for (i in k.indices) { val sy = y + i - e; if (sy in 0 until m) a += tmp[sy * m + x] * k[i] }
                ((a * 255f).roundToInt().coerceIn(0, 255) shl 24) or 0xFFFFFF
            }
            return ShadowMask(bmp, (rT + 2 * e + 0.5f) * d, (e * d).toFloat(), (m * d).toFloat())
        }
    }
}

private const val MC_CODES = "0123456789abcdeflrmno"

private val MC_COLORS = intArrayOf(
    0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
    0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
)
