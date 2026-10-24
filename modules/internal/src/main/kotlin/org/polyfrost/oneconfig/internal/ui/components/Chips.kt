package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.heightIn
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.AccentBrush
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Chip(
    label: String,
    selected: Boolean,
    icon: String? = null,
    onClick: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val theme = LocalTheme.current
    val hoverBackground = theme.chipBackground.lerp(Accent.withAlpha(255), 0.5f * Accent.alphaF)
    val backgroundColor = animateColorAsState(
        if (isHovered) hoverBackground else theme.chipBackground
    )
    val selectedAlpha by animateFloatAsState(if (selected) 1f else 0f)
    val textColor = animateColorAsState(
        if (selected) LocalTheme.current.accentTextColor else LocalTheme.current.textColor
    )

    Row(
        modifier = Modifier
            .heightIn(min = 33.dp)
            .background(backgroundColor, LocalTheme.current.sideBarNavigationEntryShape)
            .background(AccentBrush, LocalTheme.current.sideBarNavigationEntryShape, selectedAlpha)
            .border(1.dp, LocalTheme.current.borderColor, LocalTheme.current.sideBarNavigationEntryShape)
            .pointerHoverIcon(PointerIcon.Hand)
            .onClick(interactionSource) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.padding(vertical = 7.5.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            icon?.let { Icon(it, color = textColor) }
            if (label.isNotEmpty()) Text(label, fontSize = 14.sp, color = textColor)
        }
    }
}
