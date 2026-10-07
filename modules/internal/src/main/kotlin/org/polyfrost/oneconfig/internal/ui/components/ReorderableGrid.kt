package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

/** Distance from a viewport edge at which a drag starts scrolling the grid */
private val AutoScrollZone = 40.dp
/** How far into the zone the item must go for auto-scroll to reach [AutoScrollMaxSpeed] */
private val AutoScrollRamp = 240.dp
/** Distance auto-scroll covers per second at its fastest */
private val AutoScrollMaxSpeed = 3200.dp

private val SettleSpec = spring<Offset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Offset(0.5f, 0.5f),
)

/**
 * Drag-to-reorder support for a [androidx.compose.foundation.lazy.grid.LazyVerticalGrid]
 *
 * The grid re-lays out around the dragged item as the pointer moves so [onMove] must mutate the backing
 * list immediately and [onDrop] fires once with the item's final index when the gesture ends
 *
 * The caller draws the dragged item above the grid, see [reorderOverlay], as a grid item cannot pass above the
 * ones after it
 *
 * Gestures are read by [reorderContainer] on the box around the grid, since grid items can leave composition mid drag
 *
 * [dragBounds] gives the slots an item may be dropped into, or null for any, and an item outside its own bounds
 * cannot be dragged
 *
 * [onClick] fires for an item clicked while it is still settling, as [reorderContainer] catches that press
 */
