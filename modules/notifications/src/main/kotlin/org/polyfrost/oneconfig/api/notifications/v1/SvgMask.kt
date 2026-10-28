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

    private fun interface Paint {
        fun at(x: Float, y: Float): Int
    }

    private class Shape(
        val polys: List<FloatArray>, val fill: Paint?, val stroke: Paint?, val strokeWidth: Float, val evenOdd: Boolean,
        val fillOpacity: Float, val strokeOpacity: Float, val mask: String?,
    )

    private class Gradient(val radial: Boolean, val attrs: String, val offsets: FloatArray, val colors: IntArray)

    private class Canvas(val w: Int, val h: Int, val vx: Float, val vy: Float, val scale: Float, val gradients: Map<String, Gradient>, val masks: Map<String, String>) {
        val maskCoverage = HashMap<String, FloatArray>()
    }

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

        val gradients = Regex("<(linear|radial)Gradient\\b([^>]*)>(.*?)</\\1Gradient>", RegexOption.DOT_MATCHES_ALL).findAll(svg).mapNotNull { m ->
            val id = attr(m.groupValues[2], "id") ?: return@mapNotNull null
            val stops = Regex("<stop\\b[^>]*>").findAll(m.groupValues[3]).map { it.value }.toList()
            val offsets = FloatArray(stops.size) { fraction(attr(stops[it], "offset"), 0f) }
            val colors = IntArray(stops.size) {
                val rgb = color(attr(stops[it], "stop-color") ?: "black") ?: 0
                withAlpha(rgb, attr(stops[it], "stop-opacity")?.toFloatOrNull() ?: 1f)
            }
            id to Gradient(m.groupValues[1] == "radial", m.groupValues[2], offsets, colors)
        }.toMap()
        val maskBlock = Regex("<mask\\b[^>]*>.*?</mask>", RegexOption.DOT_MATCHES_ALL)
        val masks = maskBlock.findAll(svg).mapNotNull { m -> attr(m.value, "id")?.let { it to m.value } }.toMap()

        val body = svg.replace(Regex("<defs\\b.*?</defs>|<clipPath\\b.*?</clipPath>", RegexOption.DOT_MATCHES_ALL), "").replace(maskBlock, "")
        val scale = size / max(vw, vh)
        val canvas = Canvas(max(1, (vw * scale).roundToInt()), max(1, (vh * scale).roundToInt()), vx, vy, scale, gradients, masks)
        val acc = draw(canvas, body, root, tint)
        val w = canvas.w
        val h = canvas.h
        val px = ByteArray(w * h * 4)
        for (i in 0 until w * h) {
            val a = acc[i * 4 + 3]
            if (a <= 0f) continue
            for (c in 0 until 3) px[i * 4 + c] = (acc[i * 4 + c] / a * 255f).roundToInt().coerceIn(0, 255).toByte()
            px[i * 4 + 3] = (a * 255f).roundToInt().coerceIn(0, 255).toByte()
        }
        return PolyBitmap(w, h, px)
    }

    private fun draw(canvas: Canvas, body: String, parent: String, tint: Int?): FloatArray {
        val rootFill = attr(parent, "fill")
        val rootStroke = attr(parent, "stroke")
        val rootWidth = attr(parent, "stroke-width")
        val shapes = Regex("<(path|rect|circle|ellipse)\\b[^>]*>").findAll(body).mapNotNull { m ->
            val el = m.value
            val polys = when (m.groupValues[1]) {
                "circle", "ellipse" -> {
                    val cx = attr(el, "cx")?.toFloatOrNull() ?: 0f
                    val cy = attr(el, "cy")?.toFloatOrNull() ?: 0f
                    val rx = (attr(el, "rx") ?: attr(el, "r"))?.toFloatOrNull() ?: return@mapNotNull null
                    val ry = (attr(el, "ry") ?: attr(el, "r"))?.toFloatOrNull() ?: return@mapNotNull null
                    listOf(FloatArray(66) { i -> val a = (i / 2) * (2 * Math.PI / 32); if (i % 2 == 0) cx + rx * cos(a).toFloat() else cy + ry * sin(a).toFloat() })
                }
                "rect" -> {
                    val x = attr(el, "x")?.toFloatOrNull() ?: 0f
                    val y = attr(el, "y")?.toFloatOrNull() ?: 0f
                    val w = attr(el, "width")?.toFloatOrNull() ?: return@mapNotNull null
                    val h = attr(el, "height")?.toFloatOrNull() ?: return@mapNotNull null
                    val rx = (attr(el, "rx") ?: attr(el, "ry"))?.toFloatOrNull() ?: 0f
                    val ry = (attr(el, "ry") ?: attr(el, "rx"))?.toFloatOrNull() ?: 0f
                    listOf(roundRect(x, y, w, h, min(rx, w / 2f), min(ry, h / 2f)))
                }
                else -> parsePath(attr(el, "d") ?: return@mapNotNull null)
            }
            val opacity = attr(el, "opacity")?.toFloatOrNull() ?: 1f
            val fill = paint(canvas, attr(el, "fill") ?: rootFill ?: "black", polys)
            val stroke = paint(canvas, attr(el, "stroke") ?: rootStroke ?: "none", polys)
            val sw = (attr(el, "stroke-width") ?: rootWidth)?.toFloatOrNull() ?: 1f
            val fillOpacity = opacity * (attr(el, "fill-opacity")?.toFloatOrNull() ?: 1f)
            val strokeOpacity = opacity * (attr(el, "stroke-opacity")?.toFloatOrNull() ?: 1f)
            val mask = attr(el, "mask")?.let { Regex("url\\(#([^)]+)\\)").find(it)?.groupValues?.get(1) }
            if (fill == null && stroke == null) null else Shape(polys, fill, stroke, sw, attr(el, "fill-rule") == "evenodd", fillOpacity, strokeOpacity, mask)
        }.toList()

        val acc = FloatArray(canvas.w * canvas.h * 4)
        val cov = FloatArray(canvas.w * canvas.h)
        for (shape in shapes) {
            val mask = shape.mask?.let { maskCoverage(canvas, it) }
            if (shape.fill != null) {
                fillCoverage(shape.polys, shape.evenOdd, canvas.w, canvas.h, canvas.vx, canvas.vy, canvas.scale, cov)
                composite(canvas, acc, cov, mask, shape.fill, shape.fillOpacity, tint)
            }
            if (shape.stroke != null) {
                strokeCoverage(shape.polys, shape.strokeWidth / 2f, canvas.w, canvas.h, canvas.vx, canvas.vy, canvas.scale, cov)
                composite(canvas, acc, cov, mask, shape.stroke, shape.strokeOpacity, tint)
            }
        }
        return acc
    }

    private fun maskCoverage(canvas: Canvas, id: String): FloatArray? {
        canvas.maskCoverage[id]?.let { return it }
        val block = canvas.masks[id] ?: return null
        val open = Regex("<mask\\b[^>]*>").find(block)?.value ?: return null
        val acc = draw(canvas, block.removePrefix(open), open, null)
        val out = FloatArray(canvas.w * canvas.h) { i ->
            val a = acc[i * 4 + 3]
            if (a <= 0f) 0f else (0.2125f * acc[i * 4] + 0.7154f * acc[i * 4 + 1] + 0.0721f * acc[i * 4 + 2])
        }
        canvas.maskCoverage[id] = out
        return out
    }

    private fun paint(canvas: Canvas, value: String, polys: List<FloatArray>): Paint? {
        val ref = Regex("url\\(#([^)]+)\\)").find(value)?.groupValues?.get(1)
        if (ref == null) return color(value)?.let { c -> Paint { _, _ -> c } }
        val g = canvas.gradients[ref] ?: return null
        val userSpace = attr(g.attrs, "gradientUnits") == "userSpaceOnUse"
        var m = transform(attr(g.attrs, "gradientTransform"))
        if (!userSpace) {
            var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
            for (p in polys) for (k in p.indices step 2) { x0 = min(x0, p[k]); x1 = max(x1, p[k]); y0 = min(y0, p[k + 1]); y1 = max(y1, p[k + 1]) }
            m = multiply(floatArrayOf(x1 - x0, 0f, 0f, y1 - y0, x0, y0), m)
        }
        val inv = invert(m) ?: return null
        fun at(name: String, def: Float) = fraction(attr(g.attrs, name), def)
        if (g.radial) {
            val cx = at("cx", 0.5f); val cy = at("cy", 0.5f); val r = at("r", 0.5f)
            if (r <= 0f) return null
            return Paint { x, y ->
                val gx = inv[0] * x + inv[2] * y + inv[4] - cx
                val gy = inv[1] * x + inv[3] * y + inv[5] - cy
                sample(g, sqrt(gx * gx + gy * gy) / r)
            }
        }
        val x1 = at("x1", 0f); val y1 = at("y1", 0f); val x2 = at("x2", 1f); val y2 = at("y2", 0f)
        val dx = x2 - x1; val dy = y2 - y1
        val len2 = dx * dx + dy * dy
        return Paint { x, y ->
            val gx = inv[0] * x + inv[2] * y + inv[4]
            val gy = inv[1] * x + inv[3] * y + inv[5]
            sample(g, if (len2 > 0f) ((gx - x1) * dx + (gy - y1) * dy) / len2 else 0f)
        }
    }

    private fun sample(g: Gradient, t: Float): Int {
        val n = g.offsets.size
        if (n == 0) return 0
        if (t <= g.offsets[0]) return g.colors[0]
        for (i in 1 until n) {
            if (t > g.offsets[i]) continue
            val span = g.offsets[i] - g.offsets[i - 1]
            val f = if (span > 0f) (t - g.offsets[i - 1]) / span else 1f
            val a = g.colors[i - 1]; val b = g.colors[i]
            var out = 0
            for (s in 0..24 step 8) {
                val ca = a ushr s and 0xFF; val cb = b ushr s and 0xFF
                out = out or ((ca + (cb - ca) * f).roundToInt().coerceIn(0, 255) shl s)
            }
            return out
        }
        return g.colors[n - 1]
    }

    private fun transform(s: String?): FloatArray {
        var m = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)
        if (s == null) return m
        for (t in Regex("(\\w+)\\s*\\(([^)]*)\\)").findAll(s)) {
            val n = numbers(t.groupValues[2])
            val op = when (t.groupValues[1]) {
                "translate" -> floatArrayOf(1f, 0f, 0f, 1f, n.getOrElse(0) { 0f }, n.getOrElse(1) { 0f })
                "scale" -> floatArrayOf(n.getOrElse(0) { 1f }, 0f, 0f, n.getOrElse(1) { n.getOrElse(0) { 1f } }, 0f, 0f)
                "rotate" -> {
                    val a = Math.toRadians(n.getOrElse(0) { 0f }.toDouble())
                    val c = cos(a).toFloat(); val sn = sin(a).toFloat()
                    val px = n.getOrElse(1) { 0f }; val py = n.getOrElse(2) { 0f }
                    floatArrayOf(c, sn, -sn, c, px - c * px + sn * py, py - sn * px - c * py)
                }
                "matrix" -> if (n.size >= 6) n.take(6).toFloatArray() else continue
                else -> continue
            }
            m = multiply(m, op)
        }
        return m
    }

    private fun multiply(a: FloatArray, b: FloatArray) = floatArrayOf(
        a[0] * b[0] + a[2] * b[1], a[1] * b[0] + a[3] * b[1],
        a[0] * b[2] + a[2] * b[3], a[1] * b[2] + a[3] * b[3],
        a[0] * b[4] + a[2] * b[5] + a[4], a[1] * b[4] + a[3] * b[5] + a[5],
    )

    private fun invert(m: FloatArray): FloatArray? {
        val d = m[0] * m[3] - m[1] * m[2]
        if (d == 0f) return null
        return floatArrayOf(m[3] / d, -m[1] / d, -m[2] / d, m[0] / d, (m[2] * m[5] - m[3] * m[4]) / d, (m[1] * m[4] - m[0] * m[5]) / d)
    }

    private fun roundRect(x: Float, y: Float, w: Float, h: Float, rx: Float, ry: Float): FloatArray {
        if (rx <= 0f || ry <= 0f) return floatArrayOf(x, y, x + w, y, x + w, y + h, x, y + h, x, y)
        val seg = 8
        val corners = arrayOf(floatArrayOf(x + w - rx, y + ry, -90f), floatArrayOf(x + w - rx, y + h - ry, 0f), floatArrayOf(x + rx, y + h - ry, 90f), floatArrayOf(x + rx, y + ry, 180f))
        val out = FloatArray(4 * (seg + 1) * 2 + 2)
        var k = 0
        for (c in corners) for (i in 0..seg) {
            val a = Math.toRadians((c[2] + 90f * i / seg).toDouble())
            out[k++] = c[0] + rx * cos(a).toFloat()
            out[k++] = c[1] + ry * sin(a).toFloat()
        }
        out[k++] = out[0]
        out[k] = out[1]
        return out
    }

    private fun fraction(v: String?, def: Float): Float {
        val s = v?.trim() ?: return def
        return if (s.endsWith('%')) s.dropLast(1).toFloatOrNull()?.div(100f) ?: def else s.toFloatOrNull() ?: def
    }

    private fun withAlpha(argb: Int, opacity: Float) = (((argb ushr 24) * opacity.coerceIn(0f, 1f)).roundToInt() shl 24) or (argb and 0xFFFFFF)

    private fun composite(canvas: Canvas, acc: FloatArray, cov: FloatArray, mask: FloatArray?, paint: Paint, opacity: Float, tint: Int?) {
        for (i in cov.indices) {
            var c = cov[i] * opacity
            if (mask != null) c *= mask[i]
            if (c <= 0f) continue
            val argb = tint ?: paint.at(canvas.vx + (i % canvas.w + 0.5f) / canvas.scale, canvas.vy + (i / canvas.w + 0.5f) / canvas.scale)
            c *= (argb ushr 24) / 255f
            if (c <= 0f) continue
            val o = i * 4
            acc[o] = (argb shr 16 and 0xFF) / 255f * c + acc[o] * (1f - c)
            acc[o + 1] = (argb shr 8 and 0xFF) / 255f * c + acc[o + 1] * (1f - c)
            acc[o + 2] = (argb and 0xFF) / 255f * c + acc[o + 2] * (1f - c)
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
