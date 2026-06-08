package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import org.polyfrost.oneconfig.internal.ui.api.settings.MultiSelectDropdownOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.fadingEdges
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val DropdownShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val ItemShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape

@Suppress("UNCHECKED_CAST")
@Composable
fun MultiSelectDropdownOption(data: MultiSelectDropdownOptionData) {
    val theme = LocalTheme.current

    val options: List<String> = remember(data.prop) {
        data.optionLabels ?: data.options?.toList() ?: emptyList()
    }

    // Checkable (multi-select): property = Array<Boolean> indexed by option
    // Not checkable (single-select): property = Int (selected index, -1 = none)
    var selectedFlags by remember(data.prop) {
        if (data.checkable) {
            val v = data.prop.get()
            val arr: BooleanArray = when (v) {
                is BooleanArray -> v
                is Array<*> -> BooleanArray(options.size) { i -> v.getOrNull(i) == true }
                else -> BooleanArray(options.size)
            }
            mutableStateOf(arr.copyOf())
        } else {
            mutableStateOf(BooleanArray(0))
        }
    }

    var selectedIdx by remember(data.prop) {
        if (!data.checkable) {
            val v = data.prop.get()
            mutableStateOf(if (v is Int) v else -1)
        } else {
            mutableStateOf(-1)
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var triggerHeightPx by remember { mutableStateOf(0) }

    val triggerInteraction = rememberInteractionSource()
    val isHovered by triggerInteraction.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        if (expanded) Accent else theme.borderColor
    )
    val textColor by animateColorAsState(
        if (isHovered || expanded) theme.textColor else theme.textColorSecondary
    )
    val backgroundColor by animateColorAsState(
        if (expanded) Accent.copy(0.2f).compositeOver(theme.componentBackground)
        else theme.componentBackground
    )
    val chevronRotation by animateFloatAsState(if (expanded) 0f else 180f)

    val triggerLabel = when {
        !data.checkable -> options.getOrElse(selectedIdx) { "—" }
        else -> {
            val count = selectedFlags.count { it }
            when {
                count == 0 -> "None selected"
                count == options.size -> "All selected"
                else -> "$count / ${options.size} selected"
            }
        }
    }

    Box {
        Row(
            modifier = Modifier
                .width(300.dp)
                .height(32.dp)
                .onSizeChanged { triggerHeightPx = it.height }
                .background(backgroundColor, DropdownShape)
                .border(1.dp, borderColor, DropdownShape)
                .onClick(triggerInteraction) { expanded = !expanded }
                .hoverable(triggerInteraction)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(triggerLabel, color = textColor, fontSize = 13.sp)
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
                    exit = fadeOut(),
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .clip(DropdownShape)
                            .background(theme.componentBackground, DropdownShape)
                            .border(1.dp, theme.borderColor, DropdownShape)
                            .heightIn(max = 280.dp)
                            .fadingEdges(scrollState, theme.componentBackground)
                            .verticalScroll(scrollState)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        options.forEachIndexed { index, option ->
                            if (data.checkable) {
                                CheckableListItem(
                                    label = option,
                                    icon = data.icon,
                                    checked = selectedFlags.getOrElse(index) { false },
                                    onToggle = {
                                        val updated = selectedFlags.copyOf()
                                        updated[index] = !updated[index]
                                        selectedFlags = updated
                                        (data.prop as Property<Any>).set(updated)
                                    },
                                )
                            } else {
                                PlainListItem(
                                    label = option,
                                    icon = data.icon,
                                    selected = selectedIdx == index,
                                    onSelect = {
                                        selectedIdx = index
                                        (data.prop as Property<Any>).set(index)
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckableListItem(
    label: String,
    icon: String?,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        if (isHovered) theme.modCardBackground else theme.modCardBackground.copy(0f)
    )
    val contentColor by animateColorAsState(
        if (isHovered) theme.textColor else theme.textColorSecondary
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ItemShape)
            .background(bgColor, ItemShape)
            .onClick(interactionSource, onToggle)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        CheckboxIndicator(checked = checked, onToggle = onToggle)

        icon?.takeIf { it.isNotEmpty() }?.let { Icon(it, modifier = Modifier.size(16.dp), color = contentColor) }

        Text(label, color = contentColor, fontSize = 13.sp)
    }
}

@Composable
private fun PlainListItem(
    label: String,
    icon: String?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor by animateColorAsState(
        if (selected) Accent
        else if (isHovered) theme.modCardBackground
        else theme.modCardBackground.copy(0f)
    )
    val contentColor by animateColorAsState(
        if (selected || isHovered) theme.textColor else theme.textColorSecondary
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ItemShape)
            .background(bgColor, ItemShape)
            .onClick(interactionSource, onSelect)
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        icon?.takeIf { it.isNotEmpty() }?.let { Icon(it, modifier = Modifier.size(16.dp), color = contentColor) }

        Text(label, color = contentColor, fontSize = 13.sp)
    }
}
