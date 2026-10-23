package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
@Composable
fun IconButton(iconName: String, modifier: Modifier, onClick: () -> Unit) {
    IconButton(iconName, modifier, true, onClick)
}

@ExperimentalOneConfigUiApi
@Composable
fun IconButton(iconName: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val textColor = OneConfigTheme.current.textColor
    componentsBridge().IconButton(iconName, modifier, textColor.copy(alpha = 0.7f), textColor, enabled, false, onClick)
}

@ExperimentalOneConfigUiApi
@Composable
fun IconButton(
    iconName: String,
    modifier: Modifier,
    foreground: Color,
    hoveredForeground: Color,
    enabled: Boolean,
    hoverBorder: Boolean,
    onClick: () -> Unit,
) {
    componentsBridge().IconButton(iconName, modifier, foreground, hoveredForeground, enabled, hoverBorder, onClick)
}
