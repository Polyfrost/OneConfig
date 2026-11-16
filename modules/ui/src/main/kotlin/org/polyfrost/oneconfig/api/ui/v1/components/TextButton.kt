@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle
import org.polyfrost.oneconfig.api.ui.v1.theme.contrastAgainst

@Deprecated("Use ControlStyle", ReplaceWith("ControlStyle", "org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle"))
@ExperimentalOneConfigUiApi
public enum class ButtonStyle {
    Primary,
    Secondary,
    Danger,
    DangerFilled,
}

@Suppress("DEPRECATION")
internal fun ButtonStyle.toControlStyle(): ControlStyle = when (this) {
    ButtonStyle.Primary -> ControlStyle.Primary
    ButtonStyle.Secondary -> ControlStyle.Secondary
    ButtonStyle.Danger -> ControlStyle.Danger
    ButtonStyle.DangerFilled -> ControlStyle.DangerFilled
}

internal fun buttonIndication(style: ControlStyle, glass: Boolean = true): ControlIndication? = when (style) {
    ControlStyle.Ghost -> ControlDefaults.Underlay
    ControlStyle.Tinted, ControlStyle.Glass, ControlStyle.Footer -> ControlDefaults.PressOnly
    ControlStyle.Primary, ControlStyle.Secondary, ControlStyle.Danger, ControlStyle.DangerFilled -> if (glass && glassAvailable) null else ControlDefaults.Underlay
}

