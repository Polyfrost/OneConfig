@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public object SelectDefaults {
    public val minWidth: Dp = 160.dp

    public val horizontalPadding: Dp = 12.dp

    public const val MAX_NAMED: Int = 2
}

private const val FADE_MILLIS = 150
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
    ) {
        if (styledLabel == null) {
            val entries = options.map { option ->
                MenuAction(label(option), onClick = { onSelect(option) }, checked = option == selected)
            }
            TriggeredDropdownMenu(expanded, { expanded = false }, entries, trigger, matchAnchorWidth = true)
        } else {
            TriggeredDropdownMenu(expanded, { expanded = false }, trigger, matchAnchorWidth = true) {
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
    ) {
        TriggeredDropdownMenu(expanded, { expanded = false }, trigger, matchAnchorWidth = true) {
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
    menu: @Composable () -> Unit,
) {
    LaunchedEffect(enabled) { if (!enabled) onExpandedChange(false) }
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val shape = theme.shapes.field
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val fill by animateColorAsState(
        if (expanded) accent.copy(alpha = EXPANDED_TINT_ALPHA).compositeOver(theme.componentBackground) else theme.componentBackground,
        Motion.spec(tween(FADE_MILLIS)),
    )
    val border by animateColorAsState(if (expanded) accent else theme.borderColor, Motion.spec(tween(FADE_MILLIS)))
    val content by animateColorAsState(
        when {
            muted -> theme.mutedText
            hovered || expanded -> theme.textColor
            else -> theme.textColorSecondary
        },
        Motion.spec(tween(FADE_MILLIS)),
    )
    val chevron by animateFloatAsState(if (expanded) 0f else 180f, Motion.spec(tween(FADE_MILLIS)))
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
                .glassFrame(shape, { fill }, { border }, hover = enabled)
                .oneConfigClickable(
                    text,
                    { onExpandedChange(!expanded) },
                    role = A11yRole.Option,
                    enabled = enabled,
                    shape = shape,
                    indication = !glassAvailable,
                    interactionSource = source,
                )
                .padding(horizontal = SelectDefaults.horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val textModifier = Modifier.weight(1f, fill = false).padding(end = ButtonDefaults.contentSpacing)
            val textStyle = theme.typography.body.copy(fontSize = ButtonDefaults.fontSize(size))
            if (styled != null) {
                Text(styled, textModifier, Color.Unspecified, textStyle, 1, TextOverflow.Ellipsis, colorProducer = { content })
            } else {
                Text(text, textModifier, Color.Unspecified, textStyle, 1, TextOverflow.Ellipsis, colorProducer = { content })
            }
            Icon("up", null, Modifier.size(ButtonDefaults.iconSize(size)).graphicsLayer { rotationZ = chevron }, tintProducer = { content })
        }
        menu()
    }
}
