package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.IconButtonStyle
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton as KitIconButton

enum class IconButtonType {
    Ghost,
    Filled;
}

internal fun iconButtonLabel(iconName: String): String = when (iconName) {
    "left-arrow" -> translated("oneconfig.button.back", "Back")
    "right-arrow" -> translated("oneconfig.button.forward", "Forward")
    "close" -> translated("oneconfig.button.close", "Close")
    "folder" -> translated("oneconfig.button.browse", "Browse")
    "trash" -> translated("oneconfig.list.remove", "Remove")
    else -> iconName.replace('-', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
fun IconButton(
    iconName: String,
    modifier: Modifier = Modifier,
    foreground: Color = LocalTheme.current.textColor.copy(0.7f),
    hoveredForeground: Color = LocalTheme.current.textColor,
    enabled: Boolean = true,
    /** Outlines the button on hover; off by default so small inline icons (field clear buttons) stay bare */
    hoverBorder: Boolean = false,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val tint = Motion.animateColorAsState(if (hovered) hoveredForeground else foreground)
    val label = iconButtonLabel(iconName)
    KitIconButton(
        iconName,
        label,
        onClick,
        modifier,
        enabled = enabled,
        style = IconButtonStyle.Ghost,
        size = if (hoverBorder) ControlSize.Regular else ControlSize.Compact,
        tooltip = if (hoverBorder) label else null,
        tint = tint.value,
        interactionSource = source,
    )
}

@Composable
fun SelectableIconButton(
    iconName: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    foreground: Color = LocalTheme.current.textColorSecondary,
    hoveredForeground: Color = LocalTheme.current.textColor,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val tint = Motion.animateColorAsState(if (hovered || selected) hoveredForeground else foreground)
    val label = iconButtonLabel(iconName)
    KitIconButton(
        iconName,
        label,
        onClick,
        modifier,
        style = IconButtonStyle.Ghost,
        size = ControlSize.Compact,
        selected = selected,
        tooltip = null,
        tint = if (selected) Color.Unspecified else tint.value,
        interactionSource = source,
    )
}
