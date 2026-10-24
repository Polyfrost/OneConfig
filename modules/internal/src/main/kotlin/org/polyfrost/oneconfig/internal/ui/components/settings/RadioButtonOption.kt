package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.*
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateDpAsState
import org.polyfrost.compose.animation.core.spring
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.*
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.layout.onSizeChanged
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.coerceAtLeast
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.min
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.RadioButtonOptionData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.poly.richText
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
    val optionWidth = LocalOptionWidth.current

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

    val value = rememberPropValue(data.prop)
    val selectedIdx = if (value is Enum<*>) value.ordinal else value as? Int ?: 0

    // each item's width is measured so the indicator can slide between them
    val font = theme.typography.family.resolve(FontWeight.Normal)
    val itemWidths = remember(options, font, optionWidth) {
        val measure = PolyFont(font, 12f)
        val share = (optionWidth - RadioPadding * 2) / options.size.coerceAtLeast(1)
        options.map { min((measure.measureTextWidth(richText(it, font, font, font).first) + 16f).dp, share) }
    }
    var rowHeight by remember(options.size) { mutableIntStateOf(0) }
    val indicatorReady = selectedIdx in itemWidths.indices

    Box(
        modifier = Modifier
            .widthIn(max = optionWidth)
            .border(1.dp, theme.borderColor, RadioShape)
            .clip(RadioShape)
            .background(theme.componentBackground),
    ) {
        Box(
            modifier = Modifier.padding(RadioPadding)
        ) {
            if (indicatorReady) {
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidths.take(selectedIdx).fold(0.dp) { acc, w -> acc + w },
                    animationSpec = spring(),
                )
                val indicatorWidth by animateDpAsState(
                    targetValue = itemWidths[selectedIdx],
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
                        .offset(x = indicatorOffset)
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
                    val color = animateColorAsState(
                        if (selected || isHovered) theme.textColor else theme.textColorSecondary
                    )
                    Box(
                        modifier = Modifier
                            .width(itemWidths[index])
                            .onClick(interactionSource) {
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
                            Modifier.widthIn(max = itemWidths[index] - 16.dp),
                            color = color,
                            fontSize = 12.sp,
                            softWrap = true,
                        )
                    }
                }
            }
        }
    }
}
