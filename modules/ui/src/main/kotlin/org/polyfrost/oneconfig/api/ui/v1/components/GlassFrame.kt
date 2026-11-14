@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.glassBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

internal val glassAvailable: Boolean get() = glassBridge() != null

@Composable
internal fun Modifier.glassFrame(
    shape: Shape,
    fill: () -> Color,
    border: () -> Color,
    accentAlpha: () -> Float = { 0f },
    hover: Boolean = true,
    glass: Boolean = true,
    pull: Dp = 1.dp,
): Modifier {
    val bridge = glassBridge()
    if (bridge == null) {
        val accent = OneConfigTheme.current.accentBrush
        return drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val stroke = Stroke(1.dp.toPx())
            onDrawWithContent {
                val f = fill()
                if (f.alpha > 0f) drawOutline(outline, f)
                val a = accentAlpha()
                if (a > 0f) drawOutline(outline, accent, a)
                drawContent()
                val b = border()
                if (b.alpha > 0f) drawOutline(outline, b, style = stroke)
            }
        }
    }
    val pulled = if (hover) bridge.glassHover(this, shape, pull) else this
    val hovered = if (pulled === this) this else pulled.graphicsLayer {}
    val glassed = if (glass) bridge.liquidGlass(hovered, shape) else hovered
    return bridge.animatedFrame(glassed, shape, fill, accentAlpha, border)
}
