@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.isKeyboardMode
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle

@ExperimentalOneConfigUiApi
public enum class IconButtonStyle {
    Ghost,

    Framed,

    Filled,

    Overlay,

    Hairline,

    Glass,
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
    shape: Shape = IconButtonDefaults.shape(style),
    restTint: Color = Color.Unspecified,
    active: Boolean = false,
    frameSize: Dp = Dp.Unspecified,
    iconSize: Dp = Dp.Unspecified,
    sound: UiSound? = UiSound.Click,
    glass: Boolean = true,
    focusRing: Boolean = true,
    dangerColor: Color = Color.Unspecified,
) {
    val glyphSize = if (iconSize.isSpecified) iconSize else IconButtonDefaults.iconSize(size)
    IconButtonFrame(contentDescription, onClick, modifier, enabled, style, size, selected, danger, tooltip, tooltipPlacement, interactionSource, shape, restTint, active, frameSize, sound, glass, focusRing, dangerColor) { color ->
        Icon(icon, null, Modifier.size(glyphSize), if (tint == Color.Unspecified) color else tint)
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
    shape: Shape = IconButtonDefaults.shape(style),
    restTint: Color = Color.Unspecified,
    active: Boolean = false,
    frameSize: Dp = Dp.Unspecified,
    iconSize: Dp = Dp.Unspecified,
    sound: UiSound? = UiSound.Click,
    glass: Boolean = true,
    focusRing: Boolean = true,
    dangerColor: Color = Color.Unspecified,
) {
    val glyphSize = if (iconSize.isSpecified) iconSize else IconButtonDefaults.iconSize(size)
    IconButtonFrame(contentDescription, onClick, modifier, enabled, style, size, selected, danger, tooltip, tooltipPlacement, interactionSource, shape, restTint, active, frameSize, sound, glass, focusRing, dangerColor) { color ->
        Image(painter, null, Modifier.size(glyphSize), colorFilter = ColorFilter.tint(if (tint == Color.Unspecified) color else tint))
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(
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
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = IconButtonDefaults.shape(style),
    restTint: Color = Color.Unspecified,
    active: Boolean = false,
    frameSize: Dp = Dp.Unspecified,
    sound: UiSound? = UiSound.Click,
    glass: Boolean = true,
    focusRing: Boolean = true,
    dangerColor: Color = Color.Unspecified,
    content: @Composable (tint: Color) -> Unit,
) {
    IconButtonFrame(contentDescription, onClick, modifier, enabled, style, size, selected, danger, tooltip, tooltipPlacement, interactionSource, shape, restTint, active, frameSize, sound, glass, focusRing, dangerColor, content)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
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
    shape: Shape = IconButtonDefaults.shape(style),
    restTint: Color = Color.Unspecified,
    active: Boolean = false,
    frameSize: Dp = Dp.Unspecified,
    iconSize: Dp = Dp.Unspecified,
    sound: UiSound? = UiSound.Click,
    glass: Boolean = true,
    focusRing: Boolean = true,
) {
    IconButton(
        icon, contentDescription, onClick, modifier, enabled, style, size, selected, danger, tooltip, tooltipPlacement, tint,
        interactionSource, shape, restTint, active, frameSize, iconSize, sound, glass, focusRing, Color.Unspecified,
    )
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
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
    shape: Shape = IconButtonDefaults.shape(style),
    restTint: Color = Color.Unspecified,
    active: Boolean = false,
    frameSize: Dp = Dp.Unspecified,
    iconSize: Dp = Dp.Unspecified,
    sound: UiSound? = UiSound.Click,
    glass: Boolean = true,
    focusRing: Boolean = true,
) {
    IconButton(
        painter, contentDescription, onClick, modifier, enabled, style, size, selected, danger, tooltip, tooltipPlacement, tint,
        interactionSource, shape, restTint, active, frameSize, iconSize, sound, glass, focusRing, Color.Unspecified,
    )
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(
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
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = IconButtonDefaults.shape(style),
    restTint: Color = Color.Unspecified,
    active: Boolean = false,
    frameSize: Dp = Dp.Unspecified,
    sound: UiSound? = UiSound.Click,
    glass: Boolean = true,
    focusRing: Boolean = true,
    content: @Composable (tint: Color) -> Unit,
) {
    IconButton(
        contentDescription, onClick, modifier, enabled, style, size, selected, danger, tooltip, tooltipPlacement,
        interactionSource, shape, restTint, active, frameSize, sound, glass, focusRing, Color.Unspecified, content,
    )
}

@ExperimentalOneConfigUiApi
public object IconButtonDefaults {
    public val style: IconButtonStyle = IconButtonStyle.Ghost

    public val size: ControlSize = ControlSize.Regular

    public val tooltipPlacement: TooltipPlacement = TooltipPlacement.BelowCenter

    public const val DANGER_HOVER_ALPHA: Float = 0.15f

    public const val DANGER_BORDER_ALPHA: Float = 0.5f

    @Composable
    public fun shape(style: IconButtonStyle): Shape {
        val theme = OneConfigTheme.current
        if (style == IconButtonStyle.Overlay) return theme.shapes.circle
        val base = theme.buttonShape
        return remember(base) { ProportionalCornerShape(base) }
    }

    public fun iconSize(size: ControlSize): Dp = ControlDefaults.metrics(size).iconSize

    public fun indication(style: IconButtonStyle, selected: Boolean = false, danger: Boolean = false): ControlIndication = when {
        style == IconButtonStyle.Filled || selected -> ControlDefaults.Underlay
        style == IconButtonStyle.Overlay -> ControlDefaults.Media
        style == IconButtonStyle.Glass -> ControlDefaults.PressOnly
        danger && (style == IconButtonStyle.Framed || style == IconButtonStyle.Hairline) -> ControlDefaults.PressOnly
        style == IconButtonStyle.Framed || style == IconButtonStyle.Hairline -> ControlDefaults.Framed
        else -> ControlDefaults.Bare
    }

    @Composable
    public fun contentColor(
        style: IconButtonStyle,
        hovered: Boolean,
        selected: Boolean = false,
        danger: Boolean = false,
        restTint: Color = Color.Unspecified,
        dangerColor: Color = Color.Unspecified,
    ): Color {
        val theme = OneConfigTheme.current
        val accented = style == IconButtonStyle.Filled || selected
        return when {
            accented && danger -> buttonContentColor(ControlStyle.DangerFilled, dangerColor)
            accented -> theme.accentTextColor
            style == IconButtonStyle.Overlay -> theme.onMedia
            style == IconButtonStyle.Glass -> theme.textColor
            hovered && danger -> if (dangerColor.isSpecified) dangerColor else theme.error
            hovered -> theme.textColor
            restTint.isSpecified -> restTint
            else -> theme.textColorSecondary
        }
    }

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun contentColor(
        style: IconButtonStyle,
        hovered: Boolean,
        selected: Boolean = false,
        danger: Boolean = false,
        restTint: Color = Color.Unspecified,
    ): Color = contentColor(style, hovered, selected, danger, restTint, Color.Unspecified)
}

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
    shape: Shape,
    restTint: Color,
    active: Boolean,
    frameSize: Dp,
    sound: UiSound?,
    glass: Boolean,
    focusRing: Boolean,
    dangerColor: Color,
    glyph: @Composable (Color) -> Unit,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    val keyboard = isKeyboardMode()
    val lit = enabled && (hovered || active)
    val held = enabled && active
    val accented = style == IconButtonStyle.Filled || selected
    val framed = style == IconButtonStyle.Framed
    val ghost = style == IconButtonStyle.Ghost && !accented
    val error = if (dangerColor.isSpecified) dangerColor else theme.error
    val dangerFill = error.copy(alpha = IconButtonDefaults.DANGER_HOVER_ALPHA)
    val fill by Motion.animateColorAsState(
        when {
            accented && danger -> error
            accented -> Color.Transparent
            style == IconButtonStyle.Overlay -> theme.onMediaShadow
            style == IconButtonStyle.Glass -> (if (danger) error else theme.componentBackground)
                .copy(alpha = if (lit) ButtonDefaults.GLASS_HOVER_FILL_ALPHA else ButtonDefaults.GLASS_FILL_ALPHA)
            lit && danger && framed -> dangerFill.compositeOver(theme.componentBackground)
            lit && danger -> dangerFill
            framed -> theme.componentBackground
            ghost && enabled && pressed -> theme.textColor.copy(alpha = theme.textColor.alpha * theme.pressedOverlayAlpha)
            ghost && lit -> theme.textColor.copy(alpha = theme.textColor.alpha * theme.hoverOverlayAlpha)
            else -> theme.textColor.copy(alpha = 0f)
        },
        label = "iconButtonFill",
    )
    val accent by Motion.animateFloatAsState(
        when {
            !accented || danger -> 0f
            lit -> ButtonDefaults.FILLED_HOVER_ALPHA
            else -> 1f
        },
        label = "iconButtonAccent",
    )
    val frame by Motion.animateColorAsState(
        when {
            accented || style == IconButtonStyle.Overlay || style == IconButtonStyle.Glass -> Color.Transparent
            lit && danger && (framed || style == IconButtonStyle.Hairline) -> error.copy(alpha = IconButtonDefaults.DANGER_BORDER_ALPHA)
            held && (framed || style == IconButtonStyle.Hairline) -> theme.textColorSecondary
            framed -> theme.borderColor
            ghost && lit && !danger -> theme.borderColor
            else -> theme.borderColor.copy(alpha = 0f)
        },
        label = "iconButtonBorder",
    )
    val content by Motion.animateColorAsState(IconButtonDefaults.contentColor(style, lit, selected, danger, restTint, dangerColor), label = "iconButtonContent")
    val glassed = glass && (framed || accented || style == IconButtonStyle.Glass)
    val edge = if (style == IconButtonStyle.Glass && !accented) Modifier.overMediaEdge(shape, theme.style == ThemeStyle.Pixel) else Modifier
    Box(
        modifier = modifier
            .size(if (frameSize.isSpecified) frameSize else ControlDefaults.metrics(size).height)
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .glassFrame(shape, { fill }, { frame }, { accent }, hover = enabled && glassed, glass = glassed, pull = 0.75.dp)
            .then(edge)
            .oneConfigClickable(
                label,
                onClick,
                enabled = enabled,
                selected = if (selected) true else null,
                shape = shape,
                indication = !ghost && !(accented && !danger && glassed && glassAvailable),
                sound = sound,
                interactionSource = source,
                recipe = IconButtonDefaults.indication(style, selected, danger),
                focusRing = focusRing,
            ),
        contentAlignment = Alignment.Center,
    ) {
        glyph(content)
        val tooltipShown = rememberTooltipShown(tooltip != null && (hovered || focused && keyboard))
        if (tooltipShown && tooltip != null) TooltipPopup(tooltip, Modifier, placement)
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
