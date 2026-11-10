package org.polyfrost.compose.render

public data class PolyShadow(
    val color: PolyColor = PolyColor(0x80000000.toInt()),
    val blur: Float = 8f,
    val offsetX: Float = 0f,
    val offsetY: Float = 4f,
    val spread: Float = 0f,
) {
    public companion object {
        public fun drop(color: PolyColor = PolyColor(0x80000000.toInt()), blur: Float = 8f, dy: Float = 4f): PolyShadow =
            PolyShadow(color, blur, 0f, dy, 0f)

        public fun ambient(color: PolyColor = PolyColor(0x40000000), blur: Float = 16f): PolyShadow =
            PolyShadow(color, blur, 0f, 0f, 0f)
    }
}
