package org.polyfrost.compose.ui.graphics.shadow

import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp

data class Shadow(
    val radius: Dp,
    val color: PolyColor = PolyColor(0xFF000000.toInt()),
    val spread: Dp = 0.dp,
    val offset: DpOffset = DpOffset.Zero,
    val alpha: Float = 1f,
) {
    val drawColor get() = if (alpha >= 1f) color else color.multiplyAlpha(alpha)
}
