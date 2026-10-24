package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.background
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.AccentBrush
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun CheckboxIndicator(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val accentAlpha by animateFloatAsState(if (checked) 1f else 0f)
    val borderColor = animateColorAsState(
        when {
            checked -> Accent
            isHovered -> theme.textColorSecondary
            else -> theme.borderColor
        }
    )
    val tickTarget = if (checked) 1f else 0f
    val tickAlpha = animateFloatAsState(tickTarget)
    val tickVisible by remember { derivedStateOf { tickAlpha.value > 0.01f } }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(theme.checkBoxShape)
            .background(theme.componentBackground)
            .background(AccentBrush, alpha = accentAlpha)
            .border(1.5.dp, borderColor, theme.checkBoxShape)
            .onClick(interactionSource, onToggle)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        if (tickVisible) {
            Icon("tick", modifier = Modifier.size(26.dp).alpha(tickAlpha, tickTarget), color = theme.textColor)
        }
    }
}

@Composable
fun CheckboxControl(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (checked) "On" else "Off", color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
        CheckboxIndicator(checked, onToggle = { onCheckedChange(!checked) })
    }
}
