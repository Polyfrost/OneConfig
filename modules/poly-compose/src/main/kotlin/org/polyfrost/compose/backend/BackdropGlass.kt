package org.polyfrost.compose.backend

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

fun DrawScope.drawBackdropGlass(size: Size, cornerRadiusPx: Float, bandPx: Float, refraction: Float, alpha: Float) {
    if (size.width <= 0f || size.height <= 0f || alpha <= 0f) return
    drawIntoCanvas { c ->
        (c as? PolyDrawCanvas)?.polyOp {
            it.renderer.backdropGlass(0f, 0f, size.width, size.height, cornerRadiusPx, bandPx, refraction, alpha)
        }
    }
}
