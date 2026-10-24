package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import org.polyfrost.compose.foundation.ScrollState
import org.polyfrost.compose.foundation.VerticalScrollbar
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.BoxScope
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxHeight
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.lazy.LazyListState
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.key.onKeyDown
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.coerceAtLeast
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.poly.scrollKeys
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
internal fun ScrollbarBox(
    state: LazyListState,
    gutter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = ScrollbarBox(state.scroll, gutter, modifier, content)

@Composable
internal fun ScrollbarBox(
    state: ScrollState,
    gutter: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val theme = LocalTheme.current
    val thickness = 8.dp
    Box(modifier.onPointerEvent { it.type == PointerEventType.Scroll && state.wheel(it.scroll) }.onKeyDown(scrollKeys(state))) {
        content()
        VerticalScrollbar(
            state,
            color = theme.textColorSecondary.withAlpha(0.40f),
            hoverColor = theme.textColorSecondary.withAlpha(0.70f),
            modifier = Modifier.padding(end = ((gutter - thickness) / 2).coerceAtLeast(0.dp)).fillMaxHeight().align(Alignment.TopEnd),
            thickness = thickness,
            radius = theme.scrollBarShape.radius.dp,
        )
    }
}
