package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.layout.absoluteOffset
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.lazy.LazyListState
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.scale
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

/** Distance from a viewport edge at which a drag starts scrolling the grid */
private val AutoScrollZone = 40.dp
/** How far into the zone the item must go for auto-scroll to reach [AutoScrollMaxSpeed] */
private val AutoScrollRamp = 240.dp
/** Distance auto-scroll covers per second at its fastest */
private val AutoScrollMaxSpeed = 3200.dp

private val DragThreshold = 4.dp

private const val SettleMillis = 250f

class GridReorderState<T> internal constructor(
    private val gridState: LazyListState,
    private val items: List<T>,
    private val keyOf: (T) -> Any,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDrop: (index: Int) -> Unit,
    private val dragBounds: (index: Int) -> IntRange,
    internal val onClick: (key: Any) -> Unit,
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    /** Key of the item to draw in the overlay either the dragged one or one still settling */
    var overlayKey by mutableStateOf<Any?>(null)
        private set

    internal var overlayX by mutableFloatStateOf(0f)
        private set
    internal var overlayY by mutableFloatStateOf(0f)
        private set

    internal var settling by mutableStateOf(false)
        private set

    internal var originX by mutableFloatStateOf(0f)
    internal var originY by mutableFloatStateOf(0f)

    internal var viewportStart = 0f
    internal var viewportEnd = 0f

    internal val itemBounds = HashMap<Any, FloatArray>()

    private var pressedKey: Any? = null
    private var downX = 0f
    private var downY = 0f
    private var pointerX = 0f
    private var pointerY = 0f
    private var grabX = 0f
    private var grabY = 0f

    private fun indexOf(key: Any) = items.indexOfFirst { keyOf(it) == key }

    internal fun onPress(key: Any, x: Float, y: Float) {
        if (overlayKey != null) return
        pressedKey = key
        downX = x
        downY = y
        pointerX = x
        pointerY = y
    }

    private fun onDragStart(): Boolean {
        val key = pressedKey ?: return false
        val bounds = itemBounds[key] ?: return false
        if (indexOf(key) < 0) return false
        grabX = downX - bounds[0]
        grabY = downY - bounds[1]
        overlayKey = key
        settling = false
        draggingKey = key
        return true
    }

    internal fun onDrag(x: Float, y: Float) {
        pointerX = x
        pointerY = y
        if (pressedKey == null) return
        if (draggingKey == null && (hypot(x - downX, y - downY) < DragThreshold.value || !onDragStart())) return
        overlayX = x - grabX
        overlayY = y - grabY
        settleOnHoveredItem()
    }

    internal fun onScroll() {
        if (draggingKey == null && pressedKey != null && onDragStart()) {
            overlayX = pointerX - grabX
            overlayY = pointerY - grabY
        }
    }

    internal fun onRelease(key: Any): Boolean {
        if (pressedKey != key) return false
        pressedKey = null
        if (draggingKey == null) return true
        settleOnHoveredItem()
        onDrop(indexOf(key))
        draggingKey = null
        settling = true
        return false
    }

    /**
     * Swaps the dragged item into the slot it now covers most
     *
     * Comparing against how much of its own slot it still covers adds hysteresis so a card hovering a
     * boundary does not flip back and forth and the gaps between cards are not dead zones
     */
    private fun settleOnHoveredItem() {
        val key = draggingKey ?: return
        val from = indexOf(key).takeIf { it >= 0 } ?: return
        val own = itemBounds[key] ?: return
        val x = overlayX
        val y = overlayY
        val w = own[2]
        val h = own[3]
        fun overlap(r: FloatArray) =
            max(0f, min(x + w, r[0] + r[2]) - max(x, r[0])) * max(0f, min(y + h, r[1] + r[3]) - max(y, r[1]))
        var best = -1
        var bestOverlap = overlap(own)
        for (i in dragBounds(from)) {
            if (i == from) continue
            val other = itemBounds[keyOf(items[i])] ?: continue
            val overlap = overlap(other)
            if (overlap > bestOverlap) {
                best = i
                bestOverlap = overlap
            }
        }
        if (best < 0) return
        onMove(from, best)
        UiSounds.play(UiSoundEvent.SLIDER_TICK)
    }

    /**
     * Moves the dragged item into the slot under it, and scrolls the grid while it is near the top or bottom edge,
     * faster the further past it
     */
    internal suspend fun dragLoop() {
        var lastFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val key = draggingKey ?: break
            val seconds = min((frame - lastFrame) / 1e9f, 1f / 30f)
            lastFrame = frame
            val top = overlayY
            val bottom = top + (itemBounds[key]?.get(3) ?: 0f)
            val zone = AutoScrollZone.value
            val depth = when {
                top < viewportStart + zone -> top - (viewportStart + zone)
                bottom > viewportEnd - zone -> bottom - (viewportEnd - zone)
                else -> 0f
            }
            if (depth != 0f) {
                val ramp = (depth / AutoScrollRamp.value).coerceIn(-1f, 1f)
                gridState.scroll.scrollTo(gridState.scroll.value + ramp * abs(ramp) * AutoScrollMaxSpeed.value * seconds)
            }
            settleOnHoveredItem()
        }
    }

    internal suspend fun settle() {
        val fromX = overlayX
        val fromY = overlayY
        val start = withFrameNanos { it }
        var t = 0f
        while (t < 1f) {
            t = ((withFrameNanos { it } - start) / 1e6f / SettleMillis).coerceIn(0f, 1f)
            val key = overlayKey ?: break
            val slot = itemBounds[key] ?: break
            val eased = 1f - (1f - t) * (1f - t) * (1f - t)
            overlayX = fromX + (slot[0] - fromX) * eased
            overlayY = fromY + (slot[1] - fromY) * eased
        }
        settling = false
        overlayKey = null
    }
}

