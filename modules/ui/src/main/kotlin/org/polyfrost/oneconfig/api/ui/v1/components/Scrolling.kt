@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.glassBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val SCROLLBAR_IDLE_ALPHA = 0.40f
private const val SCROLLBAR_HOVER_ALPHA = 0.70f
private const val SCROLLBAR_HOVER_MILLIS = 300
private val ScrollbarThickness = 8.dp
private val ScrollbarMinimalHeight = 24.dp

private val MaskOpaque = Color.Black

private val ScrollContainerFade = 24.dp

@ExperimentalOneConfigUiApi
public fun Modifier.smoothWheelScroll(state: ScrollableState, orientation: Orientation = Orientation.Vertical): Modifier =
    glassBridge()?.wheelScroll(this, state, orientation) ?: this

@ExperimentalOneConfigUiApi
@Composable
public fun ScrollContainer(
    state: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    scrollbar: Boolean = true,
    fadingEdges: Boolean = false,
    gutter: Dp? = null,
    smoothWheel: Boolean = true,
    content: @Composable () -> Unit,
) {
    val vertical = orientation == Orientation.Vertical
    val style = themedScrollbarStyle()
    val reserved = if (scrollbar) gutter ?: defaultGutter(style) else 0.dp
    Box(modifier.then(if (smoothWheel) Modifier.smoothWheelScroll(state, orientation) else Modifier), propagateMinConstraints = true) {
        val viewport = Modifier
            .then(if (fadingEdges) Modifier.fadingEdgesMask(state, ScrollContainerFade, orientation) else Modifier)
            .then(if (vertical) Modifier.verticalScroll(state) else Modifier.horizontalScroll(state))
        if (vertical) {
            Column(viewport.padding(end = reserved)) { content() }
        } else {
            Row(viewport.padding(bottom = reserved)) { content() }
        }
        if (scrollbar) GutterScrollbar(rememberScrollbarAdapter(state), !vertical, false, style, reserved)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ScrollContainer(
    state: LazyListState,
    modifier: Modifier = Modifier,
    gutter: Dp? = null,
    scrollbar: Boolean = true,
    fadingEdges: Boolean = false,
    adapter: ScrollbarAdapter? = null,
    reserveGutter: Boolean = false,
    smoothWheel: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val horizontal by remember(state) { derivedStateOf { state.layoutInfo.orientation == Orientation.Horizontal } }
    val reverse by remember(state) { derivedStateOf { state.layoutInfo.reverseLayout } }
    val resolved = adapter ?: rememberScrollbarAdapter(state)
    LazyScrollContainer(state, resolved, horizontal, reverse, modifier, gutter, scrollbar, fadingEdges, reserveGutter, smoothWheel, content)
}

@ExperimentalOneConfigUiApi
@Composable
public fun ScrollContainer(
    state: LazyGridState,
    modifier: Modifier = Modifier,
    gutter: Dp? = null,
    scrollbar: Boolean = true,
    fadingEdges: Boolean = false,
    adapter: ScrollbarAdapter? = null,
    reserveGutter: Boolean = false,
    smoothWheel: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val horizontal by remember(state) { derivedStateOf { state.layoutInfo.orientation == Orientation.Horizontal } }
    val reverse by remember(state) { derivedStateOf { state.layoutInfo.reverseLayout } }
    val resolved = adapter ?: rememberScrollbarAdapter(state)
    LazyScrollContainer(state, resolved, horizontal, reverse, modifier, gutter, scrollbar, fadingEdges, reserveGutter, smoothWheel, content)
}

@Composable
private fun LazyScrollContainer(
    state: ScrollableState,
    adapter: ScrollbarAdapter,
    horizontal: Boolean,
    reverse: Boolean,
    modifier: Modifier,
    gutter: Dp?,
    scrollbar: Boolean,
    fadingEdges: Boolean,
    reserveGutter: Boolean,
    smoothWheel: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val orientation = if (horizontal) Orientation.Horizontal else Orientation.Vertical
    val style = themedScrollbarStyle()
    val width = gutter ?: defaultGutter(style)
    val reserved = scrollbar && reserveGutter
    Box(modifier.then(if (smoothWheel) Modifier.smoothWheelScroll(state, orientation) else Modifier)) {
        if (fadingEdges || reserved) {
            val viewport = Modifier
                .then(if (!reserved) Modifier else if (horizontal) Modifier.padding(bottom = width) else Modifier.padding(end = width))
                .then(if (fadingEdges) Modifier.fadingEdgesMask(state, ScrollContainerFade, orientation) else Modifier)
            Box(viewport) { content() }
        } else {
            content()
        }
        if (scrollbar) GutterScrollbar(adapter, horizontal, reverse, style, width)
    }
}

@Composable
private fun BoxScope.GutterScrollbar(adapter: ScrollbarAdapter, horizontal: Boolean, reverse: Boolean, style: ScrollbarStyle, gutter: Dp) {
    val inset = ((gutter - style.thickness) / 2).coerceAtLeast(0.dp)
    Box(Modifier.matchParentSize()) {
        ThemedScrollbar(
            adapter,
            horizontal = horizontal,
            reverse = reverse,
            style = style,
            modifier = if (horizontal) {
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = inset)
            } else {
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = inset)
            },
        )
    }
}

@Composable
private fun defaultGutter(style: ScrollbarStyle): Dp = style.thickness + OneConfigTheme.current.spacing.xs * 2

