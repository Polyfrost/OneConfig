package org.polyfrost.oneconfig.internal.ui.components.settings

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.Select
import org.polyfrost.oneconfig.api.ui.v1.components.fadingEdgesMask
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.components.smoothWheelScroll
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.ui.api.settings.DropdownOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.animateAccentColorAsState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

private val MenuPadding = 4.dp
private val DropdownShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val MenuItemShape @Composable get() = DropdownShape.concentric(MenuPadding)

@Composable
fun DropdownOption(data: DropdownOptionData) {
    DropdownOption(data, true)
}

@Composable
fun DropdownOption(data: DropdownOptionData, enabled: Boolean) {
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

    Dropdown(options, selectedIdx, { index ->
        selectedIdx = index
        @Suppress("UNCHECKED_CAST")
        if (enumClass != null) {
            (data.prop as Property<Any>).set(values[index])
        } else if (data.prop.type == String::class.java) {
            (data.prop as Property<Any>).set(optionValues.getOrElse(index) { options[index] })
        } else {
            (data.prop as Property<Any>).set(index)
        }
    }, LocalOptionWidth.current, enabled)
}

/** [options] can be anything [Text] can show, such as translated text */
@Composable
fun Dropdown(options: List<Any>, selected: Int, onSelect: (Int) -> Unit, width: Dp = LocalOptionWidth.current) {
    Dropdown(options, selected, onSelect, width, true)
}

@Composable
fun Dropdown(options: List<Any>, selected: Int, onSelect: (Int) -> Unit, width: Dp, enabled: Boolean) {
    val labels = options.map { it.asRenderText() }
    Select(
        selected = selected,
        options = labels.indices.toList(),
        onSelect = onSelect,
        modifier = Modifier.width(width).heightIn(min = OptionControlHeight),
        label = { labels[it] },
        enabled = enabled,
        placeholder = "—",
    )
}

@Composable
internal fun DropdownFrame(
    label: Any,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    width: Dp = LocalOptionWidth.current,
    enabled: Boolean = true,
    menu: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalTheme.current
    val triggerInteraction = rememberInteractionSource()
    val isHovered by triggerInteraction.collectIsHoveredAsState()
    val borderColor = animateAccentColorAsState(
        expanded,
        if (expanded) Accent
        else theme.borderColor
    )
    val textColor = Motion.animateColorAsState(if ((isHovered && enabled) || expanded) theme.textColor else theme.textColorSecondary)
    val backgroundColor = animateAccentColorAsState(
        expanded,
        if (expanded) Accent.copy(0.2f).compositeOver(theme.componentBackground)
        else theme.componentBackground
    )
    val chevronRotation = Motion.animateFloatAsState(if (expanded) 0f else 180f)
    val disabledAlpha = OneConfigTheme.current.disabledAlpha

    Box {
        Row(
            modifier = Modifier
                .width(width)
                .height(OptionControlHeight)
                .graphicsLayer { alpha = if (enabled) 1f else disabledAlpha }
                .liquidGlass(DropdownShape)
                .animatedFrame(DropdownShape, fill = { backgroundColor.value }, border = { borderColor.value })
                .oneConfigClickable(
                    label = label.asRenderText(),
                    onClick = { onExpandedChange(!expanded) },
                    role = A11yRole.Option,
                    enabled = enabled,
                    shape = DropdownShape,
                    indication = false,
                    interactionSource = triggerInteraction,
                )
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
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
            onDismissRequest = { onExpandedChange(false) },
            offset = DpOffset(0.dp, 10.dp),
        ) {
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .width(width)
                    .clip(DropdownShape)
                    .liquidGlass(DropdownShape, frosted = true)
                    .background(theme.componentBackground, DropdownShape)
                    .border(1.dp, theme.borderColor, DropdownShape)
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .smoothWheelScroll(scrollState)
                        .fadingEdgesMask(scrollState)
                        .verticalScroll(scrollState)
                        .padding(MenuPadding)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = menu,
                )
            }
        }
    }
}

@Composable
internal fun DropdownItem(
    label: Any,
    selected: Boolean = false,
    icon: String? = null,
    leading: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val itemShape = MenuItemShape

    val bgColor = animateAccentColorAsState(
        selected to isHovered,
        if (selected) Accent
        else if (isHovered) theme.modCardBackground
        else theme.modCardBackground.copy(0f)
    )
    val contentColor = Motion.animateColorAsState(if (selected || isHovered) theme.textColor else theme.textColorSecondary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .graphicsLayer {}
            .animatedFrame(itemShape, fill = { bgColor.value }, border = { Color.Transparent })
            .oneConfigClickable(
                label = label.asRenderText(),
                onClick = onClick,
                role = A11yRole.MenuItem,
                selected = selected,
                shape = itemShape,
                indication = false,
                interactionSource = interactionSource,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        leading()
        icon?.takeIf { it.isNotEmpty() }?.let { Icon(it, modifier = Modifier.size(16.dp), tint = { contentColor.value }) }
        Text(label, color = Color.Unspecified, fontSize = 13.sp, colorProducer = { contentColor.value })
    }
}
