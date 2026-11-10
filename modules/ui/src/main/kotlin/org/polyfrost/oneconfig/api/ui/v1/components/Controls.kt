@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.isKeyboardMode
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val ICON_BUTTON_SIZE = 34.dp
private val TOGGLE_HEIGHT = 38.dp
private val CHECK_SIZE = 20.dp

@ExperimentalOneConfigUiApi
@Composable
public fun IconButton(iconName: String, tooltip: String?, modifier: Modifier, onClick: () -> Unit) {
    IconButton(iconName, tooltip, null, modifier, null, OneConfigTheme.current.textColor, TooltipPlacement.BelowStart, onClick)
}

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
    IconButton(tooltip, label, modifier, background, placement, onClick) {
        Icon(iconName, tint, Modifier.size(16.dp))
    }
}

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
            .size(ICON_BUTTON_SIZE)
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
        if (tooltip != null && (hovered || focused && keyboard)) TooltipPopup(tooltip, placement)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ToggleRow(label: String, checked: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val rowBorder by animateColorAsState(if (hovered) theme.textColorSecondary else theme.borderColor)
    val shape = theme.buttonShape
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TOGGLE_HEIGHT)
            .clip(shape)
            .background(theme.componentBackground, shape)
            .border(1.dp, rowBorder, shape)
            .hoverable(source)
            .a11y(label, A11yRole.Checkbox, null, checked, shape, true)
            .onClick(source, onToggle)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), theme.textColor, 14.sp, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
        val boxColor by animateColorAsState(if (checked) accent else theme.componentBackground)
        val boxBorder by animateColorAsState(
            when {
                checked -> accent
                hovered -> theme.textColorSecondary
                else -> theme.borderColor
            },
        )
        Box(
            modifier = Modifier
                .size(CHECK_SIZE)
                .clip(theme.checkBoxShape)
                .background(boxColor, theme.checkBoxShape)
                .border(1.5.dp, boxBorder, theme.checkBoxShape),
            contentAlignment = Alignment.Center,
        ) {
            this@Row.AnimatedVisibility(visible = checked, enter = fadeIn(), exit = fadeOut()) {
                val tick = theme.accentTextColor
                Canvas(Modifier.size(12.dp)) {
                    val w = size.width
                    val h = size.height
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

@ExperimentalOneConfigUiApi
public class TabItem<T>(public val value: T, public val label: String)

@ExperimentalOneConfigUiApi
@Composable
public fun <T> TabRow(tabs: List<TabItem<T>>, selected: T, modifier: Modifier, onSelect: (T) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { tab ->
            val isSelected = tab.value == selected
            TabButton(tab.label, isSelected, Modifier.weight(1f)) { onSelect(tab.value) }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val fill by animateColorAsState(if (selected) accent else theme.componentBackground)
    val border by animateColorAsState(
        when {
            selected -> accent
            hovered -> theme.textColorSecondary
            else -> theme.borderColor
        },
    )
    val shape = theme.buttonShape
    Box(
        modifier = modifier
            .heightIn(min = TOGGLE_HEIGHT)
            .clip(shape)
            .background(fill, shape)
            .border(1.dp, border, shape)
            .hoverable(source)
            .a11y(label, A11yRole.Tab, selected, null, shape, true)
            .onClick(source, onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        val content = if (selected) theme.accentTextColor else theme.textColor
        Text(label, Modifier, content, 14.sp, null, BaselineShift.None, FontWeight.Normal, TextAlign.Center, 1, TextOverflow.Ellipsis)
    }
}