class GridReorderState internal constructor(
    private val gridState: LazyGridState,
    private val scope: CoroutineScope,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDrop: (index: Int) -> Unit,
    private val dragBounds: (index: Int) -> IntRange?,
    internal val onClick: (key: Any) -> Unit,
    private val density: () -> Density,
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    /** Key of the item to draw in the overlay either the dragged one or one still settling */
    var overlayKey by mutableStateOf<Any?>(null)
        private set

    var overlaySize by mutableStateOf(IntSize.Zero)
        private set

    private var draggingIndex by mutableIntStateOf(-1)

    /** Where the dragged item sat when the gesture started in viewport coordinates */
    private var initialOffset by mutableStateOf(Offset.Zero)
    private var dragDelta by mutableStateOf(Offset.Zero)

    /** How far the settling item still is from its slot */
    private val settleOffset = Animatable(Offset.Zero, Offset.VectorConverter)
    private var settling by mutableStateOf(false)

    private var slot = Offset.Zero
    private var referenceKey: Any? = null
    private var referenceOffset = Offset.Zero
    private var settle: Job? = null
    private var dragLoop: Job? = null

    internal var containerCoordinates: LayoutCoordinates? = null
    internal val itemCoordinates = mutableMapOf<Any, LayoutCoordinates>()

    /**
     * Overlay position in the coordinate space of the box wrapping the grid
     *
     * A settling item is placed relative to its slot so it moves with the grid as it scrolls
     */
    val overlayOffset: Offset
        get() = if (settling) trackSlot() + settleOffset.value else initialOffset + dragDelta

    /**
     * Where the overlay item's slot is now
     *
     * While out of view it follows the middle visible item, which is unlikely to scroll away in one frame
     */
    private fun trackSlot(): Offset {
        val items = gridState.layoutInfo.visibleItemsInfo
        val own = items.firstOrNull { it.key == overlayKey }
        if (own != null) {
            slot = own.offset.toOffset()
        } else {
            items.firstOrNull { it.key == referenceKey }?.let { slot += it.offset.toOffset() - referenceOffset }
        }
        val reference = items.getOrNull(items.size / 2)
        referenceKey = reference?.key
        referenceOffset = reference?.offset?.toOffset() ?: Offset.Zero
        return slot
    }

    private fun infoAt(index: Int): LazyGridItemInfo? =
        gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

    private fun drawnBounds(info: LazyGridItemInfo): Rect {
        val container = containerCoordinates ?: return info.bounds()
        val item = itemCoordinates[info.key] ?: return info.bounds()
        return container.localBoundingBoxOf(item, clipBounds = false)
    }

    internal fun isOverSettlingItem(position: Offset): Boolean =
        settling && Rect(overlayOffset, overlaySize.toSize()).contains(position)

    internal fun draggableKeyAt(position: Offset): Any? {
        if (isOverSettlingItem(position)) return overlayKey
        itemCoordinates.values.removeAll { !it.isAttached }
        val info = gridState.layoutInfo.visibleItemsInfo.firstOrNull { drawnBounds(it).contains(position) } ?: return null
        val bounds = dragBounds(info.index)
        return info.key.takeIf { bounds == null || info.index in bounds }
    }

    /** Lifts the item with [key] into the overlay, from where it is if still settling */
    internal fun onDragStart(key: Any): Boolean {
        val info = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return false
        val from = if (settling && overlayKey == key) overlayOffset else drawnBounds(info).topLeft
        settle?.cancel()
        settling = false
        draggingKey = key
        overlayKey = key
        overlaySize = info.size
        draggingIndex = info.index
        initialOffset = from
        dragDelta = Offset.Zero
        trackSlot()
        dragLoop = scope.launch { dragLoop() }
        return true
    }

    internal fun onDrag(delta: Offset) {
        if (draggingKey != null) dragDelta += delta
    }

    internal fun onDragEnd() {
        val key = draggingKey ?: return
        settleOnHoveredItem()
        val index = draggingIndex
        // the grid only moves the item to its new slot next frame, so measure from that slot
        val fromSlot = overlayOffset - (infoAt(index)?.offset?.toOffset() ?: trackSlot())
        stop()
        onDrop(index)
        settling = true
        settle = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            settleOffset.snapTo(fromSlot)
            settleOffset.animateTo(Offset.Zero, SettleSpec)
            settling = false
            if (overlayKey == key) overlayKey = null
        }
    }

    /** Ends the drag without dropping, for when the backing list is replaced and the index no longer applies */
    internal fun cancel() {
        if (draggingKey == null) return
        stop()
        overlayKey = null
    }

    private fun stop() {
        dragLoop?.cancel()
        dragLoop = null
        draggingKey = null
        draggingIndex = -1
        dragDelta = Offset.Zero
    }

    /**
     * Swaps the dragged item into the slot it now covers most
     *
     * Comparing against how much of its own slot it still covers adds hysteresis so a card hovering a
     * boundary does not flip back and forth and the gaps between cards are not dead zones
     *
     * Once it covers none of the slots [dragBounds] allows it moves to the nearest one, or to any visible one if
     * its own slot scrolled out of view
     */
    private fun settleOnHoveredItem() {
        if (draggingKey == null) return
        val info = infoAt(draggingIndex)
        val bounds = dragBounds(draggingIndex)
        val dragged = Rect(overlayOffset, overlaySize.toSize())
        val allowed = gridState.layoutInfo.visibleItemsInfo.filter { bounds == null || it.index in bounds }
        var best: LazyGridItemInfo? = null
        var bestOverlap = info?.let { dragged.overlap(it.bounds()) } ?: 0f
        allowed.forEach { other ->
            if (other.index == draggingIndex) return@forEach
            val overlap = dragged.overlap(other.bounds())
            if (overlap > bestOverlap) {
                best = other
                bestOverlap = overlap
            }
        }
        if (best == null && bounds != null && bestOverlap == 0f) {
            fun distance(item: LazyGridItemInfo) = (item.bounds().center - dragged.center).getDistance()
            val ownDistance = info?.let(::distance) ?: Float.POSITIVE_INFINITY
            best = allowed.minByOrNull(::distance)?.takeIf { distance(it) < ownDistance }
        }
        val target = best ?: return
        // override scroll to stay in place instead of following the first visible item
        val first = gridState.firstVisibleItemIndex
        if (first in minOf(draggingIndex, target.index)..maxOf(draggingIndex, target.index)) {
            gridState.requestScrollToItem(first, gridState.firstVisibleItemScrollOffset)
        }
        onMove(draggingIndex, target.index)
        draggingIndex = target.index
        UiSounds.play(UiSoundEvent.SLIDER_TICK)
    }

    /**
     * Moves the dragged item into the slot under it, and scrolls the grid while it is near the top or bottom edge,
     * faster the further past it
     */
    private suspend fun dragLoop() {
        var lastFrame = withFrameNanos { it }
        while (scope.isActive && draggingKey != null) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - lastFrame) / 1e9f).coerceAtMost(1 / 30f)
            lastFrame = frame
            trackSlot()
            settleOnHoveredItem()
            val top = overlayOffset.y
            val bottom = top + overlaySize.height
            val layout = gridState.layoutInfo
            val bounds = dragBounds(draggingIndex)
            val inView = layout.visibleItemsInfo.filter {
                it.offset.y >= layout.viewportStartOffset && it.offset.y + it.size.height <= layout.viewportEndOffset
            }
            val canScrollUp = bounds == null || inView.isEmpty() || bounds.first < inView.first().index
            val canScrollDown = bounds == null || inView.isEmpty() || bounds.last > inView.last().index
            val (zone, rampDistance, maxSpeed) = with(density()) {
                Triple(AutoScrollZone.toPx(), AutoScrollRamp.toPx(), AutoScrollMaxSpeed.toPx())
            }
            // an item picked up inside the zone should only scroll when further dragged towards the edge
            val intoTop = minOf(layout.viewportStartOffset + zone, initialOffset.y) - top
            val intoBottom = bottom - maxOf(layout.viewportEndOffset - zone, initialOffset.y + overlaySize.height)
            val depth = when {
                intoTop > 0f && canScrollUp -> -intoTop
                intoBottom > 0f && canScrollDown -> intoBottom
                else -> 0f
            }
            val ramp = (depth / rampDistance).coerceIn(-1f, 1f)
            // raw delta so a wheel scroll mid drag cannot interrupt this loop
            if (depth != 0f) gridState.dispatchRawDelta(ramp * abs(ramp) * maxSpeed * seconds)
        }
    }
}

