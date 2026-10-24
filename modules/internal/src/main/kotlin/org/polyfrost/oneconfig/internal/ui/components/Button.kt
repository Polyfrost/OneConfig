package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

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

    val iconColor = animateColorAsState(
        if (!enabled) {
            LocalTheme.current.textColorSecondary.copy(alpha = 0.35f)
        } else {
            if (isHovered) hoveredForeground else foreground
        }
    )
    val borderColor = animateColorAsState(
        if (hoverBorder && enabled && isHovered) LocalTheme.current.borderColor
        else Color.Transparent
    )

    if (!hoverBorder) {
        Icon(iconName, color = iconColor, modifier = Modifier.onClick(interactionSource, enabled, onClick).then(modifier))
        return
    }
    Box(
        Modifier.onClick(interactionSource, enabled, onClick).then(modifier)
            .border(1.dp, borderColor, LocalTheme.current.buttonShape).padding(4.dp)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
    ) {
        Icon(iconName, color = iconColor, modifier = Modifier.size((modifier.sizeDp() - 8f).dp))
    }
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

    val iconColor = animateColorAsState(
        if (isHovered || selected) hoveredForeground else foreground
    )

    Icon(
        iconName,
        color = iconColor, modifier = Modifier.onClick(interactionSource, onClick).then(modifier)
    )
}
