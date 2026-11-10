package org.polyfrost.compose.layout

public data class PolyInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val horizontal: Float get() = left + right
    val vertical: Float get() = top + bottom
}

public fun PolyInsets(all: Float): PolyInsets = PolyInsets(all, all, all, all)
public fun PolyInsets(horizontal: Float, vertical: Float): PolyInsets = PolyInsets(horizontal, vertical, horizontal, vertical)
