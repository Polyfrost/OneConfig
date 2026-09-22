package org.polyfrost.compose.render

import org.jetbrains.skia.*

class RenderContext(val canvas: Canvas) {
    val paint = Paint()

    private var _absX = 0f
    private var _absY = 0f
    private val absXStack = ArrayDeque<Float>()
    private val absYStack = ArrayDeque<Float>()

    val absoluteX get() = _absX
    val absoluteY get() = _absY

    fun rect(x: Float, y: Float, w: Float, h: Float, color: PolyColor, radius: Float = 0f) {
        if (color.alpha == 0) return
        paint.color = color.argb
        paint.mode = PaintMode.FILL
        if (radius > 0f) canvas.drawRRect(RRect.makeXYWH(x, y, w, h, radius), paint)
        else canvas.drawRect(Rect.makeXYWH(x, y, w, h), paint)
    }

    fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: PolyColor, strokeWidth: Float, radius: Float = 0f) {
        if (color.alpha == 0) return
        paint.color = color.argb
        paint.mode = PaintMode.STROKE
        paint.strokeWidth = strokeWidth
        if (radius > 0f) canvas.drawRRect(RRect.makeXYWH(x, y, w, h, radius), paint)
        else canvas.drawRect(Rect.makeXYWH(x, y, w, h), paint)
        paint.mode = PaintMode.FILL
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: PolyColor, strokeWidth: Float = 1f) {
        if (color.alpha == 0) return
        paint.color = color.argb
        paint.mode = PaintMode.STROKE
        paint.strokeWidth = strokeWidth
        canvas.drawLine(x1, y1, x2, y2, paint)
        paint.mode = PaintMode.FILL
    }

    fun text(text: String, x: Float, y: Float, color: PolyColor, font: Font) {
        if (color.alpha == 0 || text.isEmpty()) return
        paint.color = color.argb
        canvas.drawString(text, x, y, font, paint)
    }

    fun measureText(text: String, font: Font): Float = font.measureTextWidth(text)

    fun save() {
        canvas.save()
        absXStack.addLast(_absX)
        absYStack.addLast(_absY)
    }

    fun restore() {
        canvas.restore()
        _absX = absXStack.removeLast()
        _absY = absYStack.removeLast()
    }

    fun translate(dx: Float, dy: Float) {
        canvas.translate(dx, dy)
        _absX += dx
        _absY += dy
    }

    fun image(image: Image, x: Float, y: Float, w: Float, h: Float, paint: Paint) {
        paint.isAntiAlias = true
        canvas.drawImageRect(
            image,
            Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
            Rect.makeXYWH(x, y, w, h),
            IMAGE_SAMPLING,
            paint,
            true,
        )
    }

    fun scale(sx: Float, sy: Float) = canvas.scale(sx, sy)

    fun clipRect(x: Float, y: Float, w: Float, h: Float) = canvas.clipRect(Rect.makeXYWH(x, y, w, h))
    fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float) = canvas.clipRRect(RRect.makeXYWH(x, y, w, h, radius), true)

    private companion object {
        private val IMAGE_SAMPLING: SamplingMode = FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE)
    }
}
