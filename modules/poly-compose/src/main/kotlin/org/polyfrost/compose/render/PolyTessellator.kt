package org.polyfrost.compose.render

import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.tan
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.FilterQuality
import org.polyfrost.compose.ui.graphics.shadow.Shadow
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

abstract class PolyTessellator : PolyRenderer {
    protected class State(
        @JvmField var sx: Float, @JvmField var sy: Float, @JvmField var tx: Float, @JvmField var ty: Float,
        @JvmField var alpha: Float, @JvmField var clip: FloatArray?, @JvmField var rclip: RClip?,
    ) {
        fun copy() = State(sx, sy, tx, ty, alpha, clip, rclip)
    }

    protected class RClip(val edge: FloatArray, val inner: FloatArray, val outer: FloatArray, val parent: RClip?, val safe: FloatArray, val box: FloatArray) {
        val invLen = FloatArray(edge.size / 2) { e ->
            val n = edge.size / 2
            1f / max(hypot(edge[(e + 1) % n * 2] - edge[e * 2], edge[(e + 1) % n * 2 + 1] - edge[e * 2 + 1]), 1e-4f)
        }
    }

    protected var state = State(1f, 1f, 0f, 0f, 1f, null, null)
    private val stack = ArrayDeque<State>()

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

    protected abstract fun bitmapTexture(bitmap: ImageBitmap): Any

