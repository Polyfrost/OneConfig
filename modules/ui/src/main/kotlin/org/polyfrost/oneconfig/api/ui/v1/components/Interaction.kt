@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.focusRing
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme

@ExperimentalOneConfigUiApi
public enum class UiSound {
    Click,
    Open,
    Close,
    SliderTick,
    HudSelect,
    HudDragStart,
    HudDragEnd,
    HudResizeStart,
    HudResizeEnd,
}

@ExperimentalOneConfigUiApi
public fun playUiSound(sound: UiSound) {
    runCatching { componentsBridge().playSound(sound) }
}

@ExperimentalOneConfigUiApi
public enum class OverlayTone {
    Auto,

    Darken,

    Lighten,
}

@ExperimentalOneConfigUiApi
public enum class OverlayLayer {
    Over,

    Behind,
}

private const val DARKEN_HOVER_ALPHA = 0.12f
private const val DARKEN_PRESSED_ALPHA = 0.2f

@ExperimentalOneConfigUiApi
@Immutable
public class ControlIndication(
    public val hoverAlpha: Float? = null,
    public val pressedAlpha: Float? = null,
    public val tone: OverlayTone = OverlayTone.Auto,
    public val layer: OverlayLayer = OverlayLayer.Over,
    public val hoverBorder: Boolean = false,
    public val borderWidth: Dp = 1.dp,
) {
    public fun hoverAlpha(theme: OneConfigUITheme): Float =
        hoverAlpha ?: if (tone == OverlayTone.Darken) DARKEN_HOVER_ALPHA else theme.hoverOverlayAlpha

    public fun pressedAlpha(theme: OneConfigUITheme): Float =
        pressedAlpha ?: if (tone == OverlayTone.Darken) DARKEN_PRESSED_ALPHA else theme.pressedOverlayAlpha

    public fun toneColor(theme: OneConfigUITheme): Color = when (tone) {
        OverlayTone.Auto -> theme.textColor
        OverlayTone.Darken -> Color.Black
        OverlayTone.Lighten -> Color.White
    }

    public fun copy(
        hoverAlpha: Float? = this.hoverAlpha,
        pressedAlpha: Float? = this.pressedAlpha,
        tone: OverlayTone = this.tone,
        layer: OverlayLayer = this.layer,
        hoverBorder: Boolean = this.hoverBorder,
        borderWidth: Dp = this.borderWidth,
    ): ControlIndication = ControlIndication(hoverAlpha, pressedAlpha, tone, layer, hoverBorder, borderWidth)
}

@ExperimentalOneConfigUiApi
public fun Modifier.controlInteraction(
    interactionSource: InteractionSource,
    shape: Shape? = null,
    recipe: ControlIndication = ControlDefaults.Bare,
    enabled: Boolean = true,
    focusRing: Boolean = true,
    pointerIcon: PointerIcon? = PointerIcon.Hand,
): Modifier = composed {
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.buttonShape
    this
        .then(if (enabled && pointerIcon != null) Modifier.pointerHoverIcon(pointerIcon) else Modifier)
        .then(if (focusRing) Modifier.focusRing(outline) else Modifier)
        .interactionLayers(interactionSource, outline, recipe, enabled)
}

private fun Modifier.interactionLayers(
    source: InteractionSource,
    shape: Shape,
    recipe: ControlIndication,
    enabled: Boolean,
): Modifier = composed {
    val theme = OneConfigTheme.current
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val target = when {
        !enabled -> 0f
        pressed -> recipe.pressedAlpha(theme)
        hovered -> recipe.hoverAlpha(theme)
        else -> 0f
    }
    val overlay = Motion.animateFloatAsState(target, tween(Motion.fast), "controlOverlay")
    val ring = Motion.animateFloatAsState(if (enabled && recipe.hoverBorder && (hovered || pressed)) 1f else 0f, tween(Motion.fast), "controlBorder")
    val tone = recipe.toneColor(theme)
    val borderColor = theme.textColorSecondary
    val behind = recipe.layer == OverlayLayer.Behind
    val width = recipe.borderWidth
    drawWithContent {
        val amount = overlay.value
        val outline = if (amount > 0f || ring.value > 0f) shape.createOutline(size, layoutDirection, this) else null
        if (behind && outline != null && amount > 0f) drawOutline(outline, tone.copy(alpha = amount * tone.alpha))
        drawContent()
        if (!behind && outline != null && amount > 0f) drawOutline(outline, tone.copy(alpha = amount * tone.alpha))
        val b = ring.value
        if (b > 0f) {
            val px = width.toPx()
            inset(px / 2f) {
                drawOutline(shape.createOutline(size, layoutDirection, this), borderColor.copy(alpha = borderColor.alpha * b), style = Stroke(px))
            }
        }
    }
}

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
    recipe: ControlIndication? = null,
    pointerIcon: PointerIcon? = PointerIcon.Hand,
    focusRing: Boolean = true,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.buttonShape
    val latest by rememberUpdatedState(onClick)
    val latestSound by rememberUpdatedState(sound)
    a11y(label, role, selected, checked, outline, enabled && focusRing)
        .then(if (enabled && pointerIcon != null) Modifier.pointerHoverIcon(pointerIcon) else Modifier)
        .then(if (indication && enabled) Modifier.interactionLayers(source, outline, recipe ?: ControlDefaults.Underlay, true) else Modifier)
        .then(if (enabled) componentsBridge().keepSearchFocus(Modifier) else Modifier)
        .clickable(interactionSource = source, indication = null, enabled = enabled) {
            latestSound?.let(::playUiSound)
            latest()
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

@ExperimentalOneConfigUiApi
public fun Modifier.hiddenUntilHover(visible: Boolean): Modifier = composed {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, Motion.tweenSpec(Motion.standard), label = "hiddenUntilHover")
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
