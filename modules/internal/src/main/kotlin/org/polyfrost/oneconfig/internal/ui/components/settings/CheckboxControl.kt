package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.animatedFrame
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.animateAccentColorAsState
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

    val accentAlpha = Motion.animateFloatAsState(if (checked) 1f else 0f)
    val borderColor = animateAccentColorAsState(
        checked to isHovered,
        when {
            checked -> Accent
            isHovered -> theme.textColorSecondary
            else -> theme.borderColor
        }
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer {}
            .clip(theme.checkBoxShape)
            .animatedFrame(theme.checkBoxShape, fill = { theme.componentBackground }, accentAlpha = { accentAlpha.value }, borderWidth = 1.5.dp, border = { borderColor.value })
            .oneConfigClickable(
                label = null,
                onClick = onToggle,
                role = A11yRole.Checkbox,
                checked = checked,
                shape = theme.checkBoxShape,
                indication = false,
                interactionSource = interactionSource,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(checked, enter = Motion.enter(fadeIn()), exit = Motion.exit(fadeOut())) {
            Icon("tick", modifier = Modifier.size(26.dp), color = theme.textColor)
        }
    }
}

@Composable
fun CheckboxControl(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (checked) translated("oneconfig.control.on", "On") else translated("oneconfig.control.off", "Off"), color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
        CheckboxIndicator(checked, onToggle = { onCheckedChange(!checked) })
    }
}
