package org.polyfrost.compose.foundation

import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.applyShape
import org.polyfrost.compose.ui.element
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.LinearGradient
import org.polyfrost.compose.ui.graphics.RectangleShape
import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.unit.Dp

fun Modifier.background(color: PolyColor, shape: Shape = RectangleShape) = element { background = color; applyShape(shape) }

fun Modifier.background(brush: Brush, shape: Shape = RectangleShape) = element { gradient = brush; applyShape(shape) }

fun Modifier.border(width: Dp, color: PolyColor, shape: Shape = RectangleShape) =
    element { borderColor = color; borderWidth = width.value; applyShape(shape) }

fun Modifier.border(width: Dp, brush: Brush, shape: Shape = RectangleShape) = element {
    val colors = brush.colors
    borderColor = colors.firstOrNull() ?: PolyColor.TRANSPARENT
    if (brush is LinearGradient && brush.angle == 90f && colors.size == 2) borderColorBottom = colors[1]
    borderWidth = width.value
    applyShape(shape)
}
