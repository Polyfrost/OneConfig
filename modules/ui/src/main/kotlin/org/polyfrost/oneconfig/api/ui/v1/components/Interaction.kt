@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public enum class UiSound {
    Click,
    Open,
    Close,
    SliderTick,
}

@ExperimentalOneConfigUiApi
public fun playUiSound(sound: UiSound) {
    runCatching { componentsBridge().playSound(sound) }
}

private const val OVERLAY_FADE_MILLIS = 120

@ExperimentalOneConfigUiApi
public fun Modifier.oneConfigClickable(
    label: String?,
    onClick: () -> Unit,
    role: A11yRole = A11yRole.Button,
    enabled: Boolean = true,
    selected: Boolean? = null,
    checked: Boolean? = null,
    shape: Shape? = null,
    indication: Boolean = true,
    sound: UiSound? = UiSound.Click,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.buttonShape
    val latest by rememberUpdatedState(onClick)
    val latestSound by rememberUpdatedState(sound)
    a11y(label, role, selected, checked, outline, enabled)
        .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
        .then(if (indication && enabled) Modifier.stateOverlay(source, outline) else Modifier)
        .then(if (enabled) componentsBridge().keepSearchFocus(Modifier) else Modifier)
        .clickable(interactionSource = source, indication = null, enabled = enabled) {
            latestSound?.let(::playUiSound)
            latest()
        }
}

private fun Modifier.stateOverlay(source: MutableInteractionSource, shape: Shape): Modifier = composed {
    val theme = OneConfigTheme.current
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val target = when {
        pressed -> theme.pressedOverlayAlpha
        hovered -> theme.hoverOverlayAlpha
        else -> 0f
    }
    val overlay by animateFloatAsState(target, Motion.spec(tween(OVERLAY_FADE_MILLIS)), label = "stateOverlay")
    val color = theme.textColor
    drawBehind {
        if (overlay > 0f) drawOutline(shape.createOutline(size, layoutDirection, this), color.copy(alpha = overlay * color.alpha))
    }
}

@ExperimentalOneConfigUiApi
public class HoverState(
    public val interactionSource: MutableInteractionSource,
    public val hovered: Boolean,
)

@ExperimentalOneConfigUiApi
@Composable
public fun rememberHoverState(): HoverState {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    return HoverState(source, hovered)
}

private const val HIDDEN_FADE_MILLIS = 150

@ExperimentalOneConfigUiApi
public fun Modifier.hiddenUntilHover(visible: Boolean): Modifier = composed {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, Motion.spec(tween(HIDDEN_FADE_MILLIS)), label = "hiddenUntilHover")
    val placed = visible || alpha > 0f
    focusProperties { canFocus = visible }
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                if (placed) placeable.place(0, 0)
            }
        }
        .alpha(alpha)
}
