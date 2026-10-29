package org.polyfrost.oneconfig.api.notifications.v1

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

internal object Mat {
    fun identity() = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)

    fun translate(x: Float, y: Float) = floatArrayOf(1f, 0f, 0f, 1f, x, y)

    fun scale(x: Float, y: Float) = floatArrayOf(x, 0f, 0f, y, 0f, 0f)

    fun mul(a: FloatArray, b: FloatArray) = floatArrayOf(
        a[0] * b[0] + a[2] * b[1], a[1] * b[0] + a[3] * b[1],
        a[0] * b[2] + a[2] * b[3], a[1] * b[2] + a[3] * b[3],
        a[0] * b[4] + a[2] * b[5] + a[4], a[1] * b[4] + a[3] * b[5] + a[5],
    )

    fun invert(m: FloatArray): FloatArray? {
        val d = m[0] * m[3] - m[1] * m[2]
        if (d == 0f || !d.isFinite()) return null
        return floatArrayOf(m[3] / d, -m[1] / d, -m[2] / d, m[0] / d, (m[2] * m[5] - m[3] * m[4]) / d, (m[1] * m[4] - m[0] * m[5]) / d)
    }

    fun scaleOf(m: FloatArray) = sqrt(abs(m[0] * m[3] - m[1] * m[2]))

    fun map(m: FloatArray, p: FloatArray) = FloatArray(p.size) { i ->
        if (i % 2 == 0) m[0] * p[i] + m[2] * p[i + 1] + m[4] else m[1] * p[i - 1] + m[3] * p[i] + m[5]
    }

    fun mapRect(m: FloatArray, r: FloatArray): FloatArray {
        val p = map(m, floatArrayOf(r[0], r[1], r[2], r[1], r[2], r[3], r[0], r[3]))
        return floatArrayOf(min(min(p[0], p[2]), min(p[4], p[6])), min(min(p[1], p[3]), min(p[5], p[7])), max(max(p[0], p[2]), max(p[4], p[6])), max(max(p[1], p[3]), max(p[5], p[7])))
    }

    fun rectPoly(r: FloatArray) = floatArrayOf(r[0], r[1], r[2], r[1], r[2], r[3], r[0], r[3])

    fun parse(s: String?): FloatArray {
        var m = identity()
        if (s.isNullOrBlank()) return m
        for (t in Regex("([A-Za-z]+)\\s*\\(([^)]*)\\)").findAll(s)) {
            val n = SvgPath.numbers(t.groupValues[2])
            val op = when (t.groupValues[1]) {
                "translate" -> translate(n.getOrElse(0) { 0f }, n.getOrElse(1) { 0f })
                "scale" -> scale(n.getOrElse(0) { 1f }, n.getOrElse(1) { n.getOrElse(0) { 1f } })
                "rotate" -> {
                    val a = Math.toRadians(n.getOrElse(0) { 0f }.toDouble())
                    val c = cos(a).toFloat()
                    val sn = sin(a).toFloat()
                    val px = n.getOrElse(1) { 0f }
                    val py = n.getOrElse(2) { 0f }
                    floatArrayOf(c, sn, -sn, c, px - c * px + sn * py, py - sn * px - c * py)
                }
                "skewX" -> floatArrayOf(1f, 0f, tan(Math.toRadians(n.getOrElse(0) { 0f }.toDouble())).toFloat(), 1f, 0f, 0f)
                "skewY" -> floatArrayOf(1f, tan(Math.toRadians(n.getOrElse(0) { 0f }.toDouble())).toFloat(), 0f, 1f, 0f, 0f)
                "matrix" -> if (n.size >= 6) n.copyOf(6) else return identity()
                else -> return identity()
            }
            m = mul(m, op)
        }
        return m
    }
}

internal class Polyline(val pts: FloatArray, val closed: Boolean)

internal class SvgPath {
    private var verbs = ByteArray(16)
    private var nv = 0
    private var pts = FloatArray(32)
    private var np = 0

    private fun verb(v: Byte) {
        if (nv == verbs.size) verbs = verbs.copyOf(nv * 2)
        verbs[nv++] = v
    }

    private fun pt(x: Float, y: Float) {
        if (np + 2 > pts.size) pts = pts.copyOf(pts.size * 2)
        pts[np++] = x
        pts[np++] = y
    }

