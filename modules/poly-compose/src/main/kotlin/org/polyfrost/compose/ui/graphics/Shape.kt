package org.polyfrost.compose.ui.graphics

import androidx.compose.runtime.State
import org.polyfrost.compose.animation.AnimatedColor
import org.polyfrost.compose.render.PolyColor

interface Shape {
    val radius: Float
    val percent: Float get() = -1f
}

object RectangleShape : Shape {
    override val radius get() = 0f
}

class ColorFilter private constructor(val color: PolyColor, val animated: State<PolyColor>?) {
    companion object {
        fun tint(color: PolyColor) = ColorFilter(color, null)
        fun tint(color: AnimatedColor) = ColorFilter(color.target, color)
    }
}
