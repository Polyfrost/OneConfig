package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onClick: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()

    val isHovered by interactionSource.collectIsHoveredAsState()

    val iconColor by animateColorAsState(
        if (isHovered) hoveredForeground else foreground
    )

    Icon(
        iconName,
        color = iconColor, modifier = Modifier.onClick(interactionSource, onClick).then(modifier)
    )
}