@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public object SelectDefaults {
    public val minWidth: Dp = 160.dp

    public val horizontalPadding: Dp = FieldDefaults.horizontalPadding(ControlSize.Regular)

    public val leadingLabelSpacing: Dp = 6.dp

    public const val MAX_NAMED: Int = 2
}

@ExperimentalOneConfigUiApi
public enum class SelectValueAlignment {
    Start,

    End,
}

@ExperimentalOneConfigUiApi
public enum class SelectOpenStyle {
    Tint,

    Accent,
}

private const val EXPANDED_TINT_ALPHA = 0.2f

@ExperimentalOneConfigUiApi
@Composable
public fun <T> Select(
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    enabled: Boolean = true,
    placeholder: String? = null,
    size: ControlSize = ControlSize.Regular,
    interactionSource: MutableInteractionSource? = null,
    styledLabel: ((T) -> AnnotatedString)? = null,
    contentDescription: String? = null,
    placement: Placement = Placement.BelowStart,
    leadingLabel: String? = null,
    valueAlignment: SelectValueAlignment = SelectValueAlignment.Start,
    openStyle: SelectOpenStyle = SelectOpenStyle.Tint,
    popupSounds: Boolean = LocalPopupSounds.current,
) {
    var expanded by remember { mutableStateOf(false) }
    val trigger = rememberPopupTrigger()
    val isSelected = options.contains(selected)
    val current = if (isSelected) label(selected) else null
    SelectField(
        text = current ?: placeholder ?: kitString("oneconfig.ui.select.placeholder", "Select"),
        styled = if (isSelected && styledLabel != null) styledLabel(selected) else null,
        muted = current == null,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        enabled = enabled,
        size = size,
        trigger = trigger,
        interactionSource = interactionSource,
        contentDescription = contentDescription,
        leadingLabel = leadingLabel,
        valueAlignment = valueAlignment,
        openStyle = openStyle,
    ) {
        CompositionLocalProvider(LocalPopupSounds provides popupSounds) {
            if (styledLabel == null) {
                val entries = options.map { option ->
                    MenuAction(label(option), onClick = { onSelect(option) }, checked = option == selected)
                }
                TriggeredDropdownMenu(expanded, { expanded = false }, entries, trigger, placement = placement, matchAnchorWidth = true)
            } else {
                TriggeredDropdownMenu(expanded, { expanded = false }, trigger, placement = placement, matchAnchorWidth = true) {
                    options.forEach { option ->
                        val on = option == selected
                        MenuItemRow(
                            label = label(option),
                            onClick = {
                                expanded = false
                                onSelect(option)
                            },
                            size = MenuItemSize.Compact,
                            checked = on,
                            reserveCheck = true,
                            styledLabel = styledLabel(option),
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun <T> MultiSelect(
    selected: Set<T>,
    options: List<T>,
    onChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    styledLabel: ((T) -> AnnotatedString)? = null,
    contentDescription: String? = null,
    popupSounds: Boolean = LocalPopupSounds.current,
) {
    MultiSelectContent(selected, options, onChange, modifier, label, enabled, interactionSource, styledLabel, false, contentDescription, popupSounds)
}

@ExperimentalOneConfigUiApi
@Composable
public fun <T> MultiSelect(
    selected: Set<T>,
    options: List<T>,
    onChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    styledLabel: ((T) -> AnnotatedString)? = null,
    selectAll: Boolean,
    contentDescription: String? = null,
    popupSounds: Boolean = LocalPopupSounds.current,
) {
    MultiSelectContent(selected, options, onChange, modifier, label, enabled, interactionSource, styledLabel, selectAll, contentDescription, popupSounds)
}

@Composable
private fun <T> MultiSelectContent(
    selected: Set<T>,
    options: List<T>,
    onChange: (Set<T>) -> Unit,
    modifier: Modifier,
    label: (T) -> String,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    styledLabel: ((T) -> AnnotatedString)?,
    selectAll: Boolean,
    contentDescription: String?,
    popupSounds: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val trigger = rememberPopupTrigger()
    val chosen = options.filter { it in selected }
    val summary = when {
        chosen.isEmpty() -> null
        chosen.size <= SelectDefaults.MAX_NAMED -> chosen.joinToString(", ") { label(it) }
        else -> kitString("oneconfig.ui.select.count", "${chosen.size} selected", chosen.size)
    }
    val styledSummary = if (styledLabel != null && chosen.isNotEmpty() && chosen.size <= SelectDefaults.MAX_NAMED) {
        buildAnnotatedString {
            chosen.forEachIndexed { index, option ->
                if (index > 0) append(", ")
                append(styledLabel(option))
            }
        }
    } else {
        null
    }
    SelectField(
        text = summary ?: kitString("oneconfig.ui.select.none", "None"),
        styled = styledSummary,
        muted = summary == null,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        enabled = enabled,
        size = ControlSize.Regular,
        trigger = trigger,
        interactionSource = interactionSource,
        contentDescription = contentDescription,
    ) {
        CompositionLocalProvider(LocalPopupSounds provides popupSounds) {
            TriggeredDropdownMenu(expanded, { expanded = false }, trigger, matchAnchorWidth = true) {
                if (selectAll && options.size > 1) {
                    val all = chosen.size == options.size
                    MenuItemRow(
                        label = if (all) {
                            kitString("oneconfig.option.multi_select.deselect_all", "Deselect all")
                        } else {
                            kitString("oneconfig.menu.text.select_all", "Select all")
                        },
                        onClick = { onChange(if (all) selected - options.toSet() else selected + options) },
                    )
                    Divider(Modifier.padding(vertical = MenuDefaults.itemSpacing))
                }
                options.forEach { option ->
                    val on = option in selected
                    MenuItemRow(
                        label = label(option),
                        onClick = { onChange(if (on) selected - option else selected + option) },
                        selected = on,
                        styledLabel = styledLabel?.invoke(option),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectField(
    text: String,
    styled: AnnotatedString?,
    muted: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    size: ControlSize,
    trigger: PopupTrigger,
    interactionSource: MutableInteractionSource?,
    contentDescription: String?,
    leadingLabel: String? = null,
    valueAlignment: SelectValueAlignment = SelectValueAlignment.Start,
    openStyle: SelectOpenStyle = SelectOpenStyle.Tint,
    menu: @Composable () -> Unit,
) {
    LaunchedEffect(enabled) { if (!enabled) onExpandedChange(false) }
    val stateText = if (expanded) kitString("oneconfig.ui.select.expanded", "expanded") else kitString("oneconfig.ui.select.collapsed", "collapsed")
    var spokenExpanded by remember { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        if (expanded == spokenExpanded) return@LaunchedEffect
        spokenExpanded = expanded
        if (!A11y.keyboardMode) A11y.announce(stateText)
    }
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val shape = FieldDefaults.shape
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val fieldFill = FieldDefaults.fill
    val accentOpen = expanded && openStyle == SelectOpenStyle.Accent
    val fill = when {
        accentOpen -> accent
        expanded -> accent.copy(alpha = EXPANDED_TINT_ALPHA).compositeOver(fieldFill)
        else -> fieldFill
    }
    val content by Motion.animateColorAsState(
        when {
            accentOpen -> theme.accentTextColor
            muted -> theme.mutedText
            hovered || expanded -> theme.textColor
            else -> theme.textColorSecondary
        },
        Motion.colorSpec(),
    )
    val chevron by Motion.animateFloatAsState(if (expanded) 0f else 180f)
    val metrics = ControlDefaults.metrics(size)
    Box(modifier, propagateMinConstraints = true) {
        Row(
            modifier = Modifier
                .popupTrigger(trigger)
                .onKeyEvent { event ->
                    val open = enabled && !expanded && event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown
                    if (open) onExpandedChange(true)
                    open
                }
                .defaultMinSize(minWidth = SelectDefaults.minWidth)
                .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
                .heightIn(min = size.height)
                .fieldFrame(FieldState.of(false, expanded), fill, shape, hoverPull = true, enabled = enabled)
                .oneConfigClickable(
                    listOfNotNull(contentDescription ?: leadingLabel, text, stateText).joinToString(", "),
                    { onExpandedChange(!expanded) },
                    role = A11yRole.Option,
                    enabled = enabled,
                    shape = shape,
                    interactionSource = source,
                    recipe = ControlDefaults.PressOnly,
                )
                .padding(horizontal = SelectDefaults.horizontalPadding),
            horizontalArrangement = if (valueAlignment == SelectValueAlignment.End && leadingLabel == null) Arrangement.End else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val textStyle = theme.typography.body.copy(fontSize = ButtonDefaults.fontSize(size))
            val labelColor = if (accentOpen) theme.accentTextColor else theme.mutedText
            if (valueAlignment == SelectValueAlignment.End) {
                if (leadingLabel != null) {
                    Text(leadingLabel, Modifier.padding(end = ButtonDefaults.contentSpacing), labelColor, textStyle, 1, TextOverflow.Clip, softWrap = false)
                }
                Row(Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
                    SelectValueText(text, styled, Modifier.weight(1f, fill = false).padding(end = ButtonDefaults.contentSpacing), textStyle) { content }
                    Icon("up", null, Modifier.size(metrics.iconSize).graphicsLayer { rotationZ = chevron }, tintProducer = { content })
                }
            } else if (leadingLabel != null) {
                Row(
                    Modifier.weight(1f, fill = false).padding(end = ButtonDefaults.contentSpacing),
                    horizontalArrangement = Arrangement.spacedBy(SelectDefaults.leadingLabelSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(leadingLabel, Modifier, labelColor, textStyle, 1, TextOverflow.Clip, softWrap = false)
                    SelectValueText(text, styled, Modifier.weight(1f, fill = false), textStyle) { content }
                }
            } else {
                SelectValueText(text, styled, Modifier.weight(1f, fill = false).padding(end = ButtonDefaults.contentSpacing), textStyle) { content }
            }
            if (valueAlignment == SelectValueAlignment.Start) {
                Icon("up", null, Modifier.size(metrics.iconSize).graphicsLayer { rotationZ = chevron }, tintProducer = { content })
            }
        }
        menu()
    }
}

@Composable
private fun SelectValueText(text: String, styled: AnnotatedString?, modifier: Modifier, style: TextStyle, color: ColorProducer) {
    if (styled != null) {
        Text(styled, modifier, Color.Unspecified, style, 1, TextOverflow.Ellipsis, colorProducer = color)
    } else {
        Text(text, modifier, Color.Unspecified, style, 1, TextOverflow.Ellipsis, colorProducer = color)
    }
}
