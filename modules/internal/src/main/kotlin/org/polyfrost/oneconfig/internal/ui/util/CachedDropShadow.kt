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
import org.polyfrost.compose.render.PolyShadow

@Stable
fun Modifier.cachedDropShadow(shape: Shape, shadow: Shadow, hole: Boolean = false): Modifier {
    require(shadow.brush == null) { "cachedDropShadow does not support gradient brushes" }
    return drawBehind {
        if (size.width <= 0f || size.height <= 0f) return@drawBehind
        val outline = shape.createOutline(size, layoutDirection, this)
        val radius = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
        val spread = shadow.spread.toPx()
        val ox = shadow.offset.x.toPx()
        val oy = shadow.offset.y.toPx()
        val w = size.width
        val h = size.height
        val poly = PolyShadow(PolyColor(shadow.color.toArgb()).multiplyAlpha(shadow.alpha), shadow.radius.toPx(), 0f, 0f, 0f)
        drawIntoCanvas { c ->
            (c as? PolyDrawCanvas)?.polyOp {
                if (hole) it.renderer.shadowWithHole(ox - spread, oy - spread, w + spread * 2f, h + spread * 2f, radius + spread, poly, 0f, 0f, w, h, radius)
                else it.renderer.shadow(ox - spread, oy - spread, w + spread * 2f, h + spread * 2f, radius + spread, poly)
            }
        }
    }
}
