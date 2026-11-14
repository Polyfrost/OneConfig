@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion

private val AutoScrollZone = 40.dp
private val AutoScrollRamp = 240.dp
private val AutoScrollMaxSpeed = 3200.dp
private const val SCROLL_LIFT_THRESHOLD = 1f
private const val LIFT_SCALE = 1.04f

private val SettleSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Offset(0.5f, 0.5f),
)

internal class ReorderSlot(val key: Any, val index: Int, val offset: Offset, val size: IntSize) {
    val bounds: Rect get() = Rect(offset, size.toSize())
}

internal interface ReorderViewport {
    val vertical: Boolean
    val viewportStart: Int
    val viewportEnd: Int
    val totalCount: Int
    val firstIndex: Int
    val firstOffset: Int
    fun slots(crossPadding: Float): List<ReorderSlot>
    fun keepScroll(index: Int, offset: Int)
    fun scrollBy(delta: Float)
}

private class GridViewport(private val state: LazyGridState) : ReorderViewport {
    override val vertical: Boolean get() = state.layoutInfo.orientation == Orientation.Vertical
    override val viewportStart: Int get() = state.layoutInfo.viewportStartOffset
    override val viewportEnd: Int get() = state.layoutInfo.viewportEndOffset
    override val totalCount: Int get() = state.layoutInfo.totalItemsCount
    override val firstIndex: Int get() = state.firstVisibleItemIndex
    override val firstOffset: Int get() = state.firstVisibleItemScrollOffset

    override fun slots(crossPadding: Float): List<ReorderSlot> {
        val pad = if (vertical) Offset(crossPadding, 0f) else Offset(0f, crossPadding)
        return state.layoutInfo.visibleItemsInfo.map { ReorderSlot(it.key, it.index, it.offset.toOffset() + pad, it.size) }
    }

    override fun keepScroll(index: Int, offset: Int) {
        state.requestScrollToItem(index, offset)
    }

    override fun scrollBy(delta: Float) {
        state.dispatchRawDelta(delta)
    }
}

private class ListViewport(private val state: LazyListState) : ReorderViewport {
    override val vertical: Boolean get() = state.layoutInfo.orientation == Orientation.Vertical
    override val viewportStart: Int get() = state.layoutInfo.viewportStartOffset
    override val viewportEnd: Int get() = state.layoutInfo.viewportEndOffset
    override val totalCount: Int get() = state.layoutInfo.totalItemsCount
    override val firstIndex: Int get() = state.firstVisibleItemIndex
    override val firstOffset: Int get() = state.firstVisibleItemScrollOffset

    override fun slots(crossPadding: Float): List<ReorderSlot> {
        val info = state.layoutInfo
        val vertical = info.orientation == Orientation.Vertical
        val viewport = if (vertical) info.viewportSize.width else info.viewportSize.height
        val cross = (viewport - 2 * crossPadding).roundToInt().coerceAtLeast(0)
        return info.visibleItemsInfo.map {
            if (vertical) {
                ReorderSlot(it.key, it.index, Offset(crossPadding, it.offset.toFloat()), IntSize(cross, it.size))
            } else {
                ReorderSlot(it.key, it.index, Offset(it.offset.toFloat(), crossPadding), IntSize(it.size, cross))
            }
        }
    }

    override fun keepScroll(index: Int, offset: Int) {
        state.requestScrollToItem(index, offset)
    }

    override fun scrollBy(delta: Float) {
        state.dispatchRawDelta(delta)
    }
}

