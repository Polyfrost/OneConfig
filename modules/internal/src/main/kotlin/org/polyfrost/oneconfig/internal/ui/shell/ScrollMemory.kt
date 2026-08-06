package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

/**
 * The first visible item of a scrollable page, and how far it is scrolled past.
 *
 * [token] records what the page was showing when the anchor was taken (like a search query).
 * Only remount if the token is the same.
 */
data class ScrollAnchor(val index: Int, val offset: Int, val token: Any? = null)

/**
 * A [LazyListState] that outlives the Compose scene.
 *
 * Displaying another screen over the OneConfig UI disposes the scene and rebuilds it on the way back, which
 * loses everything held in composition. Pages that want to come back where they were keep their position in
 * [ShellState.scrollAnchors] under a stable [key] — a mod id, a page name — instead.
 *
 * [resetToken] identifies the token being stored, for things like search queries, if this changes it will scroll
 * to the top of the page.
 */
@Composable
fun rememberRestorableLazyListState(key: String, resetToken: Any? = null): LazyListState {
    val anchor = remember(key) { ShellState.scrollAnchors[key]?.takeIf { it.token == resetToken } }
    val state = rememberLazyListState(anchor?.index ?: 0, anchor?.offset ?: 0)
    ScrollToTopOnChange(state, resetToken)
    LaunchedEffect(state, key) {
        snapshotFlow { ScrollAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }
            .collect { ShellState.scrollAnchors[key] = it.copy(token = resetToken) }
    }
    return state
}

/**
 * Jumps [state] back to the top whenever [key] changes.
 */
@Composable
fun ScrollToTopOnChange(state: LazyListState, key: Any?) {
    val previous = remember(state) { LastKey(key) }
    if (previous.value != key) {
        previous.value = key
        state.requestScrollToItem(0)
    }
}

private class LastKey(var value: Any?)

/** [rememberRestorableLazyListState] for a grid. */
@Composable
fun rememberRestorableLazyGridState(key: String): LazyGridState {
    val anchor = remember(key) { ShellState.scrollAnchors[key] }
    val state = rememberLazyGridState(anchor?.index ?: 0, anchor?.offset ?: 0)
    LaunchedEffect(state, key) {
        snapshotFlow { ScrollAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }
            .collect { ShellState.scrollAnchors[key] = it }
    }
    return state
}
