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
private const val FADE_MILLIS = 150

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(iconName: String, tooltip: String?, modifier: Modifier, onClick: () -> Unit) {
    IconButton(iconName, tooltip ?: iconName, onClick, modifier, tooltip = tooltip, tooltipPlacement = TooltipPlacement.BelowStart)
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
    IconButton(
        iconName,
        label ?: tooltip ?: iconName,
        onClick,
        modifier,
        style = if (background != null) IconButtonStyle.Framed else IconButtonStyle.Ghost,
        tooltip = tooltip,
        tooltipPlacement = placement,
        tint = tint,
    )
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
    IconButtonFrame(
        label ?: tooltip.orEmpty(),
        onClick,
        modifier,
        true,
        if (background != null) IconButtonStyle.Framed else IconButtonStyle.Ghost,
        ControlSize.Regular,
        false,
        false,
        tooltip,
        placement,
        null,
    ) { glyph() }
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
    val accent = OneConfigTheme.accent
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
            Text(label, Modifier, theme.textColor, if (size == ControlSize.Compact) 13.sp else 14.sp, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
            if (description != null) {
                Text(description, Modifier, theme.textColorSecondary, 12.sp, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
            }
        }
        val boxColor by animateColorAsState(if (checked) accent else theme.componentBackground, spec)
        val boxBorder by animateColorAsState(
            when {
                checked -> accent
                active -> theme.textColorSecondary
                else -> theme.borderColor
            },
            spec,
        )
        Box(
            modifier = Modifier
                .size(CHECK_SIZE)
                .clip(theme.checkBoxShape)
                .background(boxColor, theme.checkBoxShape)
                .border(1.5.dp, boxBorder, theme.checkBoxShape),
            contentAlignment = Alignment.Center,
        ) {
            this@Row.AnimatedVisibility(visible = checked, enter = Motion.enter(fadeIn()), exit = Motion.exit(fadeOut())) {
                val tick = theme.accentTextColor
                Canvas(Modifier.size(12.dp)) {
                    val w = this.size.width
                    val h = this.size.height
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.52f)
                        lineTo(w * 0.42f, h * 0.72f)
                        lineTo(w * 0.8f, h * 0.3f)
                    }
                    drawPath(path, color = tick, style = Stroke(width = w * 0.16f, cap = StrokeCap.Round))
                }
            }
        }
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
