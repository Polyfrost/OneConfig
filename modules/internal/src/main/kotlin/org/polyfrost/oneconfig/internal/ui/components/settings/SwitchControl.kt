package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.spring
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.offset
import org.polyfrost.compose.foundation.layout.offsetX
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.background
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.AccentBrush
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun SwitchControl(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val accentAlpha by animateFloatAsState(if (checked) 1f else 0f)
    val thumbX = if (checked) 24f else 3f
    val thumbOffset = animateFloatAsState(thumbX, animationSpec = spring())
    val borderColor = animateColorAsState(
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
                .background(LocalTheme.current.controlTrackColor, LocalTheme.current.circleShape)
                .background(AccentBrush, LocalTheme.current.circleShape, accentAlpha)
                .border(1.dp, borderColor, LocalTheme.current.circleShape)
                .onClick(interactionSource) { onCheckedChange(!checked) }
                .pointerHoverIcon(PointerIcon.Hand),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbX.dp)
                    .offsetX(thumbOffset, thumbX)
                    .size(15.dp)
                    .background(LocalTheme.current.controlThumbColor, LocalTheme.current.circleShape),
            )
        }
    }
}
