package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun CheckboxControl(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(if (checked) Accent else LocalTheme.current.componentBackground)
    val borderColor by animateColorAsState(
        when {
            checked -> Accent
            isHovered -> LocalTheme.current.textColorSecondary
            else -> LocalTheme.current.borderColor
        }
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (checked) "On" else "Off", color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(LocalTheme.current.checkBoxShape)
                .background(bgColor)
                .border(1.5.dp, borderColor, LocalTheme.current.checkBoxShape)
                .onClick(interactionSource) { onCheckedChange(!checked) }
                .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(checked, enter = fadeIn(), exit = fadeOut()) {
                Icon("tick", modifier = Modifier.size(26.dp), color = LocalTheme.current.textColor)
            }
        }
    }
}
