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
) {
    var expanded by remember { mutableStateOf(false) }
    val current = if (options.contains(selected)) label(selected) else null
    val entries = options.map { option ->
        MenuAction(label(option), onClick = { onSelect(option) }, checked = option == selected)
    }
    SelectField(
        text = current ?: placeholder ?: kitString("oneconfig.ui.select.placeholder", "Select"),
        muted = current == null,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        enabled = enabled,
        size = size,
    ) {
        DropdownMenu(expanded, { expanded = false }, entries, matchAnchorWidth = true)
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
) {
    var expanded by remember { mutableStateOf(false) }
    val chosen = options.filter { it in selected }
    val summary = when {
        chosen.isEmpty() -> null
        chosen.size <= SelectDefaults.MAX_NAMED -> chosen.joinToString(", ") { label(it) }
        else -> kitString("oneconfig.ui.select.count", "${chosen.size} selected", chosen.size)
    }
    SelectField(
        text = summary ?: kitString("oneconfig.ui.select.none", "None"),
        muted = summary == null,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        enabled = enabled,
        size = ControlSize.Regular,
    ) {
        DropdownMenu(expanded, { expanded = false }, matchAnchorWidth = true) {
            options.forEach { option ->
                val on = option in selected
                MenuItem(label(option), null, null, MenuItemSize.Compact, false, on, true, null, Modifier, null) {
                    onChange(if (on) selected - option else selected + option)
                }
            }
        }
    }
}

@Composable
private fun SelectField(
    text: String,
    muted: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    size: ControlSize,
    menu: @Composable () -> Unit,
) {
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val shape = theme.shapes.field
    val source = remember { MutableInteractionSource() }
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
            Text(
                text,
                Modifier.weight(1f, fill = false).padding(end = ButtonDefaults.contentSpacing),
                Color.Unspecified,
                theme.typography.body.copy(fontSize = ButtonDefaults.fontSize(size)),
                1,
                TextOverflow.Ellipsis,
                colorProducer = { content },
            )
            Icon("up", null, Modifier.size(ButtonDefaults.iconSize(size)).graphicsLayer { rotationZ = chevron }, tintProducer = { content })
        }
        menu()
    }
}
