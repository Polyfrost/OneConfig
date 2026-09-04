package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

/** Distance from a viewport edge at which a drag starts scrolling the grid */
private const val AutoScrollZonePx = 64f
private const val AutoScrollSpeedPx = 12f

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
 * The grid does not draw the dragged item because a lazy list clips to its viewport and would cut the card
 * in half at the edges
 *
 * The caller draws it in an overlay instead see [overlayKey] and [reorderOverlay]
 */
class GridReorderState internal constructor(
    private val gridState: LazyGridState,
    private val scope: CoroutineScope,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDrop: (index: Int) -> Unit,
    private val canSwap: (from: Int, to: Int) -> Boolean,
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
    private val settleOffset = Animatable(Offset.Zero, Offset.VectorConverter)
    private var settling = false
    private var autoScroll: Job? = null

    /** Overlay position in the coordinate space of the box wrapping the grid */
    val overlayOffset: Offset
        get() = if (settling) settleOffset.value else initialOffset + dragDelta

    private fun infoAt(index: Int): LazyGridItemInfo? =
        gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

    internal fun onDragStart(key: Any) {
        val info = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        draggingKey = key
        overlayKey = key
        overlaySize = info.size
        draggingIndex = info.index
        initialOffset = info.offset.toOffset()
        dragDelta = Offset.Zero
        settling = false
        autoScroll = scope.launch { autoScroll() }
    }

    internal fun onDrag(delta: Offset) {
        if (draggingKey == null) return
        dragDelta += delta
        settleOnHoveredItem()
    }

    internal fun onDragEnd() {
        val key = draggingKey ?: return
        val index = draggingIndex
        val landing = overlayOffset
        val slot = infoAt(index)?.offset?.toOffset()
        stop()
        onDrop(index)
        if (slot == null) {
            overlayKey = null
            return
        }
        scope.launch {
            settling = true
            settleOffset.snapTo(landing)
            settleOffset.animateTo(slot, SettleSpec)
            settling = false
            if (overlayKey == key) overlayKey = null
        }
    }

    internal fun onDragCancel() {
        stop()
        overlayKey = null
    }

    private fun stop() {
        autoScroll?.cancel()
        autoScroll = null
        draggingKey = null
        draggingIndex = -1
        dragDelta = Offset.Zero
    }

    /**
     * Swaps the dragged item into the slot it now covers most
     *
     * Comparing against how much of its own slot it still covers adds hysteresis so a card hovering a
     * boundary does not flip back and forth and the gaps between cards are not dead zones
     */
    private fun settleOnHoveredItem() {
        val info = infoAt(draggingIndex) ?: return
        val dragged = Rect(initialOffset + dragDelta, info.size.toSize())
        var best: LazyGridItemInfo? = null
        var bestOverlap = dragged.overlap(Rect(info.offset.toOffset(), info.size.toSize()))
        gridState.layoutInfo.visibleItemsInfo.forEach { other ->
            if (other.index == draggingIndex) return@forEach
            if (!canSwap(draggingIndex, other.index)) return@forEach
            val overlap = dragged.overlap(Rect(other.offset.toOffset(), other.size.toSize()))
            if (overlap > bestOverlap) {
                best = other
                bestOverlap = overlap
            }
        }
        val target = best ?: return
        onMove(draggingIndex, target.index)
        draggingIndex = target.index
        UiSounds.play(UiSoundEvent.SLIDER_TICK)
    }

    /** Scrolls the grid while the pointer is held near the top or bottom edge */
    private suspend fun autoScroll() {
        while (scope.isActive && draggingKey != null) {
            withFrameNanos { }
            val info = infoAt(draggingIndex) ?: continue
            val top = initialOffset.y + dragDelta.y
            val bottom = top + info.size.height
            val layout = gridState.layoutInfo
            val speed = when {
                top < layout.viewportStartOffset + AutoScrollZonePx -> -AutoScrollSpeedPx
                bottom > layout.viewportEndOffset - AutoScrollZonePx -> AutoScrollSpeedPx
                else -> 0f
            }
            if (speed != 0f && gridState.scrollBy(speed) != 0f) settleOnHoveredItem()
        }
    }
}

@Composable
fun rememberGridReorderState(
    gridState: LazyGridState,
    onMove: (from: Int, to: Int) -> Unit,
    onDrop: (index: Int) -> Unit,
    canSwap: (from: Int, to: Int) -> Boolean = { _, _ -> true },
): GridReorderState {
    val scope = rememberCoroutineScope()
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentCanSwap by rememberUpdatedState(canSwap)
    return remember(gridState, scope) {
        GridReorderState(
            gridState = gridState,
            scope = scope,
            onMove = { from, to -> currentOnMove(from, to) },
            onDrop = { index -> currentOnDrop(index) },
            canSwap = { from, to -> currentCanSwap(from, to) },
        )
    }
}

/**
 * Makes a grid item draggable
 *
 * [key] must be the same key the item was declared with since the grid is looked up by key not by index
 *
 * The item stays in the layout while dragged but is drawn by the overlay so its slot keeps its size and
 * its pointer input keeps receiving the gesture
 */
@Composable
fun Modifier.reorderableItem(state: GridReorderState, key: Any): Modifier = this
    // the layer caches the card's rasterised content, so a frame that changed nothing replays it
    // instead of drawing the card again
    .graphicsLayer { alpha = if (state.overlayKey == key) 0f else 1f }
    .pointerInput(key, state) {
        detectDragGestures(
            onDragStart = { state.onDragStart(key) },
            onDrag = { change, amount ->
                change.consume()
                state.onDrag(amount)
            },
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragCancel() },
        )
    }

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

private fun IntOffset.toOffset() = Offset(x.toFloat(), y.toFloat())

private fun IntSize.toSize() = Size(width.toFloat(), height.toFloat())