    fun moveTo(x: Float, y: Float) { verb(MOVE); pt(x, y) }
    fun lineTo(x: Float, y: Float) { verb(LINE); pt(x, y) }
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) { verb(CUBIC); pt(x1, y1); pt(x2, y2); pt(x, y) }
    fun close() = verb(CLOSE)

    fun transform(m: FloatArray): SvgPath {
        val out = SvgPath()
        out.verbs = verbs.copyOf(nv)
        out.nv = nv
        out.pts = Mat.map(m, pts.copyOf(np))
        out.np = np
        return out
    }

    fun flatten(tol: Float): List<Polyline> {
        val out = ArrayList<Polyline>()
        val cur = Floats()
        var sx = 0f; var sy = 0f; var x = 0f; var y = 0f
        var drew = false
        var p = 0
        fun begin() { if (cur.size == 0) cur.add(sx, sy) }
        fun flush(closed: Boolean) {
            if (drew && cur.size >= 2) out.add(Polyline(cur.toArray(), closed))
            cur.clear()
            drew = false
        }
        for (i in 0 until nv) {
            when (verbs[i]) {
                MOVE -> {
                    flush(false)
                    sx = pts[p++]; sy = pts[p++]; x = sx; y = sy
                    cur.add(x, y)
                }
                LINE -> {
                    begin()
                    x = pts[p++]; y = pts[p++]
                    cur.add(x, y)
                    drew = true
                }
                CUBIC -> {
                    begin()
                    val x1 = pts[p++]; val y1 = pts[p++]; val x2 = pts[p++]; val y2 = pts[p++]; val x3 = pts[p++]; val y3 = pts[p++]
                    val dd = max(hypot(x - 2 * x1 + x2, y - 2 * y1 + y2), hypot(x1 - 2 * x2 + x3, y1 - 2 * y2 + y3))
                    val n = ceil(sqrt(0.75f * dd / tol)).toInt().coerceIn(1, 512)
                    for (k in 1..n) {
                        val t = k / n.toFloat(); val u = 1f - t
                        val a = u * u * u; val b = 3 * u * u * t; val c = 3 * u * t * t; val d = t * t * t
                        cur.add(a * x + b * x1 + c * x2 + d * x3, a * y + b * y1 + c * y2 + d * y3)
                    }
                    x = x3; y = y3
                    drew = true
                }
                CLOSE -> {
                    drew = drew || cur.size >= 2
                    flush(true)
                    x = sx; y = sy
                }
            }
        }
        flush(false)
        return out
    }

    fun bounds(): FloatArray? {
        if (np == 0) return null
        var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
        var span = 0f
        for (i in 0 until np step 2) span = max(span, max(abs(pts[i]), abs(pts[i + 1])))
        for (line in flatten(max(span, 1f) * 1e-4f)) {
            val p = line.pts
            for (i in p.indices step 2) { x0 = min(x0, p[i]); x1 = max(x1, p[i]); y0 = min(y0, p[i + 1]); y1 = max(y1, p[i + 1]) }
        }
        return if (x0 > x1) null else floatArrayOf(x0, y0, x1, y1)
    }

    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) {
        val kx = rx * KAPPA; val ky = ry * KAPPA
        moveTo(cx + rx, cy)
        cubicTo(cx + rx, cy + ky, cx + kx, cy + ry, cx, cy + ry)
        cubicTo(cx - kx, cy + ry, cx - rx, cy + ky, cx - rx, cy)
        cubicTo(cx - rx, cy - ky, cx - kx, cy - ry, cx, cy - ry)
        cubicTo(cx + kx, cy - ry, cx + rx, cy - ky, cx + rx, cy)
        close()
    }

    fun roundRect(x: Float, y: Float, w: Float, h: Float, rx: Float, ry: Float) {
        if (rx <= 0f || ry <= 0f) {
            moveTo(x, y); lineTo(x + w, y); lineTo(x + w, y + h); lineTo(x, y + h); close()
            return
        }
        val kx = rx * KAPPA; val ky = ry * KAPPA
        moveTo(x + rx, y)
        lineTo(x + w - rx, y)
        cubicTo(x + w - rx + kx, y, x + w, y + ry - ky, x + w, y + ry)
        lineTo(x + w, y + h - ry)
        cubicTo(x + w, y + h - ry + ky, x + w - rx + kx, y + h, x + w - rx, y + h)
        lineTo(x + rx, y + h)
        cubicTo(x + rx - kx, y + h, x, y + h - ry + ky, x, y + h - ry)
        lineTo(x, y + ry)
        cubicTo(x, y + ry - ky, x + rx - kx, y, x + rx, y)
        close()
    }

    companion object {
        private const val MOVE: Byte = 0
        private const val LINE: Byte = 1
        private const val CUBIC: Byte = 2
        private const val CLOSE: Byte = 3
        private const val KAPPA = 0.5522848f

        private val NUMBER = Regex("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?")
        private val TOKEN = Regex("[A-Za-z]|[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?")

        fun numbers(s: String): FloatArray = NUMBER.findAll(s).map { it.value.toFloat() }.toList().toFloatArray()

        fun parse(d: String): SvgPath {
            val path = SvgPath()
            val tokens = TOKEN.findAll(d).map { it.value }.toMutableList()
            var x = 0f; var y = 0f; var sx = 0f; var sy = 0f
            var cx = 0f; var cy = 0f
            var i = 0
            var cmd = ' '
            var prev = ' '
            fun num(): Float {
                val t = tokens[i]
                if (t[0].isLetter()) throw NumberFormatException(t)
                i++
                return t.toFloat()
            }
            fun flag(): Boolean {
                val t = tokens[i]
                if (t[0] != '0' && t[0] != '1') throw NumberFormatException(t)
                if (t.length > 1) tokens[i] = t.substring(1) else i++
                return t[0] == '1'
            }
            fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, ex: Float, ey: Float) {
                path.cubicTo(x1, y1, x2, y2, ex, ey)
                cx = x2; cy = y2; x = ex; y = ey
            }
            try {
                while (i < tokens.size) {
                    val t = tokens[i]
                    if (t[0].isLetter()) { cmd = t[0]; i++ } else if (cmd == ' ') break
                    val rel = cmd.isLowerCase()
                    val ox = if (rel) x else 0f
                    val oy = if (rel) y else 0f
                    when (cmd.uppercaseChar()) {
                        'M' -> {
                            x = ox + num(); y = oy + num(); sx = x; sy = y
                            path.moveTo(x, y)
                            cmd = if (rel) 'l' else 'L'
                        }
                        'L' -> { val nx = ox + num(); val ny = oy + num(); path.lineTo(nx, ny); x = nx; y = ny }
                        'H' -> { x = ox + num(); path.lineTo(x, y) }
                        'V' -> { y = oy + num(); path.lineTo(x, y) }
                        'C' -> cubic(ox + num(), oy + num(), ox + num(), oy + num(), ox + num(), oy + num())
                        'S' -> {
                            val reflect = prev.uppercaseChar() == 'C' || prev.uppercaseChar() == 'S'
                            val x1 = if (reflect) 2 * x - cx else x
                            val y1 = if (reflect) 2 * y - cy else y
                            cubic(x1, y1, ox + num(), oy + num(), ox + num(), oy + num())
                        }
                        'Q', 'T' -> {
                            val q = cmd.uppercaseChar() == 'Q'
                            val reflect = prev.uppercaseChar() == 'Q' || prev.uppercaseChar() == 'T'
                            val qx = if (q) ox + num() else if (reflect) 2 * x - cx else x
                            val qy = if (q) oy + num() else if (reflect) 2 * y - cy else y
                            val ex = ox + num(); val ey = oy + num()
                            path.cubicTo(x + 2f / 3f * (qx - x), y + 2f / 3f * (qy - y), ex + 2f / 3f * (qx - ex), ey + 2f / 3f * (qy - ey), ex, ey)
                            cx = qx; cy = qy; x = ex; y = ey
                        }
                        'A' -> {
                            val rx = num(); val ry = num(); val rot = num()
                            val large = flag(); val sweep = flag()
                            val ex = ox + num(); val ey = oy + num()
                            path.arc(x, y, rx, ry, rot, large, sweep, ex, ey)
                            x = ex; y = ey; cx = x; cy = y
                        }
                        'Z' -> { path.close(); x = sx; y = sy }
                        else -> break
                    }
                    if (cmd.uppercaseChar() != 'C' && cmd.uppercaseChar() != 'S' && cmd.uppercaseChar() != 'Q' && cmd.uppercaseChar() != 'T') { cx = x; cy = y }
                    prev = cmd
                }
            } catch (_: NumberFormatException) {
            } catch (_: IndexOutOfBoundsException) {
            }
            return path
        }

        private fun hypot(x: Float, y: Float) = sqrt(x * x + y * y)
    }

    private fun arc(x0: Float, y0: Float, rx0: Float, ry0: Float, rotation: Float, large: Boolean, sweep: Boolean, ex: Float, ey: Float) {
        if (x0 == ex && y0 == ey) return
        var rx = abs(rx0.toDouble()); var ry = abs(ry0.toDouble())
        if (rx == 0.0 || ry == 0.0) { lineTo(ex, ey); return }
        val phi = Math.toRadians(rotation.toDouble())
        val cp = cos(phi); val sp = sin(phi)
        val dx = (x0 - ex) / 2.0; val dy = (y0 - ey) / 2.0
        val x1 = cp * dx + sp * dy; val y1 = -sp * dx + cp * dy
        val lambda = x1 * x1 / (rx * rx) + y1 * y1 / (ry * ry)
        if (lambda > 1) { rx *= sqrt(lambda); ry *= sqrt(lambda) }
        val den = rx * rx * y1 * y1 + ry * ry * x1 * x1
        val coef = (if (den == 0.0) 0.0 else sqrt(max(0.0, (rx * rx * ry * ry - den) / den))) * if (large == sweep) -1 else 1
        val ccx = coef * rx * y1 / ry; val ccy = -coef * ry * x1 / rx
        val mx = cp * ccx - sp * ccy + (x0 + ex) / 2; val my = sp * ccx + cp * ccy + (y0 + ey) / 2
        val t1 = atan2((y1 - ccy) / ry, (x1 - ccx) / rx)
        var dt = atan2((-y1 - ccy) / ry, (-x1 - ccx) / rx) - t1
        if (sweep && dt < 0) dt += 2 * PI else if (!sweep && dt > 0) dt -= 2 * PI
        val n = ceil(abs(dt) / (PI / 2) - 1e-9).toInt().coerceAtLeast(1)
        val step = dt / n
        val k = 4.0 / 3.0 * tan(step / 4)
        var a = t1
        fun px(ux: Double, uy: Double) = (mx + cp * rx * ux - sp * ry * uy).toFloat()
        fun py(ux: Double, uy: Double) = (my + sp * rx * ux + cp * ry * uy).toFloat()
        for (s in 0 until n) {
            val b = a + step
            val ca = cos(a); val sa = sin(a); val cb = cos(b); val sb = sin(b)
            val ax1 = ca - k * sa; val ay1 = sa + k * ca
            val bx2 = cb + k * sb; val by2 = sb - k * cb
            val last = s == n - 1
            cubicTo(px(ax1, ay1), py(ax1, ay1), px(bx2, by2), py(bx2, by2), if (last) ex else px(cb, sb), if (last) ey else py(cb, sb))
            a = b
        }
    }
}