@ExperimentalOneConfigUiApi
public class GridReorderState internal constructor(
    private val viewport: ReorderViewport,
    private val scope: CoroutineScope,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDragEnded: () -> Unit,
    private val dropRange: (index: Int) -> IntRange?,
    internal val onClick: (key: Any) -> Unit,
    private val crossPadding: () -> Dp,
    private val density: () -> Density,
) {
    public var draggingKey: Any? by mutableStateOf(null)
        private set

    public var overlayKey: Any? by mutableStateOf(null)
        private set

    public val isDragging: Boolean get() = draggingKey != null

    internal var overlaySize by mutableStateOf(IntSize.Zero)
        private set

    private var draggingIndex by mutableIntStateOf(-1)
    private var initialOffset by mutableStateOf(Offset.Zero)
    private var dragDelta by mutableStateOf(Offset.Zero)
    private val settleOffset = Animatable(Offset.Zero, Offset.VectorConverter)
    private var settling by mutableStateOf(false)
    private val lift = Animatable(1f)

    private var slot = Offset.Zero
    private var referenceKey: Any? = null
    private var referenceOffset = Offset.Zero
    private var settle: Job? = null
    private var dragLoop: Job? = null

    internal var autoScrollers = 0
    internal var containerCoordinates: LayoutCoordinates? = null
    internal val items = HashMap<Any, ReorderItemNode>()

    internal val liftScale: Float get() = lift.value

    internal val overlayOffset: Offset
        get() = if (settling) trackSlot() + settleOffset.value else initialOffset + dragDelta

    public fun cancel() {
        if (draggingKey == null) return
        stop()
        overlayKey = null
    }

    public fun moveBy(key: Any, delta: Int): Boolean {
        if (draggingKey != null || delta == 0) return false
        val item = items[key]?.takeIf { it.enabled } ?: return false
        val from = slots().firstOrNull { it.key == key }?.index ?: item.index
        val to = from + delta
        val range = dropRange(from)
        if (to !in 0 until viewport.totalCount || (range != null && to !in range)) return false
        keepScroll(from, to)
        onMove(from, to)
        onDragEnded()
        playUiSound(UiSound.SliderTick)
        return true
    }

    private fun slots(): List<ReorderSlot> = viewport.slots(with(density()) { crossPadding().toPx() })

    private fun slotAt(index: Int): ReorderSlot? = slots().firstOrNull { it.index == index }

    private fun trackSlot(): Offset {
        val slots = slots()
        val own = slots.firstOrNull { it.key == overlayKey }
        if (own != null) {
            slot = own.offset
        } else {
            slots.firstOrNull { it.key == referenceKey }?.let { slot += it.offset - referenceOffset }
        }
        val reference = slots.getOrNull(slots.size / 2)
        referenceKey = reference?.key
        referenceOffset = reference?.offset ?: Offset.Zero
        return slot
    }

    private fun drawnBounds(info: ReorderSlot): Rect {
        val container = containerCoordinates ?: return info.bounds
        val item = items[info.key]?.coordinates ?: return info.bounds
        if (!item.isAttached || !container.isAttached) return info.bounds
        return container.localBoundingBoxOf(item, clipBounds = false)
    }

    internal fun isOverSettlingItem(position: Offset): Boolean =
        settling && Rect(overlayOffset, overlaySize.toSize()).contains(position)

    internal fun draggableKeyAt(position: Offset): Any? {
        if (isOverSettlingItem(position)) return overlayKey
        val info = slots().lastOrNull { drawnBounds(it).contains(position) } ?: return null
        if (items[info.key]?.enabled != true) return null
        val range = dropRange(info.index)
        return info.key.takeIf { range == null || info.index in range }
    }

    internal fun startDrag(key: Any): Boolean {
        val info = slots().firstOrNull { it.key == key } ?: return false
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
        animateLift(if (Motion.reduced) 1f else LIFT_SCALE)
        dragLoop = scope.launch { dragLoop() }
        return true
    }

    internal fun dragBy(delta: Offset) {
        if (draggingKey != null) dragDelta += delta
    }

    internal fun endDrag() {
        val key = draggingKey ?: return
        settleOnHoveredItem()
        val fromSlot = overlayOffset - (slotAt(draggingIndex)?.offset ?: trackSlot())
        stop()
        onDragEnded()
        settling = true
        settle = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            settleOffset.snapTo(fromSlot)
            settleOffset.animateTo(Offset.Zero, Motion.spec(SettleSpec))
            settling = false
            if (overlayKey == key) overlayKey = null
        }
    }

    private fun stop() {
        dragLoop?.cancel()
        dragLoop = null
        draggingKey = null
        draggingIndex = -1
        dragDelta = Offset.Zero
        animateLift(1f)
    }

    private fun animateLift(target: Float) {
        scope.launch { lift.animateTo(target, Motion.spec(spring())) }
    }

    private fun keepScroll(from: Int, to: Int) {
        val first = viewport.firstIndex
        if (first in minOf(from, to)..maxOf(from, to)) viewport.keepScroll(first, viewport.firstOffset)
    }

    private fun settleOnHoveredItem() {
        if (draggingKey == null) return
        val slots = slots()
        val own = slots.firstOrNull { it.index == draggingIndex }
        val range = dropRange(draggingIndex)
        val dragged = Rect(overlayOffset, overlaySize.toSize())
        val allowed = slots.filter { range == null || it.index in range }
        var best: ReorderSlot? = null
        var bestOverlap = own?.let { dragged.overlap(it.bounds) } ?: 0f
        allowed.forEach { other ->
            if (other.index == draggingIndex) return@forEach
            val overlap = dragged.overlap(other.bounds)
            if (overlap > bestOverlap) {
                best = other
                bestOverlap = overlap
            }
        }
        if (best == null && range != null && bestOverlap == 0f) {
            fun distance(item: ReorderSlot) = (item.bounds.center - dragged.center).getDistance()
            val ownDistance = own?.let(::distance) ?: Float.POSITIVE_INFINITY
            best = allowed.minByOrNull(::distance)?.takeIf { distance(it) < ownDistance }
        }
        val target = best ?: return
        keepScroll(draggingIndex, target.index)
        onMove(draggingIndex, target.index)
        draggingIndex = target.index
        playUiSound(UiSound.SliderTick)
    }

    private suspend fun dragLoop() {
        var lastFrame = withFrameNanos { it }
        while (scope.isActive && draggingKey != null) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - lastFrame) / 1e9f).coerceAtMost(1 / 30f)
            lastFrame = frame
            trackSlot()
            settleOnHoveredItem()
            if (autoScrollers > 0) autoScroll(seconds)
        }
    }

    private fun autoScroll(seconds: Float) {
        val vertical = viewport.vertical
        fun Offset.main() = if (vertical) y else x
        fun IntSize.main() = if (vertical) height else width
        val start = overlayOffset.main()
        val end = start + overlaySize.main()
        val viewStart = viewport.viewportStart
        val viewEnd = viewport.viewportEnd
        val range = dropRange(draggingIndex)
        val inView = slots().filter { it.offset.main() >= viewStart && it.offset.main() + it.size.main() <= viewEnd }
        val canScrollBack = range == null || inView.isEmpty() || range.first < inView.first().index
        val canScrollForward = range == null || inView.isEmpty() || range.last > inView.last().index
        val (zone, rampDistance, maxSpeed) = with(density()) {
            Triple(AutoScrollZone.toPx(), AutoScrollRamp.toPx(), AutoScrollMaxSpeed.toPx())
        }
        val intoStart = minOf(viewStart + zone, initialOffset.main()) - start
        val intoEnd = end - maxOf(viewEnd - zone, initialOffset.main() + overlaySize.main())
        val depth = when {
            intoStart > 0f && canScrollBack -> -intoStart
            intoEnd > 0f && canScrollForward -> intoEnd
            else -> 0f
        }
        val ramp = (depth / rampDistance).coerceIn(-1f, 1f)
        if (depth != 0f) viewport.scrollBy(ramp * abs(ramp) * maxSpeed * seconds)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun rememberGridReorderState(
    lazyState: LazyGridState,
    onMove: (from: Int, to: Int) -> Unit,
    onDragEnd: () -> Unit = {},
    dropRange: (index: Int) -> IntRange? = { null },
    onClick: (key: Any) -> Unit = {},
    crossAxisPadding: Dp = 0.dp,
): GridReorderState = rememberLazyReorderState(lazyState, onMove, onDragEnd, dropRange, onClick, crossAxisPadding) { GridViewport(lazyState) }

@ExperimentalOneConfigUiApi
@Composable
public fun rememberGridReorderState(
    lazyState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
    onDragEnd: () -> Unit = {},
    dropRange: (index: Int) -> IntRange? = { null },
    onClick: (key: Any) -> Unit = {},
    crossAxisPadding: Dp = 0.dp,
): GridReorderState = rememberLazyReorderState(lazyState, onMove, onDragEnd, dropRange, onClick, crossAxisPadding) { ListViewport(lazyState) }

@Composable
private fun rememberLazyReorderState(
    lazyState: Any,
    onMove: (from: Int, to: Int) -> Unit,
    onDragEnd: () -> Unit,
    dropRange: (index: Int) -> IntRange?,
    onClick: (key: Any) -> Unit,
    crossAxisPadding: Dp,
    viewport: () -> ReorderViewport,
): GridReorderState {
    val scope = rememberCoroutineScope()
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentDropRange by rememberUpdatedState(dropRange)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentPadding by rememberUpdatedState(crossAxisPadding)
    val currentDensity by rememberUpdatedState(LocalDensity.current)
    return remember(lazyState, scope) {
        GridReorderState(
            viewport = viewport(),
            scope = scope,
            onMove = { from, to -> currentOnMove(from, to) },
            onDragEnded = { currentOnDragEnd() },
            dropRange = { index -> currentDropRange(index) },
            onClick = { key -> currentOnClick(key) },
            crossPadding = { currentPadding },
            density = { currentDensity },
        )
    }
}

@ExperimentalOneConfigUiApi
public fun Modifier.reorderContainer(state: GridReorderState): Modifier = pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
        val key = state.draggableKeyAt(down.position) ?: return@awaitEachGesture
        val caught = state.isOverSettlingItem(down.position)
        var last = down.position
        if (caught) {
            down.consume()
        } else {
            var scrolled = 0f
            do {
                val change = awaitPressedChange(down.id, consume = false) ?: return@awaitEachGesture
                val scroll = currentEvent.type == PointerEventType.Scroll
                if (scroll) scrolled += abs(change.scrollDelta.y)
                val passedSlop = scrolled >= SCROLL_LIFT_THRESHOLD ||
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                if (passedSlop) {
                    if (!scroll) change.consume()
                    last = change.position
                }
            } while (!passedSlop)
        }
        if (!state.startDrag(key)) return@awaitEachGesture
        state.dragBy(last - down.position)
        var click = caught
        try {
            while (true) {
                val change = awaitPressedChange(down.id, consume = true) ?: break
                if (currentEvent.type == PointerEventType.Scroll ||
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                ) {
                    click = false
                }
                state.dragBy(change.position - last)
                last = change.position
            }
        } finally {
            state.endDrag()
        }
        if (click) state.onClick(key)
    }
}.onPlaced { state.containerCoordinates = it }

