@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.animateIntSizeAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.polyfrost.compose.host.controller.focusEntry
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import kotlin.math.roundToInt

internal val ChoiceInset = 3.dp

internal class SlidingIndicatorState(count: Int) {
    private var container: LayoutCoordinates? = null
    private val items = arrayOfNulls<LayoutCoordinates>(count)
    private val bounds = List(count) { mutableStateOf(IntRect.Zero) }

    fun bounds(index: Int): IntRect = bounds.getOrNull(index)?.value ?: IntRect.Zero

    fun attachContainer(coordinates: LayoutCoordinates) {
        container = coordinates
        for (index in items.indices) update(index)
    }

    fun attachItem(index: Int, coordinates: LayoutCoordinates) {
        if (index !in items.indices) return
        items[index] = coordinates
        update(index)
    }

    private fun update(index: Int) {
        val parent = container?.takeIf { it.isAttached } ?: return
        val item = items[index]?.takeIf { it.isAttached } ?: return
        val position = parent.localPositionOf(item, Offset.Zero)
        val origin = IntOffset(position.x.roundToInt(), position.y.roundToInt())
        bounds[index].value = IntRect(origin, item.size)
    }
}

@Composable
internal fun rememberSlidingIndicatorState(count: Int): SlidingIndicatorState = remember(count) { SlidingIndicatorState(count) }

internal fun Modifier.slidingIndicatorContainer(state: SlidingIndicatorState): Modifier =
    onGloballyPositioned { state.attachContainer(it) }

internal fun Modifier.slidingIndicatorItem(state: SlidingIndicatorState, index: Int): Modifier =
    onGloballyPositioned { state.attachItem(index, it) }

@Composable
internal fun SlidingIndicator(
    state: SlidingIndicatorState,
    selected: Int,
    modifier: Modifier,
    area: (IntRect) -> IntRect = { it },
) {
    val current = area(state.bounds(selected))
    val measured = current.width > 0 && current.height > 0
    val last = remember { arrayOf(IntRect.Zero) }
    if (measured) last[0] = current
    val target = last[0]
    val alpha by Motion.animateFloatAsState(if (selected >= 0) 1f else 0f, label = "slidingIndicatorAlpha")
    if (target.width <= 0 || target.height <= 0 || alpha <= 0f && selected < 0) return
    val offset by animateIntOffsetAsState(
        target.topLeft,
        Motion.finiteSpec(spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)),
        label = "slidingIndicatorOffset",
    )
    val size by animateIntSizeAsState(
        target.size,
        Motion.finiteSpec(spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold)),
        label = "slidingIndicatorSize",
    )
    Box(
        Modifier
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints.fixed(size.width.coerceAtLeast(0), size.height.coerceAtLeast(0)))
                layout(0, 0) { placeable.place(offset.x, offset.y) }
            }
            .graphicsLayer { this.alpha = alpha }
            .then(modifier),
    )
}

internal class ChoiceFocus(count: Int) {
    val requesters: List<FocusRequester> = List(count) { FocusRequester() }

    var focused: Int by mutableIntStateOf(-1)
}

@Composable
internal fun rememberChoiceFocus(count: Int): ChoiceFocus = remember(count) { ChoiceFocus(count) }

internal fun Modifier.choiceGroup(
    focus: ChoiceFocus,
    selectedIndex: Int,
    enabled: Boolean,
    selectable: (Int) -> Boolean,
    activation: TabActivation = TabActivation.Automatic,
    orientation: Orientation = Orientation.Horizontal,
    onPick: (Int) -> Unit,
): Modifier = then(
    focus.requesters.getOrNull(selectedIndex)?.let { Modifier.focusEntry(it, enabled) } ?: Modifier,
).semantics { selectableGroup() }.onKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown || !enabled) return@onKeyEvent false
    val manual = activation == TabActivation.Manual
    val horizontal = orientation == Orientation.Horizontal
    val step = when (event.key) {
        Key.DirectionLeft -> if (manual && !horizontal) return@onKeyEvent false else -1
        Key.DirectionUp -> if (manual && horizontal) return@onKeyEvent false else -1
        Key.DirectionRight -> if (manual && !horizontal) return@onKeyEvent false else 1
        Key.DirectionDown -> if (manual && horizontal) return@onKeyEvent false else 1
        else -> return@onKeyEvent false
    }
    val count = focus.requesters.size
    var index = if (focus.focused in 0 until count) focus.focused else selectedIndex.coerceAtLeast(0)
    repeat(count) {
        index = (index + step).mod(count)
        if (selectable(index)) {
            if (!manual) onPick(index)
            runCatching { focus.requesters[index].requestFocus() }
            return@onKeyEvent true
        }
    }
    false
}

internal fun Modifier.choiceItem(focus: ChoiceFocus, index: Int): Modifier {
    val requester = focus.requesters.getOrNull(index) ?: return this
    return focusRequester(requester).onFocusChanged {
        if (it.isFocused) focus.focused = index else if (focus.focused == index) focus.focused = -1
    }
}

@Composable
internal fun Modifier.choiceTrack(shape: Shape): Modifier {
    val theme = OneConfigTheme.current
    val track = theme.componentBackground
    val border = theme.borderColor
    return glassFrame(shape, { track }, { border }, hover = false)
}

@Composable
internal fun Modifier.choiceIndicator(shape: Shape, glass: Boolean): Modifier =
    glassFrame(shape, { Color.Transparent }, { Color.Transparent }, { 1f }, hover = false, glass = glass)
