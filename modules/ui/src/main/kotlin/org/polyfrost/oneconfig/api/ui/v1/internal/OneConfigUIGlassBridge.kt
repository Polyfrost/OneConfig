package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi

@ExperimentalOneConfigUiApi
@InternalOneConfigUiApi
public interface OneConfigUIGlassBridge {
    @Composable
    public fun glassHover(modifier: Modifier, shape: Shape, pull: Dp): Modifier

    @Composable
    public fun liquidGlass(modifier: Modifier, shape: Shape): Modifier

    public fun animatedFrame(modifier: Modifier, shape: Shape, fill: () -> Color, accentAlpha: () -> Float, border: () -> Color): Modifier
}

@ExperimentalOneConfigUiApi
private val loadedGlassBridge: OneConfigUIGlassBridge? by lazy {
    loadBridge(OneConfigUIGlassBridge::class.java, "kit controls draw without the shell's glass")
}

@ExperimentalOneConfigUiApi
internal fun glassBridge(): OneConfigUIGlassBridge? = loadedGlassBridge
