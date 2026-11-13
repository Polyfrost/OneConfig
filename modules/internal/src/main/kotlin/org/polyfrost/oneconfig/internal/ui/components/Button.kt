package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
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
    /** Outlines the button on hover; off by default so small inline icons (field clear buttons) stay bare */
    hoverBorder: Boolean = false,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()

    val isHovered by interactionSource.collectIsHoveredAsState()

    val iconColor = Motion.animateColorAsState(
        if (!enabled) {
            theme.textColorSecondary.copy(alpha = theme.disabledAlpha)
        } else {
            if (isHovered) hoveredForeground else foreground
        }
    )
    val borderColor = Motion.animateColorAsState(
        if (hoverBorder && enabled && isHovered) theme.borderColor
        else theme.borderColor.copy(0f)
    )

    Icon(
        iconName,
        tint = { iconColor.value },
        modifier = Modifier
            .oneConfigClickable(
                label = null,
                onClick = onClick,
                enabled = enabled,
                shape = theme.buttonShape,
                indication = false,
                interactionSource = interactionSource,
            )
            .then(modifier)
            // inset the glyph so the outline doesn't hug it; size bordered buttons 8dp larger to keep the icon size
            .then(
                if (hoverBorder) Modifier.animatedFrame(theme.buttonShape, border = { borderColor.value }).padding(4.dp)
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

    val iconColor = Motion.animateColorAsState(
        if (isHovered || selected) hoveredForeground else foreground
    )

    Icon(
        iconName,
        tint = { iconColor.value },
        modifier = Modifier
            .oneConfigClickable(
                label = null,
                onClick = onClick,
                selected = selected,
                shape = LocalTheme.current.buttonShape,
                indication = false,
                interactionSource = interactionSource,
            )
            .then(modifier)
    )
}