@ExperimentalOneConfigUiApi
public fun Modifier.reorderableItem(state: GridReorderState, key: Any, index: Int, enabled: Boolean = true): Modifier = this
    .then(ReorderItemElement(state, key, index, enabled))
    .graphicsLayer { alpha = if (state.overlayKey == key) 0f else 1f }

@ExperimentalOneConfigUiApi
public fun Modifier.reorderOverlay(state: GridReorderState): Modifier = this
    .zIndex(1f)
    .offset { state.overlayOffset.round() }
    .layout { measurable, _ ->
        val size = state.overlaySize
        val placeable = measurable.measure(Constraints.fixed(size.width, size.height))
        layout(size.width, size.height) { placeable.place(0, 0) }
    }
    .graphicsLayer {
        scaleX = state.liftScale
        scaleY = state.liftScale
    }

@ExperimentalOneConfigUiApi
public fun Modifier.reorderAutoScroll(state: GridReorderState): Modifier = this.then(ReorderAutoScrollElement(state))

private suspend fun AwaitPointerEventScope.awaitPressedChange(id: PointerId, consume: Boolean): PointerInputChange? {
    val event = awaitPointerEvent(PointerEventPass.Initial)
    val change = event.changes.firstOrNull { it.id == id } ?: return null
    if (consume && event.type != PointerEventType.Scroll) change.consume()
    return change.takeIf { it.pressed }
}

