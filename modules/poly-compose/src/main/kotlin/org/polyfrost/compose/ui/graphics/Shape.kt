package org.polyfrost.compose.ui.graphics

import org.polyfrost.compose.render.PolyColor

interface Shape {
    val radius: Float
    val percent: Float get() = -1f
}

object RectangleShape : Shape {
    override val radius get() = 0f
}

class ColorFilter private constructor(val color: PolyColor) {
    companion object {
        fun tint(color: PolyColor) = ColorFilter(color)
    }
}