@Composable
fun <T> rememberGridReorderState(
    gridState: LazyListState,
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    onDrop: (index: Int) -> Unit,
    dragBounds: (index: Int) -> IntRange,
    onClick: (key: Any) -> Unit = {},
): GridReorderState<T> {
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentDragBounds by rememberUpdatedState(dragBounds)
    val currentOnClick by rememberUpdatedState(onClick)
    val state = remember(gridState, items) {
        GridReorderState(
            gridState = gridState,
            items = items,
            keyOf = key,
            onMove = { from, to -> currentOnMove(from, to) },
            onDrop = { index -> currentOnDrop(index) },
            dragBounds = { index -> currentDragBounds(index) },
            onClick = { clicked -> currentOnClick(clicked) },
        )
    }
    LaunchedEffect(state, state.draggingKey) {
        if (state.draggingKey != null) state.dragLoop()
    }
    LaunchedEffect(state, state.settling) {
        if (state.settling) state.settle()
    }
    return state
}

fun Modifier.reorderContainer(state: GridReorderState<*>): Modifier = this
    .onPlaced {
        state.originX = it.x
        state.originY = it.y
    }
    .onPointerEvent {
        if (it.type == PointerEventType.Scroll) state.onScroll()
        false
    }

fun Modifier.reorderableItem(state: GridReorderState<*>, key: Any): Modifier = this
    .alpha(if (state.overlayKey == key) 0f else 1f)
    .onPlaced { state.itemBounds[key] = floatArrayOf(it.x, it.y, it.width, it.height) }
    .onPointerEvent { e ->
        when (e.type) {
            PointerEventType.Press -> e.button == 0 && run {
                state.onPress(key, e.x, e.y)
                true
            }
            PointerEventType.Move -> {
                state.onDrag(e.x, e.y)
                false
            }
            PointerEventType.Release -> e.button == 0 && run {
                if (state.onRelease(key) && e.inside) {
                    UiSounds.play(UiSoundEvent.CLICK)
                    state.onClick(key)
                }
                true
            }
            else -> false
        }
    }

@Composable
fun Modifier.reorderOverlay(state: GridReorderState<*>): Modifier {
    val scale by animateFloatAsState(if (state.draggingKey != null) 1.04f else 1f)
    val bounds = state.overlayKey?.let { state.itemBounds[it] } ?: return this
    return this
        .size(bounds[2].dp, bounds[3].dp)
        .absoluteOffset((state.overlayX - state.originX).dp, (state.overlayY - state.originY).dp)
        .scale(scale)
}
