package org.polyfrost.compose.mc

import org.jetbrains.skia.Canvas

object McFontQueue {
    var measureWidth: ((text: String, scale: Float) -> Float)? = null
    var measureHeight: ((scale: Float) -> Float)? = null

    var renderer: ((canvas: Canvas, text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) -> Unit)? = null

    fun measureTextWidth(text: String, scale: Float): Float =
        measureWidth?.invoke(text, scale) ?: (text.length * 6f * scale)

    fun measureTextHeight(scale: Float): Float =
        measureHeight?.invoke(scale) ?: (9f * scale)
}
