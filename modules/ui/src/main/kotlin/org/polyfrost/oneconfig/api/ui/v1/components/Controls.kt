@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.ui.v1.a11y.isKeyboardMode
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val CHECK_SIZE = 20.dp
private val LEGACY_ICON_BUTTON_SIZE = 34.dp

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
            .oneConfigClickable(label ?: tooltip, onClick, shape = shape, indication = false, interactionSource = source),
        contentAlignment = Alignment.Center,
    ) {
        glyph()
        if (tooltip != null && (hovered || focused && keyboard)) TooltipPopup(tooltip, Modifier, placement)
    }
}

@ExperimentalOneConfigUiApi
public enum class ToggleControl {
    Switch,

    Checkbox,
}

@ExperimentalOneConfigUiApi
public object ToggleRowDefaults {
    public val size: ControlSize = ControlSize.Large

    public val horizontalPadding: Dp = 12.dp

    public val verticalPadding: Dp = 6.dp

    public val contentSpacing: Dp = 12.dp
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
    control: ToggleControl = ToggleControl.Checkbox,
    framed: Boolean = true,
    size: ControlSize = ToggleRowDefaults.size,
    interactionSource: MutableInteractionSource? = null,
    trackColor: Color = Color.Unspecified,
    contentPadding: PaddingValues? = null,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val shape = theme.buttonShape
    val direction = LocalLayoutDirection.current
    val flush = contentPadding != null &&
        (contentPadding.calculateStartPadding(direction) == 0.dp || contentPadding.calculateEndPadding(direction) == 0.dp)
    val rowBorder by Motion.animateColorAsState(if (hovered && enabled) theme.textColorSecondary else theme.borderColor, label = "toggleRowBorder")
    val frame = if (framed) {
        Modifier
            .clip(shape)
            .background(theme.componentBackground, shape)
            .border(1.dp, rowBorder, shape)
    } else {
        Modifier
    }
    RowLayout(
        label,
        description,
        size,
        modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .then(frame)
            .oneConfigClickable(
                if (description != null) "$label, $description" else label,
                { onCheckedChange(!checked) },
                role = if (control == ToggleControl.Switch) A11yRole.Switch else A11yRole.Checkbox,
                enabled = enabled,
                checked = checked,
                shape = shape,
                indication = !framed,
                interactionSource = source,
                recipe = when {
                    flush -> ControlDefaults.Item
                    else -> ControlDefaults.Framed
                },
            ),
        contentPadding,
    ) {
        when (control) {
            ToggleControl.Switch -> SwitchTrack(checked, hovered && enabled, trackColor, Modifier)
            ToggleControl.Checkbox -> CheckboxBox(checked, hovered && enabled, Modifier)
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ControlRow(
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    size: ControlSize = ToggleRowDefaults.size,
    trailing: @Composable RowScope.() -> Unit,
) {
    val theme = OneConfigTheme.current
    RowLayout(
        label,
        description,
        size,
        modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha },
        null,
        trailing,
    )
}

@Composable
private fun RowLayout(
    label: String,
    description: String?,
    size: ControlSize,
    modifier: Modifier,
    contentPadding: PaddingValues?,
    trailing: @Composable RowScope.() -> Unit,
) {
    val theme = OneConfigTheme.current
    val typography = theme.typography
    val inset = if (contentPadding != null) {
        Modifier.padding(contentPadding)
    } else {
        Modifier
            .heightIn(min = size.height)
            .padding(
                horizontal = ToggleRowDefaults.horizontalPadding,
                vertical = if (description != null) ToggleRowDefaults.verticalPadding else 0.dp,
            )
    }
    Row(
        modifier = modifier.then(inset),
        horizontalArrangement = Arrangement.spacedBy(ToggleRowDefaults.contentSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(theme.spacing.xxs)) {
            Text(label, Modifier, theme.textColor, if (size == ControlSize.Compact) typography.body.copy(fontSize = 13.sp) else typography.body, textAlign = TextAlign.Start)
            if (description != null) {
                Text(description, Modifier, theme.textColorSecondary, typography.small, textAlign = TextAlign.Start)
            }
        }
        trailing()
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
