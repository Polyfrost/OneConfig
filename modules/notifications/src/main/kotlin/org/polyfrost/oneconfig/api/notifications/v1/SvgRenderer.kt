package org.polyfrost.oneconfig.api.notifications.v1

import java.io.StringReader
import java.util.Base64
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import org.polyfrost.compose.render.PolyBitmap
import org.w3c.dom.Element
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

internal class SvgStyle(
    var fill: String = "black",
    var fillOpacity: Float = 1f,
    var fillEvenOdd: Boolean = false,
    var clipEvenOdd: Boolean = false,
    var stroke: String = "none",
    var strokeOpacity: Float = 1f,
    var strokeWidth: String = "1",
    var cap: Int = SvgStroker.BUTT,
    var join: Int = SvgStroker.MITER,
    var miterLimit: Float = 4f,
    var dashArray: String = "none",
    var dashOffset: String = "0",
    var color: Int = SvgColor.BLACK,
    var visible: Boolean = true,
    var linearFilters: Boolean = true,
) {
    fun copy() = SvgStyle(fill, fillOpacity, fillEvenOdd, clipEvenOdd, stroke, strokeOpacity, strokeWidth, cap, join, miterLimit, dashArray, dashOffset, color, visible, linearFilters)
}

internal abstract class SvgPaint {
    /** Writes the premultiplied colour at device position ([x], [y]) into [out] */
    abstract fun eval(x: Float, y: Float, out: FloatArray)
}

private class SolidPaint(argb: Int, opacity: Float) : SvgPaint() {
    private val a = (argb ushr 24) / 255f * opacity
    private val r = (argb shr 16 and 0xFF) / 255f * a
    private val g = (argb shr 8 and 0xFF) / 255f * a
    private val b = (argb and 0xFF) / 255f * a

    override fun eval(x: Float, y: Float, out: FloatArray) {
        out[0] = r; out[1] = g; out[2] = b; out[3] = a
    }
}

private class GradientPaint(
    private val inv: FloatArray,
    private val offsets: FloatArray,
    private val colors: FloatArray,
    private val spread: Int,
    private val opacity: Float,
    private val t: (Float, Float) -> Float,
) : SvgPaint() {
    override fun eval(x: Float, y: Float, out: FloatArray) {
        var v = t(inv[0] * x + inv[2] * y + inv[4], inv[1] * x + inv[3] * y + inv[5])
        if (v.isNaN()) { out.fill(0f); return }
        v = when (spread) {
            1 -> { val m = abs(v) % 2f; if (m > 1f) 2f - m else m }
            2 -> v - floor(v)
            else -> v
        }
        val n = offsets.size
        var i = 0
        while (i < n && offsets[i] < v) i++
        val o = when {
            i == 0 -> 0
            i == n -> (n - 1) * 4
            else -> -1
        }
        val r: Float; val g: Float; val b: Float; val a: Float
        if (o >= 0) {
            r = colors[o]; g = colors[o + 1]; b = colors[o + 2]; a = colors[o + 3]
        } else {
            val span = offsets[i] - offsets[i - 1]
            val f = if (span > 0f) (v - offsets[i - 1]) / span else 1f
            val p = (i - 1) * 4; val q = i * 4
            r = colors[p] + (colors[q] - colors[p]) * f
            g = colors[p + 1] + (colors[q + 1] - colors[p + 1]) * f
            b = colors[p + 2] + (colors[q + 2] - colors[p + 2]) * f
            a = colors[p + 3] + (colors[q + 3] - colors[p + 3]) * f
        }
        val pa = a * opacity
        out[0] = r * pa; out[1] = g * pa; out[2] = b * pa; out[3] = pa
    }
}

private class TilePaint(private val inv: FloatArray, private val tile: FloatArray, private val tw: Int, private val th: Int, private val opacity: Float) : SvgPaint() {
    override fun eval(x: Float, y: Float, out: FloatArray) {
        val u = inv[0] * x + inv[2] * y + inv[4] - 0.5f
        val v = inv[1] * x + inv[3] * y + inv[5] - 0.5f
        SvgCanvas.bilinear(tile, tw, th, u, v, true, out)
        for (c in 0..3) out[c] *= opacity
    }
}

internal class SvgDoc(val root: Element) {
    val ids = HashMap<String, Element>()
    private val propCache = HashMap<Element, Map<String, String>>()
    private val styleCache = HashMap<Element, SvgStyle>()

    init {
        index(root)
    }

    private fun index(el: Element) {
        el.getAttribute("id").takeIf { it.isNotEmpty() }?.let { ids.putIfAbsent(it, el) }
        for (c in children(el)) index(c)
    }

    fun isSvg(el: Element) = el.namespaceURI == null || el.namespaceURI == SVG_NS

    fun props(el: Element): Map<String, String> = propCache.getOrPut(el) {
        val map = HashMap<String, String>()
        val attrs = el.attributes
        for (i in 0 until attrs.length) {
            val a = attrs.item(i)
            if (a.namespaceURI != null) continue
            cssValue(a.nodeValue)?.let { map[a.localName ?: a.nodeName] = it }
        }
        map["style"]?.let { style ->
            for (decl in style.split(';')) {
                val k = decl.indexOf(':')
                if (k <= 0) continue
                cssValue(decl.substring(k + 1).replace("!important", ""))?.let { map[decl.substring(0, k).trim().lowercase()] = it }
            }
        }
        map
    }

    fun href(el: Element): Element? {
        val ref = el.getAttributeNS(XLINK_NS, "href").ifEmpty { el.getAttribute("href") }.trim()
        return if (ref.startsWith("#")) ids[ref.substring(1)] else null
    }

    fun styleOf(el: Element): SvgStyle = styleCache[el] ?: run {
        val parent = el.parentNode as? Element
        derive(parent?.let(::styleOf) ?: SvgStyle(), el).also { styleCache[el] = it }
    }

    fun derive(parent: SvgStyle, el: Element): SvgStyle {
        val p = props(el)
        if (p.isEmpty()) return parent
        val s = parent.copy()
        p["color"]?.let { SvgColor.parse(it, parent.color)?.let { c -> s.color = c } }
        p["fill"]?.takeIf(::validPaint)?.let { s.fill = it }
        p["stroke"]?.takeIf(::validPaint)?.let { s.stroke = it }
        p["fill-opacity"]?.let(::opacity)?.let { s.fillOpacity = it }
        p["stroke-opacity"]?.let(::opacity)?.let { s.strokeOpacity = it }
        when (p["fill-rule"]) { "evenodd" -> s.fillEvenOdd = true; "nonzero" -> s.fillEvenOdd = false }
        when (p["clip-rule"]) { "evenodd" -> s.clipEvenOdd = true; "nonzero" -> s.clipEvenOdd = false }
        p["stroke-width"]?.takeIf { length(it) != null }?.let { s.strokeWidth = it }
        when (p["stroke-linecap"]) { "butt" -> s.cap = SvgStroker.BUTT; "round" -> s.cap = SvgStroker.ROUND; "square" -> s.cap = SvgStroker.SQUARE }
        when (p["stroke-linejoin"]) {
            "miter", "miter-clip", "arcs" -> s.join = SvgStroker.MITER
            "round" -> s.join = SvgStroker.ROUND
            "bevel" -> s.join = SvgStroker.BEVEL
        }
        p["stroke-miterlimit"]?.toFloatOrNull()?.takeIf { it >= 1f }?.let { s.miterLimit = it }
        p["stroke-dasharray"]?.takeIf { it != "inherit" }?.let { s.dashArray = it }
        p["stroke-dashoffset"]?.takeIf { length(it) != null }?.let { s.dashOffset = it }
        when (p["visibility"]) { "visible" -> s.visible = true; "hidden", "collapse" -> s.visible = false }
        when (p["color-interpolation-filters"]) { "sRGB", "auto" -> s.linearFilters = false; "linearRGB" -> s.linearFilters = true }
        return s
    }

