@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val CHIP_FADE_MILLIS = 150

@ExperimentalOneConfigUiApi
public object ChipDefaults {
    public val size: ControlSize = ControlSize.Regular

    public const val HOVER_ACCENT_ALPHA: Float = 0.5f

    public val shape: Shape
        @Composable get() = OneConfigTheme.current.sideBarNavigationEntryShape

    public fun contentSpacing(size: ControlSize): Dp = if (size == ControlSize.Compact) 6.dp else 10.dp

    public fun horizontalPadding(size: ControlSize): Dp = if (size == ControlSize.Compact) 10.dp else 12.dp

    public fun fontSize(size: ControlSize): TextUnit = if (size == ControlSize.Compact) 13.sp else 14.sp

    public fun iconSize(size: ControlSize): Dp = if (size == ControlSize.Compact) 14.dp else 16.dp

    public val fill: Color
        @Composable get() = OneConfigTheme.current.chipBackground

    @Composable
    public fun contentColor(selected: Boolean): Color =
        if (selected) OneConfigTheme.current.accentTextColor else OneConfigTheme.current.textColor
}

@ExperimentalOneConfigUiApi
@Composable
public fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true,
    size: ControlSize = ChipDefaults.size,
    role: A11yRole = A11yRole.Button,
    contentDescription: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val active = hovered && enabled
    val rest = ChipDefaults.fill
    val hoverFill = OneConfigTheme.accent.copy(alpha = ChipDefaults.HOVER_ACCENT_ALPHA).compositeOver(rest)
    val fill by animateColorAsState(if (active) hoverFill else rest, Motion.spec(tween(CHIP_FADE_MILLIS)))
    val accentAlpha by animateFloatAsState(if (selected) 1f else 0f, Motion.spec(tween(CHIP_FADE_MILLIS)))
    val content by animateColorAsState(ChipDefaults.contentColor(selected), Motion.spec(tween(CHIP_FADE_MILLIS)))
    val border = theme.borderColor
    val shape = ChipDefaults.shape
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .heightIn(min = size.height)
            .glassFrame(shape, { fill }, { border }, { accentAlpha }, hover = enabled)
            .oneConfigClickable(
                contentDescription ?: label,
                onClick,
                role = role,
                enabled = enabled,
                selected = selected,
                shape = shape,
                indication = !glassAvailable,
                interactionSource = source,
            )
            .padding(horizontal = ChipDefaults.horizontalPadding(size)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChipDefaults.contentSpacing(size), Alignment.CenterHorizontally),
    ) {
        when {
            leading != null -> leading()
            icon != null -> Icon(icon, content, Modifier.size(ChipDefaults.iconSize(size)))
        }
        Text(label, Modifier, content, ChipDefaults.fontSize(size), null, BaselineShift.None, FontWeight.Normal, TextAlign.Center, 1, TextOverflow.Ellipsis)
        trailing?.invoke()
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Chip(label, selected, onClick, Modifier)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun Chip(label: String, selected: Boolean, icon: String?, onClick: () -> Unit) {
    Chip(label, selected, onClick, Modifier, icon = icon)
}