@Composable
fun rememberGridReorderState(
    gridState: LazyGridState,
    onMove: (from: Int, to: Int) -> Unit,
    onDrop: (index: Int) -> Unit,
    dragBounds: (index: Int) -> IntRange? = { null },
    onClick: (key: Any) -> Unit = {},
): GridReorderState {
    val scope = rememberCoroutineScope()
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentDragBounds by rememberUpdatedState(dragBounds)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentDensity by rememberUpdatedState(LocalDensity.current)
    return remember(gridState, scope) {
        GridReorderState(
            gridState = gridState,
            scope = scope,
            onMove = { from, to -> currentOnMove(from, to) },
            onDrop = { index -> currentOnDrop(index) },
            dragBounds = { index -> currentDragBounds(index) },
            onClick = { key -> currentOnClick(key) },
            density = { currentDensity },
        )
    }
}

/**
 * Reads drags for the grid's items, applied to the box wrapping the grid with the grid at its origin
 *
 * A drag starts once the primary button moves past touch slop so items can still be clicked, but an item still
 * settling is caught on press since it would move out from under the pointer first
 *
 * Events are read before the grid so it does not scroll along with the drag, but wheel scrolling still reaches it
 */
fun Modifier.reorderContainer(state: GridReorderState): Modifier = pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
        val key = state.draggableKeyAt(down.position) ?: return@awaitEachGesture
        val caught = state.isOverSettlingItem(down.position)
        var last = down.position
        if (caught) {
            down.consume()
        } else {
            do {
                val change = awaitPressedChange(down.id, consume = false) ?: return@awaitEachGesture
                val passedSlop = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                if (passedSlop) {
                    change.consume()
                    last = change.position
                }
            } while (!passedSlop)
        }
        if (!state.onDragStart(key)) return@awaitEachGesture
        state.onDrag(last - down.position)
        var click = caught
        try {
            while (true) {
                val change = awaitPressedChange(down.id, consume = true) ?: break
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) click = false
                state.onDrag(change.position - last)
                last = change.position
            }
        } finally {
            state.onDragEnd()
        }
        if (click) state.onClick(key)
    }
}.onPlaced { state.containerCoordinates = it }

/** The next change of the pointer while still held, consumed if [consume] unless it is a wheel scroll */
private suspend fun AwaitPointerEventScope.awaitPressedChange(id: PointerId, consume: Boolean): PointerInputChange? {
    val event = awaitPointerEvent(PointerEventPass.Initial)
    val change = event.changes.firstOrNull { it.id == id } ?: return null
    if (consume && event.type != PointerEventType.Scroll) change.consume()
    return change.takeIf { it.pressed }
}

/** Hides the item while the overlay draws it in its place, [key] must be the key it was declared with */
fun Modifier.reorderableItem(state: GridReorderState, key: Any): Modifier = this
    .onPlaced { state.itemCoordinates[key] = it }
    .graphicsLayer { alpha = if (state.overlayKey == key) 0f else 1f }

/**
 * Positions and sizes the overlay copy of the dragged item
 *
 * Apply to the same content the grid would have drawn placed in a box that wraps the grid
 */
@Composable
fun Modifier.reorderOverlay(state: GridReorderState): Modifier {
    val density = LocalDensity.current
    val size = with(density) { state.overlaySize.let { it.width.toDp() to it.height.toDp() } }
    val scale by animateFloatAsState(if (state.draggingKey != null) 1.04f else 1f)
    return this
        .zIndex(1f)
        .offset { state.overlayOffset.round() }
        .size(size.first, size.second)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

private fun Rect.overlap(other: Rect): Float {
    val width = minOf(right, other.right) - maxOf(left, other.left)
    val height = minOf(bottom, other.bottom) - maxOf(top, other.top)
    return if (width > 0f && height > 0f) width * height else 0f
}

private fun LazyGridItemInfo.bounds() = Rect(offset.toOffset(), size.toSize())

private fun IntOffset.toOffset() = Offset(x.toFloat(), y.toFloat())

private fun IntSize.toSize() = Size(width.toFloat(), height.toFloat())
