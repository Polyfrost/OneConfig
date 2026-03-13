package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.DropdownOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val DropdownShape = RoundedCornerShape(6.dp)

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
    val options: List<String> = when {
        enumClass != null -> enumClass.enumConstants.map { (it as Enum<*>).name }
        data.options != null -> data.options!!.toList()
        else -> emptyList()
    }

    val initialValue = data.prop.get()
    var selectedIdx by remember(data.prop) {
        mutableStateOf(if (initialValue is Enum<*>) initialValue.ordinal else initialValue as? Int ?: 0)
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
                .width(300.dp)
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
            Text(selectedName, color = textColor, fontSize = 13.sp)
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
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .background(theme.componentBackground, theme.sideBarNavigationEntryShape)
                            .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape)
                            .clip(theme.sideBarNavigationEntryShape)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            options.forEachIndexed { index, option ->
                                val optionSource = rememberInteractionSource()
                                val selected = index == selectedIdx
                                val isHovered by optionSource.collectIsHoveredAsState()

                                val backgroundColor by animateColorAsState(
                                    if (selected) Accent
                                    else if (isHovered) theme.componentBackground.copy(1f)
                                    else theme.componentBackground.copy(0f)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(backgroundColor, theme.sideBarNavigationEntryShape)
                                        .onClick(optionSource) {
                                            selectedIdx = index
                                            @Suppress("UNCHECKED_CAST")
                                            if (enumClass != null) {
                                                (data.prop as Property<Any>).set(enumClass.enumConstants[index])
                                            } else {
                                                (data.prop as Property<Any>).set(index)
                                            }
                                            expanded = false
                                        }
                                        .hoverable(optionSource)
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
