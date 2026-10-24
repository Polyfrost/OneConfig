package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

enum class IconButtonType {
    Ghost,
    Filled;
}

@Composable
fun IconButton(
    iconName: String,
    modifier: Modifier = Modifier,
    foreground: Color = LocalTheme.current.textColor.copy(0.7f),
    hoveredForeground: Color = LocalTheme.current.textColor,
    enabled: Boolean = true,
    hoverBorder: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()

    val isHovered by interactionSource.collectIsHoveredAsState()

    val iconColor by animateColorAsState(
        if (!enabled) {
            LocalTheme.current.textColorSecondary.copy(alpha = 0.35f)
        } else {
            if (isHovered) hoveredForeground else foreground
        }
    )
    val borderColor by animateColorAsState(
        if (hoverBorder && enabled && isHovered) LocalTheme.current.borderColor
        else LocalTheme.current.borderColor.copy(0f)
    )

    Icon(
        iconName,
        color = iconColor,
        modifier = Modifier.onClick(interactionSource, enabled, onClick).then(modifier)
            .then(
                if (hoverBorder) Modifier.border(1.dp, borderColor, LocalTheme.current.buttonShape).padding(4.dp)
                else Modifier
            )
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
    val interactionSource = rememberInteractionSource()

    val isHovered by interactionSource.collectIsHoveredAsState()

    val iconColor by animateColorAsState(
        if (isHovered || selected) hoveredForeground else foreground
    )

    Icon(
        iconName,
        color = iconColor, modifier = Modifier.onClick(interactionSource, onClick).then(modifier)
    )
}