    private fun validPaint(v: String) = v == "none" || v.startsWith("url(") || SvgColor.parse(v, 0) != null

    companion object {
        const val SVG_NS = "http://www.w3.org/2000/svg"
        const val XLINK_NS = "http://www.w3.org/1999/xlink"

        private val VAR = Regex("^var\\(\\s*--[^,)]*(?:,\\s*(.*))?\\)$")
        private val LENGTH = Regex("^([-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?)\\s*(px|pt|pc|mm|cm|in|em|ex|%)?$")

        private fun cssValue(raw: String): String? {
            val v = raw.trim()
            if (!v.startsWith("var(")) return v
            val m = VAR.find(v) ?: return null
            return m.groupValues[1].trim().takeIf { it.isNotEmpty() }?.let(::cssValue) ?: "black"
        }

        fun children(el: Element): List<Element> {
            val out = ArrayList<Element>()
            var n = el.firstChild
            while (n != null) {
                if (n is Element) out.add(n)
                n = n.nextSibling
            }
            return out
        }

        fun opacity(v: String): Float? {
            val f = if (v.endsWith('%')) v.dropLast(1).toFloatOrNull()?.div(100f) else v.toFloatOrNull()
            return f?.coerceIn(0f, 1f)
        }

        /** Returns the number and unit of a length, or null when it does not parse */
        fun length(v: String): Pair<Float, String>? {
            val m = LENGTH.find(v.trim()) ?: return null
            return m.groupValues[1].toFloat() to m.groupValues[2]
        }

        fun urlRef(v: String?): String? = v?.let { Regex("url\\(\\s*['\"]?#([^)'\"]+)['\"]?\\s*\\)").find(it)?.groupValues?.get(1) }

        private val factory: DocumentBuilderFactory by lazy {
            DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isValidating = false
                isXIncludeAware = false
                isExpandEntityReferences = true
                runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
            }
        }

        private val builders = ThreadLocal.withInitial<DocumentBuilder> {
            synchronized(factory) { factory.newDocumentBuilder() }.apply {
                setEntityResolver { _, _ -> InputSource(StringReader("")) }
                setErrorHandler(object : ErrorHandler {
                    override fun warning(e: SAXParseException) {}
                    override fun error(e: SAXParseException) {}
                    override fun fatalError(e: SAXParseException) = throw e
                })
            }
        }

        fun parse(svg: String): SvgDoc? {
            val text = svg.trimStart('﻿', ' ', '\t', '\r', '\n')
            val root = builders.get().parse(InputSource(StringReader(text))).documentElement ?: return null
            if (root.localName != "svg") return null
            return SvgDoc(root)
        }
    }
}

internal class SvgCanvas(private val doc: SvgDoc, val w: Int, val h: Int) {
    class Ctx(val m: FloatArray, val style: SvgStyle, val vw: Float, val vh: Float)

    private val cov = FloatArray(w * h)
    private val tmp = FloatArray(4)
    private val active = HashSet<Element>()

    fun render(root: Element, m: FloatArray, vw: Float, vh: Float): FloatArray {
        val dst = FloatArray(w * h * 4)
        val ctx = Ctx(m, doc.derive(SvgStyle(), root), vw, vh)
        effects(root, ctx, dst, null, { groupBBox(root, ctx) }) { renderChildren(root, ctx, it) }
        return dst
    }

    private fun renderChildren(el: Element, ctx: Ctx, dst: FloatArray) {
        for (c in SvgDoc.children(el)) render(c, ctx, dst)
    }

    private fun render(el: Element, parent: Ctx, dst: FloatArray) {
        if (!doc.isSvg(el) || el.localName !in RENDERED || !active.add(el)) return
        try {
            val p = doc.props(el)
            if (p["display"] == "none" || active.size > 64) return
            val style = doc.derive(parent.style, el)
            val m = Mat.mul(parent.m, Mat.parse(p["transform"]))
            val ctx = Ctx(m, style, parent.vw, parent.vh)
            when (el.localName) {
                "svg" -> {
                    val x = len(p["x"], ctx, 0, 0f); val y = len(p["y"], ctx, 1, 0f)
                    val vw = len(p["width"], ctx, 0, ctx.vw); val vh = len(p["height"], ctx, 1, ctx.vh)
                    nested(el, ctx, dst, x, y, vw, vh)
                }
                "use" -> {
                    val target = doc.href(el) ?: return
                    val x = len(p["x"], ctx, 0, 0f); val y = len(p["y"], ctx, 1, 0f)
                    val inner = Ctx(Mat.mul(m, Mat.translate(x, y)), style, ctx.vw, ctx.vh)
                    effects(el, ctx, dst, null, { bbox(target, inner)?.let { Mat.mapRect(Mat.translate(x, y), it) } }) { layer ->
                        if (target.localName == "symbol" || target.localName == "svg") {
                            if (!active.add(target)) return@effects
                            try {
                                val tp = doc.props(target)
                                val tStyle = doc.derive(style, target)
                                val tCtx = Ctx(inner.m, tStyle, ctx.vw, ctx.vh)
                                val vw = len(p["width"] ?: tp["width"], tCtx, 0, ctx.vw)
                                val vh = len(p["height"] ?: tp["height"], tCtx, 1, ctx.vh)
                                nested(target, tCtx, layer, 0f, 0f, vw, vh)
                            } finally {
                                active.remove(target)
                            }
                        } else render(target, inner, layer)
                    }
                }
                "g", "a", "switch" -> effects(el, ctx, dst, null, { groupBBox(el, ctx) }) { renderChildren(el, ctx, it) }
                "image" -> effects(el, ctx, dst, null, { imageRect(el, ctx) }) { drawImage(el, ctx, it) }
                else -> {
                    val path = shapePath(el, ctx) ?: return
                    effects(el, ctx, dst, null, { path.bounds() }) { drawShape(el, path, ctx, it) }
                }
            }
        } finally {
            active.remove(el)
        }
    }

    private fun nested(el: Element, ctx: Ctx, dst: FloatArray, x: Float, y: Float, vw: Float, vh: Float) {
        if (vw <= 0f || vh <= 0f) return
        val p = doc.props(el)
        val vb = viewBox(p["viewBox"])
        val inner = Mat.mul(Mat.mul(ctx.m, Mat.translate(x, y)), vb?.let { viewBoxMatrix(it, vw, vh, p["preserveAspectRatio"]) } ?: Mat.identity())
        val child = Ctx(inner, ctx.style, vb?.get(2) ?: vw, vb?.get(3) ?: vh)
        val overflow = p["overflow"]
        val clip = if (overflow == "visible" || overflow == "auto") null else floatArrayOf(x, y, x + vw, y + vh)
        effects(el, ctx, dst, clip, { groupBBox(el, child)?.let { Mat.mapRect(Mat.mul(Mat.invert(ctx.m) ?: Mat.identity(), inner), it) } }) {
            renderChildren(el, child, it)
        }
    }

