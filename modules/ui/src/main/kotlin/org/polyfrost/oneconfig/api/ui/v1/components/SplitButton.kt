@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val FADE_MILLIS = 150

@ExperimentalOneConfigUiApi
public object SplitButtonDefaults {
    public val chevronPadding: Dp = 8.dp

    public val divider: Dp = 1.dp
}

@ExperimentalOneConfigUiApi
@Composable
public fun SplitButton(
    text: String,
    onClick: () -> Unit,
    menu: List<MenuEntry>,
    modifier: Modifier = Modifier,
    style: ControlStyle = ControlStyle.Primary,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(SplitButtonDefaults.divider)) {
            TextButton(text, onClick, Modifier.fillMaxHeight(), style, size, enabled)
            SplitChevron(expanded, { expanded = !expanded }, style, size, enabled)
        }
        DropdownMenu(expanded, { expanded = false }, menu, placement = Placement.BelowEnd)
    }
}

@Composable
private fun SplitChevron(expanded: Boolean, onToggle: () -> Unit, style: ControlStyle, size: ControlSize, enabled: Boolean) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val active = (hovered || expanded) && enabled
    val fill by animateColorAsState(
        when (style) {
            ControlStyle.Primary, ControlStyle.Ghost -> Color.Transparent
            ControlStyle.DangerFilled -> theme.error.copy(alpha = if (active) ButtonDefaults.FILLED_HOVER_ALPHA else 1f)
            ControlStyle.Danger -> theme.error.copy(alpha = if (active) ButtonDefaults.DANGER_HOVER_ALPHA else ButtonDefaults.DANGER_REST_ALPHA)
            ControlStyle.Secondary -> theme.componentBackground
        },
        Motion.spec(tween(FADE_MILLIS)),
    )
    val accentAlpha by animateFloatAsState(
        when {
            style != ControlStyle.Primary -> 0f
            active -> ButtonDefaults.FILLED_HOVER_ALPHA
            else -> 1f
        },
        Motion.spec(tween(FADE_MILLIS)),
    )
    val border by animateColorAsState(
        when {
            style != ControlStyle.Secondary -> Color.Transparent
            active -> theme.textColorSecondary
            else -> theme.borderColor
        },
        Motion.spec(tween(FADE_MILLIS)),
    )
    val chevron by animateFloatAsState(if (expanded) 0f else 180f, Motion.spec(tween(FADE_MILLIS)))
    val shape = ButtonDefaults.shape
    val ghost = style == ControlStyle.Ghost
    val label = kitString("oneconfig.ui.splitbutton.more", "More options")
    Row(
        modifier = Modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .heightIn(min = size.height)
            .fillMaxHeight()
            .glassFrame(shape, { fill }, { border }, { accentAlpha }, hover = enabled && !ghost, glass = !ghost)
            .oneConfigClickable(label, onToggle, enabled = enabled, selected = expanded, shape = shape, indication = ghost || !glassAvailable, interactionSource = source)
            .padding(horizontal = SplitButtonDefaults.chevronPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon("up", null, Modifier.size(ButtonDefaults.iconSize(size)).graphicsLayer { rotationZ = chevron }, ButtonDefaults.contentColor(style))
    }
}
