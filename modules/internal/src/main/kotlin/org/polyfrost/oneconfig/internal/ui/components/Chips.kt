package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.themes.Accent
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
    val hoverBackground = Accent.copy(alpha = 0.5f).compositeOver(theme.chipBackground)
    val backgroundColor = animateColorAsState(
        if (isHovered) hoverBackground else theme.chipBackground
    )
    val selectedAlpha = animateFloatAsState(if (selected) 1f else 0f)
    val textColor = animateColorAsState(
        if (selected) LocalTheme.current.accentTextColor else LocalTheme.current.textColor
    )
    val borderColor = theme.borderColor

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 33.dp)
            .graphicsLayer {}
            .animatedFrame(
                theme.sideBarNavigationEntryShape,
                fill = { backgroundColor.value },
                accentAlpha = { selectedAlpha.value },
                border = { borderColor },
            )
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
            icon?.let { Icon(it, tint = { textColor.value }) }
            Text(label, fontSize = 14.sp, color = Color.Unspecified, colorProducer = { textColor.value })
        }
    }
}