package org.polyfrost.compose.render

import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.FilterQuality
import org.polyfrost.compose.ui.graphics.StrokeCap
import org.polyfrost.compose.ui.graphics.shadow.Shadow

class RenderContext(val renderer: PolyRenderer) {
    private var _absX = 0f
    private var _absY = 0f
    private val absXStack = ArrayDeque<Float>()
    private val absYStack = ArrayDeque<Float>()

    val absoluteX get() = _absX
    val absoluteY get() = _absY

    fun rect(x: Float, y: Float, w: Float, h: Float, color: PolyColor, radius: Float = 0f) {
        if (color.alpha == 0) return
        renderer.rect(x, y, w, h, color.argb, radius)
    }

    fun rectWithHole(x: Float, y: Float, w: Float, h: Float, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float, color: PolyColor) {
        if (color.alpha == 0) return
        renderer.rectWithHole(x, y, w, h, holeX, holeY, holeW, holeH, holeRadius, color.argb)
    }

    fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: PolyColor, strokeWidth: Float, radius: Float = 0f) {
        if (color.alpha == 0) return
        renderer.rectStroke(x, y, w, h, color.argb, strokeWidth, radius)
    }

    fun rectStrokeGradient(x: Float, y: Float, w: Float, h: Float, top: PolyColor, bottom: PolyColor, strokeWidth: Float, radius: Float = 0f) {
        if (top.alpha == 0 && bottom.alpha == 0) return
        renderer.rectStrokeGradient(x, y, w, h, top.argb, bottom.argb, strokeWidth, radius)
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: PolyColor, strokeWidth: Float = 1f) {
        if (color.alpha == 0) return
        renderer.line(x1, y1, x2, y2, color.argb, strokeWidth)
    }

    fun fillPath(path: PolyPath, color: PolyColor) {
        if (color.alpha == 0) return
        renderer.fillPath(path, color.argb)
    }

    fun strokePolyline(points: FloatArray, closed: Boolean, color: PolyColor, strokeWidth: Float, cap: StrokeCap = StrokeCap.Butt) {
        if (color.alpha == 0) return
        renderer.strokePolyline(points, closed, color.argb, strokeWidth, cap.ordinal)
    }

    fun text(text: String, x: Float, y: Float, color: PolyColor, font: PolyFont) {
        if (color.alpha == 0 || text.isEmpty()) return
        renderer.text(text, x, y, color.argb, font)
    }

    fun textOutline(text: String, x: Float, y: Float, color: PolyColor, font: PolyFont, width: Float) {
        if (color.alpha == 0 || text.isEmpty() || width <= 0f) return
        renderer.textOutline(text, x, y, color.argb, font, width)
    }

    fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) {
        if (text.isEmpty()) return
        renderer.mcText(text, x, y, color, shadow, scale)
    }

    fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float) {
        if (text.isEmpty()) return
        renderer.mcTextOutline(text, x, y, color, outlineColor, scale)
    }

    fun measureText(text: String, font: PolyFont): Float = font.measureTextWidth(text)

    fun live(draw: RenderContext.() -> Unit) = renderer.live { RenderContext(it).draw() }

    fun save() {
        renderer.save()
        absXStack.addLast(_absX)
        absYStack.addLast(_absY)
    }

    fun saveLayerAlpha(alpha: Float) {
        renderer.saveLayerAlpha(alpha)
        absXStack.addLast(_absX)
        absYStack.addLast(_absY)
    }

    fun restore() {
        renderer.restore()
        _absX = absXStack.removeLast()
        _absY = absYStack.removeLast()
    }

    fun translate(dx: Float, dy: Float) {
        renderer.translate(dx, dy)
        _absX += dx
        _absY += dy
    }

    fun image(bitmap: ImageBitmap, x: Float, y: Float, w: Float, h: Float, filterQuality: FilterQuality = FilterQuality.Low) =
        renderer.image(bitmap, x, y, w, h, filterQuality)

    fun image(
        bitmap: ImageBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: PolyColor,
        filterQuality: FilterQuality = FilterQuality.Low,
    ) {
        if (tint.alpha == 0) return
        renderer.image(bitmap, x, y, w, h, u0, v0, u1, v1, tint.argb, filterQuality)
    }

    fun rectGradient(x: Float, y: Float, w: Float, h: Float, gradient: Brush, radius: Float = 0f) =
        renderer.rectGradient(x, y, w, h, gradient, radius)

    fun shadow(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: Shadow) {
        if (shadow.drawColor.alpha == 0) return
        renderer.shadow(x, y, w, h, radius, shadow)
    }

    fun scale(sx: Float, sy: Float) = renderer.scale(sx, sy)

    fun clipRect(x: Float, y: Float, w: Float, h: Float) = renderer.clipRect(x, y, w, h)
    fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float) = renderer.clipRRect(x, y, w, h, radius)
}
