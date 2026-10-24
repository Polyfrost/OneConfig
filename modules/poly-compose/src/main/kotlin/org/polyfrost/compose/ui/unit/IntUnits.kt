package org.polyfrost.compose.ui.unit

data class IntSize(val width: Int, val height: Int) {
    companion object {
        val Zero = IntSize(0, 0)
    }
}

data class IntOffset(val x: Int, val y: Int) {
    operator fun plus(other: IntOffset) = IntOffset(x + other.x, y + other.y)
    operator fun minus(other: IntOffset) = IntOffset(x - other.x, y - other.y)

    companion object {
        val Zero = IntOffset(0, 0)
    }
}