internal class Floats {
    var data = FloatArray(32)
    var size = 0

    fun add(x: Float, y: Float) {
        if (size + 2 > data.size) data = data.copyOf(data.size * 2)
        data[size++] = x
        data[size++] = y
    }

    fun clear() { size = 0 }
    fun toArray(): FloatArray = data.copyOf(size)
}

internal object SvgStroker {
    const val BUTT = 0
    const val ROUND = 1
    const val SQUARE = 2
    const val MITER = 0
    const val BEVEL = 2

    fun dash(lines: List<Polyline>, pattern: FloatArray, offset: Float): List<Polyline> {
        val total = pattern.sum()
        if (total <= 0f) return lines
        val out = ArrayList<Polyline>()
        for (line in lines) {
            val src = if (line.closed) line.pts + floatArrayOf(line.pts[0], line.pts[1]) else line.pts
            var phase = offset % total
            if (phase < 0) phase += total
            var k = 0
            while (phase >= pattern[k]) { phase -= pattern[k]; k = (k + 1) % pattern.size }
            var left = pattern[k] - phase
            var on = k % 2 == 0
            val cur = Floats()
            if (on) cur.add(src[0], src[1])
            for (s in 0 until src.size / 2 - 1) {
                var ax = src[s * 2]; var ay = src[s * 2 + 1]
                val bx = src[s * 2 + 2]; val by = src[s * 2 + 3]
                var len = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
                while (len > left) {
                    val f = left / len
                    ax += (bx - ax) * f; ay += (by - ay) * f
                    len -= left
                    if (on) {
                        cur.add(ax, ay)
                        if (cur.size >= 4) out.add(Polyline(cur.toArray(), false))
                        cur.clear()
                    } else cur.add(ax, ay)
                    on = !on
                    k = (k + 1) % pattern.size
                    left = pattern[k]
                }
                left -= len
                if (on) cur.add(bx, by)
            }
            if (on && cur.size >= 4) out.add(Polyline(cur.toArray(), false))
        }
        return out
    }

