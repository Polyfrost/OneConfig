package org.polyfrost.compose.render

import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.FilterQuality
import org.polyfrost.compose.ui.graphics.shadow.Shadow

interface PolyRenderer {
    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float)

    fun rectWithHole(x: Float, y: Float, w: Float, h: Float, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float, color: Int)
    fun rectGradient(x: Float, y: Float, w: Float, h: Float, gradient: Brush, radius: Float)

    fun shadow(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: Shadow)
    fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: Int, strokeWidth: Float, radius: Float)

    fun rectStrokeGradient(x: Float, y: Float, w: Float, h: Float, top: Int, bottom: Int, strokeWidth: Float, radius: Float)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float)
    fun fillPath(path: PolyPath, color: Int)

    fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont)

    fun textOutline(text: String, x: Float, y: Float, color: Int, font: PolyFont, width: Float)

    fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float)

    fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float)

    fun image(bitmap: ImageBitmap, x: Float, y: Float, w: Float, h: Float, filterQuality: FilterQuality = FilterQuality.Low) =
        image(bitmap, x, y, w, h, 0f, 0f, 1f, 1f, -1, filterQuality)

    fun image(bitmap: ImageBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: Int, filterQuality: FilterQuality = FilterQuality.Low)

    fun live(draw: (PolyRenderer) -> Unit)

    fun save()
    fun restore()

    fun saveLayerAlpha(alpha: Float)

    fun saveLayerFade(fade: PolyFade) = save()
    fun translate(dx: Float, dy: Float)
    fun scale(sx: Float, sy: Float)
    fun clipRect(x: Float, y: Float, w: Float, h: Float)
    fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float)
}

class PolyFade(val vertical: Boolean, val stops: FloatArray, val alphas: FloatArray) {
    init { require(stops.size == alphas.size && stops.isNotEmpty()) }

    fun alphaAt(pos: Float): Float {
        if (pos <= stops[0]) return alphas[0]
        for (i in 1 until stops.size) if (pos < stops[i]) {
            val t = (pos - stops[i - 1]) / (stops[i] - stops[i - 1])
            return alphas[i - 1] + (alphas[i] - alphas[i - 1]) * t
        }
        return alphas.last()
    }

    fun span(pos: Float): Int {
        for (i in stops.indices) if (pos < stops[i]) return i
        return stops.size
    }

    fun opaque(i: Int) = when (i) {
        0 -> alphas[0] >= 1f
        stops.size -> alphas.last() >= 1f
        else -> alphas[i - 1] >= 1f && alphas[i] >= 1f
    }
}
