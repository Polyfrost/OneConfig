@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.ui.v1.a11y.isKeyboardMode
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.hoverable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val CHECK_SIZE = 20.dp
private val LEGACY_ICON_BUTTON_SIZE = 34.dp
private const val FADE_MILLIS = 150

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(iconName: String, tooltip: String?, modifier: Modifier, onClick: () -> Unit) {
    LegacyIconButton(iconName, tooltip, modifier, TooltipPlacement.BelowStart, onClick, null, OneConfigTheme.current.textColor, null)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(
    iconName: String,
    tooltip: String?,
    label: String?,
    modifier: Modifier,
    background: Color?,
    tint: Color,
    placement: TooltipPlacement,
    onClick: () -> Unit,
) {
    LegacyIconButton(iconName, tooltip, modifier, placement, onClick, label, tint, background)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(
    tooltip: String?,
    label: String?,
    modifier: Modifier,
    background: Color?,
    placement: TooltipPlacement,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    LegacyIconButtonFrame(tooltip, label, modifier, background, placement, onClick, glyph)
}

@Composable
private fun LegacyIconButton(
    iconName: String,
    tooltip: String?,
    modifier: Modifier,
    placement: TooltipPlacement,
    onClick: () -> Unit,
    label: String?,
    tint: Color,
    background: Color?,
) {
    LegacyIconButtonFrame(tooltip, label, modifier, background, placement, onClick) {
        Icon(iconName, null, Modifier.size(16.dp), tint)
    }
}

@Composable
private fun LegacyIconButtonFrame(
    tooltip: String?,
    label: String?,
    modifier: Modifier,
    background: Color?,
    placement: TooltipPlacement,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val keyboard = isKeyboardMode()
    val fill by animateColorAsState(background ?: Color.Transparent)
    val hairline by animateColorAsState(theme.borderColor.copy(alpha = if (hovered) theme.borderColor.alpha else 0f))
    val shape = theme.buttonShape
    Box(
        modifier = modifier
            .size(LEGACY_ICON_BUTTON_SIZE)
            .clip(shape)
            .background(fill, shape)
            .border(1.dp, hairline, shape)
            .hoverable(source)
            .onFocusChanged { focused = it.isFocused }
            .a11y(label ?: tooltip, A11yRole.Button, null, null, shape, true)
            .onClick(source, onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        glyph()
        if (tooltip != null && (hovered || focused && keyboard)) TooltipPopup(tooltip, Modifier, placement)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    size: ControlSize = ControlSize.Large,
    interactionSource: MutableInteractionSource? = null,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val active = hovered && enabled
    val spec = Motion.spec(tween<Color>(FADE_MILLIS))
    val rowBorder by animateColorAsState(if (active) theme.textColorSecondary else theme.borderColor, spec)
    val shape = theme.buttonShape
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = size.height)
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .clip(shape)
            .background(theme.componentBackground, shape)
            .border(1.dp, rowBorder, shape)
            .oneConfigClickable(
                label,
                { onCheckedChange(!checked) },
                role = A11yRole.Checkbox,
                enabled = enabled,
                checked = checked,
                shape = shape,
                indication = false,
                interactionSource = source,
            )
            .padding(horizontal = 12.dp, vertical = if (description != null) 6.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, Modifier, theme.textColor, OneConfigTheme.typography.body.copy(fontSize = if (size == ControlSize.Compact) 13.sp else 14.sp, fontWeight = FontWeight.Normal))
            if (description != null) {
                Text(description, Modifier, theme.textColorSecondary, OneConfigTheme.typography.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal))
            }
        }
        CheckboxBox(checked, active, Modifier)
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ToggleRow(label: String, checked: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    ToggleRow(label, checked, { onToggle() }, modifier)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun <T> TabRow(tabs: List<TabItem<T>>, selected: T, modifier: Modifier, onSelect: (T) -> Unit) {
    TabRow(tabs, selected, onSelect, modifier, size = ControlSize.Large)
}
