@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.isKeyboardMode
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public enum class IconButtonStyle {
    Ghost,

    Framed,

    Filled,

    Overlay,
}

@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(
    icon: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: IconButtonStyle = IconButtonStyle.Ghost,
    size: ControlSize = ControlSize.Regular,
    selected: Boolean = false,
    danger: Boolean = false,
    tooltip: String? = contentDescription,
    tooltipPlacement: TooltipPlacement = TooltipPlacement.BelowCenter,
    tint: Color = Color.Unspecified,
    interactionSource: MutableInteractionSource? = null,
) {
    IconButtonFrame(contentDescription, onClick, modifier, enabled, style, size, selected, danger, tooltip, tooltipPlacement, interactionSource) { color ->
        Icon(icon, null, Modifier.size(IconButtonDefaults.iconSize(size)), if (tint == Color.Unspecified) color else tint)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: IconButtonStyle = IconButtonStyle.Ghost,
    size: ControlSize = ControlSize.Regular,
    selected: Boolean = false,
    danger: Boolean = false,
    tooltip: String? = contentDescription,
    tooltipPlacement: TooltipPlacement = TooltipPlacement.BelowCenter,
    tint: Color = Color.Unspecified,
    interactionSource: MutableInteractionSource? = null,
) {
    IconButtonFrame(contentDescription, onClick, modifier, enabled, style, size, selected, danger, tooltip, tooltipPlacement, interactionSource) { color ->
        Image(painter, null, Modifier.size(IconButtonDefaults.iconSize(size)), colorFilter = ColorFilter.tint(if (tint == Color.Unspecified) color else tint))
    }
}

@ExperimentalOneConfigUiApi
public object IconButtonDefaults {
    public val style: IconButtonStyle = IconButtonStyle.Ghost

    public val size: ControlSize = ControlSize.Regular

    public val tooltipPlacement: TooltipPlacement = TooltipPlacement.BelowCenter

    public const val DANGER_HOVER_ALPHA: Float = 0.15f

    @Composable
    public fun shape(style: IconButtonStyle): Shape {
        val theme = OneConfigTheme.current
        if (style == IconButtonStyle.Overlay) return theme.shapes.circle
        val base = theme.buttonShape
        return remember(base) { ProportionalCornerShape(base) }
    }

    public fun iconSize(size: ControlSize): Dp = when (size) {
        ControlSize.Compact -> 14.dp
        ControlSize.Regular -> 16.dp
        ControlSize.Large -> 18.dp
    }

    @Composable
    public fun contentColor(style: IconButtonStyle, hovered: Boolean, selected: Boolean = false, danger: Boolean = false): Color {
        val theme = OneConfigTheme.current
        return when {
            style == IconButtonStyle.Filled || selected -> theme.accentTextColor
            style == IconButtonStyle.Overlay -> theme.onMedia
            hovered && danger -> theme.error
            hovered -> theme.textColor
            else -> theme.textColorSecondary
        }
    }
}

private const val ICON_FADE_MILLIS = 150

@Composable
internal fun IconButtonFrame(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    style: IconButtonStyle,
    size: ControlSize,
    selected: Boolean,
    danger: Boolean,
    tooltip: String?,
    placement: TooltipPlacement,
    interactionSource: MutableInteractionSource?,
    glyph: @Composable (Color) -> Unit,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val keyboard = isKeyboardMode()
    val active = hovered && enabled
    val accented = style == IconButtonStyle.Filled || selected
    val ghost = style == IconButtonStyle.Ghost && !accented
    val spec = Motion.spec(tween<Color>(ICON_FADE_MILLIS))
    val fill by animateColorAsState(
        when {
            accented -> Color.Transparent
            style == IconButtonStyle.Overlay -> theme.onMediaShadow
            active && danger -> theme.error.copy(alpha = IconButtonDefaults.DANGER_HOVER_ALPHA)
            style == IconButtonStyle.Framed -> theme.componentBackground
            ghost && enabled && pressed -> theme.textColor.copy(alpha = theme.textColor.alpha * theme.pressedOverlayAlpha)
            ghost && active -> theme.textColor.copy(alpha = theme.textColor.alpha * theme.hoverOverlayAlpha)
            ghost -> theme.textColor.copy(alpha = 0f)
            else -> Color.Transparent
        },
        spec,
    )
    val accentAlpha by animateFloatAsState(
        when {
            !accented -> 0f
            active -> ButtonDefaults.FILLED_HOVER_ALPHA
            else -> 1f
        },
        Motion.spec(tween(ICON_FADE_MILLIS)),
    )
    val frame by animateColorAsState(
        when {
            accented || style == IconButtonStyle.Overlay -> Color.Transparent
            active && danger -> theme.error.copy(alpha = 0f)
            style == IconButtonStyle.Framed && active -> theme.textColorSecondary
            style == IconButtonStyle.Framed -> theme.borderColor
            active -> theme.borderColor
            else -> theme.borderColor.copy(alpha = 0f)
        },
        spec,
    )
    val content by animateColorAsState(IconButtonDefaults.contentColor(style, active, selected, danger), spec)
    val shape = IconButtonDefaults.shape(style)
    val glass = style == IconButtonStyle.Framed || accented
    Box(
        modifier = modifier
            .size(size.height)
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .glassFrame(shape, { fill }, { frame }, { accentAlpha }, hover = enabled && glass, glass = glass, pull = 0.75.dp)
            .oneConfigClickable(
                label,
                onClick,
                enabled = enabled,
                selected = if (selected) true else null,
                shape = shape,
                indication = !ghost && (!glass || !glassAvailable),
                interactionSource = source,
            ),
        contentAlignment = Alignment.Center,
    ) {
        glyph(content)
        if (tooltip != null && (hovered || focused && keyboard)) TooltipPopup(tooltip, placement)
    }
}

private const val MAX_CORNER_FRACTION = 0.25f

private class ProportionalCornerShape(private val base: Shape) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val outline = base.createOutline(size, layoutDirection, density)
        if (outline !is Outline.Rounded) return outline
        val corner = outline.roundRect.topLeftCornerRadius
        val radius = minOf(corner.x, corner.y, size.minDimension * MAX_CORNER_FRACTION)
        return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius)))
    }

    override fun equals(other: Any?): Boolean = other is ProportionalCornerShape && other.base == base

    override fun hashCode(): Int = base.hashCode()
}