@ExperimentalOneConfigUiApi
@Composable
public fun Scrollbar(state: LazyListState, modifier: Modifier = Modifier) {
    val horizontal by remember(state) { derivedStateOf { state.layoutInfo.orientation == Orientation.Horizontal } }
    val reverse by remember(state) { derivedStateOf { state.layoutInfo.reverseLayout } }
    ThemedScrollbar(rememberScrollbarAdapter(state), horizontal, reverse, themedScrollbarStyle(), modifier)
}

@ExperimentalOneConfigUiApi
@Composable
public fun Scrollbar(state: LazyGridState, modifier: Modifier = Modifier) {
    val horizontal by remember(state) { derivedStateOf { state.layoutInfo.orientation == Orientation.Horizontal } }
    val reverse by remember(state) { derivedStateOf { state.layoutInfo.reverseLayout } }
    ThemedScrollbar(rememberScrollbarAdapter(state), horizontal, reverse, themedScrollbarStyle(), modifier)
}

@ExperimentalOneConfigUiApi
public fun Modifier.fadingEdgesMask(state: ScrollableState, length: Dp = 24.dp): Modifier =
    fadingEdgesMask(state, length, null)

internal fun Modifier.fadingEdgesMask(state: ScrollableState, length: Dp, orientation: Orientation?): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val vertical = (orientation ?: orientationOf(state)) == Orientation.Vertical
            val extent = if (vertical) size.height else size.width
            val edge = length.toPx().coerceAtMost(extent / 2f)
            if (edge <= 0f) return@drawWithContent
            val start = (backwardDistance(state) / edge).coerceIn(0f, 1f)
            val end = (forwardDistance(state, vertical) / edge).coerceIn(0f, 1f)
            if (start == 0f && end == 0f) return@drawWithContent
            val stops = arrayOf(
                0f to MaskOpaque.copy(alpha = 1f - start),
                edge / extent to MaskOpaque,
                1f - edge / extent to MaskOpaque,
                1f to MaskOpaque.copy(alpha = 1f - end),
            )
            val brush = if (vertical) Brush.verticalGradient(*stops) else Brush.horizontalGradient(*stops)
            drawRect(brush, blendMode = BlendMode.DstIn)
        }

@ExperimentalOneConfigUiApi
@Composable
public fun themedScrollbarStyle(): ScrollbarStyle {
    val base = LocalScrollbarStyle.current
    val color = OneConfigTheme.current.textColorSecondary
    return remember(base, color) {
        themedScrollbarStyle(color, base.shape, base.thickness, base.minimalHeight, base.hoverDurationMillis)
    }
}

@ExperimentalOneConfigUiApi
public fun themedScrollbarStyle(
    color: Color,
    shape: Shape,
    thickness: Dp = ScrollbarThickness,
    minimalHeight: Dp = ScrollbarMinimalHeight,
    hoverDurationMillis: Int = SCROLLBAR_HOVER_MILLIS,
): ScrollbarStyle = ScrollbarStyle(
    minimalHeight = minimalHeight,
    thickness = thickness,
    shape = shape,
    hoverDurationMillis = hoverDurationMillis,
    unhoverColor = color.copy(alpha = SCROLLBAR_IDLE_ALPHA),
    hoverColor = color.copy(alpha = SCROLLBAR_HOVER_ALPHA),
)

@Composable
private fun ThemedScrollbar(adapter: ScrollbarAdapter, horizontal: Boolean, reverse: Boolean, style: ScrollbarStyle, modifier: Modifier) {
    if (horizontal) {
        HorizontalScrollbar(adapter, modifier, reverseLayout = reverse, style = style)
    } else {
        VerticalScrollbar(adapter, modifier, reverseLayout = reverse, style = style)
    }
}

private fun orientationOf(state: ScrollableState): Orientation = when (state) {
    is LazyListState -> state.layoutInfo.orientation
    is LazyGridState -> state.layoutInfo.orientation
    else -> Orientation.Vertical
}

private fun backwardDistance(state: ScrollableState): Float {
    if (!state.canScrollBackward) return 0f
    return when (state) {
        is ScrollState -> state.value.toFloat()
        is LazyListState -> if (state.firstVisibleItemIndex > 0) Float.POSITIVE_INFINITY else state.firstVisibleItemScrollOffset.toFloat()
        is LazyGridState -> if (state.firstVisibleItemIndex > 0) Float.POSITIVE_INFINITY else state.firstVisibleItemScrollOffset.toFloat()
        else -> Float.POSITIVE_INFINITY
    }
}

private fun forwardDistance(state: ScrollableState, vertical: Boolean): Float {
    if (!state.canScrollForward) return 0f
    return when (state) {
        is ScrollState -> (state.maxValue - state.value).toFloat()
        is LazyListState -> {
            val info = state.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            if (last == null || last.index < info.totalItemsCount - 1) {
                Float.POSITIVE_INFINITY
            } else {
                (last.offset + last.size + info.afterContentPadding - info.viewportEndOffset).toFloat()
            }
        }
        is LazyGridState -> {
            val info = state.layoutInfo
            val items = info.visibleItemsInfo
            if (items.isEmpty() || items.maxOf { it.index } < info.totalItemsCount - 1) {
                Float.POSITIVE_INFINITY
            } else {
                val end = items.maxOf { if (vertical) it.offset.y + it.size.height else it.offset.x + it.size.width }
                (end + info.afterContentPadding - info.viewportEndOffset).toFloat()
            }
        }
        else -> Float.POSITIVE_INFINITY
    }
}
