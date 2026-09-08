package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

/**
 * The first visible item of a scrollable page and how far it is scrolled past
 *
 * [token] records what the page was showing when the anchor was taken such as a search query and the
 * anchor only remounts when the token matches
 */
data class ScrollAnchor(val index: Int, val offset: Int, val token: Any? = null)

/**
 * A [LazyListState] that outlives the Compose scene
 *
 * Displaying another screen over the OneConfig UI disposes the scene and rebuilds it on the way back which
 * loses everything held in composition
 *
 * Pages that want to come back where they were keep their position in [ShellState.scrollAnchors] under a
 * stable [key] such as a mod id or page name
 *
 * Changing [resetToken] scrolls back to the top of the page
 */
@Composable
fun rememberRestorableLazyListState(
    key: String,
    resetToken: Any? = null,
    contentToken: Any? = resetToken,
): LazyListState {
    val anchor = remember(key) { ShellState.scrollAnchors[key]?.takeIf { it.token == resetToken } }
    val state = rememberLazyListState(anchor?.index ?: 0, anchor?.offset ?: 0)
    ScrollToTopOnChange(state, contentToken, initial = resetToken)
    // the effect outlives token changes so read the latest one
    val currentToken by rememberUpdatedState(resetToken)
    LaunchedEffect(state, key) {
        snapshotFlow { ScrollAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }
            .collect { ShellState.scrollAnchors[key] = it.copy(token = currentToken) }
    }
    return state
}

/** Jumps [state] back to the top whenever [key] changes */
@Composable
fun ScrollToTopOnChange(state: LazyListState, key: Any?, initial: Any? = key) {
    val previous = remember(state) { LastKey(initial) }
    if (key != null && previous.value != key) {
        previous.value = key
        state.requestScrollToItem(0)
    }
}

private class LastKey(var value: Any?)

/** [rememberRestorableLazyListState] for a grid */
@Composable
fun rememberRestorableLazyGridState(key: String): LazyGridState {
    val anchor = remember(key) { ShellState.scrollAnchors[key] }
    val state = rememberLazyGridState(anchor?.index ?: 0, anchor?.offset ?: 0)
    DisposableEffect(state, key) {
        ShellState.gridStates[key] = state
        onDispose { if (ShellState.gridStates[key] === state) ShellState.gridStates.remove(key) }
    }
    LaunchedEffect(state, key) {
        snapshotFlow { ScrollAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset) }
            .collect { ShellState.scrollAnchors[key] = it }
    }
    return state
}