    fun stroke(lines: List<Polyline>, hw: Float, cap: Int, join: Int, miterLimit: Float, tol: Float): List<FloatArray> {
        val out = ArrayList<FloatArray>()
        val eps = tol * 1e-3f
        for (line in lines) {
            val src = line.pts
            val p = Floats()
            for (i in src.indices step 2) {
                if (p.size >= 2 && abs(p.data[p.size - 2] - src[i]) <= eps && abs(p.data[p.size - 1] - src[i + 1]) <= eps) continue
                p.add(src[i], src[i + 1])
            }
            var n = p.size / 2
            val pts = p.data
            val closed = line.closed && n > 2
            if (closed && abs(pts[0] - pts[(n - 1) * 2]) <= eps && abs(pts[1] - pts[(n - 1) * 2 + 1]) <= eps) n--
            if (n == 1) {
                when (cap) {
                    ROUND -> out.add(circle(pts[0], pts[1], hw, tol))
                    SQUARE -> out.add(floatArrayOf(pts[0] - hw, pts[1] - hw, pts[0] + hw, pts[1] - hw, pts[0] + hw, pts[1] + hw, pts[0] - hw, pts[1] + hw))
                }
                continue
            }
            val segs = if (closed) n else n - 1
            for (s in 0 until segs) {
                val ax = pts[s * 2]; val ay = pts[s * 2 + 1]
                val b = (s + 1) % n
                val bx = pts[b * 2]; val by = pts[b * 2 + 1]
                val len = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
                val nx = -(by - ay) / len * hw; val ny = (bx - ax) / len * hw
                out.add(floatArrayOf(ax + nx, ay + ny, bx + nx, by + ny, bx - nx, by - ny, ax - nx, ay - ny))
            }
            val joints = if (closed) 0 until n else 1 until n - 1
            for (v in joints) {
                val a = (v - 1 + n) % n; val c = (v + 1) % n
                val vx = pts[v * 2]; val vy = pts[v * 2 + 1]
                var d0x = vx - pts[a * 2]; var d0y = vy - pts[a * 2 + 1]
                var d1x = pts[c * 2] - vx; var d1y = pts[c * 2 + 1] - vy
                val l0 = sqrt(d0x * d0x + d0y * d0y); val l1 = sqrt(d1x * d1x + d1y * d1y)
                d0x /= l0; d0y /= l0; d1x /= l1; d1y /= l1
                val cross = d0x * d1y - d0y * d1x
                val dot = d0x * d1x + d0y * d1y
                if (abs(cross) < 1e-6f && dot > 0f) continue
                val shallow = hw * (1f - sqrt(((1f + dot) / 2f).coerceAtLeast(0f))) < tol
                if (join == ROUND && !shallow) { out.add(circle(vx, vy, hw, tol)); continue }
                val s = if (cross > 0f) -1f else 1f
                val ax = vx - d0y * hw * s; val ay = vy + d0x * hw * s
                val bx = vx - d1y * hw * s; val by = vy + d1x * hw * s
                val ratio = if (1f + dot > 1e-6f) sqrt(2f / (1f + dot)) else Float.MAX_VALUE
                if (join == MITER && ratio <= miterLimit && !shallow) {
                    var mx = -d0y - d1y; var my = d0x + d1x
                    val ml = sqrt(mx * mx + my * my)
                    if (ml > 1e-6f) {
                        mx = mx / ml * s * hw * ratio; my = my / ml * s * hw * ratio
                        out.add(floatArrayOf(vx, vy, ax, ay, vx + mx, vy + my, bx, by))
                        continue
                    }
                }
                out.add(floatArrayOf(vx, vy, ax, ay, bx, by))
            }
            if (!closed && cap != BUTT) {
                for (end in 0..1) {
                    val i = if (end == 0) 0 else n - 1
                    val j = if (end == 0) 1 else n - 2
                    val ex = pts[i * 2]; val ey = pts[i * 2 + 1]
                    if (cap == ROUND) { out.add(circle(ex, ey, hw, tol)); continue }
                    var dx = ex - pts[j * 2]; var dy = ey - pts[j * 2 + 1]
                    val l = sqrt(dx * dx + dy * dy)
                    dx = dx / l * hw; dy = dy / l * hw
                    out.add(floatArrayOf(ex - dy, ey + dx, ex - dy + dx, ey + dx + dy, ex + dy + dx, ey - dx + dy, ex + dy, ey - dx))
                }
            }
        }
        for (i in out.indices) out[i] = oriented(out[i])
        return out
    }

