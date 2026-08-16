package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.RadioButtonOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

private val RadioPadding = 3.dp
private val RadioShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val RadioItemShape @Composable get() = RadioShape.concentric(RadioPadding)
private val RadioItemMinHeight = 32.dp

@Composable
fun RadioButtonOption(data: RadioButtonOptionData) {
    val theme = LocalTheme.current
    val density = LocalDensity.current

    val enumClass = when {
        data.prop.type.isEnum -> data.prop.type
        data.prop.type.superclass?.isEnum == true -> data.prop.type.superclass
        else -> null
    }
    val values: List<Any?> = when {
        enumClass != null -> enumClass.enumConstants.toList()
        else -> emptyList()
    }
    val options: List<Any> = when {
        enumClass != null -> values.mapIndexed { index, value ->
            data.options?.getOrNull(index) ?: (value as Enum<*>).name
        }
        data.options != null -> data.options!!
        else -> emptyList()
    }

    val initialValue = data.prop.get()
    var selectedIdx by remember(data.prop) {
        mutableStateOf(if (initialValue is Enum<*>) initialValue.ordinal else initialValue as? Int ?: 0)
    }

    // each item's width is measured so the indicator can slide between them
    val itemWidths = remember(options.size) { Array(options.size) { mutableIntStateOf(0) } }
    var rowHeight by remember(options.size) { mutableIntStateOf(0) }
    val indicatorReady = selectedIdx in itemWidths.indices && itemWidths[selectedIdx].intValue > 0

    Box(
        modifier = Modifier
            .widthIn(max = LocalOptionWidth.current)
            .border(1.dp, theme.borderColor, RadioShape)
            .clip(RadioShape)
            .background(theme.componentBackground),
    ) {
        Box(
            modifier = Modifier.padding(RadioPadding)
        ) {
            if (indicatorReady) {
                val indicatorOffset by animateDpAsState(
                    targetValue = with(density) { itemWidths.take(selectedIdx).sumOf { it.intValue }.toDp() },
                    animationSpec = spring(),
                )
                val indicatorWidth by animateDpAsState(
                    targetValue = with(density) { itemWidths[selectedIdx].intValue.toDp() },
                    animationSpec = spring(),
                )
                val indicatorHeight by animateDpAsState(
                    targetValue = with(density) { rowHeight.toDp() }.coerceAtLeast(RadioItemMinHeight),
                    animationSpec = spring(),
                )
                Box(
                    modifier = Modifier
                        .height(indicatorHeight)
                        .width(indicatorWidth)
                        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                        .clip(RadioItemShape)
                        .background(Accent),
                )
            }

            Row(
                modifier = Modifier.onSizeChanged { rowHeight = it.height },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                options.forEachIndexed { index, option ->
                    val interactionSource = rememberInteractionSource()
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    val selected = index == selectedIdx
                    val color by animateColorAsState(
                        if (selected || isHovered) theme.textColor else theme.textColorSecondary
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .onSizeChanged { itemWidths[index].intValue = it.width }
                            .onClick(interactionSource) {
                                selectedIdx = index
                                @Suppress("UNCHECKED_CAST")
                                if (enumClass != null) {
                                    (data.prop as Property<Any>).set(values[index])
                                } else {
                                    (data.prop as Property<Any>).set(index)
                                }
                            }
                            .hoverable(interactionSource)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .heightIn(min = RadioItemMinHeight)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            option,
                            color = color,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}
