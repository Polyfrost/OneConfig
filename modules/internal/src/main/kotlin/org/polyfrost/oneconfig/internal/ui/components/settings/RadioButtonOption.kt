package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.RadioGroup
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.ui.api.settings.RadioButtonOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

private val RadioPadding = 3.dp
private val RadioShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val RadioItemShape @Composable get() = RadioShape.concentric(RadioPadding)
private val RadioItemMinHeight = OptionControlHeight - RadioPadding * 2

@Composable
fun RadioButtonOption(data: RadioButtonOptionData) {
    RadioButtonOption(data, true)
}

@Composable
fun RadioButtonOption(data: RadioButtonOptionData, enabled: Boolean) {
    val enumClass = when {
        data.prop.type.isEnum -> data.prop.type
        data.prop.type.superclass?.isEnum == true -> data.prop.type.superclass
        else -> null
    }
    val values: List<Any?> = when {
        enumClass != null -> enumClass.enumConstants.toList()
        else -> emptyList()
    }
    val languageStamp = languageStamp()
    val options: List<Any> = remember(data.prop, languageStamp) {
        when {
            enumClass != null -> values.mapIndexed { index, value ->
                data.options?.getOrNull(index) ?: (value as Enum<*>).name
            }
            data.options != null -> data.options!!
            else -> emptyList()
        }
    }

    val initialValue = data.prop.shown()
    var selectedIdx by remember(data.prop) {
        mutableStateOf(if (initialValue is Enum<*>) initialValue.ordinal else initialValue as? Int ?: 0)
    }

    val select: (Int) -> Unit = { index ->
        selectedIdx = index
        @Suppress("UNCHECKED_CAST")
        if (enumClass != null) {
            (data.prop as Property<Any>).set(values[index])
        } else {
            (data.prop as Property<Any>).set(index)
        }
    }
    val labels = remember(options, languageStamp) { options.map { it.asPlainLabel() } }
    val styled = remember(options, languageStamp) { options.styledLabelsOrNull() }
    RadioGroup(
        selected = selectedIdx,
        options = labels.indices.toList(),
        onSelect = select,
        modifier = Modifier.widthIn(max = LocalOptionWidth.current).liquidGlass(RadioShape),
        label = { labels[it] },
        orientation = Orientation.Horizontal,
        enabled = enabled,
        styledLabel = styled?.let { list -> { list[it] } },
    )
}

/**
 * [options] can be anything [Text] can show, such as translated text, or null to show only the icon
 *
 * [fillWidth] stretches the row to the width it is given and splits it evenly between the options
 *
 * [icons] when given are drawn before each option's text
 */
@Composable
fun Radio(
    options: List<Any?>,
    selected: Int,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    icons: List<String>? = null,
    onSelect: (Int) -> Unit,
) {
    val theme = LocalTheme.current
    val density = LocalDensity.current
    val fill = if (fillWidth) Modifier.fillMaxWidth() else Modifier

    // each item's width is measured so the indicator can slide between them
    val itemWidths = remember(options.size) { Array(options.size) { mutableIntStateOf(0) } }
    var rowHeight by remember(options.size) { mutableIntStateOf(0) }
    val indicatorReady = selected in itemWidths.indices && itemWidths[selected].intValue > 0

    Box(
        modifier = modifier
            .then(fill)
            .liquidGlass(RadioShape)
            .border(1.dp, theme.borderColor, RadioShape)
            .clip(RadioShape)
            .background(theme.componentBackground),
    ) {
        Box(
            modifier = fill.padding(RadioPadding)
        ) {
            if (indicatorReady) {
                val indicatorOffset = Motion.animateDpAsState(with(density) { itemWidths.take(selected).sumOf { it.intValue }.toDp() }, spring())
                val indicatorWidth = Motion.animateDpAsState(with(density) { itemWidths[selected].intValue.toDp() }, spring())
                val indicatorHeight = Motion.animateDpAsState(with(density) { rowHeight.toDp() }.coerceAtLeast(RadioItemMinHeight), spring())
                Box(
                    modifier = Modifier
                        .layout { measurable, _ ->
                            val w = indicatorWidth.value.roundToPx()
                            val h = indicatorHeight.value.roundToPx()
                            val placeable = measurable.measure(Constraints.fixed(w, h))
                            layout(w, h) { placeable.place(indicatorOffset.value.roundToPx(), 0) }
                        }
                        .clip(RadioItemShape)
                        .background(Accent),
                )
            }

            Row(
                modifier = fill.onSizeChanged { rowHeight = it.height },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                options.forEachIndexed { index, option ->
                    val interactionSource = rememberInteractionSource()
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    val isSelected = index == selected
                    val color = Motion.animateColorAsState(if (isSelected || isHovered) theme.textColor else theme.textColorSecondary)
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = fillWidth)
                            .onSizeChanged { itemWidths[index].intValue = it.width }
                            .oneConfigClickable(
                                option?.asRenderText() ?: icons?.getOrNull(index),
                                { onSelect(index) },
                                role = A11yRole.RadioButton,
                                selected = isSelected,
                                indication = false,
                                interactionSource = interactionSource,
                            )
                            .hoverable(interactionSource)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .heightIn(min = RadioItemMinHeight)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            icons?.getOrNull(index)?.let { Icon(it, modifier = Modifier.size(16.dp), tint = { color.value }) }
                            if (option != null) {
                                Text(
                                    option,
                                    color = Color.Unspecified,
                                    fontSize = 12.sp,
                                    colorProducer = { color.value },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
