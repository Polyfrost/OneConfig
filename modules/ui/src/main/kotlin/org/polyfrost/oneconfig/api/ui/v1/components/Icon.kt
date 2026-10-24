package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
@Composable
fun Icon(iconName: String, color: Color, modifier: Modifier) {
    componentsBridge().Icon(iconName, color, modifier, false)
}

@ExperimentalOneConfigUiApi
@Composable
fun Icon(iconName: String, color: Color, modifier: Modifier, fitAspectRatio: Boolean) {
    componentsBridge().Icon(iconName, color, modifier, fitAspectRatio)
}