private data class ReorderItemElement(
    val state: GridReorderState,
    val key: Any,
    val index: Int,
    val enabled: Boolean,
) : ModifierNodeElement<ReorderItemNode>() {
    override fun create(): ReorderItemNode = ReorderItemNode(state, key, index, enabled)

    override fun update(node: ReorderItemNode) {
        node.update(state, key, index, enabled)
    }
}

internal class ReorderItemNode(
    private var state: GridReorderState,
    private var key: Any,
    var index: Int,
    var enabled: Boolean,
) : Modifier.Node(), LayoutAwareModifierNode, KeyInputModifierNode {
    var coordinates: LayoutCoordinates? = null
        private set

    override fun onAttach() {
        state.items[key] = this
    }

    override fun onDetach() {
        if (state.items[key] === this) state.items.remove(key)
        coordinates = null
    }

    fun update(state: GridReorderState, key: Any, index: Int, enabled: Boolean) {
        if (state !== this.state || key != this.key) {
            if (this.state.items[this.key] === this) this.state.items.remove(this.key)
            this.state = state
            this.key = key
            if (isAttached) state.items[key] = this
        }
        this.index = index
        this.enabled = enabled
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = false

    override fun onPreKeyEvent(event: KeyEvent): Boolean {
        if (!enabled || event.type != KeyEventType.KeyDown || !event.isAltPressed) return false
        val delta = when (event.key) {
            Key.DirectionUp -> -1
            Key.DirectionDown -> 1
            else -> return false
        }
        return state.moveBy(key, delta)
    }
}

private data class ReorderAutoScrollElement(val state: GridReorderState) : ModifierNodeElement<ReorderAutoScrollNode>() {
    override fun create(): ReorderAutoScrollNode = ReorderAutoScrollNode(state)

    override fun update(node: ReorderAutoScrollNode) {
        node.rebind(state)
    }
}

private class ReorderAutoScrollNode(private var state: GridReorderState) : Modifier.Node() {
    override fun onAttach() {
        state.autoScrollers++
    }

    override fun onDetach() {
        state.autoScrollers--
    }

    fun rebind(next: GridReorderState) {
        if (next === state) return
        if (isAttached) {
            state.autoScrollers--
            next.autoScrollers++
        }
        state = next
    }
}

private fun Rect.overlap(other: Rect): Float {
    val width = minOf(right, other.right) - maxOf(left, other.left)
    val height = minOf(bottom, other.bottom) - maxOf(top, other.top)
    return if (width > 0f && height > 0f) width * height else 0f
}