    override fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) {
        if (w <= 0f || h <= 0f) return
        clippedFill(x, y, w, h, radius) { fillPolygonPx(it, it.size / 2, color, convex = true) }
    }

    override fun rectWithHole(x: Float, y: Float, w: Float, h: Float, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float, color: Int) {
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

    override fun rectGradient(x: Float, y: Float, w: Float, h: Float, gradient: Brush, radius: Float) {
        if (w <= 0f || h <= 0f) return
        val s = state
        val tex: ImageBitmap
        val uv: (Float, Float, FloatArray) -> Unit
        if (gradient.linear) {
            tex = ramp(gradient)
            uv = { fx, fy, out -> out[0] = (0.5f + 255f * gradient.position((fx - s.tx) / s.sx, (fy - s.ty) / s.sy, x, y, w, h)) / 256f; out[1] = 0.5f }
        } else {
            tex = boxRaster(gradient, w / h)
            uv = { fx, fy, out -> out[0] = ((fx - s.tx) / s.sx - x) / w; out[1] = ((fy - s.ty) / s.sy - y) / h }
        }
        val g = PolyGlyphPage.bitmap(tex)
        begin(g?.page ?: bitmapTexture(tex), false)
        val pageUv: (Float, Float, FloatArray) -> Unit = if (g == null) uv else { fx, fy, out -> uv(fx, fy, out); out[0] = g.u(out[0]); out[1] = g.v(out[1]) }
        clippedFill(x, y, w, h, radius) { fillPolygonPx(it, it.size / 2, -1, convex = true, uv = pageUv, keepBatch = true) }
    }

    override fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: Int, strokeWidth: Float, radius: Float) =
        stroke(x, y, w, h, color, strokeWidth, radius, null)

    override fun rectStrokeGradient(x: Float, y: Float, w: Float, h: Float, top: Int, bottom: Int, strokeWidth: Float, radius: Float) {
        val y0 = y * state.sy + state.ty
        val y1 = (y + h) * state.sy + state.ty
        stroke(x, y, w, h, top, strokeWidth, radius) { py -> Brush.lerpArgb(top, bottom, ((py - y0) / (y1 - y0)).coerceIn(0f, 1f)) }
    }

    private fun stroke(x: Float, y: Float, w: Float, h: Float, color: Int, strokeWidth: Float, radius: Float, shade: ((Float) -> Int)?) {
        if (w <= 0f || h <= 0f || strokeWidth <= 0f) return
        val swPx = strokeWidth * scaleAvg()
        val alphaMul = if (swPx < 1f) swPx else 1f
        val half = (if (swPx < 1f) 1f / scaleAvg() else strokeWidth) / 2f
        val segs = cornerSegments(radius + half)
        val outer = rrectPoints(x - half, y - half, w + half * 2f, h + half * 2f, if (radius > 0f) radius + half else 0f, segs)
        val inner = rrectPoints(x + half, y + half, w - half * 2f, h - half * 2f, max(0f, radius - half), segs)
        unclipped(x - half, y - half, w + half * 2f, h + half * 2f, if (radius > 0f) radius + half else 0f) {
            strokeRing(outer, inner, outer.size / 2, mulAlpha(color, alphaMul), shade?.let { f -> { py: Float -> mulAlpha(f(py), alphaMul) } }, band = half * 2f * scaleAvg() > 1.001f)
        }
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float) {
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

    override fun fillPath(path: PolyPath, color: Int) {
        for (loop in flatten(path)) {
            transformInPlace(loop)
            fillPolygonPx(loop, loop.size / 2, color, convex = false)
        }
    }

    override fun strokePolyline(points: FloatArray, closed: Boolean, color: Int, strokeWidth: Float, cap: Int) {
        if (points.size < 4 || strokeWidth <= 0f) return
        val p = dedupe(points)
        val loop = (closed || hypot(points[0] - points[points.size - 2], points[1] - points[points.size - 1]) <= 1e-3f) && p.size >= 6
        transformInPlace(p)
        val n = p.size / 2
        if (n < 2) return
        val swPx = strokeWidth * scaleAvg()
        val c = mulAlpha(color, if (swPx < 1f) swPx else 1f)
        val half = max(swPx, 1f) / 2f
        if (!loop && cap == CAP_SQUARE) {
            fun extend(i: Int, from: Int) {
                val dx = p[i * 2] - p[from * 2]; val dy = p[i * 2 + 1] - p[from * 2 + 1]
                val l = hypot(dx, dy)
                p[i * 2] += dx / l * half; p[i * 2 + 1] += dy / l * half
            }
            extend(0, 1)
            extend(n - 1, n - 2)
        }
        val left = FloatArray(n * 2)
        val right = FloatArray(n * 2)
        for (i in 0 until n) {
            val prev = if (loop || i > 0) (i + n - 1) % n else -1
            val next = if (loop || i < n - 1) (i + 1) % n else -1
            var mx = 0f; var my = 0f
            if (prev >= 0) {
                val dx = p[i * 2] - p[prev * 2]; val dy = p[i * 2 + 1] - p[prev * 2 + 1]
                val l = hypot(dx, dy)
                mx -= dy / l; my += dx / l
            }
            if (next >= 0) {
                val dx = p[next * 2] - p[i * 2]; val dy = p[next * 2 + 1] - p[i * 2 + 1]
                val l = hypot(dx, dy)
                mx -= dy / l; my += dx / l
            }
            if (prev >= 0 && next >= 0) { mx /= 2f; my /= 2f }
            val d2 = mx * mx + my * my
            val k = if (d2 > 1e-6f) min(1f / d2, 4f) * half else half
            left[i * 2] = p[i * 2] + mx * k; left[i * 2 + 1] = p[i * 2 + 1] + my * k
            right[i * 2] = p[i * 2] - mx * k; right[i * 2 + 1] = p[i * 2 + 1] - my * k
        }
        if (loop) {
            val leftOuter = abs(signedArea(left, n)) >= abs(signedArea(right, n))
            strokeRing(if (leftOuter) left else right, if (leftOuter) right else left, n, c, band = half * 2f > 1.001f)
            return
        }
        val segs = if (cap == CAP_ROUND) max(4, min(32, ceil(sqrt(half) * 3.2f).toInt())) else 0
        val out = FloatArray((n * 2 + (segs - 1).coerceAtLeast(0) * 2) * 2)
        var o = 0
        fun cap(i: Int, from: FloatArray) {
            val cx = p[i * 2]; val cy = p[i * 2 + 1]
            val a0 = kotlin.math.atan2(from[i * 2 + 1] - cy, from[i * 2] - cx)
            for (s in 1 until segs) {
                val a = a0 - Math.PI.toFloat() * s / segs
                out[o++] = cx + cos(a) * half; out[o++] = cy + sin(a) * half
            }
        }
        for (i in 0 until n) { out[o++] = left[i * 2]; out[o++] = left[i * 2 + 1] }
        cap(n - 1, left)
        for (i in n - 1 downTo 0) { out[o++] = right[i * 2]; out[o++] = right[i * 2 + 1] }
        cap(0, right)
        fillPolygonPx(out, o / 2, c, convex = false)
    }

    override fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont) = glyphs(text, x, y, color, font, 0f, false)

    override fun textOutline(text: String, x: Float, y: Float, color: Int, font: PolyFont, width: Float) =
        glyphs(text, x, y, color, font, width * scaleAvg(), true)

    private fun glyphs(text: String, x: Float, y: Float, color: Int, font: PolyFont, outlinePx: Float, round: Boolean) {
        val face = PolyGlyphPage.rasterizer?.face(font.name) ?: return
        val c = mulAlpha(color, state.alpha)
        if (c ushr 24 == 0) return
        val bucket = (font.size * scaleAvg() * 4f).roundToInt()
        val atlas = PolyGlyphPage.atlas(face, bucket, outlinePx, round)
        var penX = x
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            val id = face.glyph(cp)
            val g = PolyGlyphPage.glyph(atlas, face, bucket, id, outlinePx, round)
            if (g != null) {
                val px = (penX * state.sx + state.tx).roundToInt() + g.offX
                val py = (y * state.sy + state.ty).roundToInt() + g.offY
                begin(g.page, false)
                quadPx(px.toFloat(), py.toFloat(), (px + g.w).toFloat(), (py + g.h).toFloat(), g.u0, g.v0, g.u1, g.v1, c)
            }
            penX += face.advance(id) * font.size
        }
    }

    override fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) {
        text.lines().forEachIndexed { index, line ->
            val top = y + index * 9f * scale
            if (shadow) mcLine(line, x + scale, top + scale, color, scale, true)
            mcLine(line, x, top, color, scale, false)
        }
    }

    override fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float) {
        val c = mulAlpha(outlineColor, state.alpha)
        val rects = if (c ushr 24 == 0) null else PolyGlyphPage.rasterizer?.let { r -> mcOutlines.getOrPut(text) { mcOutline(r, text) } }
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

    private fun mcLine(line: String, x: Float, top: Float, color: Int, scale: Float, shadow: Boolean) {
        val baseline = top + 7f * scale
        var c = color
        var bold = false
        var pen = x
        var i = 0
        while (i < line.length) {
            val cp = line.codePointAt(i)
            i += Character.charCount(cp)
            if (cp == 0xA7 && i < line.length) {
                val code = MC_CODES.indexOf(line[i].lowercaseChar())
                i++
                when {
                    code in 0..15 -> { c = (color and -0x1000000) or MC_COLORS[code]; bold = false }
                    code == 16 -> bold = true
                    code == 17 -> { c = color; bold = false }
                }
                continue
            }
            val drawn = if (shadow) (c and -0x1000000) or ((c and 0xFCFCFC) shr 2) else c
            val main = PolyGlyphPage.rasterizer?.face(if (bold) "minecraft-bold" else "minecraft")
            val s = String(Character.toChars(cp))
            pen += if (main != null && main.glyph(cp) != 0) {
                val font = PolyFont(if (bold) "minecraft-bold" else "minecraft", 10f * scale)
                text(s, pen, baseline, drawn, font)
                main.advance(main.glyph(cp)) * font.size
            } else {
                val font = PolyFont("unifont", 8f * scale)
                val uni = PolyGlyphPage.rasterizer?.face("unifont") ?: continue
                text(s, pen, baseline, drawn, font)
                uni.advance(uni.glyph(cp)) * font.size
            }
        }
    }

    override fun image(bitmap: ImageBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: Int, filterQuality: FilterQuality) {
        val c = mulAlpha(tint, state.alpha)
        if (c ushr 24 == 0) return
        val g = PolyGlyphPage.bitmap(bitmap)
        begin(g?.page ?: bitmapTexture(bitmap), false, filterQuality == FilterQuality.None)
        val x0 = x * state.sx + state.tx
        val y0 = y * state.sy + state.ty
        if (g == null) quadPx(x0, y0, x0 + w * state.sx, y0 + h * state.sy, u0, v0, u1, v1, c)
        else quadPx(x0, y0, x0 + w * state.sx, y0 + h * state.sy, g.u(u0), g.v(v0), g.u(u1), g.v(v1), c)
    }

    override fun shadow(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: Shadow) {
        val s = scaleAvg()
        val color = shadow.drawColor.argb
        val spread = shadow.spread.value
        val blur = shadow.radius.value
        if (color ushr 24 == 0 || s <= 0f) return
        val bx = x - spread + shadow.offset.x.value
        val by = y - spread + shadow.offset.y.value
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
        for (j in 0 until 3) for (i in 0 until 3) {
            if (xs[i + 1] <= xs[i] || ys[j + 1] <= ys[j]) continue
            image(m.bitmap, xs[i], ys[j], xs[i + 1] - xs[i], ys[j + 1] - ys[j], us[i], vs[j], us[i + 1], vs[j + 1], color)
        }
    }

    open fun stateKey(): Any {
        val s = state
        val out = arrayListOf(s.sx, s.sy, s.tx, s.ty, s.alpha)
        s.clip?.forEach(out::add) ?: out.add(-1f)
        var c = s.rclip
        while (c != null) { c.box.forEach(out::add); c = c.parent }
        return out
    }

    fun saveState(): Any = state.copy()

    fun restoreState(saved: Any) { state = (saved as State).copy() }

    override fun save() {
        stack.addLast(state)
        state = state.copy()
    }

    override fun restore() {
        state = stack.removeLast()
    }

    override fun saveLayerAlpha(alpha: Float) {
        save()
        state.alpha *= alpha
    }

    override fun translate(dx: Float, dy: Float) {
        state.tx += dx * state.sx
        state.ty += dy * state.sy
    }

    override fun scale(sx: Float, sy: Float) {
        state.sx *= sx
        state.sy *= sy
    }

    override fun clipRect(x: Float, y: Float, w: Float, h: Float) {
        val x0 = x * state.sx + state.tx
        val y0 = y * state.sy + state.ty
        val x1 = x0 + w * state.sx
        val y1 = y0 + h * state.sy
        val prev = state.clip
        val next = if (prev == null) floatArrayOf(floor(x0), floor(y0), ceil(x1), ceil(y1))
        else floatArrayOf(max(floor(x0), prev[0]), max(floor(y0), prev[1]), min(ceil(x1), prev[2]), min(ceil(y1), prev[3]))
        state.clip = next
    }

    override fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float) {
        clipRect(x, y, w, h)
        if (radius <= 0f) return
        val p = rrectPoints(x, y, w, h, radius)
        if (signedArea(p, p.size / 2) < 0f) {
            for (i in 0 until p.size / 4) {
                val j = p.size / 2 - 1 - i
                val tx = p[i * 2]; val ty = p[i * 2 + 1]
                p[i * 2] = p[j * 2]; p[i * 2 + 1] = p[j * 2 + 1]
                p[j * 2] = tx; p[j * 2 + 1] = ty
            }
        }
        val n = p.size / 2
        val inner = FloatArray(p.size); val outer = FloatArray(p.size)
        offsetPolygon(p, n, 1f, inner, outer)
        val box = boxPx(x, y, w, h, radius)
        val l = box[0]; val t = box[1]; val rt = box[2]; val b = box[3]
        val rx = box[4] + 1f; val ry = box[5] + 1f
        val safe = floatArrayOf(l + rx, t + 1f, rt - rx, b - 1f, l + 1f, t + ry, rt - 1f, b - ry)
        state.rclip = RClip(p, inner, outer, state.rclip, safe, box)
    }

    private fun boxPx(x: Float, y: Float, w: Float, h: Float, radius: Float): FloatArray {
        val r = max(0f, min(radius, min(w, h) / 2f))
        val ax = x * state.sx + state.tx; val bx = (x + w) * state.sx + state.tx
        val ay = y * state.sy + state.ty; val by = (y + h) * state.sy + state.ty
        return floatArrayOf(min(ax, bx), min(ay, by), max(ax, bx), max(ay, by), r * abs(state.sx), r * abs(state.sy))
    }

    private inline fun clippedFill(x: Float, y: Float, w: Float, h: Float, radius: Float, fill: (FloatArray) -> Unit) {
        val saved = state.rclip
        if (saved == null || state.sx <= 0f || state.sy <= 0f) {
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
        val b = boxPx(x, y, w, h, radius)
        var c: RClip? = saved
        while (c != null) {
            val k = c.box
            val e = 1e-3f
            if (b[0] < k[0] - e || b[1] < k[1] - e || b[2] > k[2] + e || b[3] > k[3] + e || b[4] < k[4] - e || b[5] < k[5] - e) break
            c = c.parent
        }
        state.rclip = c
        try {
            draw()
        } finally {
            state.rclip = saved
        }
    }

    private fun whitePage(): Any = batchTexture as? PolyGlyphPage.Page ?: PolyGlyphPage.pages[0]

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

    protected fun flush() {
        if (count == 0) return
        submit(batchTexture, batchPremul, batchNearest, verts.copyOf(count * 4), colors.copyOf(count), floatArrayOf(minX, minY, maxX, maxY))
        count = 0
        minX = Float.MAX_VALUE; minY = Float.MAX_VALUE; maxX = -Float.MAX_VALUE; maxY = -Float.MAX_VALUE
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
            var k = 0
            val cx = floatArrayOf(x + r, x + w - r, x + w - r, x + r)
            val cy = floatArrayOf(y + r, y + r, y + h - r, y + h - r)
            for (corner in 0 until 4) {
                val start = Math.PI.toFloat() * (1f + corner * 0.5f)
                for (i in 0..segs) {
                    val a = start + (Math.PI.toFloat() / 2f) * i / segs
                    out[k++] = cx[corner] + cos(a) * r
                    out[k++] = cy[corner] + sin(a) * r
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

    private fun fillPolygonPx(p: FloatArray, n: Int, color: Int, convex: Boolean, uv: ((Float, Float, FloatArray) -> Unit)? = null, keepBatch: Boolean = false) {
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

    private fun strokeRing(outerPts: FloatArray, innerPts: FloatArray, n: Int, color: Int, shade: ((Float) -> Int)? = null, band: Boolean = true) {
        val c = mulAlpha(color, state.alpha)
        if ((shade == null && c ushr 24 == 0) || n < 3) return
        val dir = if (signedArea(outerPts, n) > 0f) 1f else -1f
        val oIn = FloatArray(n * 2); val oOut = FloatArray(n * 2)
        val iIn = FloatArray(n * 2); val iOut = FloatArray(n * 2)
        offsetPolygon(outerPts, n, dir, oIn, oOut)
        offsetPolygon(innerPts, n, -dir, iIn, iOut)
        begin(whitePage(), false)
        val w = PolyGlyphPage.WHITE_UV
        fun at(p: FloatArray, i: Int) = if (shade == null) c else mulAlpha(shade(p[i * 2 + 1]), state.alpha)
        fun clear(p: FloatArray, i: Int) = at(p, i) and 0x00FFFFFF
        fun q(a: FloatArray, ca: (FloatArray, Int) -> Int, b: FloatArray, cb: (FloatArray, Int) -> Int, i: Int, j: Int) = quadVerts(
            a[i * 2], a[i * 2 + 1], w, w, at(a, i), b[i * 2], b[i * 2 + 1], w, w, cb(b, i),
            b[j * 2], b[j * 2 + 1], w, w, cb(b, j), a[j * 2], a[j * 2 + 1], w, w, ca(a, j),
        )
        for (i in 0 until n) {
            val j = (i + 1) % n
            if (band) q(oIn, ::at, iIn, ::at, i, j)
            q(oIn, ::at, oOut, ::clear, i, j)
            q(iIn, ::at, iOut, ::clear, i, j)
        }
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
        for (i in 0 until n) {
            val prev = (i + n - 1) % n
            val next = (i + 1) % n
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
        val out = ArrayList<Float>(p.size)
        var i = 0
        while (i < p.size) {
            val x = p[i]; val y = p[i + 1]
            val n = out.size
            if (n < 2 || hypot(out[n - 2] - x, out[n - 1] - y) > 1e-3f) { out.add(x); out.add(y) }
            i += 2
        }
        if (out.size >= 4 && hypot(out[0] - out[out.size - 2], out[1] - out[out.size - 1]) <= 1e-3f) {
            out.removeAt(out.size - 1); out.removeAt(out.size - 1)
        }
        return out.toFloatArray()
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
        qx[qn] = x; qy[qn] = y; qu[qn] = u; qv[qn] = v; qc[qn] = color
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
        if (crosses == 0 && rclip == null) {
            for (i in 0 until 4) emit(qx[i], qy[i], qu[i], qv[i], qc[i])
        } else {
            val quad = Poly()
            for (i in 0 until 4) quad.add(qx[i], qy[i], qu[i], qv[i], qc[i])
            var polys = listOf(quad)
            if (crosses != 0) polys = boxCut(quad, box!!, crosses)
            if (rclip != null && polys.isNotEmpty()) polys = clipQuad(polys, rclip)
            for (p in polys) emitFan(p)
        }
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
                val c = Brush.lerpArgb(p.c[i], p.c[j], t)
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
                val c = Brush.lerpArgb(p.c[i], p.c[j], t)
                inside.add(x, y, u, v, c)
                outside?.add(x, y, u, v, c)
            }
        }
        return inside
    }

    private fun clipQuad(quad: List<Poly>, clip: RClip): List<Poly> {
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

    private fun cut(p: Poly, clip: RClip, out: MutableList<Poly>) {
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
            val band = Poly()
            rest = split(rest, clip.inner, e, band)
            if (band.size >= 3) out.add(covered(band, clip))
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

    private class ShadowMask(val bitmap: ImageBitmap, val slice: Float, val extent: Float, val width: Float)

    companion object {
        private fun <K, V> lru(max: Int) = object : LinkedHashMap<K, V>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > max
        }

        private val ramps = lru<Pair<List<PolyColor>, List<Float>?>, ImageBitmap>(64)
        private val boxRasters = lru<Pair<Brush, Int>, ImageBitmap>(32)
        private val masks = lru<Long, ShadowMask>(32)
        private val mcOutlines = lru<String, FloatArray>(256)
        private val mcRasters = HashMap<Pair<PolyGlyphPage.Face, Int>, PolyGlyphPage.Raster?>()

        private const val MC_GRID = 2
        private const val CAP_ROUND = 1
        private const val CAP_SQUARE = 2

        private fun mcOutline(r: PolyGlyphPage.Rasterizer, text: String): FloatArray {
            val placed = ArrayList<PolyGlyphPage.Raster>()
            val at = ArrayList<Int>()
            text.lines().forEachIndexed { index, line ->
                val baseline = (index * 9 + 7) * MC_GRID
                var pen = 0f
                var bold = false
                var i = 0
                while (i < line.length) {
                    val cp = line.codePointAt(i)
                    i += Character.charCount(cp)
                    if (cp == 0xA7 && i < line.length) {
                        val code = MC_CODES.indexOf(line[i++].lowercaseChar())
                        if (code == 16) bold = true else if (code in 0..15 || code == 17) bold = false
                        continue
                    }
                    val main = r.face(if (bold) "minecraft-bold" else "minecraft")
                    val face: PolyGlyphPage.Face
                    val em: Float
                    if (main != null && main.glyph(cp) != 0) { face = main; em = 10f } else { face = r.face("unifont") ?: continue; em = 8f }
                    val id = face.glyph(cp)
                    val g = mcRasters.getOrPut(face to id) { face.raster(id, em * MC_GRID) }
                    if (g != null && g.w > 0 && g.h > 0) {
                        placed += g
                        at += (pen * MC_GRID).roundToInt() + g.offX
                        at += baseline + g.offY
                    }
                    pen += face.advance(id) * em
                }
            }
            if (placed.isEmpty()) return FloatArray(0)
            val grow = MC_GRID
            var x0 = Int.MAX_VALUE; var y0 = Int.MAX_VALUE; var x1 = Int.MIN_VALUE; var y1 = Int.MIN_VALUE
            for (k in placed.indices) {
                x0 = min(x0, at[k * 2]); y0 = min(y0, at[k * 2 + 1])
                x1 = max(x1, at[k * 2] + placed[k].w); y1 = max(y1, at[k * 2 + 1] + placed[k].h)
            }
            x0 -= grow; y0 -= grow; x1 += grow; y1 += grow
            val w = x1 - x0
            val h = y1 - y0
            val ink = BooleanArray(w * h)
            for (k in placed.indices) {
                val g = placed[k]
                val gx = at[k * 2] - x0
                val gy = at[k * 2 + 1] - y0
                for (py in 0 until g.h) for (px in 0 until g.w) {
                    if ((g.coverage[py * g.w + px].toInt() and 0xFF) >= 128) ink[(gy + py) * w + gx + px] = true
                }
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

        private fun bitmapOf(w: Int, h: Int, argb: (Int, Int) -> Int): ImageBitmap {
            val px = ByteArray(w * h * 4)
            for (y in 0 until h) for (x in 0 until w) {
                val c = argb(x, y)
                val o = (y * w + x) * 4
                px[o] = (c shr 16).toByte(); px[o + 1] = (c shr 8).toByte(); px[o + 2] = c.toByte(); px[o + 3] = (c ushr 24).toByte()
            }
            return ImageBitmap(w, h, px)
        }

        private fun ramp(g: Brush): ImageBitmap = ramps.getOrPut(g.colors to g.stops) { bitmapOf(256, 1) { x, _ -> g.sample(x / 255f) } }

        private fun boxRaster(g: Brush, aspect: Float): ImageBitmap {
            val q = (aspect * 16f).roundToInt().coerceIn(1, 1024)
            return boxRasters.getOrPut(g to q) {
                val a = q / 16f
                val w = if (a >= 1f) 64 else max(8, (64 * a).roundToInt())
                val h = if (a >= 1f) max(8, (64 / a).roundToInt()) else 64
                bitmapOf(w, h) { x, y -> g.colorAt((x + 0.5f) / w * a, (y + 0.5f) / h, 0f, 0f, a, 1f) }
            }
        }

        private const val MAX_MASK_SIGMA = 6f

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

private const val MC_CODES = "0123456789abcdeflr"
private val MC_COLORS = intArrayOf(
    0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
    0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
)
