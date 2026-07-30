package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.DropdownOptionData
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

private val MenuPadding = 4.dp
private val DropdownShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val MenuItemShape @Composable get() = DropdownShape.concentric(MenuPadding)

@Composable
fun DropdownOption(data: DropdownOptionData) {
    val theme = LocalTheme.current
    var expanded by remember { mutableStateOf(false) }
    var triggerHeightPx by remember { mutableStateOf(0) }

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
    val optionValues = data.optionValues ?: options

    val initialValue = data.prop.get()
    var selectedIdx by remember(data.prop) {
        mutableStateOf(
            when (initialValue) {
                is Enum<*> -> initialValue.ordinal
                is String -> optionValues.indexOf(initialValue).takeIf { it >= 0 } ?: 0
                else -> initialValue as? Int ?: 0
            }
        )
    }
    val selectedName = options.getOrElse(selectedIdx) { "—" }

    val triggerInteraction = rememberInteractionSource()
    val isHovered by triggerInteraction.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        if (expanded) Accent
        else theme.borderColor
    )
    val textColor by animateColorAsState(
        if (isHovered || expanded) theme.textColor
        else theme.textColorSecondary
    )
    val backgroundColor by animateColorAsState(
        if (expanded) Accent.copy(0.2f).compositeOver(theme.componentBackground)
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
                .onSizeChanged { triggerHeightPx = it.height }
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
                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                color = textColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon("up", modifier = Modifier.rotate(chevronRotation), color = textColor)
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, triggerHeightPx + 10),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val scrollState = rememberScrollState()
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
                                .fadingEdges(scrollState, theme.componentBackground)
                                .verticalScroll(scrollState)
                                .padding(MenuPadding)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            options.forEachIndexed { index, option ->
                                val optionSource = rememberInteractionSource()
                                val selected = index == selectedIdx
                                val isHovered by optionSource.collectIsHoveredAsState()

                                val backgroundColor by animateColorAsState(
                                    if (selected) Accent
                                    else if (isHovered) theme.modCardBackground
                                    else theme.modCardBackground.copy(0f)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(backgroundColor, itemShape)
                                        .onClick(optionSource) {
                                            selectedIdx = index
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
                    }
                }
            }
        }
    }
}
