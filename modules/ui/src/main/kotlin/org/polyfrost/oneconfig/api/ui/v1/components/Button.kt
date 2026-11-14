@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val BARE_FADE_MILLIS = 150

private const val BARE_DISABLED_ALPHA = 0.35f

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(iconName: String, modifier: Modifier, onClick: () -> Unit) {
    BareIconButton(iconName, modifier, OneConfigTheme.current.textColor.copy(alpha = 0.7f), OneConfigTheme.current.textColor, true, false, onClick)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(iconName: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val textColor = OneConfigTheme.current.textColor
    BareIconButton(iconName, modifier, textColor.copy(alpha = 0.7f), textColor, enabled, false, onClick)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(
    iconName: String,
    modifier: Modifier,
    foreground: Color,
    hoveredForeground: Color,
    enabled: Boolean,
    hoverBorder: Boolean,
    onClick: () -> Unit,
) {
    BareIconButton(iconName, modifier, foreground, hoveredForeground, enabled, hoverBorder, onClick)
}

@Composable
private fun BareIconButton(
    iconName: String,
    modifier: Modifier,
    foreground: Color,
    hoveredForeground: Color,
    enabled: Boolean,
    hoverBorder: Boolean,
    onClick: () -> Unit,
) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val tint by Motion.animateColorAsState(
        when {
            !enabled -> theme.textColorSecondary.copy(alpha = BARE_DISABLED_ALPHA)
            hovered -> hoveredForeground
            else -> foreground
        },
        tween(BARE_FADE_MILLIS),
    )
    val outline by Motion.animateColorAsState(
        if (hoverBorder && enabled && hovered) theme.borderColor else theme.borderColor.copy(alpha = 0f),
        tween(BARE_FADE_MILLIS),
    )
    Icon(
        iconName,
        null,
        Modifier
            .onClick(source, enabled, onClick)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .then(modifier)
            .then(if (hoverBorder) Modifier.border(1.dp, outline, theme.buttonShape).padding(4.dp) else Modifier),
        tint,
    )
}
