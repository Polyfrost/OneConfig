package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import org.polyfrost.oneconfig.internal.ui.components.animatedFrame as internalAnimatedFrame
import org.polyfrost.oneconfig.internal.ui.components.glassHover as internalGlassHover
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass as internalLiquidGlass

class OneConfigUIGlassBridgeImpl : OneConfigUIGlassBridge {
    @Composable
    override fun glassHover(modifier: Modifier, shape: Shape, pull: Dp): Modifier = modifier.internalGlassHover(shape, pull)

    @Composable
    override fun liquidGlass(modifier: Modifier, shape: Shape): Modifier = modifier.internalLiquidGlass(shape)

    override fun animatedFrame(modifier: Modifier, shape: Shape, fill: () -> Color, accentAlpha: () -> Float, border: () -> Color): Modifier =
        modifier.internalAnimatedFrame(shape, fill, accentAlpha, border = border)
}
