package org.polyfrost.compose.foundation.shape

import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.unit.Dp

class RoundedCornerShape private constructor(override val radius: Float, override val percent: Float) : Shape {
    constructor(size: Dp) : this(size.value, -1f)
    constructor(percent: Int) : this(0f, percent.toFloat())

    override fun equals(other: Any?) = other is RoundedCornerShape && other.radius == radius && other.percent == percent
    override fun hashCode() = 31 * radius.hashCode() + percent.hashCode()
}

val CircleShape = RoundedCornerShape(50)
