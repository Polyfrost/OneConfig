@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce

private val POPUP_GAP = 6.dp

@ExperimentalOneConfigUiApi
@Composable
public fun AnchoredPopupButton(
    icon: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowEnd,
    gap: Dp = POPUP_GAP,
    enabled: Boolean = true,
    style: IconButtonStyle = IconButtonStyle.Ghost,
    size: ControlSize = ControlSize.Regular,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    PopupButtonFrame(contentDescription, modifier, placement, gap, enabled, content) { open, toggle, triggerModifier ->
        IconButton(
            icon,
            contentDescription,
            toggle,
            triggerModifier,
            enabled = enabled,
            style = style,
            size = size,
            selected = open,
            tooltip = if (open) null else contentDescription,
            interactionSource = interactionSource,
        )
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun AnchoredPopupButton(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowEnd,
    gap: Dp = POPUP_GAP,
    enabled: Boolean = true,
    style: IconButtonStyle = IconButtonStyle.Ghost,
    size: ControlSize = ControlSize.Regular,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    PopupButtonFrame(contentDescription, modifier, placement, gap, enabled, content) { open, toggle, triggerModifier ->
        IconButton(
            painter,
            contentDescription,
            toggle,
            triggerModifier,
            enabled = enabled,
            style = style,
            size = size,
            selected = open,
            tooltip = if (open) null else contentDescription,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun PopupButtonFrame(
    contentDescription: String,
    modifier: Modifier,
    placement: Placement,
    gap: Dp,
    enabled: Boolean,
    content: @Composable (close: () -> Unit) -> Unit,
    button: @Composable (open: Boolean, toggle: () -> Unit, triggerModifier: Modifier) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val trigger = rememberPopupTrigger()
    LaunchedEffect(enabled) { if (!enabled) open = false }
    Box(modifier) {
        button(open, { open = !open }, Modifier.popupTrigger(trigger))
        AnchoredPopup(open, { open = false }, placement, gap, trigger = trigger) {
            Announce(contentDescription)
            content { open = false }
        }
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun AnchoredPopupButton(icon: String, tooltip: String, content: @Composable (close: () -> Unit) -> Unit) {
    AnchoredPopupButton(icon, tooltip, Modifier, content = content)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun AnchoredPopupButton(
    icon: String,
    tooltip: String,
    modifier: Modifier,
    alignment: Alignment.Horizontal,
    gap: Dp,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    val placement = when (alignment) {
        Alignment.Start -> Placement.BelowStart
        Alignment.CenterHorizontally -> Placement.BelowCenter
        else -> Placement.BelowEnd
    }
    AnchoredPopupButton(icon, tooltip, modifier, placement, gap, content = content)
}
