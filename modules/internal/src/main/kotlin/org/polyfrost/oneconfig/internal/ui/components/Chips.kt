package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    val backgroundColor by animateColorAsState(
        if (selected) Accent else if (isHovered) hoverBackground else theme.chipBackground
    )
    val textColor by animateColorAsState(
        when {
            selected -> theme.accentTextColor
            // the half-accent tint can sit closer to either text color depending on theme and accent
            isHovered -> readableOn(hoverBackground, theme.accentTextColor, theme.textColor)
            else -> theme.textColor
        }
    )

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 33.dp)
            .background(backgroundColor, LocalTheme.current.sideBarNavigationEntryShape)
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
            Text(label, fontSize = 14.sp, color = textColor)
        }
    }
}

private fun readableOn(background: Color, a: Color, b: Color): Color {
    val bg = background.luminance()
    return if (contrast(a.luminance(), bg) >= contrast(b.luminance(), bg)) a else b
}

private fun contrast(l1: Float, l2: Float) = (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
