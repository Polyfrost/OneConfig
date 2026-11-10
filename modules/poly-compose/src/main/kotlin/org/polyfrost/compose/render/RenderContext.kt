package org.polyfrost.compose.render


public class RenderContext(public val renderer: PolyRenderer) {
    @JvmField
    public var cacheNodes: Boolean = false

    private var _absX = 0f
    private var _absY = 0f
    private val absXStack = ArrayDeque<Float>()
    private val absYStack = ArrayDeque<Float>()

    public val absoluteX: Float get() = _absX
    public val absoluteY: Float get() = _absY

    public fun rect(x: Float, y: Float, w: Float, h: Float, color: PolyColor, radius: Float = 0f) {
        if (color.alpha == 0) return
        renderer.rect(x, y, w, h, color.argb, radius)
    }

    public fun rectWithHole(x: Float, y: Float, w: Float, h: Float, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float, color: PolyColor) {
        if (color.alpha == 0) return
        renderer.rectWithHole(x, y, w, h, holeX, holeY, holeW, holeH, holeRadius, color.argb)
    }

    public fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: PolyColor, strokeWidth: Float, radius: Float = 0f) {
        if (color.alpha == 0) return
        renderer.rectStroke(x, y, w, h, color.argb, strokeWidth, radius)
    }

    public fun rectStrokeGradient(x: Float, y: Float, w: Float, h: Float, top: PolyColor, bottom: PolyColor, strokeWidth: Float, radius: Float = 0f) {
        if (top.alpha == 0 && bottom.alpha == 0) return
        renderer.rectStrokeGradient(x, y, w, h, PolyGradient.vertical(top, bottom), strokeWidth, radius)
    }

    public fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: PolyColor, strokeWidth: Float = 1f) {
        if (color.alpha == 0) return
        renderer.line(x1, y1, x2, y2, color.argb, strokeWidth)
    }

    public fun fillPath(path: PolyPath, color: PolyColor) {
        if (color.alpha == 0) return
        renderer.fillPath(path, color.argb)
    }

    public fun text(text: String, x: Float, y: Float, color: PolyColor, font: PolyFont) {
        if (color.alpha == 0 || text.isEmpty()) return
        renderer.text(text, x, y, color.argb, font)
    }

    public fun textOutline(text: String, x: Float, y: Float, color: PolyColor, font: PolyFont, width: Float) {
        if (color.alpha == 0 || text.isEmpty() || width <= 0f) return
        renderer.textOutline(text, x, y, color.argb, font, width)
    }

    public fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) {
        if (text.isEmpty()) return
        renderer.mcText(text, x, y, color, shadow, scale)
    }

    public fun mcTextLive(text: String, x: Float, y: Float, color: () -> Int, shadow: Boolean, scale: Float) {
        if (text.isEmpty()) return
        renderer.mcTextLive(text, x, y, color, shadow, scale)
    }

    public fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float) {
        if (text.isEmpty()) return
        renderer.mcTextOutline(text, x, y, color, outlineColor, scale)
    }

    public fun measureText(text: String, font: PolyFont): Float = font.measureTextWidth(text)

    public fun live(draw: RenderContext.() -> Unit): Unit = renderer.live { RenderContext(it).draw() }

    public fun save() {
        renderer.save()
        absXStack.addLast(_absX)
        absYStack.addLast(_absY)
    }

    public fun saveLayerAlpha(alpha: Float) {
        renderer.saveLayerAlpha(alpha)
        absXStack.addLast(_absX)
        absYStack.addLast(_absY)
    }

    public fun restore() {
        renderer.restore()
        _absX = absXStack.removeLast()
        _absY = absYStack.removeLast()
    }

    public fun deviceTransform(): FloatArray? = renderer.deviceTransform()

    public fun translate(dx: Float, dy: Float) {
        renderer.translate(dx, dy)
        _absX += dx
        _absY += dy
    }

    public fun image(bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float, filterQuality: FilterQuality = FilterQuality.Low): Unit =
        renderer.image(bitmap, x, y, w, h, filterQuality)

    public fun image(
        bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: PolyColor,
        filterQuality: FilterQuality = FilterQuality.Low,
    ) {
        if (tint.alpha == 0) return
        renderer.image(bitmap, x, y, w, h, u0, v0, u1, v1, tint.argb, filterQuality)
    }

    public fun rectGradient(x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, radius: Float = 0f): Unit =
        renderer.rectGradient(x, y, w, h, gradient, radius)

    public fun shadow(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: PolyShadow) {
        if (shadow.color.alpha == 0) return
        renderer.shadow(x, y, w, h, radius, shadow)
    }

    public fun scale(sx: Float, sy: Float): Unit = renderer.scale(sx, sy)

    public fun clipRect(x: Float, y: Float, w: Float, h: Float): Unit = renderer.clipRect(x, y, w, h)
    public fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float): Unit = renderer.clipRRect(x, y, w, h, radius)
}