    fun circle(cx: Float, cy: Float, r: Float, tol: Float): FloatArray {
        val step = if (r > tol) 2 * acos(1 - tol / r) else PI.toFloat() / 4
        val n = ceil(2 * PI / step).toInt().coerceIn(8, 720)
        return FloatArray(n * 2) { i -> val a = (i / 2) * 2 * PI / n; if (i % 2 == 0) cx + r * cos(a).toFloat() else cy + r * sin(a).toFloat() }
    }

    private fun oriented(p: FloatArray): FloatArray {
        var area = 0f
        val n = p.size / 2
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1]
        }
        if (area >= 0f) return p
        val r = FloatArray(p.size)
        for (i in 0 until n) { r[i * 2] = p[(n - 1 - i) * 2]; r[i * 2 + 1] = p[(n - 1 - i) * 2 + 1] }
        return r
    }
}

internal object Raster {
    private const val SS = 16

    /** Writes coverage for every pixel of the returned [x0, y0, x1, y1] rect (inclusive) into [out]; null when nothing is covered */
    fun fill(polys: List<FloatArray>, evenOdd: Boolean, w: Int, h: Int, out: FloatArray): IntArray? {
        var e = 0
        for (p in polys) e += p.size / 2
        if (e == 0) return null
        val ex = FloatArray(e); val ey0 = FloatArray(e); val ey1 = FloatArray(e); val slope = FloatArray(e); val dir = IntArray(e)
        var n = 0
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (p in polys) {
            val pts = p.size / 2
            if (pts < 2) continue
            for (s in 0 until pts) {
                val ax = p[s * 2]; val ay = p[s * 2 + 1]
                val t = (s + 1) % pts
                val bx = p[t * 2]; val by = p[t * 2 + 1]
                if (!ax.isFinite() || !ay.isFinite() || !bx.isFinite() || !by.isFinite()) return null
                minX = min(minX, ax); maxX = max(maxX, ax); minY = min(minY, ay); maxY = max(maxY, ay)
                if (ay == by) continue
                if (ay < by) { ex[n] = ax; ey0[n] = ay; ey1[n] = by; dir[n] = 1 } else { ex[n] = bx; ey0[n] = by; ey1[n] = ay; dir[n] = -1 }
                slope[n] = (bx - ax) / (by - ay)
                n++
            }
        }
        if (n == 0) return null
        val bx0 = max(0, floor(minX).toInt()); val bx1 = min(w - 1, ceil(maxX).toInt())
        val by0 = max(0, floor(minY).toInt()); val by1 = min(h - 1, ceil(maxY).toInt() - 1)
        if (bx0 > bx1 || by0 > by1) return null
        val order = (0 until n).sortedBy { ey0[it] }.toIntArray()
        val active = IntArray(n)
        var na = 0
        var next = 0
        val xs = FloatArray(n); val ds = IntArray(n)
        val direct = FloatArray(w + 2); val diff = FloatArray(w + 2)
        val left = bx0.toFloat(); val right = (bx1 + 1).toFloat()
        for (y in by0..by1) {
            java.util.Arrays.fill(direct, bx0, bx1 + 2, 0f)
            java.util.Arrays.fill(diff, bx0, bx1 + 2, 0f)
            for (s in 0 until SS) {
                val sy = y + (s + 0.5f) / SS
                while (next < n && ey0[order[next]] <= sy) active[na++] = order[next++]
                var k = 0; var j = 0
                for (a in 0 until na) {
                    val id = active[a]
                    if (ey1[id] <= sy) continue
                    active[j++] = id
                    val x = ex[id] + (sy - ey0[id]) * slope[id]
                    var q = k
                    while (q > 0 && xs[q - 1] > x) { xs[q] = xs[q - 1]; ds[q] = ds[q - 1]; q-- }
                    xs[q] = x; ds[q] = dir[id]
                    k++
                }
                na = j
                var wind = 0
                for (i in 0 until k - 1) {
                    wind += ds[i]
                    if (if (evenOdd) (wind and 1) == 0 else wind == 0) continue
                    val xa = max(xs[i], left); val xb = min(xs[i + 1], right)
                    if (xb <= xa) continue
                    val ia = xa.toInt(); val ib = xb.toInt()
                    if (ia == ib) { direct[ia] += xb - xa; continue }
                    direct[ia] += ia + 1 - xa
                    diff[ia + 1] += 1f
                    diff[ib] -= 1f
                    direct[ib] += xb - ib
                }
            }
            var run = 0f
            val row = y * w
            for (x in bx0..bx1) {
                run += diff[x]
                out[row + x] = min(1f, (direct[x] + run) / SS)
            }
        }
        return intArrayOf(bx0, by0, bx1, by1)
    }
}

