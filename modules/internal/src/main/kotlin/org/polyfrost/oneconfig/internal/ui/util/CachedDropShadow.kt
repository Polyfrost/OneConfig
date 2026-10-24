package org.polyfrost.oneconfig.internal.ui.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import org.polyfrost.compose.backend.PolyDrawCanvas
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.graphics.shadow.Shadow as PolyShadow

@Stable
fun Modifier.cachedDropShadow(shape: Shape, shadow: Shadow): Modifier {
    require(shadow.brush == null) { "cachedDropShadow does not support gradient brushes" }
    return drawBehind {
        if (size.width <= 0f || size.height <= 0f) return@drawBehind
        val outline = shape.createOutline(size, layoutDirection, this)
        val radius = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
        val spread = shadow.spread.toPx()
        val ox = shadow.offset.x.toPx()
        val oy = shadow.offset.y.toPx()
        val poly = PolyShadow(Dp(shadow.radius.toPx()), PolyColor(shadow.color.toArgb()), alpha = shadow.alpha)
        drawIntoCanvas { c ->
            (c as? PolyDrawCanvas)?.polyOp {
                it.renderer.shadow(ox - spread, oy - spread, size.width + spread * 2f, size.height + spread * 2f, radius + spread, poly)
            }
        }
    }
}