    private inline fun effects(el: Element, ctx: Ctx, dst: FloatArray, clipRect: FloatArray?, bboxOf: () -> FloatArray?, draw: (FloatArray) -> Unit) {
        val p = doc.props(el)
        val opacity = p["opacity"]?.let(SvgDoc::opacity) ?: 1f
        if (opacity <= 0f) return
        val clip = SvgDoc.urlRef(p["clip-path"])?.let { doc.ids[it] }?.takeIf { it.localName == "clipPath" }
        val mask = SvgDoc.urlRef(p["mask"])?.let { doc.ids[it] }?.takeIf { it.localName == "mask" }
        val filter = SvgDoc.urlRef(p["filter"])?.let { doc.ids[it] }?.takeIf { it.localName == "filter" }
        if (opacity >= 1f && clip == null && mask == null && filter == null && clipRect == null) {
            draw(dst)
            return
        }
        var layer = FloatArray(w * h * 4)
        draw(layer)
        val bbox = if (clip != null || mask != null || filter != null) bboxOf() else null
        if (filter != null) layer = applyFilter(filter, layer, ctx, bbox) ?: return
        val clipCov = clip?.let { clipCoverage(it, ctx, bbox, 0) }
        val maskCov = mask?.let { maskCoverage(it, ctx, bbox) }
        val rectCov = clipRect?.let { polyCoverage(Mat.map(ctx.m, Mat.rectPoly(it))) }
        for (i in 0 until w * h) {
            val o = i * 4
            if (layer[o + 3] <= 0f) continue
            var f = opacity
            if (clipCov != null) f *= clipCov[i]
            if (maskCov != null) f *= maskCov[i]
            if (rectCov != null) f *= rectCov[i]
            if (f <= 0f) continue
            val k = 1f - layer[o + 3] * f
            for (c in 0..3) dst[o + c] = layer[o + c] * f + dst[o + c] * k
        }
    }

    private fun polyCoverage(poly: FloatArray): FloatArray {
        val out = FloatArray(w * h)
        Raster.fill(listOf(poly), false, w, h, out)
        return out
    }

    private fun len(v: String?, ctx: Ctx, axis: Int, def: Float): Float {
        val (n, unit) = v?.let(SvgDoc::length) ?: return def
        return when (unit) {
            "", "px" -> n
            "pt" -> n * 1.25f
            "pc" -> n * 15f
            "mm" -> n * 90f / 25.4f
            "cm" -> n * 90f / 2.54f
            "in" -> n * 90f
            "em" -> n * 16f
            "ex" -> n * 8f
            "%" -> n / 100f * when (axis) {
                0 -> ctx.vw
                1 -> ctx.vh
                else -> sqrt((ctx.vw * ctx.vw + ctx.vh * ctx.vh) / 2f)
            }
            else -> def
        }
    }

    private fun frac(v: String?, def: Float): Float {
        val s = v?.trim() ?: return def
        return if (s.endsWith('%')) s.dropLast(1).toFloatOrNull()?.div(100f) ?: def else s.toFloatOrNull() ?: def
    }

    private fun shapePath(el: Element, ctx: Ctx): SvgPath? {
        val p = doc.props(el)
        val path = SvgPath()
        when (el.localName) {
            "path" -> return SvgPath.parse(p["d"] ?: return null)
            "rect" -> {
                val x = len(p["x"], ctx, 0, 0f); val y = len(p["y"], ctx, 1, 0f)
                val rw = len(p["width"], ctx, 0, 0f); val rh = len(p["height"], ctx, 1, 0f)
                if (rw <= 0f || rh <= 0f) return null
                var rx = p["rx"]?.let { len(it, ctx, 0, -1f) }?.takeIf { it >= 0f }
                var ry = p["ry"]?.let { len(it, ctx, 1, -1f) }?.takeIf { it >= 0f }
                if (rx == null) rx = ry ?: 0f
                if (ry == null) ry = rx
                path.roundRect(x, y, rw, rh, min(rx, rw / 2f), min(ry, rh / 2f))
            }
            "circle" -> {
                val r = len(p["r"], ctx, 2, 0f)
                if (r <= 0f) return null
                path.ellipse(len(p["cx"], ctx, 0, 0f), len(p["cy"], ctx, 1, 0f), r, r)
            }
            "ellipse" -> {
                var rx = p["rx"]?.let { len(it, ctx, 0, -1f) }?.takeIf { it >= 0f }
                var ry = p["ry"]?.let { len(it, ctx, 1, -1f) }?.takeIf { it >= 0f }
                if (rx == null) rx = ry
                if (ry == null) ry = rx
                if (rx == null || ry == null || rx <= 0f || ry <= 0f) return null
                path.ellipse(len(p["cx"], ctx, 0, 0f), len(p["cy"], ctx, 1, 0f), rx, ry)
            }
            "line" -> {
                path.moveTo(len(p["x1"], ctx, 0, 0f), len(p["y1"], ctx, 1, 0f))
                path.lineTo(len(p["x2"], ctx, 0, 0f), len(p["y2"], ctx, 1, 0f))
            }
            "polyline", "polygon" -> {
                val n = SvgPath.numbers(p["points"] ?: return null)
                if (n.size < 4) return null
                path.moveTo(n[0], n[1])
                for (i in 2 until n.size - 1 step 2) path.lineTo(n[i], n[i + 1])
                if (el.localName == "polygon") path.close()
            }
            else -> return null
        }
        return path
    }

    private fun drawShape(el: Element, path: SvgPath, ctx: Ctx, dst: FloatArray) {
        val st = ctx.style
        if (!st.visible) return
        val bbox by lazy(LazyThreadSafetyMode.NONE) { path.bounds() }
        if (el.localName != "line") {
            paint(st.fill, st, ctx, { bbox }, st.fillOpacity)?.let { paint ->
                Raster.fill(path.transform(ctx.m).flatten(TOL).map { it.pts }, st.fillEvenOdd, w, h, cov)?.let { composite(dst, cov, it, paint) }
            }
        }
        val sw = len(st.strokeWidth, ctx, 2, 1f)
        if (sw <= 0f) return
        val paint = paint(st.stroke, st, ctx, { bbox }, st.strokeOpacity) ?: return
        val scale = Mat.scaleOf(ctx.m).coerceAtLeast(1e-6f)
        var lines = path.flatten(TOL / scale)
        dashes(st, ctx)?.let { lines = SvgStroker.dash(lines, it, len(st.dashOffset, ctx, 2, 0f)) }
        val pieces = SvgStroker.stroke(lines, sw / 2f, st.cap, st.join, st.miterLimit, TOL / scale)
        Raster.fill(pieces.map { Mat.map(ctx.m, it) }, false, w, h, cov)?.let { composite(dst, cov, it, paint) }
    }

    private fun dashes(st: SvgStyle, ctx: Ctx): FloatArray? {
        if (st.dashArray == "none") return null
        val parts = st.dashArray.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val d = FloatArray(parts.size) { len(parts[it], ctx, 2, -1f) }
        if (d.any { it < 0f } || d.sum() <= 0f) return null
        return if (d.size % 2 == 0) d else d + d
    }

    private fun composite(dst: FloatArray, coverage: FloatArray, b: IntArray, paint: SvgPaint) {
        for (y in b[1]..b[3]) for (x in b[0]..b[2]) {
            val i = y * w + x
            val c = coverage[i]
            if (c <= 0f) continue
            paint.eval(x + 0.5f, y + 0.5f, tmp)
            val a = tmp[3] * c
            if (a <= 0f) continue
            val o = i * 4
            val k = 1f - a
            dst[o] = tmp[0] * c + dst[o] * k
            dst[o + 1] = tmp[1] * c + dst[o + 1] * k
            dst[o + 2] = tmp[2] * c + dst[o + 2] * k
            dst[o + 3] = a + dst[o + 3] * k
        }
    }

