@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

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

private const val FADE_MILLIS = 150

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
) {
    TextButtonContent(text, null, onClick, modifier, style, size, enabled, fillWidth, leading, trailing, interactionSource)
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
) {
    TextButtonContent(text.text, text, onClick, modifier, style, size, enabled, fillWidth, leading, trailing, interactionSource)
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
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val active = hovered && enabled
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
    val content = ButtonDefaults.contentColor(style)
    val shape = ButtonDefaults.shape
    val ghost = style == ControlStyle.Ghost
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .heightIn(min = size.height)
            .glassFrame(shape, { fill }, { border }, { accentAlpha }, hover = enabled && !ghost, glass = !ghost)
            .oneConfigClickable(text, onClick, enabled = enabled, shape = shape, indication = ghost || !glassAvailable, interactionSource = source)
            .padding(horizontal = ButtonDefaults.horizontalPadding(size), vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.contentSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        val labelStyle = OneConfigTheme.typography.body.copy(fontSize = ButtonDefaults.fontSize(size), fontWeight = FontWeight.Normal)
        if (styled != null) Text(styled, Modifier, content, labelStyle, 1, TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        else Text(text, Modifier, content, labelStyle, 1, TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        trailing?.invoke()
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
) {
    val tint = ButtonDefaults.contentColor(style)
    TextButton(
        text, onClick, modifier, style, size, enabled, fillWidth,
        leading = { Icon(icon, null, Modifier.size(ButtonDefaults.iconSize(size)), tint) },
        trailing = trailing,
        interactionSource = interactionSource,
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
) {
    val tint = ButtonDefaults.contentColor(style)
    TextButton(
        text, onClick, modifier, style, size, enabled, fillWidth,
        leading = { Icon(icon, null, Modifier.size(ButtonDefaults.iconSize(size)), tint) },
        trailing = trailing,
        interactionSource = interactionSource,
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
