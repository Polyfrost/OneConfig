package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.heightIn
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.foundation.verticalScroll
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
import org.polyfrost.oneconfig.internal.ui.api.settings.MultiSelectDropdownOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.poly.FadingEdges
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

private val MenuPadding = 4.dp
private val DropdownShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val ItemShape @Composable get() = DropdownShape.concentric(MenuPadding)

@Suppress("UNCHECKED_CAST")
@Composable
fun MultiSelectDropdownOption(data: MultiSelectDropdownOptionData) {
    val theme = LocalTheme.current

    val options: List<Any> = remember(data.prop) {
        data.optionLabels ?: data.options?.toList() ?: emptyList()
    }

    // checkable means the property is an Array<Boolean> indexed by option
    // otherwise it is an Int holding the selected index where -1 is none
    val value = rememberPropValue(data.prop)
    val selectedFlags: BooleanArray = when (value) {
        is BooleanArray -> value
        is Array<*> -> BooleanArray(options.size) { i -> value.getOrNull(i) == true }
        else -> BooleanArray(options.size)
    }
    val selectedIdx = value as? Int ?: -1

    var expanded by remember { mutableStateOf(false) }

    val triggerInteraction = rememberInteractionSource()
    val isHovered by triggerInteraction.collectIsHoveredAsState()

    val borderColor = animateColorAsState(
        if (expanded) Accent else theme.borderColor
    )
    val textColor = animateColorAsState(
        if (isHovered || expanded) theme.textColor else theme.textColorSecondary
    )
    val backgroundColor = animateColorAsState(
        if (expanded) theme.componentBackground.lerp(Accent.withAlpha(255), 0.2f)
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
                .width(LocalOptionWidth.current)
                .height(32.dp)
                .background(backgroundColor, DropdownShape)
                .border(1.dp, borderColor, DropdownShape)
                .onClick(triggerInteraction) { expanded = !expanded }
                .hoverable(triggerInteraction)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                triggerLabel,
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .onPlaced { menuHeight = it.height }
                        .verticalScroll(scrollState)
                        .padding(MenuPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (data.checkable && options.size > 1) {
                        val allSelected = selectedFlags.count { it } == options.size
                        CheckableListItem(
                            label = if (allSelected) "Deselect all" else "Select all",
                            icon = null,
                            checked = allSelected,
                            onToggle = { (data.prop as Property<Any>).set(BooleanArray(options.size) { !allSelected }) },
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.borderColor))
                    }
                    options.forEachIndexed { index, option ->
                        if (data.checkable) {
                            CheckableListItem(
                                label = option,
                                icon = data.icon,
                                checked = selectedFlags.getOrElse(index) { false },
                                onToggle = {
                                    val updated = selectedFlags.copyOf(options.size)
                                    updated[index] = !updated[index]
                                    (data.prop as Property<Any>).set(updated)
                                },
                            )
                        } else {
                            PlainListItem(
                                label = option,
                                icon = data.icon,
                                selected = selectedIdx == index,
                                onSelect = {
                                    (data.prop as Property<Any>).set(index)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                FadingEdges(scrollState, theme.componentBackground, Modifier.fillMaxWidth().height(menuHeight.dp))
            }
        }
    }
}

@Composable
private fun CheckableListItem(
    label: Any,
    icon: String?,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = animateColorAsState(
        if (isHovered) theme.modCardBackground else theme.modCardBackground.copy(0f)
    )
    val contentColor = animateColorAsState(
        if (isHovered) theme.textColor else theme.textColorSecondary
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
    label: Any,
    icon: String?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bgColor = animateColorAsState(
        if (selected) Accent
        else if (isHovered) theme.modCardBackground
        else theme.modCardBackground.copy(0f)
    )
    val contentColor = animateColorAsState(
        if (selected || isHovered) theme.textColor else theme.textColorSecondary
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
