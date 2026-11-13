package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(iconName: String, modifier: Modifier, onClick: () -> Unit) {
    IconButton(iconName, iconName, onClick, modifier, tooltip = null)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(iconName: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    IconButton(iconName, iconName, onClick, modifier, enabled = enabled, tooltip = null)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(
    iconName: String,
    modifier: Modifier,
    foreground: Color,
    hoveredForeground: Color,
    enabled: Boolean,
    hoverBorder: Boolean,
    onClick: () -> Unit,
) {
    IconButton(iconName, iconName, onClick, modifier, enabled = enabled, tooltip = null, tint = foreground)
}
