package org.polyfrost.compose.render


interface PolyRenderer {
    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float)

    fun rectWithHole(x: Float, y: Float, w: Float, h: Float, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float, color: Int)
    fun rectGradient(x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, radius: Float)

    fun shadow(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: PolyShadow)
    fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: Int, strokeWidth: Float, radius: Float)

    fun rectStrokeGradient(x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, strokeWidth: Float, radius: Float)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float)
    fun fillPath(path: PolyPath, color: Int)

    fun strokePolyline(points: FloatArray, closed: Boolean, color: Int, strokeWidth: Float, cap: Int) {
        val n = points.size / 2
        for (i in 0 until (if (closed) n else n - 1)) {
            val j = (i + 1) % n
            line(points[i * 2], points[i * 2 + 1], points[j * 2], points[j * 2 + 1], color, strokeWidth)
        }
    }

    fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont)

    fun textOutline(text: String, x: Float, y: Float, color: Int, font: PolyFont, width: Float)

    fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float)

    fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float)

    fun image(bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float, filterQuality: FilterQuality = FilterQuality.Low) =
        image(bitmap, x, y, w, h, 0f, 0f, 1f, 1f, -1, filterQuality)

    fun image(bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: Int, filterQuality: FilterQuality = FilterQuality.Low)

    fun live(draw: (PolyRenderer) -> Unit)

    fun save()
    fun restore()

    fun saveLayerAlpha(alpha: Float)

    fun translate(dx: Float, dy: Float)
    fun scale(sx: Float, sy: Float)
    fun clipRect(x: Float, y: Float, w: Float, h: Float)
    fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float)
}

interface CapturingRenderer {
    fun mark(): Any
    fun since(mark: Any): Any
    fun valid(captured: Any): Boolean
    fun append(captured: Any)
    fun stateKey(): Any
    fun saveState(): Any
    fun restoreState(saved: Any)
}
