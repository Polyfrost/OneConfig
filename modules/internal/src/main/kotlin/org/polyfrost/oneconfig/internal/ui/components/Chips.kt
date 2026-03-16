package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val backgroundColor by animateColorAsState(
        if (selected) Accent else LocalTheme.current.chipBackground
    )
    val textColor by animateColorAsState(
        if (selected) LocalTheme.current.accentTextColor else LocalTheme.current.textColor
    )

    Row(
        modifier = Modifier
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
