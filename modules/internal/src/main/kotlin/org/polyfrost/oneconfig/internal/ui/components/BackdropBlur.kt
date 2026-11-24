package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import org.polyfrost.compose.backend.drawPoly

/**
 * Draws the blurred screen behind this element inside [shape]
 *
 * Only shows on screens that make the blurred copy each frame, drawing nothing elsewhere, and counts towards
 * [BackdropUsers] while it's composed
 */
@Composable
fun Modifier.backdropBlur(shape: Shape): Modifier {
    BackdropUsers.Register()
    return backdropBlurDraw(shape)
}

private fun Modifier.backdropBlurDraw(shape: Shape): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    val radius = (shape.createOutline(size, layoutDirection, this) as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
    drawPoly { ctx ->
        val r = ctx.renderer
        r.save()
        r.clipRRect(0f, 0f, w, h, radius)
        r.backdrop(0f, 0f, w, h, 1f)
        r.restore()
    }
}

/** How many surfaces drawing [backdropBlur] are on screen, so a screen only makes the blurred copy while one is */
object BackdropUsers {
    @Volatile
    var count: Int = 0
        private set

    @Composable
    internal fun Register() {
        DisposableEffect(Unit) {
            count++
            onDispose { count-- }
        }
    }
}