    private fun paint(spec: String, st: SvgStyle, ctx: Ctx, bbox: () -> FloatArray?, opacity: Float): SvgPaint? {
        if (opacity <= 0f) return null
        val ref = SvgDoc.urlRef(spec)
        if (ref != null) {
            val target = doc.ids[ref]
            return when (target?.localName) {
                "linearGradient", "radialGradient" -> gradient(target, ctx, bbox, opacity)
                "pattern" -> pattern(target, ctx, bbox, opacity)
                else -> {
                    val fallback = spec.substringAfter(')').trim()
                    if (fallback.isEmpty() || fallback == "none") null else SvgColor.parse(fallback, st.color)?.let { SolidPaint(it, opacity) }
                }
            }
        }
        if (spec == "none") return null
        return SvgColor.parse(spec, st.color)?.let { SolidPaint(it, opacity) }
    }

    private fun chainAttr(el: Element, name: String, kinds: Set<String>): String? {
        var e: Element? = el
        var guard = 0
        while (e != null && guard++ < 16) {
            doc.props(e)[name]?.let { return it }
            e = doc.href(e)?.takeIf { it.localName in kinds }
        }
        return null
    }

    private fun chainChildren(el: Element, kinds: Set<String>, accept: (Element) -> Boolean): List<Element> {
        var e: Element? = el
        var guard = 0
        while (e != null && guard++ < 16) {
            val c = SvgDoc.children(e).filter(accept)
            if (c.isNotEmpty()) return c
            e = doc.href(e)?.takeIf { it.localName in kinds }
        }
        return emptyList()
    }

    private fun bboxMatrix(b: FloatArray) = floatArrayOf(b[2] - b[0], 0f, 0f, b[3] - b[1], b[0], b[1])

    private fun gradient(g: Element, ctx: Ctx, bbox: () -> FloatArray?, opacity: Float): SvgPaint? {
        val stops = chainChildren(g, GRADIENTS) { it.localName == "stop" }
        if (stops.isEmpty()) return null
        val offsets = FloatArray(stops.size)
        val colors = FloatArray(stops.size * 4)
        var last = 0f
        for ((i, stop) in stops.withIndex()) {
            val sp = doc.props(stop)
            last = max(last, frac(sp["offset"], 0f).coerceIn(0f, 1f))
            offsets[i] = last
            val color = SvgColor.parse(sp["stop-color"] ?: "black", doc.styleOf(stop).color) ?: SvgColor.BLACK
            colors[i * 4] = (color shr 16 and 0xFF) / 255f
            colors[i * 4 + 1] = (color shr 8 and 0xFF) / 255f
            colors[i * 4 + 2] = (color and 0xFF) / 255f
            colors[i * 4 + 3] = (color ushr 24) / 255f * (sp["stop-opacity"]?.let(SvgDoc::opacity) ?: 1f)
        }
        fun attr(name: String) = doc.props(g)[name]
        val obb = attr("gradientUnits") != "userSpaceOnUse"
        var gm = Mat.parse(attr("gradientTransform"))
        if (obb) {
            val b = bbox() ?: return null
            if (b[2] - b[0] <= 0f || b[3] - b[1] <= 0f) return null
            gm = Mat.mul(bboxMatrix(b), gm)
        }
        val inv = Mat.invert(Mat.mul(ctx.m, gm)) ?: return null
        fun coord(name: String, def: Float, axis: Int, fallback: String? = null): Float {
            val v = attr(name) ?: fallback?.let(::attr)
            if (obb) return frac(v, def)
            return len(v, ctx, axis, def * when (axis) { 0 -> ctx.vw; 1 -> ctx.vh; else -> sqrt((ctx.vw * ctx.vw + ctx.vh * ctx.vh) / 2f) })
        }
        val spread = when (attr("spreadMethod")) { "reflect" -> 1; "repeat" -> 2; else -> 0 }
        if (g.localName == "linearGradient") {
            val x1 = coord("x1", 0f, 0); val y1 = coord("y1", 0f, 1); val x2 = coord("x2", 1f, 0); val y2 = coord("y2", 0f, 1)
            val dx = x2 - x1; val dy = y2 - y1
            val l2 = dx * dx + dy * dy
            if (l2 <= 0f) return SolidPaint(argbOf(colors, stops.size - 1), opacity)
            return GradientPaint(inv, offsets, colors, spread, opacity) { x, y -> ((x - x1) * dx + (y - y1) * dy) / l2 }
        }
        val cx = coord("cx", 0.5f, 0); val cy = coord("cy", 0.5f, 1); val r = coord("r", 0.5f, 2)
        val fx = coord("fx", 0.5f, 0, "cx"); val fy = coord("fy", 0.5f, 1, "cy")
        if (r <= 0f) return SolidPaint(argbOf(colors, stops.size - 1), opacity)
        val cdx = cx - fx; val cdy = cy - fy
        if (cdx * cdx + cdy * cdy < 1e-10f) return GradientPaint(inv, offsets, colors, spread, opacity) { x, y -> sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / r }
        val a = cdx * cdx + cdy * cdy - r * r
        return GradientPaint(inv, offsets, colors, spread, opacity) { x, y ->
            val px = x - fx; val py = y - fy
            val b = px * cdx + py * cdy
            val c = px * px + py * py
            if (abs(a) < 1e-9f) {
                if (b == 0f) Float.NaN else (c / (2 * b)).takeIf { it >= 0f } ?: Float.NaN
            } else {
                val disc = b * b - a * c
                if (disc < 0f) Float.NaN else {
                    val s = sqrt(disc)
                    val t1 = (b + s) / a; val t2 = (b - s) / a
                    val t = max(t1, t2)
                    if (t >= 0f) t else if (min(t1, t2) >= 0f) min(t1, t2) else Float.NaN
                }
            }
        }
    }

