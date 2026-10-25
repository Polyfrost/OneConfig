package org.polyfrost.oneconfig.api.notifications.v1

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyBitmap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@ApiStatus.Internal
object SvgMask {
    private val cache = ConcurrentHashMap<String, Optional>()

    private class Optional(val bitmap: PolyBitmap?)

    private class Shape(val polys: List<FloatArray>, val fill: Int?, val stroke: Int?, val strokeWidth: Float, val evenOdd: Boolean)

    fun get(resourcePath: String, pixelSize: Int, tint: Int? = null): PolyBitmap? {
        val size = pixelSize.coerceIn(1, 2048)
        return cache.getOrPut("$resourcePath@$size@$tint") {
            Optional(runCatching { readResource(resourcePath)?.let { rasterize(String(it), size, tint) } }.getOrNull())
        }.bitmap
    }

    fun rasterize(svg: String, size: Int, tint: Int? = null): PolyBitmap? {
        val root = Regex("<svg\\b[^>]*>").find(svg)?.value ?: return null
        val view = attr(root, "viewBox")?.let(::numbers)
        val vx = view?.getOrNull(0) ?: 0f
        val vy = view?.getOrNull(1) ?: 0f
        val vw = view?.getOrNull(2) ?: attr(root, "width")?.toFloatOrNull() ?: size.toFloat()
        val vh = view?.getOrNull(3) ?: attr(root, "height")?.toFloatOrNull() ?: size.toFloat()
        val rootFill = attr(root, "fill")
        val rootStroke = attr(root, "stroke")
        val rootWidth = attr(root, "stroke-width")

        val body = svg.replace(Regex("<defs\\b.*?</defs>|<clipPath\\b.*?</clipPath>", RegexOption.DOT_MATCHES_ALL), "")
        val shapes = Regex("<(path|rect|circle|ellipse)\\b[^>]*>").findAll(body).mapNotNull { m ->
            val el = m.value
            val polys = if (m.groupValues[1] == "circle" || m.groupValues[1] == "ellipse") {
                val cx = attr(el, "cx")?.toFloatOrNull() ?: 0f
                val cy = attr(el, "cy")?.toFloatOrNull() ?: 0f
                val rx = (attr(el, "rx") ?: attr(el, "r"))?.toFloatOrNull() ?: return@mapNotNull null
                val ry = (attr(el, "ry") ?: attr(el, "r"))?.toFloatOrNull() ?: return@mapNotNull null
                listOf(FloatArray(66) { i -> val a = (i / 2) * (2 * Math.PI / 32); if (i % 2 == 0) cx + rx * cos(a).toFloat() else cy + ry * sin(a).toFloat() })
            } else if (m.groupValues[1] == "rect") {
                val x = attr(el, "x")?.toFloatOrNull() ?: 0f
                val y = attr(el, "y")?.toFloatOrNull() ?: 0f
                val w = attr(el, "width")?.toFloatOrNull() ?: return@mapNotNull null
                val h = attr(el, "height")?.toFloatOrNull() ?: return@mapNotNull null
                listOf(floatArrayOf(x, y, x + w, y, x + w, y + h, x, y + h, x, y))
            } else parsePath(attr(el, "d") ?: return@mapNotNull null)
            val fill = color(attr(el, "fill") ?: rootFill ?: "black")
            val stroke = color(attr(el, "stroke") ?: rootStroke ?: "none")
            val sw = (attr(el, "stroke-width") ?: rootWidth)?.toFloatOrNull() ?: 1f
            if (fill == null && stroke == null) null else Shape(polys, fill, stroke, sw, attr(el, "fill-rule") == "evenodd")
        }.toList()

        val scale = size / max(vw, vh)
        val w = max(1, (vw * scale).roundToInt())
        val h = max(1, (vh * scale).roundToInt())
        val acc = FloatArray(w * h * 4)
        val cov = FloatArray(w * h)
        for (shape in shapes) {
            if (shape.fill != null) {
                fillCoverage(shape.polys, shape.evenOdd, w, h, vx, vy, scale, cov)
                composite(acc, cov, tint ?: shape.fill)
            }
            if (shape.stroke != null) {
                strokeCoverage(shape.polys, shape.strokeWidth / 2f, w, h, vx, vy, scale, cov)
                composite(acc, cov, tint ?: shape.stroke)
            }
        }
        val px = ByteArray(w * h * 4)
        for (i in 0 until w * h) {
            val a = acc[i * 4 + 3]
            if (a <= 0f) continue
            for (c in 0 until 3) px[i * 4 + c] = (acc[i * 4 + c] / a * 255f).roundToInt().coerceIn(0, 255).toByte()
            px[i * 4 + 3] = (a * 255f).roundToInt().coerceIn(0, 255).toByte()
        }
        return PolyBitmap(w, h, px)
    }

