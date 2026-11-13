@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private data class CardGridHeaderKey(val name: String, val run: Int)

private sealed class CardGridEntry<out T> {
    class Header(val name: String, val key: CardGridHeaderKey) : CardGridEntry<Nothing>()

    class Item<T>(val value: T, val key: Any) : CardGridEntry<T>()
}

@ExperimentalOneConfigUiApi
@Composable
public fun <T> CardGrid(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    minCellWidth: Dp = 180.dp,
    state: LazyGridState = rememberLazyGridState(),
    group: ((T) -> String)? = null,
    content: @Composable (T) -> Unit,
) {
    val spacing = OneConfigTheme.current.spacing
    val entries = remember(items, key, group) { cardGridEntries(items, key, group) }
    val gutter = LocalScrollbarStyle.current.thickness + spacing.xs * 2
    Box(modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minCellWidth),
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(end = gutter, bottom = spacing.l),
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            for (entry in entries) {
                when (entry) {
                    is CardGridEntry.Header -> item(key = entry.key, span = { GridItemSpan(maxLineSpan) }, contentType = CardGridHeaderKey::class) {
                        SectionHeader(
                            entry.name,
                            Modifier
                                .animateItem(fadeInSpec = null, placementSpec = Motion.itemPlacementSpec(), fadeOutSpec = null)
                                .padding(top = if (entry.key.run == 0) 0.dp else spacing.m),
                        )
                    }
                    is CardGridEntry.Item -> item(key = entry.key) {
                        Box(Modifier.animateItem(fadeInSpec = null, placementSpec = Motion.itemPlacementSpec(), fadeOutSpec = null)) {
                            content(entry.value)
                        }
                    }
                }
            }
        }
        Box(Modifier.matchParentSize()) {
            VerticalScrollbar(
                rememberScrollbarAdapter(state),
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(horizontal = spacing.xs),
            )
        }
    }
}

private fun <T> cardGridEntries(items: List<T>, key: (T) -> Any, group: ((T) -> String)?): List<CardGridEntry<T>> {
    if (group == null) return items.map { CardGridEntry.Item(it, key(it)) }
    val out = ArrayList<CardGridEntry<T>>(items.size + 4)
    var current: String? = null
    var run = 0
    for (item in items) {
        val name = group(item)
        if (name != current) {
            out += CardGridEntry.Header(name, CardGridHeaderKey(name, run++))
            current = name
        }
        out += CardGridEntry.Item(item, key(item))
    }
    return out
}
