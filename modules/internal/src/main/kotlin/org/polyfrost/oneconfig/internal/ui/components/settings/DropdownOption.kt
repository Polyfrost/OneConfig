package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.DropdownOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

private val MenuPadding = 4.dp
private val DropdownShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val MenuItemShape @Composable get() = DropdownShape.concentric(MenuPadding)

@Composable
fun DropdownOption(data: DropdownOptionData) {
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

    val initialValue = data.prop.shown()
    var selectedIdx by remember(data.prop) {
        mutableStateOf(
            when (initialValue) {
                is Enum<*> -> values.indexOf(initialValue).takeIf { it >= 0 } ?: 0
                is String -> optionValues.indexOf(initialValue).takeIf { it >= 0 } ?: 0
                else -> initialValue as? Int ?: 0
            }
        )
    }

    Dropdown(options, selectedIdx, onSelect = { index ->
        selectedIdx = index
        @Suppress("UNCHECKED_CAST")
        if (enumClass != null) {
            (data.prop as Property<Any>).set(values[index])
        } else if (data.prop.type == String::class.java) {
            (data.prop as Property<Any>).set(optionValues.getOrElse(index) { options[index] })
        } else {
            (data.prop as Property<Any>).set(index)
        }
    })
}

/** [options] can be anything [Text] can show, such as translated text */
@Composable
fun Dropdown(options: List<Any>, selected: Int, onSelect: (Int) -> Unit, width: Dp = LocalOptionWidth.current) {
    val theme = LocalTheme.current
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.getOrElse(selected) { "—" }

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
        if (expanded) Accent.copy(0.2f).compositeOver(theme.componentBackground)
        else theme.componentBackground
    )
    val chevronRotation = animateFloatAsState(
        if (expanded) 0f else 180f
    )

    Box {
        Row(
            modifier = Modifier
                .width(width)
                .height(32.dp)
                .graphicsLayer {}
                .animatedFrame(DropdownShape, fill = { backgroundColor.value }, border = { borderColor.value })
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
                color = Color.Unspecified,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                colorProducer = { textColor.value },
            )
            Icon("up", modifier = Modifier.graphicsLayer { rotationZ = chevronRotation.value }, tint = { textColor.value })
        }

        SimpleDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 10.dp),
        ) {
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .width(width)
                    .clip(DropdownShape)
                    .background(theme.componentBackground, DropdownShape)
                    .border(1.dp, theme.borderColor, DropdownShape)
            ) {
                val itemShape = MenuItemShape
                Column(
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .fadingEdges(scrollState, theme.componentBackground)
                        .wheelScroll(scrollState)
                        .verticalScroll(scrollState)
                        .padding(MenuPadding)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    options.forEachIndexed { index, option ->
                        val optionSource = rememberInteractionSource()
                        val isSelected = index == selected
                        val isHovered by optionSource.collectIsHoveredAsState()

                        val backgroundColor = animateColorAsState(
                            if (isSelected) Accent
                            else if (isHovered) theme.modCardBackground
                            else theme.modCardBackground.copy(0f)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {}
                                .animatedFrame(itemShape, fill = { backgroundColor.value }, border = { Color.Transparent })
                                .onClick(optionSource) {
                                    onSelect(index)
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
