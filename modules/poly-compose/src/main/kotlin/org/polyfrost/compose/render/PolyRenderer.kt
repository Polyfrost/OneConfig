package org.polyfrost.compose.render


interface PolyRenderer {
    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float)

    fun rectWithHole(x: Float, y: Float, w: Float, h: Float, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float, color: Int)
    fun rectGradient(x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, radius: Float)

    fun shadow(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: PolyShadow)

    fun shadowWithHole(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: PolyShadow, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float) =
        shadow(x, y, w, h, radius, shadow)
    fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: Int, strokeWidth: Float, radius: Float)

    fun rectStrokeGradient(x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, strokeWidth: Float, radius: Float)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float)
    fun fillPath(path: PolyPath, color: Int)

    /**
     * Fills [path] with [gradient] laid out over the box at [x], [y] sized [w] by [h], as [rectGradient] lays it out over its rectangle.
     * Renderers without gradient support fill it with the gradient's color at the box's center.
     */
    fun fillPathGradient(path: PolyPath, x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient) =
        fillPath(path, gradient.colorAt(x + w / 2f, y + h / 2f, x, y, w, h))

    fun strokePolyline(points: FloatArray, closed: Boolean, color: Int, strokeWidth: Float, cap: Int, join: Int = JOIN_MITER) {
        val n = points.size / 2
        for (i in 0 until (if (closed) n else n - 1)) {
            val j = (i + 1) % n
            line(points[i * 2], points[i * 2 + 1], points[j * 2], points[j * 2 + 1], color, strokeWidth)
        }
    }

    /**
     * Strokes like [strokePolyline] with [gradient] laid out over the box at [x], [y] sized [w] by [h], as in [fillPathGradient].
     * Renderers without gradient support stroke it with the gradient's color at the box's center.
     */
    fun strokePolylineGradient(
        points: FloatArray, closed: Boolean, x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, strokeWidth: Float, cap: Int, join: Int,
    ) = strokePolyline(points, closed, gradient.colorAt(x + w / 2f, y + h / 2f, x, y, w, h), strokeWidth, cap, join)

    fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont)

    fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont, letterSpacing: Float) {
        if (letterSpacing == 0f) return text(text, x, y, color, font)
        var cx = x
        var i = 0
        while (i < text.length) {
            val ch = text.substring(i, i + Character.charCount(text.codePointAt(i)))
            text(ch, cx, y, color, font)
            cx += font.measureTextWidth(ch) + letterSpacing
            i += ch.length
        }
    }

    /** Draws [text] sheared so a point `d` above the baseline shifts right by `d * skewX`. */
    fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont, letterSpacing: Float, skewX: Float) =
        text(text, x, y, color, font, letterSpacing)

    fun textOutline(text: String, x: Float, y: Float, color: Int, font: PolyFont, width: Float)

    fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float)

    fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float)

    fun mcTextLive(text: String, x: Float, y: Float, color: () -> Int, shadow: Boolean, scale: Float) =
        live { it.mcText(text, x, y, color(), shadow, scale) }

    fun image(bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float, filterQuality: FilterQuality = FilterQuality.Low) =
        image(bitmap, x, y, w, h, 0f, 0f, 1f, 1f, -1, filterQuality)

    fun image(bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: Int, filterQuality: FilterQuality = FilterQuality.Low)

    fun live(draw: (PolyRenderer) -> Unit)

    fun save()
    fun restore()

    fun saveLayerAlpha(alpha: Float)
    fun saveLayerFade(fade: PolyFade) = save()

    fun deviceTransform(): FloatArray? = null

    fun translate(dx: Float, dy: Float)
    fun scale(sx: Float, sy: Float)
    fun rotate(degrees: Float) {}
    fun animatedScale(sx: Float, sy: Float) = scale(sx, sy)
    fun clipRect(x: Float, y: Float, w: Float, h: Float)
    fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float)

    /** Removes the rectangle from the clip. Renderers that can't remove areas from it keep drawing over the rectangle. */
    fun clipOutRect(x: Float, y: Float, w: Float, h: Float) {}

    companion object {
        const val CAP_BUTT = 0
        const val CAP_ROUND = 1
        const val CAP_SQUARE = 2
        const val JOIN_MITER = 0
        const val JOIN_ROUND = 1
    }
}

class PolyFade(val vertical: Boolean, val stops: FloatArray, val alphas: FloatArray, val colors: IntArray? = null) {
    init { require(stops.size == alphas.size && stops.isNotEmpty() && (colors == null || colors.size == stops.size)) }

    fun alphaAt(pos: Float): Float {
        if (pos <= stops[0]) return alphas[0]
        for (i in 1 until stops.size) if (pos < stops[i]) {
            val t = (pos - stops[i - 1]) / (stops[i] - stops[i - 1])
            return alphas[i - 1] + (alphas[i] - alphas[i - 1]) * t
        }
        return alphas.last()
    }

    fun colorAt(pos: Float): Int {
        val c = colors ?: return 0xFFFFFF
        if (pos <= stops[0]) return c[0]
        for (i in 1 until stops.size) if (pos < stops[i]) return PolyGradient.lerpArgb(c[i - 1], c[i], (pos - stops[i - 1]) / (stops[i] - stops[i - 1]))
        return c.last()
    }

    fun span(pos: Float): Int {
        for (i in stops.indices) if (pos < stops[i]) return i
        return stops.size
    }

    fun opaque(i: Int) = colors == null && when (i) {
        0 -> alphas[0] >= 1f
        stops.size -> alphas.last() >= 1f
        else -> alphas[i - 1] >= 1f && alphas[i] >= 1f
    }
}

interface CapturingRenderer {
    fun mark(): Any
    fun since(mark: Any): Any
    fun valid(captured: Any): Boolean
    fun append(captured: Any)
    fun stateKey(): Any
    fun sameState(key: Any): Boolean
    fun saveState(): Any
    fun restoreState(saved: Any)
}
