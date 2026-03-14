package org.polyfrost.compose.layout

data class PolyInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val horizontal get() = left + right
    val vertical get() = top + bottom
}

fun PolyInsets(all: Float) = PolyInsets(all, all, all, all)
fun PolyInsets(horizontal: Float, vertical: Float) = PolyInsets(horizontal, vertical, horizontal, vertical)