    private fun composite(acc: FloatArray, cov: FloatArray, argb: Int) {
        val ca = (argb ushr 24) / 255f
        val r = (argb shr 16 and 0xFF) / 255f
        val g = (argb shr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        for (i in cov.indices) {
            val c = cov[i] * ca
            if (c <= 0f) continue
            val o = i * 4
            acc[o] = r * c + acc[o] * (1f - c)
            acc[o + 1] = g * c + acc[o + 1] * (1f - c)
            acc[o + 2] = b * c + acc[o + 2] * (1f - c)
            acc[o + 3] = c + acc[o + 3] * (1f - c)
        }
    }

    private fun strokeCoverage(polys: List<FloatArray>, half: Float, w: Int, h: Int, vx: Float, vy: Float, scale: Float, out: FloatArray) {
        out.fill(0f)
        val reach = half + 1f / scale
        for (poly in polys) {
            for (s in 0 until poly.size / 2 - 1) {
                val ax = poly[s * 2]; val ay = poly[s * 2 + 1]; val bx = poly[s * 2 + 2]; val by = poly[s * 2 + 3]
                val x0 = max(0, ((min(ax, bx) - reach - vx) * scale).toInt())
                val x1 = min(w - 1, ((max(ax, bx) + reach - vx) * scale).toInt())
                val y0 = max(0, ((min(ay, by) - reach - vy) * scale).toInt())
                val y1 = min(h - 1, ((max(ay, by) + reach - vy) * scale).toInt())
                val dx = bx - ax; val dy = by - ay
                val len2 = dx * dx + dy * dy
                for (y in y0..y1) for (x in x0..x1) {
                    val px = vx + (x + 0.5f) / scale
                    val py = vy + (y + 0.5f) / scale
                    val t = if (len2 > 0f) (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0f, 1f) else 0f
                    val ex = px - (ax + dx * t); val ey = py - (ay + dy * t)
                    val c = ((half - sqrt(ex * ex + ey * ey)) * scale + 0.5f).coerceIn(0f, 1f)
                    val i = y * w + x
                    if (c > out[i]) out[i] = c
                }
            }
        }
    }

    private fun fillCoverage(polys: List<FloatArray>, evenOdd: Boolean, w: Int, h: Int, vx: Float, vy: Float, scale: Float, out: FloatArray) {
        out.fill(0f)
        val n = 4
        var xs = FloatArray(64)
        var dirs = IntArray(64)
        for (y in 0 until h) for (sy in 0 until n) {
            val py = vy + (y + (sy + 0.5f) / n) / scale
            var count = 0
            for (poly in polys) {
                val pts = poly.size / 2
                for (s in 0 until pts) {
                    val ax = poly[s * 2]; val ay = poly[s * 2 + 1]
                    val bx = poly[(s + 1) % pts * 2]; val by = poly[(s + 1) % pts * 2 + 1]
                    if ((ay <= py) == (by <= py)) continue
                    if (count == xs.size) { xs = xs.copyOf(count * 2); dirs = dirs.copyOf(count * 2) }
                    xs[count] = ax + (py - ay) / (by - ay) * (bx - ax)
                    dirs[count] = if (by > ay) 1 else -1
                    count++
                }
            }
            for (i in 1 until count) {
                val x = xs[i]; val d = dirs[i]; var j = i - 1
                while (j >= 0 && xs[j] > x) { xs[j + 1] = xs[j]; dirs[j + 1] = dirs[j]; j-- }
                xs[j + 1] = x; dirs[j + 1] = d
            }
            var ci = 0
            var wind = 0
            for (x in 0 until w) for (sx in 0 until n) {
                val px = vx + (x + (sx + 0.5f) / n) / scale
                while (ci < count && xs[ci] <= px) wind += dirs[ci++]
                if (if (evenOdd) (wind and 1) != 0 else wind != 0) out[y * w + x] += 1f / (n * n)
            }
        }
    }

    fun parsePath(d: String): List<FloatArray> {
        val tokens = Regex("[A-Za-z]|[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?").findAll(d).map { it.value }.toList()
        val polys = ArrayList<FloatArray>()
        val cur = ArrayList<Float>()
        var x = 0f; var y = 0f; var sx = 0f; var sy = 0f
        var cx = 0f; var cy = 0f
        var i = 0
        var cmd = 'M'
        var prev = ' '
        fun num() = tokens[i++].toFloat()
        fun line(nx: Float, ny: Float) { cur.add(nx); cur.add(ny); x = nx; y = ny }
        fun flush() { if (cur.size >= 2) polys.add(cur.toFloatArray()); cur.clear() }
        fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, ex: Float, ey: Float) {
            val x0 = x; val y0 = y
            for (k in 1..16) {
                val t = k / 16f; val u = 1f - t
                line(u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * ex,
                    u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * ey)
            }
            cx = x2; cy = y2
        }
        while (i < tokens.size) {
            val t = tokens[i]
            if (t[0].isLetter()) { cmd = t[0]; i++ }
            val rel = cmd.isLowerCase()
            val ox = if (rel) x else 0f
            val oy = if (rel) y else 0f
            when (cmd.uppercaseChar()) {
                'M' -> {
                    flush()
                    x = ox + num(); y = oy + num(); sx = x; sy = y
                    cur.add(x); cur.add(y)
                    cmd = if (rel) 'l' else 'L'
                }
                'L' -> line(ox + num(), oy + num())
                'H' -> line(ox + num(), y)
                'V' -> line(x, oy + num())
                'C' -> cubic(ox + num(), oy + num(), ox + num(), oy + num(), ox + num(), oy + num())
                'S' -> {
                    val reflect = prev.uppercaseChar() == 'C' || prev.uppercaseChar() == 'S'
                    val x1 = if (reflect) 2 * x - cx else x
                    val y1 = if (reflect) 2 * y - cy else y
                    cubic(x1, y1, ox + num(), oy + num(), ox + num(), oy + num())
                }
                'Q', 'T' -> {
                    val (qx, qy) = if (cmd.uppercaseChar() == 'Q') (ox + num()) to (oy + num())
                    else if (prev.uppercaseChar() == 'Q' || prev.uppercaseChar() == 'T') (2 * x - cx) to (2 * y - cy) else x to y
                    val ex = ox + num(); val ey = oy + num()
                    cubic(x + 2f / 3f * (qx - x), y + 2f / 3f * (qy - y), ex + 2f / 3f * (qx - ex), ey + 2f / 3f * (qy - ey), ex, ey)
                    cx = qx; cy = qy
                }
                'Z' -> { line(sx, sy); flush(); cur.add(sx); cur.add(sy) }
                else -> return polys.also { flush() }
            }
            prev = cmd
        }
        flush()
        return polys
    }

    private fun attr(el: String, name: String): String? = Regex("\\s$name=\"([^\"]*)\"").find(el)?.groupValues?.get(1)

    private fun numbers(s: String) = s.trim().split(Regex("[\\s,]+")).mapNotNull { it.toFloatOrNull() }

    private fun color(v: String): Int? = when (val c = v.trim().lowercase()) {
        "none", "transparent" -> null
        "white" -> -1
        "black", "currentcolor" -> 0xFF000000.toInt()
        else -> if (c.startsWith('#')) {
            val hex = if (c.length == 4) c.drop(1).map { "$it$it" }.joinToString("") else c.drop(1)
            hex.toLongOrNull(16)?.let { (0xFF000000 or it).toInt() }
        } else 0xFF000000.toInt()
    }

    private fun readResource(path: String): ByteArray? {
        val loaders = listOf(SvgMask::class.java.classLoader, Thread.currentThread().contextClassLoader)
        for (loader in loaders) {
            val stream = loader?.getResourceAsStream(path.removePrefix("/")) ?: SvgMask::class.java.getResourceAsStream(path)
            if (stream != null) return stream.use { it.readBytes() }
        }
        return null
    }
}
