@file:OptIn(ExperimentalOneConfigUiApi::class, ExperimentalVersionOverloading::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce

private val POPUP_GAP = 6.dp

internal val LocalPopupSounds = staticCompositionLocalOf { false }

@ExperimentalOneConfigUiApi
@Composable
public fun ProvidePopupSounds(enabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPopupSounds provides enabled, content = content)
}

@ExperimentalOneConfigUiApi
@Composable
public fun AnchoredPopupButton(
    icon: String,
    contentDescription: String,
    @IntroducedAt("1.8.0") modifier: Modifier = Modifier,
    @IntroducedAt("1.8.0") placement: Placement = Placement.BelowEnd,
    @IntroducedAt("1.8.0") gap: Dp = POPUP_GAP,
    @IntroducedAt("1.8.0") enabled: Boolean = true,
    @IntroducedAt("1.8.0") style: IconButtonStyle = IconButtonStyle.Ghost,
    @IntroducedAt("1.8.0") size: ControlSize = ControlSize.Regular,
    @IntroducedAt("1.8.0") interactionSource: MutableInteractionSource? = null,
    @IntroducedAt("1.8.0") selectedWhenOpen: Boolean = false,
    @IntroducedAt("1.8.0") tooltip: String? = contentDescription,
    @IntroducedAt("1.8.0") tooltipPlacement: TooltipPlacement = TooltipPlacement.BelowCenter,
    @IntroducedAt("1.8.0") sound: UiSound? = UiSound.Click,
    @IntroducedAt("1.8.0") popupSounds: Boolean = LocalPopupSounds.current,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    PopupButtonFrame(contentDescription, modifier, placement, gap, enabled, popupSounds, content) { open, toggle, triggerModifier ->
        IconButton(
            icon,
            contentDescription,
            toggle,
            triggerModifier,
            enabled = enabled,
            style = style,
            size = size,
            selected = selectedWhenOpen && open,
            active = open,
            tooltip = if (open) null else tooltip,
            tooltipPlacement = tooltipPlacement,
            interactionSource = interactionSource,
            sound = sound,
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
    selectedWhenOpen: Boolean = false,
    tooltip: String? = contentDescription,
    tooltipPlacement: TooltipPlacement = TooltipPlacement.BelowCenter,
    sound: UiSound? = UiSound.Click,
    popupSounds: Boolean = LocalPopupSounds.current,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    PopupButtonFrame(contentDescription, modifier, placement, gap, enabled, popupSounds, content) { open, toggle, triggerModifier ->
        IconButton(
            painter,
            contentDescription,
            toggle,
            triggerModifier,
            enabled = enabled,
            style = style,
            size = size,
            selected = selectedWhenOpen && open,
            active = open,
            tooltip = if (open) null else tooltip,
            tooltipPlacement = tooltipPlacement,
            interactionSource = interactionSource,
            sound = sound,
        )
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun AnchoredPopupButton(
    contentDescription: String,
    trigger: @Composable (open: Boolean, toggle: () -> Unit, triggerModifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowEnd,
    gap: Dp = POPUP_GAP,
    enabled: Boolean = true,
    popupSounds: Boolean = LocalPopupSounds.current,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    PopupButtonFrame(contentDescription, modifier, placement, gap, enabled, popupSounds, content, trigger)
}

@Composable
private fun PopupButtonFrame(
    contentDescription: String,
    modifier: Modifier,
    placement: Placement,
    gap: Dp,
    enabled: Boolean,
    popupSounds: Boolean,
    content: @Composable (close: () -> Unit) -> Unit,
    button: @Composable (open: Boolean, toggle: () -> Unit, triggerModifier: Modifier) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val trigger = rememberPopupTrigger()
    val inherited = LocalPopupSounds.current
    LaunchedEffect(enabled) { if (!enabled) open = false }
    Box(modifier) {
        button(open, { if (enabled) open = !open }, Modifier.popupTrigger(trigger))
        CompositionLocalProvider(LocalPopupSounds provides popupSounds) {
            AnchoredPopup(open, { open = false }, placement, gap, trigger = trigger) {
                CompositionLocalProvider(LocalPopupSounds provides inherited) {
                    Announce(contentDescription)
                    content { open = false }
                }
            }
        }
    }
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