    private fun argbOf(colors: FloatArray, i: Int): Int {
        fun b(f: Float) = (f.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (b(colors[i * 4 + 3]) shl 24) or (b(colors[i * 4]) shl 16) or (b(colors[i * 4 + 1]) shl 8) or b(colors[i * 4 + 2])
    }

    private fun pattern(pat: Element, ctx: Ctx, bbox: () -> FloatArray?, opacity: Float): SvgPaint? {
        fun attr(name: String) = chainAttr(pat, name, PATTERNS)
        val content = chainChildren(pat, PATTERNS) { true }
        if (content.isEmpty()) return null
        val obb = attr("patternUnits") != "userSpaceOnUse"
        val b = if (obb || attr("patternContentUnits") == "objectBoundingBox") bbox() ?: return null else null
        val x: Float; val y: Float; val tw: Float; val th: Float
        if (obb) {
            val bw = b!![2] - b[0]; val bh = b[3] - b[1]
            x = b[0] + frac(attr("x"), 0f) * bw; y = b[1] + frac(attr("y"), 0f) * bh
            tw = frac(attr("width"), 0f) * bw; th = frac(attr("height"), 0f) * bh
        } else {
            x = len(attr("x"), ctx, 0, 0f); y = len(attr("y"), ctx, 1, 0f)
            tw = len(attr("width"), ctx, 0, 0f); th = len(attr("height"), ctx, 1, 0f)
        }
        if (tw <= 0f || th <= 0f) return null
        val space = Mat.mul(Mat.mul(ctx.m, Mat.parse(attr("patternTransform"))), Mat.translate(x, y))
        val s = Mat.scaleOf(space)
        val pw = ceil(tw * s).toInt().coerceIn(1, 1024)
        val ph = ceil(th * s).toInt().coerceIn(1, 1024)
        val vb = viewBox(attr("viewBox"))
        val contentM = when {
            vb != null -> viewBoxMatrix(vb, tw, th, attr("preserveAspectRatio"))
            attr("patternContentUnits") == "objectBoundingBox" -> Mat.scale(b!![2] - b[0], b[3] - b[1])
            else -> Mat.identity()
        }
        val tileM = Mat.mul(Mat.scale(pw / tw, ph / th), contentM)
        val sub = SvgCanvas(doc, pw, ph)
        val tile = FloatArray(pw * ph * 4)
        val tileCtx = Ctx(tileM, doc.styleOf(pat), vb?.get(2) ?: ctx.vw, vb?.get(3) ?: ctx.vh)
        for (c in content) sub.render(c, tileCtx, tile)
        val inv = Mat.invert(Mat.mul(space, Mat.scale(tw / pw, th / ph))) ?: return null
        return TilePaint(inv, tile, pw, ph, opacity)
    }

    private fun imageRect(el: Element, ctx: Ctx): FloatArray? {
        val p = doc.props(el)
        val x = len(p["x"], ctx, 0, 0f); val y = len(p["y"], ctx, 1, 0f)
        val iw = len(p["width"], ctx, 0, 0f); val ih = len(p["height"], ctx, 1, 0f)
        return if (iw > 0f && ih > 0f) floatArrayOf(x, y, x + iw, y + ih) else null
    }

    private fun drawImage(el: Element, ctx: Ctx, dst: FloatArray) {
        if (!ctx.style.visible) return
        val p = doc.props(el)
        val href = el.getAttributeNS(SvgDoc.XLINK_NS, "href").ifEmpty { el.getAttribute("href") }.trim()
        if (!href.startsWith("data:")) return
        val comma = href.indexOf(',')
        if (comma < 0 || !href.substring(0, comma).endsWith(";base64")) return
        val bytes = runCatching { Base64.getMimeDecoder().decode(href.substring(comma + 1)) }.getOrNull() ?: return
        val bmp = PolyBitmap.decode(bytes) ?: return
        val x = len(p["x"], ctx, 0, 0f); val y = len(p["y"], ctx, 1, 0f)
        val iw = len(p["width"], ctx, 0, bmp.width.toFloat()); val ih = len(p["height"], ctx, 1, bmp.height.toFloat())
        if (iw <= 0f || ih <= 0f) return
        val im = Mat.mul(Mat.mul(ctx.m, Mat.translate(x, y)), viewBoxMatrix(floatArrayOf(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), iw, ih, p["preserveAspectRatio"]))
        val inv = Mat.invert(im) ?: return
        val visible = Mat.map(im, Mat.rectPoly(floatArrayOf(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())))
        val clip = Mat.map(ctx.m, Mat.rectPoly(floatArrayOf(x, y, x + iw, y + ih)))
        val b = Raster.fill(listOf(visible), false, w, h, cov) ?: return
        val clipCov = polyCoverage(clip)
        val src = FloatArray(bmp.width * bmp.height * 4)
        for (i in 0 until bmp.width * bmp.height) {
            val a = (bmp.pixels[i * 4 + 3].toInt() and 0xFF) / 255f
            for (c in 0..2) src[i * 4 + c] = (bmp.pixels[i * 4 + c].toInt() and 0xFF) / 255f * a
            src[i * 4 + 3] = a
        }
        for (yy in b[1]..b[3]) for (xx in b[0]..b[2]) {
            val i = yy * w + xx
            val c = cov[i] * clipCov[i]
            if (c <= 0f) continue
            val px = xx + 0.5f; val py = yy + 0.5f
            bilinear(src, bmp.width, bmp.height, inv[0] * px + inv[2] * py + inv[4] - 0.5f, inv[1] * px + inv[3] * py + inv[5] - 0.5f, false, tmp)
            val a = tmp[3] * c
            if (a <= 0f) continue
            val o = i * 4
            for (k in 0..3) dst[o + k] = tmp[k] * c + dst[o + k] * (1f - a)
        }
    }

    private fun bbox(el: Element, ctx: Ctx): FloatArray? = when (el.localName) {
        "g", "a", "switch", "svg", "symbol" -> groupBBox(el, ctx)
        "image" -> imageRect(el, ctx)
        "use" -> {
            val p = doc.props(el)
            doc.href(el)?.takeIf { active.add(it) }?.let { t ->
                try {
                    bbox(t, ctx)?.let { Mat.mapRect(Mat.mul(Mat.translate(len(p["x"], ctx, 0, 0f), len(p["y"], ctx, 1, 0f)), Mat.parse(doc.props(t)["transform"])), it) }
                } finally {
                    active.remove(t)
                }
            }
        }
        else -> shapePath(el, ctx)?.bounds()
    }

    private fun groupBBox(el: Element, ctx: Ctx): FloatArray? {
        var out: FloatArray? = null
        for (c in SvgDoc.children(el)) {
            if (!doc.isSvg(c) || c.localName !in RENDERED || doc.props(c)["display"] == "none") continue
            val b = bbox(c, ctx) ?: continue
            val r = Mat.mapRect(Mat.parse(doc.props(c)["transform"]), b)
            out = out?.let { floatArrayOf(min(it[0], r[0]), min(it[1], r[1]), max(it[2], r[2]), max(it[3], r[3])) } ?: r
        }
        return out
    }

    private fun clipCoverage(clipEl: Element, ctx: Ctx, bbox: FloatArray?, depth: Int): FloatArray {
        val out = FloatArray(w * h)
        if (depth > 8) return out
        val p = doc.props(clipEl)
        var m = Mat.mul(ctx.m, Mat.parse(p["transform"]))
        if (p["clipPathUnits"] == "objectBoundingBox") m = Mat.mul(m, bboxMatrix(bbox ?: return out))
        val c = FloatArray(w * h)
        val clipStyle = doc.styleOf(clipEl)
        for (child in SvgDoc.children(clipEl)) {
            if (!doc.isSvg(child) || doc.props(child)["display"] == "none") continue
            var target = child
            var cm = Mat.mul(m, Mat.parse(doc.props(child)["transform"]))
            var st = doc.derive(clipStyle, child)
            if (child.localName == "use") {
                val t = doc.href(child) ?: continue
                val up = doc.props(child)
                cm = Mat.mul(Mat.mul(cm, Mat.translate(len(up["x"], ctx, 0, 0f), len(up["y"], ctx, 1, 0f))), Mat.parse(doc.props(t)["transform"]))
                st = doc.derive(st, t)
                target = t
            }
            if (!st.visible || doc.props(target)["display"] == "none") continue
            val path = shapePath(target, Ctx(cm, st, ctx.vw, ctx.vh)) ?: continue
            val b = Raster.fill(path.transform(cm).flatten(TOL).map { it.pts }, st.clipEvenOdd, w, h, c) ?: continue
            val own = SvgDoc.urlRef(doc.props(child)["clip-path"])?.let { doc.ids[it] }?.takeIf { it.localName == "clipPath" }
                ?.let { clipCoverage(it, Ctx(Mat.mul(m, Mat.parse(doc.props(child)["transform"])), st, ctx.vw, ctx.vh), path.bounds(), depth + 1) }
            for (y in b[1]..b[3]) for (x in b[0]..b[2]) {
                val i = y * w + x
                var v = c[i]
                if (own != null) v *= own[i]
                out[i] = out[i] + v - out[i] * v
            }
        }
        SvgDoc.urlRef(p["clip-path"])?.let { doc.ids[it] }?.takeIf { it.localName == "clipPath" && it !== clipEl }?.let {
            val outer = clipCoverage(it, ctx, bbox, depth + 1)
            for (i in out.indices) out[i] *= outer[i]
        }
        return out
    }

    private fun maskCoverage(maskEl: Element, ctx: Ctx, bbox: FloatArray?): FloatArray {
        val out = FloatArray(w * h)
        if (!active.add(maskEl)) return out
        try {
            val p = doc.props(maskEl)
            val region = if (p["maskUnits"] != "userSpaceOnUse") {
                val b = bbox ?: return out
                val bw = b[2] - b[0]; val bh = b[3] - b[1]
                val x = b[0] + frac(p["x"], -0.1f) * bw; val y = b[1] + frac(p["y"], -0.1f) * bh
                floatArrayOf(x, y, x + frac(p["width"], 1.2f) * bw, y + frac(p["height"], 1.2f) * bh)
            } else {
                val x = len(p["x"], ctx, 0, -0.1f * ctx.vw); val y = len(p["y"], ctx, 1, -0.1f * ctx.vh)
                floatArrayOf(x, y, x + len(p["width"], ctx, 0, 1.2f * ctx.vw), y + len(p["height"], ctx, 1, 1.2f * ctx.vh))
            }
            val contentM = if (p["maskContentUnits"] == "objectBoundingBox") Mat.mul(ctx.m, bboxMatrix(bbox ?: return out)) else ctx.m
            val layer = FloatArray(w * h * 4)
            renderChildren(maskEl, Ctx(contentM, doc.styleOf(maskEl), ctx.vw, ctx.vh), layer)
            val regionCov = polyCoverage(Mat.map(ctx.m, Mat.rectPoly(region)))
            val alpha = p["mask-type"] == "alpha"
            for (i in out.indices) {
                val o = i * 4
                val v = if (alpha) layer[o + 3] else 0.2126f * layer[o] + 0.7152f * layer[o + 1] + 0.0722f * layer[o + 2]
                out[i] = v.coerceIn(0f, 1f) * regionCov[i]
            }
            return out
        } finally {
            active.remove(maskEl)
        }
    }

    private class FBuf(val px: FloatArray, val linear: Boolean)

    private fun applyFilter(filter: Element, src: FloatArray, ctx: Ctx, bbox: FloatArray?): FloatArray? {
        val p = doc.props(filter)
        val obb = p["filterUnits"] != "userSpaceOnUse"
        val region = if (obb) {
            val b = bbox ?: return null
            val bw = b[2] - b[0]; val bh = b[3] - b[1]
            val x = b[0] + frac(p["x"], -0.1f) * bw; val y = b[1] + frac(p["y"], -0.1f) * bh
            floatArrayOf(x, y, x + frac(p["width"], 1.2f) * bw, y + frac(p["height"], 1.2f) * bh)
        } else {
            val x = len(p["x"], ctx, 0, -0.1f * ctx.vw); val y = len(p["y"], ctx, 1, -0.1f * ctx.vh)
            floatArrayOf(x, y, x + len(p["width"], ctx, 0, 1.2f * ctx.vw), y + len(p["height"], ctx, 1, 1.2f * ctx.vh))
        }
        if (region[2] <= region[0] || region[3] <= region[1]) return null
        val dev = Mat.mapRect(ctx.m, region)
        val r = intArrayOf(max(0, floor(dev[0]).toInt()), max(0, floor(dev[1]).toInt()), min(w, ceil(dev[2]).toInt()), min(h, ceil(dev[3]).toInt()))
        if (r[0] >= r[2] || r[1] >= r[3]) return null
        val primObb = p["primitiveUnits"] == "objectBoundingBox"
        val bw = bbox?.let { it[2] - it[0] } ?: 1f
        val bh = bbox?.let { it[3] - it[1] } ?: 1f
        val sx = sqrt(ctx.m[0] * ctx.m[0] + ctx.m[1] * ctx.m[1])
        val sy = sqrt(ctx.m[2] * ctx.m[2] + ctx.m[3] * ctx.m[3])
        val source = FBuf(cropped(src, r), false)
        val results = HashMap<String, FBuf>()
        var last = source
        val filterStyle = doc.styleOf(filter)
        for (prim in SvgDoc.children(filter)) {
            if (!doc.isSvg(prim)) continue
            val pp = doc.props(prim)
            val st = doc.derive(filterStyle, prim)
            val linear = st.linearFilters
            fun input(name: String?): FBuf = when (name) {
                null, "" -> last
                "SourceGraphic" -> source
                "SourceAlpha" -> FBuf(FloatArray(src.size).also { a -> for (i in 0 until w * h) a[i * 4 + 3] = source.px[i * 4 + 3] }, false)
                else -> results[name] ?: last
            }
            fun inp(name: String?) = convert(input(name), linear, r).px
            fun num(v: String?, def: Float) = v?.toFloatOrNull() ?: def
            val out: FloatArray = when (prim.localName) {
                "feGaussianBlur" -> {
                    val d = SvgPath.numbers(pp["stdDeviation"] ?: "0")
                    val dx = d.getOrElse(0) { 0f } * (if (primObb) bw else 1f) * sx
                    val dy = d.getOrElse(1) { d.getOrElse(0) { 0f } } * (if (primObb) bh else 1f) * sy
                    if (dx < 0f || dy < 0f) FloatArray(src.size) else blur(inp(pp["in"]), dx, dy, r)
                }
                "feOffset" -> {
                    val ox = num(pp["dx"], 0f) * (if (primObb) bw else 1f)
                    val oy = num(pp["dy"], 0f) * (if (primObb) bh else 1f)
                    offset(inp(pp["in"]), ctx.m[0] * ox + ctx.m[2] * oy, ctx.m[1] * ox + ctx.m[3] * oy, r)
                }
                "feFlood" -> flood(pp, st, linear, r)
                "feColorMatrix" -> colorMatrix(inp(pp["in"]), pp, r)
                "feComposite" -> composite2(inp(pp["in"]), inp(pp["in2"]), pp, r)
                "feBlend" -> blend(inp(pp["in"]), inp(pp["in2"]), pp["mode"] ?: "normal", r)
                "feMerge" -> {
                    val acc = FloatArray(src.size)
                    for (node in SvgDoc.children(prim)) if (node.localName == "feMergeNode") over(inp(doc.props(node)["in"]), acc, r)
                    acc
                }
                "feDropShadow" -> {
                    val input = inp(pp["in"])
                    val d = SvgPath.numbers(pp["stdDeviation"] ?: "2")
                    val alpha = FloatArray(src.size).also { a -> for (i in 0 until w * h) a[i * 4 + 3] = input[i * 4 + 3] }
                    val ox = num(pp["dx"], 2f) * (if (primObb) bw else 1f)
                    val oy = num(pp["dy"], 2f) * (if (primObb) bh else 1f)
                    val blurred = blur(alpha, d.getOrElse(0) { 0f } * sx, d.getOrElse(1) { d.getOrElse(0) { 0f } } * sy, r)
                    val shadow = offset(blurred, ctx.m[0] * ox + ctx.m[2] * oy, ctx.m[1] * ox + ctx.m[3] * oy, r)
                    val color = flood(pp, st, linear, r)
                    for (i in 0 until w * h) { val a = shadow[i * 4 + 3]; for (c in 0..3) shadow[i * 4 + c] = color[i * 4 + c] * a }
                    over(input, shadow, r)
                    shadow
                }
                else -> inp(pp["in"])
            }
            val res = FBuf(cropped(out, r), linear)
            pp["result"]?.takeIf { it.isNotEmpty() }?.let { results[it] = res }
            last = res
        }
        return convert(last, false, r).px
    }

    private fun cropped(src: FloatArray, r: IntArray): FloatArray {
        val out = FloatArray(src.size)
        for (y in r[1] until r[3]) System.arraycopy(src, (y * w + r[0]) * 4, out, (y * w + r[0]) * 4, (r[2] - r[0]) * 4)
        return out
    }

    private fun convert(b: FBuf, linear: Boolean, r: IntArray): FBuf {
        if (b.linear == linear) return b
        val out = b.px.copyOf()
        for (y in r[1] until r[3]) for (x in r[0] until r[2]) {
            val o = (y * w + x) * 4
            val a = out[o + 3]
            if (a <= 0f) continue
            for (c in 0..2) {
                val v = (out[o + c] / a).coerceIn(0f, 1f)
                out[o + c] = (if (linear) toLinear(v) else toSrgb(v)) * a
            }
        }
        return FBuf(out, linear)
    }

    private fun toLinear(v: Float) = if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    private fun toSrgb(v: Float) = if (v <= 0.0031308f) v * 12.92f else (1.055 * v.toDouble().pow(1 / 2.4) - 0.055).toFloat()

    private fun flood(pp: Map<String, String>, st: SvgStyle, linear: Boolean, r: IntArray): FloatArray {
        val argb = SvgColor.parse(pp["flood-color"] ?: "black", st.color) ?: SvgColor.BLACK
        val a = (argb ushr 24) / 255f * (pp["flood-opacity"]?.let(SvgDoc::opacity) ?: 1f)
        val rgb = floatArrayOf((argb shr 16 and 0xFF) / 255f, (argb shr 8 and 0xFF) / 255f, (argb and 0xFF) / 255f)
        if (linear) for (c in 0..2) rgb[c] = toLinear(rgb[c])
        val out = FloatArray(w * h * 4)
        for (y in r[1] until r[3]) for (x in r[0] until r[2]) {
            val o = (y * w + x) * 4
            out[o] = rgb[0] * a; out[o + 1] = rgb[1] * a; out[o + 2] = rgb[2] * a; out[o + 3] = a
        }
        return out
    }

    private fun blur(src: FloatArray, sx: Float, sy: Float, r: IntArray): FloatArray {
        var cur = src
        if (sx > 0f) cur = blur1(cur, sx, true, r)
        if (sy > 0f) cur = blur1(cur, sy, false, r)
        return cur
    }

    private fun blur1(src: FloatArray, sigma: Float, horizontal: Boolean, r: IntArray): FloatArray {
        val rad = ceil(sigma * 3f).toInt().coerceIn(1, 512)
        val k = FloatArray(rad * 2 + 1) { exp(-((it - rad) * (it - rad)) / (2f * sigma * sigma)) }
        val sum = k.sum()
        for (i in k.indices) k[i] /= sum
        val out = FloatArray(src.size)
        for (y in r[1] until r[3]) for (x in r[0] until r[2]) {
            var a0 = 0f; var a1 = 0f; var a2 = 0f; var a3 = 0f
            for (t in -rad..rad) {
                val xx = if (horizontal) x + t else x
                val yy = if (horizontal) y else y + t
                if (xx < r[0] || xx >= r[2] || yy < r[1] || yy >= r[3]) continue
                val o = (yy * w + xx) * 4
                val f = k[t + rad]
                a0 += src[o] * f; a1 += src[o + 1] * f; a2 += src[o + 2] * f; a3 += src[o + 3] * f
            }
            val o = (y * w + x) * 4
            out[o] = a0; out[o + 1] = a1; out[o + 2] = a2; out[o + 3] = a3
        }
        return out
    }

    private fun offset(src: FloatArray, dx: Float, dy: Float, r: IntArray): FloatArray {
        val out = FloatArray(src.size)
        for (y in r[1] until r[3]) for (x in r[0] until r[2]) {
            val xx = floor(x + 0.5f - dx).toInt(); val yy = floor(y + 0.5f - dy).toInt()
            if (xx < r[0] || xx >= r[2] || yy < r[1] || yy >= r[3]) continue
            System.arraycopy(src, (yy * w + xx) * 4, out, (y * w + x) * 4, 4)
        }
        return out
    }

    private fun colorMatrix(src: FloatArray, pp: Map<String, String>, r: IntArray): FloatArray {
        val type = pp["type"] ?: "matrix"
        val values = pp["values"]?.let(SvgPath::numbers)
        val m: FloatArray = when (type) {
            "saturate" -> {
                val s = values?.getOrNull(0) ?: 1f
                floatArrayOf(
                    0.213f + 0.787f * s, 0.715f - 0.715f * s, 0.072f - 0.072f * s, 0f, 0f,
                    0.213f - 0.213f * s, 0.715f + 0.285f * s, 0.072f - 0.072f * s, 0f, 0f,
                    0.213f - 0.213f * s, 0.715f - 0.715f * s, 0.072f + 0.928f * s, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            }
            "hueRotate" -> {
                val a = Math.toRadians((values?.getOrNull(0) ?: 0f).toDouble())
                val c = cos(a).toFloat(); val s = sin(a).toFloat()
                floatArrayOf(
                    0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
                    0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
                    0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            }
            "luminanceToAlpha" -> floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.2125f, 0.7154f, 0.0721f, 0f, 0f)
            else -> if (values != null && values.size >= 20) values else return src
        }
        val out = FloatArray(src.size)
        for (y in r[1] until r[3]) for (x in r[0] until r[2]) {
            val o = (y * w + x) * 4
            val a = src[o + 3]
            val cr = if (a > 0f) src[o] / a else 0f
            val cg = if (a > 0f) src[o + 1] / a else 0f
            val cb = if (a > 0f) src[o + 2] / a else 0f
            val na = (m[15] * cr + m[16] * cg + m[17] * cb + m[18] * a + m[19]).coerceIn(0f, 1f)
            for (c in 0..2) out[o + c] = (m[c * 5] * cr + m[c * 5 + 1] * cg + m[c * 5 + 2] * cb + m[c * 5 + 3] * a + m[c * 5 + 4]).coerceIn(0f, 1f) * na
            out[o + 3] = na
        }
        return out
    }

    private fun over(src: FloatArray, dst: FloatArray, r: IntArray) {
        for (y in r[1] until r[3]) for (x in r[0] until r[2]) {
            val o = (y * w + x) * 4
            val k = 1f - src[o + 3]
            for (c in 0..3) dst[o + c] = src[o + c] + dst[o + c] * k
        }
    }

    private fun composite2(a: FloatArray, b: FloatArray, pp: Map<String, String>, r: IntArray): FloatArray {
        val op = pp["operator"] ?: "over"
        val k1 = pp["k1"]?.toFloatOrNull() ?: 0f; val k2 = pp["k2"]?.toFloatOrNull() ?: 0f
        val k3 = pp["k3"]?.toFloatOrNull() ?: 0f; val k4 = pp["k4"]?.toFloatOrNull() ?: 0f
        val out = FloatArray(a.size)
        for (y in r[1] until r[3]) for (x in r[0] until r[2]) {
            val o = (y * w + x) * 4
            val aa = a[o + 3]; val ba = b[o + 3]
            if (op == "arithmetic") {
                val na = (k1 * aa * ba + k2 * aa + k3 * ba + k4).coerceIn(0f, 1f)
                for (c in 0..2) out[o + c] = (k1 * a[o + c] * b[o + c] + k2 * a[o + c] + k3 * b[o + c] + k4).coerceIn(0f, na)
                out[o + 3] = na
                continue
            }
            val (fa, fb) = when (op) {
                "in" -> ba to 0f
                "out" -> (1f - ba) to 0f
                "atop" -> ba to (1f - aa)
                "xor" -> (1f - ba) to (1f - aa)
                "lighter" -> 1f to 1f
                else -> 1f to (1f - aa)
            }
            for (c in 0..3) out[o + c] = min(1f, a[o + c] * fa + b[o + c] * fb)
        }
        return out
    }

    private fun blend(a: FloatArray, b: FloatArray, mode: String, r: IntArray): FloatArray {
        val out = FloatArray(a.size)
        for (y in r[1] until r[3]) for (x in r[0] until r[2]) {
            val o = (y * w + x) * 4
            val sa = a[o + 3]; val da = b[o + 3]
            for (c in 0..2) {
                val s = a[o + c]; val d = b[o + c]
                val cs = if (sa > 0f) s / sa else 0f
                val cd = if (da > 0f) d / da else 0f
                val mixed = when (mode) {
                    "multiply" -> cs * cd
                    "screen" -> cs + cd - cs * cd
                    "darken" -> min(cs, cd)
                    "lighten" -> max(cs, cd)
                    "overlay" -> if (cd <= 0.5f) 2 * cs * cd else 1 - 2 * (1 - cs) * (1 - cd)
                    "hard-light" -> if (cs <= 0.5f) 2 * cs * cd else 1 - 2 * (1 - cs) * (1 - cd)
                    "difference" -> abs(cs - cd)
                    "exclusion" -> cs + cd - 2 * cs * cd
                    "color-dodge" -> if (cd == 0f) 0f else if (cs >= 1f) 1f else min(1f, cd / (1 - cs))
                    "color-burn" -> if (cd >= 1f) 1f else if (cs <= 0f) 0f else 1 - min(1f, (1 - cd) / cs)
                    "soft-light" -> if (cs <= 0.5f) cd - (1 - 2 * cs) * cd * (1 - cd) else {
                        val dd = if (cd <= 0.25f) ((16 * cd - 12) * cd + 4) * cd else sqrt(cd)
                        cd + (2 * cs - 1) * (dd - cd)
                    }
                    else -> cs
                }
                out[o + c] = (1 - da) * s + (1 - sa) * d + sa * da * mixed
            }
            out[o + 3] = sa + da - sa * da
        }
        return out
    }

    companion object {
        const val TOL = 0.03f

        private val RENDERED = setOf("svg", "g", "a", "switch", "use", "path", "rect", "circle", "ellipse", "line", "polyline", "polygon", "image")
        private val GRADIENTS = setOf("linearGradient", "radialGradient")
        private val PATTERNS = setOf("pattern")

        fun viewBox(v: String?): FloatArray? {
            val n = SvgPath.numbers(v ?: return null)
            return if (n.size == 4 && n[2] > 0f && n[3] > 0f) n else null
        }

        fun viewBoxMatrix(vb: FloatArray, width: Float, height: Float, par: String?): FloatArray {
            val sx = width / vb[2]; val sy = height / vb[3]
            val parts = par?.trim()?.split(Regex("\\s+")).orEmpty().filter { it != "defer" }
            val align = parts.getOrNull(0) ?: "xMidYMid"
            if (align == "none") return floatArrayOf(sx, 0f, 0f, sy, -vb[0] * sx, -vb[1] * sy)
            val s = if (parts.getOrNull(1) == "slice") max(sx, sy) else min(sx, sy)
            val tx = when {
                align.startsWith("xMin") -> 0f
                align.startsWith("xMax") -> width - vb[2] * s
                else -> (width - vb[2] * s) / 2f
            }
            val ty = when {
                align.endsWith("YMin") -> 0f
                align.endsWith("YMax") -> height - vb[3] * s
                else -> (height - vb[3] * s) / 2f
            }
            return floatArrayOf(s, 0f, 0f, s, tx - vb[0] * s, ty - vb[1] * s)
        }

        fun bilinear(src: FloatArray, sw: Int, sh: Int, u: Float, v: Float, wrap: Boolean, out: FloatArray) {
            val x0 = floor(u).toInt(); val y0 = floor(v).toInt()
            val fx = u - x0; val fy = v - y0
            out.fill(0f)
            for (j in 0..1) for (i in 0..1) {
                var x = x0 + i; var y = y0 + j
                if (wrap) {
                    x = Math.floorMod(x, sw); y = Math.floorMod(y, sh)
                } else {
                    x = x.coerceIn(0, sw - 1); y = y.coerceIn(0, sh - 1)
                }
                val f = (if (i == 0) 1 - fx else fx) * (if (j == 0) 1 - fy else fy)
                val o = (y * sw + x) * 4
                for (c in 0..3) out[c] += src[o + c] * f
            }
        }

        fun rasterize(svg: String, size: Int, tint: Int?): PolyBitmap? {
            val doc = SvgDoc.parse(svg) ?: return null
            val root = doc.root
            val p = doc.props(root)
            val vb = viewBox(p["viewBox"])
            fun absLen(v: String?) = v?.let(SvgDoc::length)?.takeIf { it.second != "%" && it.first > 0f }?.let { (n, u) ->
                when (u) { "pt" -> n * 1.25f; "pc" -> n * 15f; "mm" -> n * 90f / 25.4f; "cm" -> n * 90f / 2.54f; "in" -> n * 90f; "em" -> n * 16f; "ex" -> n * 8f; else -> n }
            }
            val aw = absLen(p["width"]); val ah = absLen(p["height"])
            val vw = vb?.get(2) ?: aw ?: ah ?: size.toFloat()
            val vh = vb?.get(3) ?: ah ?: aw ?: size.toFloat()
            if (vw <= 0f || vh <= 0f) return null
            // A single declared side takes the viewBox's aspect ratio, so only both sides change the viewport's shape
            val pw = if (aw != null && ah != null) aw else vw
            val ph = if (aw != null && ah != null) ah else vh
            val scale = size / max(pw, ph)
            val cw = max(1, (pw * scale).roundToInt())
            val ch = max(1, (ph * scale).roundToInt())
            val toPixels = Mat.scale(cw / pw, ch / ph)
            val m = vb?.let { Mat.mul(toPixels, viewBoxMatrix(it, pw, ph, p["preserveAspectRatio"])) } ?: toPixels
            val acc = SvgCanvas(doc, cw, ch).render(root, m, vw, vh)
            val px = ByteArray(cw * ch * 4)
            val ta = tint?.let { (it ushr 24) / 255f }
            for (i in 0 until cw * ch) {
                var a = acc[i * 4 + 3]
                if (ta != null) a *= ta
                if (a <= 0f) continue
                for (c in 0 until 3) {
                    val v = if (tint != null) (tint shr (16 - c * 8) and 0xFF) / 255f else acc[i * 4 + c] / acc[i * 4 + 3]
                    px[i * 4 + c] = (v * 255f).roundToInt().coerceIn(0, 255).toByte()
                }
                px[i * 4 + 3] = (a * 255f).roundToInt().coerceIn(0, 255).toByte()
            }
            return PolyBitmap(cw, ch, px)
        }
    }
}