@ExperimentalOneConfigUiApi
@Composable
public fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ControlStyle = ControlStyle.Secondary,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    contentDescription: String? = null,
    labelStyle: TextStyle? = null,
    glass: Boolean = true,
    sound: UiSound? = UiSound.Click,
    active: Boolean = false,
    shape: Shape? = null,
    contentSpacing: Dp? = null,
    pinSlots: Boolean = false,
    dangerColor: Color = Color.Unspecified,
) {
    TextButtonContent(
        text, null, onClick, modifier, style, size, enabled, fillWidth, leading, trailing, interactionSource,
        shape ?: ButtonDefaults.shape(style), contentDescription, labelStyle, glass, sound, active,
        contentSpacing ?: ButtonDefaults.contentSpacing, pinSlots, dangerColor,
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun TextButton(
    text: AnnotatedString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ControlStyle = ControlStyle.Secondary,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    contentDescription: String? = null,
    labelStyle: TextStyle? = null,
    glass: Boolean = true,
    sound: UiSound? = UiSound.Click,
    active: Boolean = false,
    shape: Shape? = null,
    contentSpacing: Dp? = null,
    pinSlots: Boolean = false,
    dangerColor: Color = Color.Unspecified,
) {
    TextButtonContent(
        text.text, text, onClick, modifier, style, size, enabled, fillWidth, leading, trailing, interactionSource,
        shape ?: ButtonDefaults.shape(style), contentDescription, labelStyle, glass, sound, active,
        contentSpacing ?: ButtonDefaults.contentSpacing, pinSlots, dangerColor,
    )
}

@Composable
internal fun SegmentTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    style: ControlStyle,
    size: ControlSize,
    enabled: Boolean,
    shape: Shape,
    icon: String? = null,
) {
    val tint = ButtonDefaults.contentColor(style)
    val leading: (@Composable () -> Unit)? = icon?.let { { Icon(it, null, Modifier.size(ButtonDefaults.iconSize(size)), tint) } }
    TextButtonContent(text, null, onClick, modifier, style, size, enabled, false, leading, null, null, shape, null, null, true, UiSound.Click, false, ButtonDefaults.contentSpacing, false)
}

@Composable
internal fun Modifier.buttonFrame(
    style: ControlStyle,
    shape: Shape,
    enabled: Boolean,
    active: Boolean,
    glass: Boolean,
    source: MutableInteractionSource,
    dangerColor: Color = Color.Unspecified,
): Modifier {
    val theme = OneConfigTheme.current
    val accentColor = OneConfigTheme.accent
    val danger = if (dangerColor.isSpecified) dangerColor else theme.error
    val hovered by source.collectIsHoveredAsState()
    val lit = enabled && (hovered || active)
    val fill by Motion.animateColorAsState(
        when (style) {
            ControlStyle.Primary, ControlStyle.Ghost -> Color.Transparent
            ControlStyle.DangerFilled -> danger.copy(alpha = if (lit) ButtonDefaults.FILLED_HOVER_ALPHA else 1f)
            ControlStyle.Danger -> danger.copy(alpha = if (lit) ButtonDefaults.DANGER_HOVER_ALPHA else ButtonDefaults.DANGER_REST_ALPHA)
            ControlStyle.Secondary -> theme.componentBackground
            ControlStyle.Tinted -> accentColor.copy(alpha = if (lit) ButtonDefaults.TINT_HOVER_ALPHA else ButtonDefaults.TINT_REST_ALPHA)
            ControlStyle.Glass -> theme.componentBackground.copy(alpha = if (lit) ButtonDefaults.GLASS_HOVER_FILL_ALPHA else ButtonDefaults.GLASS_FILL_ALPHA)
            ControlStyle.Footer -> theme.chipBackground
        },
        label = "buttonFill",
    )
    val border by Motion.animateColorAsState(
        when (style) {
            ControlStyle.Secondary -> if (lit) theme.textColorSecondary else theme.borderColor
            ControlStyle.Tinted -> accentColor.copy(alpha = if (lit) 1f else ButtonDefaults.TINT_BORDER_ALPHA)
            else -> Color.Transparent
        },
        label = "buttonBorder",
    )
    val accent by Motion.animateFloatAsState(
        when {
            style != ControlStyle.Primary -> 0f
            lit -> ButtonDefaults.FILLED_HOVER_ALPHA
            else -> 1f
        },
        label = "buttonAccent",
    )
    val framed = glass && style != ControlStyle.Ghost && style != ControlStyle.Footer
    val frame = this
        .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
        .glassFrame(shape, { fill }, { border }, { accent }, hover = enabled && framed, glass = framed)
    return when (style) {
        ControlStyle.Glass -> frame.overMediaEdge(shape, theme.style == ThemeStyle.Pixel)
        ControlStyle.Footer -> frame.footerEdge(lit)
        else -> frame
    }
}

@Composable
internal fun buttonContentColor(style: ControlStyle, dangerColor: Color): Color = when {
    dangerColor.isUnspecified -> ButtonDefaults.contentColor(style)
    style == ControlStyle.Danger -> dangerColor
    style == ControlStyle.DangerFilled -> if (Color.White.contrastAgainst(dangerColor) >= DANGER_FILLED_WHITE_MIN_CONTRAST) Color.White else Color.Black
    else -> ButtonDefaults.contentColor(style)
}

private const val DANGER_FILLED_WHITE_MIN_CONTRAST = 3f

internal fun Modifier.overMediaEdge(shape: Shape, pixel: Boolean): Modifier = drawWithCache {
    val width = snappedStrokePx(ButtonDefaults.glassBorderWidth, pixel)
    val half = width / 2f
    val inner = shape.createOutline(Size((size.width - width).coerceAtLeast(0f), (size.height - width).coerceAtLeast(0f)), layoutDirection, this)
    val stroke = Stroke(width)
    val brush = ButtonDefaults.glassBorderBrush
    onDrawWithContent {
        drawContent()
        translate(half, half) { drawOutline(inner, brush, style = stroke) }
    }
}

@Composable
private fun Modifier.footerEdge(lit: Boolean): Modifier {
    val theme = OneConfigTheme.current
    val hairline by Motion.animateColorAsState(if (lit) theme.textColorSecondary else theme.borderColor, label = "footerEdge")
    return drawWithCache {
        val width = 1.dp.toPx()
        val y = width / 2f
        onDrawWithContent {
            drawContent()
            drawLine(hairline, Offset(0f, y), Offset(size.width, y), width)
        }
    }
}

@Composable
private fun TextButtonContent(
    text: String,
    styled: AnnotatedString?,
    onClick: () -> Unit,
    modifier: Modifier,
    style: ControlStyle,
    size: ControlSize,
    enabled: Boolean,
    fillWidth: Boolean,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    interactionSource: MutableInteractionSource?,
    shape: Shape,
    contentDescription: String?,
    labelStyle: TextStyle?,
    glass: Boolean,
    sound: UiSound?,
    active: Boolean,
    spacing: Dp,
    pinSlots: Boolean,
    dangerColor: Color = Color.Unspecified,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val metrics = ControlDefaults.metrics(size)
    val recipe = buttonIndication(style, glass)
    val content = buttonContentColor(style, dangerColor)
    val frame = modifier
        .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
        .heightIn(min = metrics.height)
        .buttonFrame(style, shape, enabled, active, glass, source, dangerColor)
        .oneConfigClickable(
            contentDescription ?: text,
            onClick,
            enabled = enabled,
            shape = shape,
            indication = recipe != null,
            sound = sound,
            interactionSource = source,
            recipe = recipe,
        )
        .padding(horizontal = metrics.horizontalPadding, vertical = 4.dp)
    val base = OneConfigTheme.typography.body.copy(fontSize = ButtonDefaults.fontSize(size), fontWeight = FontWeight.Normal)
    val resolved = if (labelStyle == null) base else base.merge(labelStyle)
    val label: @Composable (Modifier) -> Unit = { labelModifier ->
        if (styled != null) Text(styled, labelModifier, content, resolved, 1, TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        else Text(text, labelModifier, content, resolved, 1, TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
    if (pinSlots && (leading != null || trailing != null)) {
        EdgePinnedContent(frame, spacing, leading, trailing) { label(Modifier) }
        return
    }
    Row(
        modifier = frame,
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        label(Modifier)
        trailing?.invoke()
    }
}

@Composable
private fun EdgePinnedContent(
    modifier: Modifier,
    spacing: Dp,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    label: @Composable () -> Unit,
) {
    Layout(
        contents = listOf({ if (leading != null) leading() }, { if (trailing != null) trailing() }, label),
        modifier = modifier,
    ) { (leadingMeasurables, trailingMeasurables, labelMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val leadingPlaceables = leadingMeasurables.map { it.measure(loose) }
        val trailingPlaceables = trailingMeasurables.map { it.measure(loose) }
        val side = maxOf(leadingPlaceables.maxOfOrNull { it.width } ?: 0, trailingPlaceables.maxOfOrNull { it.width } ?: 0)
        val inset = if (side > 0) side + spacing.roundToPx() else 0
        val labelMax = if (constraints.hasBoundedWidth) (constraints.maxWidth - inset * 2).coerceAtLeast(0) else Constraints.Infinity
        val labelPlaceables = labelMeasurables.map { it.measure(loose.copy(maxWidth = labelMax)) }
        val labelWidth = labelPlaceables.maxOfOrNull { it.width } ?: 0
        val width = (labelWidth + inset * 2).coerceIn(constraints.minWidth, constraints.maxWidth)
        val tallest = (leadingPlaceables + trailingPlaceables + labelPlaceables).maxOfOrNull { it.height } ?: 0
        val height = tallest.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            leadingPlaceables.forEach { it.placeRelative(0, (height - it.height) / 2) }
            trailingPlaceables.forEach { it.placeRelative(width - it.width, (height - it.height) / 2) }
            labelPlaceables.forEach { it.placeRelative((width - it.width) / 2, (height - it.height) / 2) }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun TextButton(
    text: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ControlStyle = ControlStyle.Secondary,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    contentDescription: String? = null,
    labelStyle: TextStyle? = null,
    glass: Boolean = true,
    sound: UiSound? = UiSound.Click,
    active: Boolean = false,
    shape: Shape? = null,
    contentSpacing: Dp? = null,
    pinSlots: Boolean = false,
    dangerColor: Color = Color.Unspecified,
) {
    val tint = buttonContentColor(style, dangerColor)
    TextButton(
        text, onClick, modifier, style, size, enabled, fillWidth,
        leading = { Icon(icon, null, Modifier.size(ButtonDefaults.iconSize(size)), tint) },
        trailing = trailing,
        interactionSource = interactionSource,
        contentDescription = contentDescription,
        labelStyle = labelStyle,
        glass = glass,
        sound = sound,
        active = active,
        shape = shape,
        contentSpacing = contentSpacing,
        pinSlots = pinSlots,
        dangerColor = dangerColor,
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun TextButton(
    text: AnnotatedString,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ControlStyle = ControlStyle.Secondary,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    contentDescription: String? = null,
    labelStyle: TextStyle? = null,
    glass: Boolean = true,
    sound: UiSound? = UiSound.Click,
    active: Boolean = false,
    shape: Shape? = null,
    contentSpacing: Dp? = null,
    pinSlots: Boolean = false,
    dangerColor: Color = Color.Unspecified,
) {
    val tint = buttonContentColor(style, dangerColor)
    TextButton(
        text, onClick, modifier, style, size, enabled, fillWidth,
        leading = { Icon(icon, null, Modifier.size(ButtonDefaults.iconSize(size)), tint) },
        trailing = trailing,
        interactionSource = interactionSource,
        contentDescription = contentDescription,
        labelStyle = labelStyle,
        glass = glass,
        sound = sound,
        active = active,
        shape = shape,
        contentSpacing = contentSpacing,
        pinSlots = pinSlots,
        dangerColor = dangerColor,
    )
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun TextButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    TextButton(label, onClick, modifier)
}

@Suppress("DEPRECATION")
@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun TextButton(label: String, modifier: Modifier, style: ButtonStyle, onClick: () -> Unit) {
    TextButton(label, onClick, modifier, style.toControlStyle())
}

@Suppress("DEPRECATION")
@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun TextButton(label: String, icon: String?, modifier: Modifier, style: ButtonStyle, enabled: Boolean, onClick: () -> Unit) {
    if (icon != null) TextButton(label, icon, onClick, modifier, style.toControlStyle(), enabled = enabled)
    else TextButton(label, onClick, modifier, style.toControlStyle(), enabled = enabled)
}
