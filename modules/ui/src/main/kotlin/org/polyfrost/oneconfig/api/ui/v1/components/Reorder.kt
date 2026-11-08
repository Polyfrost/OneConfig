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
import androidx.compose.ui.layout.boundsInRoot
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

    private val bounds = HashMap<Any, Rect>()
    private val indices = HashMap<Any, Int>()
    private val settle = Animatable(0f)

    var draggingKey by mutableStateOf<Any?>(null)
        private set

    private var dragOffset by mutableFloatStateOf(0f)

    val isDragging: Boolean get() = draggingKey != null

    internal val isVertical: Boolean get() = orientation == Orientation.Vertical

    internal fun track(key: Any, index: Int, rect: Rect?) {
        indices[key] = index
        if (rect != null) bounds[key] = rect
    }

    internal fun forget(key: Any) {
        indices.remove(key)
        bounds.remove(key)
    }

    internal fun offsetOf(key: Any): Float = when {
        key == draggingKey -> dragOffset
        key == settlingKey -> settle.value
        else -> 0f
    }

    private var settlingKey by mutableStateOf<Any?>(null)

    internal fun start(key: Any) {
        scope.launch { settle.snapTo(0f) }
        settlingKey = null
        draggingKey = key
        dragOffset = 0f
    }

    internal fun drag(delta: Offset) {
        val key = draggingKey ?: return
        dragOffset += if (orientation == Orientation.Vertical) delta.y else delta.x
        val dragged = bounds[key] ?: return
        val from = indices[key] ?: return
        val center = dragged.mainCenter() + dragOffset
        val target = bounds.entries.firstOrNull { (other, rect) ->
            other != key && center in rect.mainStart()..rect.mainEnd()
        } ?: return
        val to = indices[target.key] ?: return
        if (to == from) return
        val shift = if (to > from) target.value.mainEnd() - dragged.mainEnd() else target.value.mainStart() - dragged.mainStart()
        dragOffset -= shift
        bounds[key] = dragged.shifted(shift)
        onMove(from, to)
    }

    internal fun end() {
        val key = draggingKey ?: return
        val left = dragOffset
        draggingKey = null
        dragOffset = 0f
        onDragEnd?.invoke()
        if (Motion.reduced || left == 0f) return
        settlingKey = key
        scope.launch {
            settle.snapTo(left)
            settle.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
            if (settlingKey == key) settlingKey = null
        }
    }

    private fun Rect.mainStart() = if (orientation == Orientation.Vertical) top else left
    private fun Rect.mainEnd() = if (orientation == Orientation.Vertical) bottom else right
    private fun Rect.mainCenter() = (mainStart() + mainEnd()) / 2f
    private fun Rect.shifted(by: Float) = if (orientation == Orientation.Vertical) translate(0f, by) else translate(by, 0f)
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
    state.track(key, index, null)
    DisposableEffect(state, key) { onDispose { state.forget(key) } }
    val dragging = state.draggingKey == key
    this
        .onGloballyPositioned { if (state.draggingKey != key) state.track(key, index, it.boundsInRoot()) }
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            val offset = state.offsetOf(key)
            if (state.isVertical) translationY = offset else translationX = offset
        }
        .then(
            if (!enabled) {
                Modifier
            } else {
                Modifier.pointerInput(state, key) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                        if (down.isConsumed) return@awaitEachGesture
                        awaitSlop(down.id, down.position) ?: return@awaitEachGesture
                        state.start(key)
                        try {
                            follow(down.id) { state.drag(it) }
                        } finally {
                            state.end()
                        }
                    }
                }
            },
        )
}

private suspend fun AwaitPointerEventScope.awaitSlop(pointer: PointerId, origin: Offset): Offset? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        val change = event.changes.firstOrNull { it.id == pointer } ?: return null
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
        if (!change.pressed) return
        val delta = change.position - change.previousPosition
        change.consume()
        if (delta != Offset.Zero) onMove(delta)
    }
}
