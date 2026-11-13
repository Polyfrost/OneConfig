@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi

private val LocalFeedbackHosted = staticCompositionLocalOf { false }

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
@Composable
public fun FeedbackHosts(content: @Composable () -> Unit) {
    if (LocalFeedbackHosted.current) {
        content()
        return
    }
    CompositionLocalProvider(LocalFeedbackHosted provides true) {
        content()
        ConfirmHost()
        ToastHostLayer(Modifier, ToastDefaults.alignment, fallback = true)
    }
}

internal class HostStack {
    private class Entry(val token: Any, val fallback: Boolean)

    private val entries = mutableStateListOf<Entry>()

    @Composable
    fun rememberSlot(fallback: Boolean): Any {
        val token = remember { Any() }
        DisposableEffect(token, fallback) {
            val entry = Entry(token, fallback)
            entries += entry
            onDispose { entries -= entry }
        }
        return token
    }

    fun isActive(token: Any): Boolean {
        val active = entries.lastOrNull { !it.fallback } ?: entries.lastOrNull()
        return active?.token === token
    }
}

internal fun mutateFeedbackState(block: () -> Unit) {
    if (Snapshot.current.readOnly) Snapshot.global { Snapshot.withMutableSnapshot(block) }
    else Snapshot.withMutableSnapshot(block)
}
