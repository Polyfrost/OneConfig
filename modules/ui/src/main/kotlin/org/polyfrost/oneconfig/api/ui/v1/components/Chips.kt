@file:OptIn(ExperimentalOneConfigUiApi::class, ExperimentalVersionOverloading::class)

package org.polyfrost.oneconfig.api.ui.v1.components

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

@ExperimentalOneConfigUiApi
public object ChipDefaults {
    public val size: ControlSize = ControlSize.Regular

    public const val HOVER_ACCENT_ALPHA: Float = 0.5f

    public val shape: Shape
        @Composable get() = OneConfigTheme.current.shapes.field

    public val indication: ControlIndication = ControlDefaults.Framed

    public fun contentSpacing(size: ControlSize): Dp = when (size) {
        ControlSize.Compact -> 6.dp
        ControlSize.Regular, ControlSize.Large -> 10.dp
        ControlSize.ExtraLarge -> 12.dp
    }

    public fun horizontalPadding(size: ControlSize): Dp = when (size) {
        ControlSize.Compact -> 10.dp
        ControlSize.Regular, ControlSize.Large -> 12.dp
        ControlSize.ExtraLarge -> ControlDefaults.metrics(size).horizontalPadding
    }

    public fun fontSize(size: ControlSize): TextUnit = when (size) {
        ControlSize.Compact -> 13.sp
        ControlSize.Regular, ControlSize.Large -> 14.sp
        ControlSize.ExtraLarge -> ControlDefaults.metrics(size).fontSize
    }

    public fun iconSize(size: ControlSize): Dp = when (size) {
        ControlSize.Compact -> 14.dp
        ControlSize.Regular, ControlSize.Large -> 16.dp
        ControlSize.ExtraLarge -> ControlDefaults.metrics(size).iconSize
    }

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
    @IntroducedAt("1.8.0") modifier: Modifier = Modifier,
    @IntroducedAt("1.8.0") icon: String? = null,
    @IntroducedAt("1.8.0") enabled: Boolean = true,
    @IntroducedAt("1.8.0") size: ControlSize = ChipDefaults.size,
    @IntroducedAt("1.8.0") role: A11yRole = A11yRole.Button,
    @IntroducedAt("1.8.0") contentDescription: String? = null,
    @IntroducedAt("1.8.0") leading: (@Composable () -> Unit)? = null,
    @IntroducedAt("1.8.0") trailing: (@Composable () -> Unit)? = null,
    @IntroducedAt("1.8.0") interactionSource: MutableInteractionSource? = null,
    @IntroducedAt("1.8.0") sound: UiSound? = UiSound.Click,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val rest = ChipDefaults.fill
    val hoverFill = OneConfigTheme.accent.copy(alpha = ChipDefaults.HOVER_ACCENT_ALPHA).compositeOver(rest)
    val fill by Motion.animateColorAsState(if (hovered && enabled) hoverFill else rest, label = "chipFill")
    val accentAlpha by Motion.animateFloatAsState(if (selected) 1f else 0f, label = "chipAccent")
    val content by Motion.animateColorAsState(ChipDefaults.contentColor(selected), label = "chipContent")
    val border = theme.borderColor
    val shape = ChipDefaults.shape
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .heightIn(min = ControlDefaults.metrics(size).height)
            .glassFrame(shape, { fill }, { border }, { accentAlpha }, hover = enabled)
            .oneConfigClickable(
                contentDescription ?: label,
                onClick,
                role = role,
                enabled = enabled,
                selected = selected,
                shape = shape,
                indication = !glassAvailable,
                sound = sound,
                interactionSource = source,
            )

            .padding(horizontal = ChipDefaults.horizontalPadding(size)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChipDefaults.contentSpacing(size), Alignment.CenterHorizontally),
    ) {
        when {
            leading != null -> leading()
            icon != null -> Icon(icon, null, Modifier.size(ChipDefaults.iconSize(size)), tint = content)
        }
        Text(
            label,
            Modifier,
            content,
            OneConfigTheme.typography.body.copy(fontSize = ChipDefaults.fontSize(size), fontWeight = FontWeight.Normal),
            1,
            TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        trailing?.invoke()
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun Chip(label: String, selected: Boolean, icon: String?, onClick: () -> Unit) {
    Chip(label, selected, onClick, Modifier, icon = icon)
}
