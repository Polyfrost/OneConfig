@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.concentric

private val ITEM_MIN_HEIGHT = ControlSize.Regular.height - ChoiceInset * 2
private val ITEM_ICON_SIZE = 16.dp

@ExperimentalOneConfigUiApi
@Composable
public fun <T> RadioGroup(
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    orientation: Orientation = Orientation.Vertical,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    styledLabel: ((T) -> AnnotatedString)? = null,
    fillWidth: Boolean = false,
    shape: Shape = OneConfigTheme.current.shapes.field,
    icon: ((T) -> String?)? = null,
    activation: TabActivation = TabActivation.Automatic,
) {
    val theme = OneConfigTheme.current
    val itemShape = shape.concentric(ChoiceInset)
    val vertical = orientation == Orientation.Vertical
    val indicator = rememberSlidingIndicatorState(options.size)
    val focus = rememberChoiceFocus(options.size)
    val selectedIndex = options.indexOf(selected)
    val fill = if (fillWidth) Modifier.fillMaxWidth() else Modifier

    Box(
        modifier = modifier
            .then(fill)
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .groupInteractions(interactionSource, enabled)
            .border(1.dp, theme.borderColor, shape)
            .clip(shape)
            .background(theme.componentBackground)
            .choiceGroup(focus, selectedIndex, enabled, { true }, activation, orientation) { onSelect(options[it]) },
    ) {
        Box(fill.padding(ChoiceInset).slidingIndicatorContainer(indicator)) {
            SlidingIndicator(indicator, selectedIndex, Modifier.clip(itemShape).background(OneConfigTheme.accent))
            val items: @Composable (Modifier) -> Unit = { itemModifier ->
                options.forEachIndexed { index, option ->
                    RadioItem(
                        text = label(option),
                        styled = styledLabel?.invoke(option),
                        icon = icon?.invoke(option),
                        selected = index == selectedIndex,
                        enabled = enabled,
                        shape = itemShape,
                        vertical = vertical,
                        modifier = itemModifier
                            .slidingIndicatorItem(indicator, index)
                            .choiceItem(focus, index),
                        onClick = { onSelect(option) },
                    )
                }
            }
            if (vertical) {
                Column(if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(IntrinsicSize.Max)) { items(Modifier.fillMaxWidth()) }
            } else {
                Row(fill, verticalAlignment = Alignment.CenterVertically) {
                    items(if (fillWidth) Modifier.weight(1f) else Modifier)
                }
            }
        }
    }
}

@Composable
private fun RadioItem(
    text: String,
    styled: AnnotatedString?,
    icon: String?,
    selected: Boolean,
    enabled: Boolean,
    shape: Shape,
    vertical: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val theme = OneConfigTheme.current
    val style = theme.typography.small
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val color by Motion.animateColorAsState(
        if (selected || (hovered && enabled)) theme.textColor else theme.textColorSecondary,
        label = "radioItemText",
    )
    Box(
        modifier = modifier
            .oneConfigClickable(
                text.ifEmpty { icon.orEmpty() },
                onClick,
                role = A11yRole.RadioButton,
                enabled = enabled,
                selected = selected,
                shape = shape,
                indication = false,
                interactionSource = source,
            )
            .heightIn(min = ITEM_MIN_HEIGHT)
            .padding(horizontal = theme.spacing.m, vertical = theme.spacing.xs),
        contentAlignment = if (vertical) Alignment.CenterStart else Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, null, Modifier.size(ITEM_ICON_SIZE), tint = color)
            if (styled != null || text.isNotEmpty()) {
                val textStyle = OneConfigTheme.typography.body.copy(fontSize = style.fontSize, fontWeight = style.fontWeight ?: FontWeight.Normal)
                val align = if (vertical) TextAlign.Start else TextAlign.Center
                if (styled != null) Text(styled, Modifier, color, textStyle, 1, TextOverflow.Ellipsis, textAlign = align)
                else Text(text, Modifier, color, textStyle, 1, TextOverflow.Ellipsis, textAlign = align)
            }
        }
    }
}
