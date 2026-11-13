@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.animateIntSizeAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.concentric

private val GROUP_PADDING = 3.dp
private val ITEM_MIN_HEIGHT = ControlSize.Regular.height - GROUP_PADDING * 2
private const val FADE_MILLIS = 150

private class RadioSlot {
    var offset by mutableStateOf(IntOffset.Zero)
    var size by mutableStateOf(IntSize.Zero)
}

@ExperimentalOneConfigUiApi
@Composable
public fun <T> RadioGroup(
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    orientation: Orientation = Orientation.Vertical,
    enabled: Boolean = true,
) {
    val theme = OneConfigTheme.current
    val groupShape = theme.shapes.field
    val itemShape = groupShape.concentric(GROUP_PADDING)
    val vertical = orientation == Orientation.Vertical
    val slots = remember(options.size) { List(options.size) { RadioSlot() } }
    val requesters = remember(options.size) { List(options.size) { FocusRequester() } }
    var focusedIndex by remember(options.size) { mutableIntStateOf(-1) }
    val selectedIndex = options.indexOf(selected)

    fun move(by: Int): Boolean {
        if (!enabled || options.isEmpty()) return false
        val from = if (focusedIndex in options.indices) focusedIndex else selectedIndex.coerceAtLeast(0)
        val to = (from + by).mod(options.size)
        onSelect(options[to])
        runCatching { requesters[to].requestFocus() }
        return true
    }

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .border(1.dp, theme.borderColor, groupShape)
            .clip(groupShape)
            .background(theme.componentBackground)
            .semantics { selectableGroup() }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft, Key.DirectionUp -> move(-1)
                    Key.DirectionRight, Key.DirectionDown -> move(1)
                    else -> false
                }
            },
    ) {
        Box(Modifier.padding(GROUP_PADDING)) {
            val target = slots.getOrNull(selectedIndex)
            if (target != null && target.size.width > 0) {
                val offset by animateIntOffsetAsState(
                    target.offset,
                    Motion.finiteSpec(spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)),
                    label = "radioIndicatorOffset",
                )
                val size by animateIntSizeAsState(
                    target.size,
                    Motion.finiteSpec(spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold)),
                    label = "radioIndicatorSize",
                )
                Box(
                    Modifier
                        .layout { measurable, _ ->
                            val placeable = measurable.measure(Constraints.fixed(size.width, size.height))
                            layout(0, 0) { placeable.place(offset.x, offset.y) }
                        }
                        .clip(itemShape)
                        .background(OneConfigTheme.accent),
                )
            }
            val items: @Composable (Modifier) -> Unit = { itemModifier ->
                options.forEachIndexed { index, option ->
                    RadioItem(
                        text = label(option),
                        selected = index == selectedIndex,
                        enabled = enabled,
                        shape = itemShape,
                        vertical = vertical,
                        slot = slots[index],
                        modifier = itemModifier
                            .focusRequester(requesters[index])
                            .onFocusChanged { if (it.isFocused) focusedIndex = index else if (focusedIndex == index) focusedIndex = -1 },
                        onClick = { onSelect(option) },
                    )
                }
            }
            if (vertical) {
                Column(Modifier.width(IntrinsicSize.Max)) { items(Modifier.fillMaxWidth()) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) { items(Modifier) }
            }
        }
    }
}

@Composable
private fun RadioItem(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    shape: Shape,
    vertical: Boolean,
    slot: RadioSlot,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val theme = OneConfigTheme.current
    val style = theme.typography.small
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val color by animateColorAsState(
        if (selected || (hovered && enabled)) theme.textColor else theme.textColorSecondary,
        Motion.spec(tween(FADE_MILLIS)),
        label = "radioItemText",
    )
    Box(
        modifier = modifier
            .onPlaced { coordinates ->
                val position = coordinates.positionInParent()
                slot.offset = IntOffset(position.x.roundToInt(), position.y.roundToInt())
                slot.size = coordinates.size
            }
            .oneConfigClickable(
                text,
                onClick,
                role = A11yRole.RadioButton,
                enabled = enabled,
                selected = selected,
                shape = shape,
                indication = false,
                interactionSource = source,
            )
            .heightIn(min = ITEM_MIN_HEIGHT)
            .padding(horizontal = theme.spacing.m, vertical = theme.spacing.xs),
        contentAlignment = if (vertical) Alignment.CenterStart else Alignment.Center,
    ) {
        Text(
            text,
            Modifier,
            color,
            style.fontSize,
            null,
            BaselineShift.None,
            style.fontWeight ?: FontWeight.Normal,
            if (vertical) TextAlign.Start else TextAlign.Center,
            1,
            TextOverflow.Ellipsis,
        )
    }
}
