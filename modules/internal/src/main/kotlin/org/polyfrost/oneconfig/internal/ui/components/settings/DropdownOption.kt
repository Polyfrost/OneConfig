package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.*
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.*
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.*
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.DropdownOptionData
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.poly.FadingEdges
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.compose.foundation.layout.size

private val MenuPadding = 4.dp
private val DropdownShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val MenuItemShape @Composable get() = DropdownShape.concentric(MenuPadding)

@Composable
fun DropdownOption(data: DropdownOptionData) {
    val theme = LocalTheme.current
    var expanded by remember { mutableStateOf(false) }

    val enumClass = when {
        data.prop.type.isEnum -> data.prop.type
        data.prop.type.superclass?.isEnum == true -> data.prop.type.superclass
        else -> null
    }
    val values: List<Any?> = when {
        enumClass != null -> data.optionValues?.takeIf { it.isNotEmpty() } ?: enumClass.enumConstants.toList()
        else -> emptyList()
    }
    val options: List<Any> = when {
        enumClass != null -> values.mapIndexed { index, value ->
            data.options?.getOrNull(index) ?: (value as Enum<*>).name
        }
        data.options != null -> data.options!!
        else -> emptyList()
    }
    val optionValues = data.optionValues ?: options

    val value = rememberPropValue(data.prop)
    val selectedIdx = when (value) {
        is Enum<*> -> values.indexOf(value).takeIf { it >= 0 } ?: 0
        is String -> optionValues.indexOf(value).takeIf { it >= 0 } ?: 0
        else -> value as? Int ?: 0
    }
    val selectedName = options.getOrElse(selectedIdx) { "—" }

    val triggerInteraction = rememberInteractionSource()
    val isHovered by triggerInteraction.collectIsHoveredAsState()
    val borderColor = animateColorAsState(
        if (expanded) Accent
        else theme.borderColor
    )
    val textColor = animateColorAsState(
        if (isHovered || expanded) theme.textColor
        else theme.textColorSecondary
    )
    val backgroundColor = animateColorAsState(
        if (expanded) theme.componentBackground.lerp(Accent.withAlpha(255), 0.2f)
        else theme.componentBackground
    )
    val chevronRotation by animateFloatAsState(
        if (expanded) 0f else 180f
    )

    Box {
        Row(
            modifier = Modifier
                .width(LocalOptionWidth.current)
                .height(32.dp)
                .background(backgroundColor, DropdownShape)
                .border(
                    1.dp,
                    borderColor,
                    DropdownShape,
                )
                .onClick(triggerInteraction) { expanded = !expanded }
                .hoverable(triggerInteraction)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selectedName,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                color = textColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon("up", textColor, Modifier.size(18.dp), rotation = chevronRotation)
        }

        SimpleDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 10.dp),
        ) {
            val scrollState = rememberScrollState()
            var menuHeight by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = Modifier
                    .width(LocalOptionWidth.current)
                    .clip(DropdownShape)
                    .background(theme.componentBackground, DropdownShape)
                    .border(1.dp, theme.borderColor, DropdownShape)
            ) {
                val itemShape = MenuItemShape
                Column(
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .onPlaced { menuHeight = it.height }
                        .verticalScroll(scrollState)
                        .padding(MenuPadding)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    options.forEachIndexed { index, option ->
                        val optionSource = rememberInteractionSource()
                        val selected = index == selectedIdx
                        val isHovered by optionSource.collectIsHoveredAsState()

                        val backgroundColor = animateColorAsState(
                            if (selected) Accent
                            else if (isHovered) theme.modCardBackground
                            else theme.modCardBackground.copy(0f)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor, itemShape)
                                .onClick(optionSource) {
                                    @Suppress("UNCHECKED_CAST")
                                    if (enumClass != null) {
                                        (data.prop as Property<Any>).set(values[index])
                                    } else if (data.prop.type == String::class.java) {
                                        (data.prop as Property<Any>).set(optionValues.getOrElse(index) { option })
                                    } else {
                                        (data.prop as Property<Any>).set(index)
                                    }
                                    expanded = false
                                }
                                .hoverable(optionSource)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(option, color = theme.textColor, fontSize = 13.sp)
                        }
                    }
                }
                FadingEdges(scrollState, theme.componentBackground, Modifier.fillMaxWidth().height(menuHeight.dp))
            }
        }
    }
}
