package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun SwitchControl(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val trackColor by animateColorAsState(if (checked) Accent else LocalTheme.current.controlTrackColor)
    val thumbOffset by animateDpAsState(if (checked) 24.dp else 3.dp, animationSpec = spring())
    val borderColor by animateColorAsState(
        if (isHovered) LocalTheme.current.textColor.copy(alpha = 0.15f) else Color.Transparent
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (checked) "On" else "Off", color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .size(42.dp, 21.dp)
                .clip(LocalTheme.current.circleShape)
                .background(trackColor)
                .border(1.dp, borderColor, LocalTheme.current.circleShape)
                .onClick(interactionSource) { onCheckedChange(!checked) }
                .pointerHoverIcon(PointerIcon.Hand),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset)
                    .size(15.dp)
                    .background(LocalTheme.current.controlThumbColor, LocalTheme.current.circleShape),
            )
        }
    }
}
