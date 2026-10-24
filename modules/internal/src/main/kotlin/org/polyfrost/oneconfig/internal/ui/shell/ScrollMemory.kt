package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import org.polyfrost.compose.foundation.lazy.LazyListState

private class ScrollAnchor(val offset: Float, val token: Any?)

private val anchors = HashMap<String, ScrollAnchor>()

@Composable
fun rememberRestorableLazyListState(
    key: String,
    resetToken: Any? = null,
    contentToken: Any? = resetToken,
): LazyListState {
    val state = remember(key) { LazyListState() }
    val restoreTo = remember(key) { anchors[key]?.takeIf { it.token == resetToken }?.offset ?: 0f }
    val lastContent = remember(state) { arrayOf(resetToken) }
    if (contentToken != null && lastContent[0] != contentToken) {
        lastContent[0] = contentToken
        state.scroll.scrollTo(0f)
    }
    val currentToken by rememberUpdatedState(resetToken)
    DisposableEffect(state, key) {
        onDispose { anchors[key] = ScrollAnchor(state.scroll.value, currentToken) }
    }
    LaunchedEffect(state) {
        if (restoreTo <= 0f) return@LaunchedEffect
        repeat(3) { if (state.scroll.maxValue < restoreTo) withFrameNanos { } }
        state.scroll.scrollTo(restoreTo)
    }
    return state
}
