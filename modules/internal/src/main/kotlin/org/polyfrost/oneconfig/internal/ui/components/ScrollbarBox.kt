package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp

@Composable
internal fun ScrollbarBox(
    state: ScrollState,
    gutter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = ScrollbarBox(state, rememberScrollbarAdapter(state), gutter, modifier, content)

@Composable
internal fun ScrollbarBox(
    state: LazyListState,
    gutter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = ScrollbarBox(state, rememberScrollbarAdapter(state), gutter, modifier, content)

@Composable
internal fun ScrollbarBox(
    state: LazyGridState,
    gutter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = ScrollbarBox(state, rememberScrollbarAdapter(state), gutter, modifier, content)

@Composable
internal fun ScrollbarBox(
    state: ScrollableState,
    adapter: ScrollbarAdapter,
    gutter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val thickness = LocalScrollbarStyle.current.thickness
    Box(modifier.scrollable(state, Orientation.Vertical, reverseDirection = true)) {
        content()
        VerticalScrollbar(
            adapter = adapter,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                .padding(end = ((gutter - thickness) / 2).coerceAtLeast(0.dp)),
        )
    }
}
