@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion

@ExperimentalOneConfigUiApi
fun <T> List<T>.withItemMoved(from: Int, to: Int): List<T>? {
    if (from !in indices || to !in indices) return null
    return toMutableList().also { it.add(to, it.removeAt(from)) }
}

@ExperimentalOneConfigUiApi
class ReorderState internal constructor(
    private val orientation: Orientation,
    private val scope: CoroutineScope,
) {
    internal var onMove: (Int, Int) -> Unit = { _, _ -> }
    internal var onDragEnd: (() -> Unit)? = null

    private class Slot(var index: Int) {
        var rect: Rect? = null
        var scale = 1f
    }

    private val slots = HashMap<Any, Slot>()
    private val settle = Animatable(0f)

    var draggingKey by mutableStateOf<Any?>(null)
        private set

    private var settlingKey by mutableStateOf<Any?>(null)
    private var pointer by mutableFloatStateOf(0f)
    private var draggedStart by mutableFloatStateOf(0f)
    private var grab = 0f
    private var movedAt = 0L

    val isDragging: Boolean get() = draggingKey != null

    internal val isVertical: Boolean get() = orientation == Orientation.Vertical

    internal fun main(offset: Offset): Float = if (isVertical) offset.y else offset.x

    internal fun index(key: Any, index: Int) {
        slots.getOrPut(key) { Slot(index) }.index = index
    }

    internal fun place(key: Any, coordinates: LayoutCoordinates) {
        val slot = slots[key] ?: return
        val start = coordinates.localToRoot(Offset.Zero)
        val end = coordinates.localToRoot(Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()))
        val rect = Rect(start, end)
        val local = if (isVertical) coordinates.size.height else coordinates.size.width
        slot.scale = if (local > 0) (rect.mainEnd() - rect.mainStart()) / local else 1f
        if (slot.rect == rect) return
        slot.rect = rect
        movedAt = 0L
        if (key == draggingKey) draggedStart = rect.mainStart()
        if (draggingKey != null) retarget()
    }

    internal fun forget(key: Any) {
        slots.remove(key)
    }

    internal fun offsetOf(key: Any): Float = when (key) {
        draggingKey -> (pointer - grab - draggedStart) / (slots[key]?.scale?.takeIf { it > 0f } ?: 1f)
        settlingKey -> settle.value
        else -> 0f
    }

    internal fun start(key: Any, at: Float): Boolean {
        val rect = slots[key]?.rect ?: return false
        scope.launch { settle.snapTo(0f) }
        settlingKey = null
        draggedStart = rect.mainStart()
        grab = at - draggedStart
        pointer = at
        movedAt = 0L
        draggingKey = key
        return true
    }

    internal fun moveTo(at: Float) {
        pointer = at
        retarget()
    }

    private fun retarget() {
        val key = draggingKey ?: return
        if (movedAt != 0L && System.nanoTime() - movedAt < LAYOUT_WAIT_NS) return
        val dragged = slots[key] ?: return
        val rect = dragged.rect ?: return
        val center = pointer - grab + (rect.mainEnd() - rect.mainStart()) / 2f
        val others = slots.entries.filter { it.key != key && it.value.rect != null }
        if (others.isEmpty()) return
        val placed = slots.values.filter { it.rect != null }
        val target = others.firstOrNull { center in it.value.rect!!.mainStart()..it.value.rect!!.mainEnd() }?.value
            ?: when {
                center < others.minOf { it.value.rect!!.mainStart() } -> placed.minBy { it.index }
                center > others.maxOf { it.value.rect!!.mainEnd() } -> placed.maxBy { it.index }
                else -> null
            }
            ?: return
        val from = dragged.index
        val to = target.index
        if (to == from) return
        for (slot in slots.values) {
            if (slot === dragged) continue
            if (from < to && slot.index in (from + 1)..to) slot.index--
            if (from > to && slot.index in to until from) slot.index++
        }
        dragged.index = to
        movedAt = System.nanoTime()
        onMove(from, to)
    }

    internal fun end() {
        val key = draggingKey ?: return
        val left = offsetOf(key)
        draggingKey = null
        movedAt = 0L
        onDragEnd?.invoke()
        if (Motion.reduced || left == 0f) return
        settlingKey = key
        scope.launch {
            settle.snapTo(left)
            settle.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
            if (settlingKey == key) settlingKey = null
        }
    }

    private fun Rect.mainStart() = if (isVertical) top else left
    private fun Rect.mainEnd() = if (isVertical) bottom else right

    private companion object {
        private const val LAYOUT_WAIT_NS = 100_000_000L
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun rememberReorderState(onMove: (from: Int, to: Int) -> Unit): ReorderState =
    rememberReorderState(Orientation.Vertical, onMove, null)

@ExperimentalOneConfigUiApi
@Composable
fun rememberReorderState(orientation: Orientation, onMove: (from: Int, to: Int) -> Unit, onDragEnd: (() -> Unit)?): ReorderState {
    val scope = rememberCoroutineScope()
    val state = remember(orientation) { ReorderState(orientation, scope) }
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onDragEnd)
    state.onMove = { from, to -> move(from, to) }
    state.onDragEnd = { end?.invoke() }
    return state
}

@ExperimentalOneConfigUiApi
fun Modifier.reorderItem(state: ReorderState, key: Any, index: Int): Modifier = reorderItem(state, key, index, true)

@ExperimentalOneConfigUiApi
fun Modifier.reorderItem(state: ReorderState, key: Any, index: Int, enabled: Boolean): Modifier = composed {
    state.index(key, index)
    DisposableEffect(state, key) { onDispose { state.forget(key) } }
    val holder = remember { ReorderCoordinates() }
    val dragging = state.draggingKey == key
    this
        .onGloballyPositioned {
            holder.coordinates = it
            state.place(key, it)
        }
        .then(
            if (!enabled) {
                Modifier
            } else {
                Modifier.pointerInput(state, key) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                        if (down.isConsumed) return@awaitEachGesture
                        val crossed = awaitSlop(down.id, down.position) ?: return@awaitEachGesture
                        val coordinates = holder.coordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture
                        if (!state.start(key, state.main(coordinates.localToRoot(down.position)))) return@awaitEachGesture
                        try {
                            state.moveTo(state.main(coordinates.localToRoot(crossed)))
                            follow(down.id) { at ->
                                holder.coordinates?.takeIf { it.isAttached }?.let { state.moveTo(state.main(it.localToRoot(at))) }
                            }
                        } finally {
                            state.end()
                        }
                    }
                }
            },
        )
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            val offset = state.offsetOf(key)
            if (state.isVertical) translationY = offset else translationX = offset
        }
}

private class ReorderCoordinates {
    var coordinates: LayoutCoordinates? = null
}

private suspend fun AwaitPointerEventScope.awaitSlop(pointer: PointerId, origin: Offset): Offset? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        val change = event.changes.firstOrNull { it.id == pointer } ?: return null
        if (event.type == PointerEventType.Scroll) continue
        if (!change.pressed || change.isConsumed) return null
        if ((change.position - origin).getDistance() > viewConfiguration.touchSlop) {
            change.consume()
            return change.position
        }
    }
}

private suspend fun AwaitPointerEventScope.follow(pointer: PointerId, onMove: (Offset) -> Unit) {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        val change = event.changes.firstOrNull { it.id == pointer } ?: return
        if (event.type == PointerEventType.Scroll) continue
        if (!change.pressed) return
        change.consume()
        if (change.position != change.previousPosition) onMove(change.position)
    }
}
