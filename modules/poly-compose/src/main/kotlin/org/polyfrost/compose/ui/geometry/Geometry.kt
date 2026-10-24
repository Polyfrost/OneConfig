package org.polyfrost.compose.ui.geometry

data class Offset(val x: Float, val y: Float) {
    operator fun plus(other: Offset) = Offset(x + other.x, y + other.y)
    operator fun minus(other: Offset) = Offset(x - other.x, y - other.y)
    operator fun times(f: Float) = Offset(x * f, y * f)
    operator fun div(f: Float) = Offset(x / f, y / f)
    operator fun unaryMinus() = Offset(-x, -y)

    companion object {
        val Zero = Offset(0f, 0f)
        val Unspecified = Offset(Float.NaN, Float.NaN)
    }
}

data class Size(val width: Float, val height: Float) {
    val minDimension get() = minOf(width, height)
    val maxDimension get() = maxOf(width, height)
    val center get() = Offset(width / 2f, height / 2f)

    companion object {
        val Zero = Size(0f, 0f)
        val Unspecified = Size(Float.NaN, Float.NaN)
    }
}

data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    constructor(offset: Offset, size: Size) : this(offset.x, offset.y, offset.x + size.width, offset.y + size.height)

    val width get() = right - left
    val height get() = bottom - top
    val size get() = Size(width, height)
    val topLeft get() = Offset(left, top)
    val center get() = Offset((left + right) / 2f, (top + bottom) / 2f)
    operator fun contains(offset: Offset) = offset.x >= left && offset.x < right && offset.y >= top && offset.y < bottom

    companion object {
        val Zero = Rect(0f, 0f, 0f, 0f)
    }
}

data class CornerRadius(val x: Float, val y: Float = x) {
    companion object {
        val Zero = CornerRadius(0f)
    }
}
