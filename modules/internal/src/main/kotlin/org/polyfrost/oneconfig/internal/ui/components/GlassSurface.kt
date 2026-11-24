package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import org.polyfrost.compose.backend.PolyCanvas
import org.polyfrost.compose.backend.drawPoly
import org.polyfrost.compose.effect.polyGlass
import org.polyfrost.compose.effect.polyGlassSplit
import org.polyfrost.oneconfig.internal.ui.themes.GlassTintColor
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.ReduceTransparency

private const val OPAQUE = 0xFF000000.toInt()

@Composable
fun Modifier.liquidGlass(shape: Shape, frosted: Boolean = false, alpha: () -> Float = { 1f }): Modifier {
    if (!LocalTheme.current.liquidGlass) return this
    return polyGlass(
        shape,
        frosted = frosted,
        tint = { GlassTintColor.fill },
        rimColor = { Color(GlassTintColor.rimRgb or OPAQUE) },
        lightColor = { Color(GlassTintColor.lightRgb or OPAQUE) },
        alpha = alpha,
        opaque = { ReduceTransparency.enabled },
    )
}

@Composable
fun Modifier.splitFrostedGlass(shape: Shape, splitX: Dp, leftAlpha: Float, rightAlpha: Float): Modifier {
    if (!LocalTheme.current.liquidGlass) return this
    return polyGlassSplit(shape, splitX, leftAlpha, rightAlpha, tint = { GlassTintColor.fill }, opaque = { ReduceTransparency.enabled })
}

private val liveScope = CanvasDrawScope()

internal fun DrawScope.drawLive(block: DrawScope.() -> Unit) {
    val density = Density(density, fontScale)
    val direction = layoutDirection
    val size = size
    drawPoly { it.renderer.live { r -> liveScope.draw(density, direction, PolyCanvas(r), size, block) } }
}
