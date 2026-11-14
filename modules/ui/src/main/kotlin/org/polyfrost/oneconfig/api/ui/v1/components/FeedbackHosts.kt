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
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi

private val LocalFeedbackDepth = staticCompositionLocalOf { 0 }

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
@Composable
public fun FeedbackHosts(content: @Composable () -> Unit) {
    val depth = LocalFeedbackDepth.current + 1
    val confirmSlot = Confirm.hosts.rememberSlot(fallback = false, depth = depth)
    val toastSlot = Toasts.hosts.rememberSlot(fallback = true, depth = depth)
    CompositionLocalProvider(LocalFeedbackDepth provides depth) {
        content()
        ConfirmHost(confirmSlot)
        ToastHostLayer(toastSlot, Modifier, ToastDefaults.alignment)
    }
}

@Composable
internal fun feedbackDepth(): Int = LocalFeedbackDepth.current

internal class HostStack {
    private class Entry(val token: Any, val fallback: Boolean, val depth: Int, val owner: Any?)

    private val entries = mutableStateListOf<Entry>()

    @Composable
    fun rememberSlot(fallback: Boolean, depth: Int): Any {
        val token = remember { Any() }
        val owner = remember { currentScreen() }
        DisposableEffect(token, fallback, depth) {
            val entry = Entry(token, fallback, depth, owner)
            entries += entry
            onDispose { entries -= entry }
        }
        return token
    }

    fun isActive(token: Any): Boolean {
        val screen = currentScreen()
        val onScreen = if (screen == null) emptyList() else entries.filter { it.owner === screen }
        val candidates = onScreen.ifEmpty { entries.toList() }
        val preferred = candidates.filter { !it.fallback }.ifEmpty { candidates }
        val deepest = preferred.maxOfOrNull { it.depth } ?: return false
        return preferred.lastOrNull { it.depth == deepest }?.token === token
    }
}

internal fun currentScreen(): Any? = runCatching { Platform.screen().current<Any?>() }.getOrNull()

internal fun mutateFeedbackState(block: () -> Unit) {
    if (Snapshot.current.readOnly) Snapshot.global { Snapshot.withMutableSnapshot(block) }
    else Snapshot.withMutableSnapshot(block)
}