internal object SvgColor {
    const val BLACK = 0xFF000000.toInt()

    private val named: Map<String, Int> by lazy {
        NAMES.split(',').associate { val i = it.indexOf(':'); it.substring(0, i) to (BLACK or it.substring(i + 1).toInt(16)) }
    }

    fun parse(value: String, current: Int): Int? {
        val v = value.trim().lowercase()
        if (v == "currentcolor") return current
        if (v == "transparent") return 0
        if (v.startsWith('#')) {
            val hex = v.substring(1)
            if (hex.any { Character.digit(it, 16) < 0 }) return null
            return when (hex.length) {
                3 -> BLACK or hex.map { "$it$it" }.joinToString("").toInt(16)
                4 -> hex.map { "$it$it" }.joinToString("").toLong(16).let { (it ushr 8 or (it and 0xFF shl 24)).toInt() }
                6 -> BLACK or hex.toInt(16)
                8 -> hex.toLong(16).let { (it ushr 8 or (it and 0xFF shl 24)).toInt() }
                else -> null
            }
        }
        val fn = Regex("^(rgba?|hsla?)\\((.*)\\)$").find(v)
        if (fn != null) {
            val parts = fn.groupValues[2].split(Regex("[\\s,/]+")).filter { it.isNotEmpty() }
            if (parts.size < 3) return null
            fun channel(s: String, scale: Float): Float? =
                if (s.endsWith('%')) s.dropLast(1).toFloatOrNull()?.div(100f)?.times(scale) else s.toFloatOrNull()
            val alpha = parts.getOrNull(3)?.let { channel(it, 1f) ?: return null } ?: 1f
            val rgb = if (fn.groupValues[1].startsWith("rgb")) {
                FloatArray(3) { (channel(parts[it], 255f) ?: return null) / 255f }
            } else {
                val hue = (parts[0].removeSuffix("deg").toFloatOrNull() ?: return null) / 360f
                val sat = channel(parts[1], 1f) ?: return null
                val light = channel(parts[2], 1f) ?: return null
                hsl(hue - floor(hue), sat.coerceIn(0f, 1f), light.coerceIn(0f, 1f))
            }
            fun b(f: Float) = (f.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            return (b(alpha) shl 24) or (b(rgb[0]) shl 16) or (b(rgb[1]) shl 8) or b(rgb[2])
        }
        return named[v]
    }

    private fun hsl(h: Float, s: Float, l: Float): FloatArray {
        val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        fun hue(t0: Float): Float {
            val t = if (t0 < 0) t0 + 1 else if (t0 > 1) t0 - 1 else t0
            return when {
                t < 1f / 6 -> p + (q - p) * 6 * t
                t < 0.5f -> q
                t < 2f / 3 -> p + (q - p) * (2f / 3 - t) * 6
                else -> p
            }
        }
        return floatArrayOf(hue(h + 1f / 3), hue(h), hue(h - 1f / 3))
    }

    private const val NAMES = "aliceblue:f0f8ff,antiquewhite:faebd7,aqua:00ffff,aquamarine:7fffd4,azure:f0ffff,beige:f5f5dc,bisque:ffe4c4," +
        "black:000000,blanchedalmond:ffebcd,blue:0000ff,blueviolet:8a2be2,brown:a52a2a,burlywood:deb887,cadetblue:5f9ea0,chartreuse:7fff00," +
        "chocolate:d2691e,coral:ff7f50,cornflowerblue:6495ed,cornsilk:fff8dc,crimson:dc143c,cyan:00ffff,darkblue:00008b,darkcyan:008b8b," +
        "darkgoldenrod:b8860b,darkgray:a9a9a9,darkgreen:006400,darkgrey:a9a9a9,darkkhaki:bdb76b,darkmagenta:8b008b,darkolivegreen:556b2f," +
        "darkorange:ff8c00,darkorchid:9932cc,darkred:8b0000,darksalmon:e9967a,darkseagreen:8fbc8f,darkslateblue:483d8b,darkslategray:2f4f4f," +
        "darkslategrey:2f4f4f,darkturquoise:00ced1,darkviolet:9400d3,deeppink:ff1493,deepskyblue:00bfff,dimgray:696969,dimgrey:696969," +
        "dodgerblue:1e90ff,firebrick:b22222,floralwhite:fffaf0,forestgreen:228b22,fuchsia:ff00ff,gainsboro:dcdcdc,ghostwhite:f8f8ff,gold:ffd700," +
        "goldenrod:daa520,gray:808080,grey:808080,green:008000,greenyellow:adff2f,honeydew:f0fff0,hotpink:ff69b4,indianred:cd5c5c,indigo:4b0082," +
        "ivory:fffff0,khaki:f0e68c,lavender:e6e6fa,lavenderblush:fff0f5,lawngreen:7cfc00,lemonchiffon:fffacd,lightblue:add8e6,lightcoral:f08080," +
        "lightcyan:e0ffff,lightgoldenrodyellow:fafad2,lightgray:d3d3d3,lightgreen:90ee90,lightgrey:d3d3d3,lightpink:ffb6c1,lightsalmon:ffa07a," +
        "lightseagreen:20b2aa,lightskyblue:87cefa,lightslategray:778899,lightslategrey:778899,lightsteelblue:b0c4de,lightyellow:ffffe0,lime:00ff00," +
        "limegreen:32cd32,linen:faf0e6,magenta:ff00ff,maroon:800000,mediumaquamarine:66cdaa,mediumblue:0000cd,mediumorchid:ba55d3," +
        "mediumpurple:9370db,mediumseagreen:3cb371,mediumslateblue:7b68ee,mediumspringgreen:00fa9a,mediumturquoise:48d1cc," +
        "mediumvioletred:c71585,midnightblue:191970,mintcream:f5fffa,mistyrose:ffe4e1,moccasin:ffe4b5,navajowhite:ffdead,navy:000080," +
        "oldlace:fdf5e6,olive:808000,olivedrab:6b8e23,orange:ffa500,orangered:ff4500,orchid:da70d6,palegoldenrod:eee8aa,palegreen:98fb98," +
        "paleturquoise:afeeee,palevioletred:db7093,papayawhip:ffefd5,peachpuff:ffdab9,peru:cd853f,pink:ffc0cb,plum:dda0dd,powderblue:b0e0e6," +
        "purple:800080,rebeccapurple:663399,red:ff0000,rosybrown:bc8f8f,royalblue:4169e1,saddlebrown:8b4513,salmon:fa8072,sandybrown:f4a460," +
        "seagreen:2e8b57,seashell:fff5ee,sienna:a0522d,silver:c0c0c0,skyblue:87ceeb,slateblue:6a5acd,slategray:708090,slategrey:708090," +
        "snow:fffafa,springgreen:00ff7f,steelblue:4682b4,tan:d2b48c,teal:008080,thistle:d8bfd8,tomato:ff6347,turquoise:40e0d0,violet:ee82ee," +
        "wheat:f5deb3,white:ffffff,whitesmoke:f5f5f5,yellow:ffff00,yellowgreen:9acd32"
}
