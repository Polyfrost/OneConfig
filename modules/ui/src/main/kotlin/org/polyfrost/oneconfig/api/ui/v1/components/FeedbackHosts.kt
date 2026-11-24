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

private val LocalFeedbackOwner = staticCompositionLocalOf<Any?> { null }

private val LocalFeedbackSuppressed = staticCompositionLocalOf { false }

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
@Composable
public fun FeedbackHosts(content: @Composable () -> Unit) {
    FeedbackHosts(null, content)
}

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
@Composable
public fun FeedbackHosts(owner: Any?, content: @Composable () -> Unit) {
    if (LocalFeedbackSuppressed.current) {
        content()
        return
    }
    val depth = LocalFeedbackDepth.current + 1
    val fallbackOwner = rememberFeedbackOwner()
    val resolved = owner ?: fallbackOwner
    val confirmSlot = Confirm.hosts.rememberSlot(fallback = false, depth = depth, owner = resolved)
    val toastSlot = Toasts.hosts.rememberSlot(fallback = true, depth = depth, owner = resolved)
    CompositionLocalProvider(LocalFeedbackDepth provides depth, LocalFeedbackOwner provides resolved) {
        content()
        ConfirmHost(confirmSlot)
        ToastHostLayer(toastSlot, Modifier, ToastDefaults.alignment)
    }
}

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
@Composable
public fun WithoutFeedbackHosts(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFeedbackSuppressed provides true, content = content)
}

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
public fun releaseFeedback(owner: Any) {
    Confirm.release(owner)
    Toasts.release(owner)
}

@Composable
internal fun feedbackDepth(): Int = LocalFeedbackDepth.current

@Composable
internal fun feedbackSuppressed(): Boolean = LocalFeedbackSuppressed.current

@Composable
internal fun rememberFeedbackOwner(): Any? {
    val inherited = LocalFeedbackOwner.current
    val screen = remember { currentScreen() }
    return inherited ?: screen
}

internal class HostSlot(val owner: Any?)

internal class HostStack(private val onOwnerGone: (Any) -> Unit) {
    private class Entry(val slot: HostSlot, val fallback: Boolean, val depth: Int)

    private val entries = mutableStateListOf<Entry>()

    @Composable
    fun rememberSlot(fallback: Boolean, depth: Int, owner: Any?): HostSlot {
        val slot = remember(owner) { HostSlot(owner) }
        DisposableEffect(slot, fallback, depth) {
            val entry = Entry(slot, fallback, depth)
            entries += entry
            onDispose {
                entries -= entry
                val gone = slot.owner
                if (gone != null && !hosts(gone)) onOwnerGone(gone)
            }
        }
        return slot
    }

    fun hosts(owner: Any?): Boolean = owner != null && entries.any { it.slot.owner === owner }

    fun isActive(slot: HostSlot): Boolean {
        val screen = currentScreen() ?: return false
        if (slot.owner !== screen) return false
        val onScreen = entries.filter { it.slot.owner === screen }
        val preferred = onScreen.filter { !it.fallback }.ifEmpty { onScreen }
        val deepest = preferred.maxOfOrNull { it.depth } ?: return false
        return preferred.lastOrNull { it.depth == deepest }?.slot === slot
    }
}

internal fun currentScreen(): Any? = runCatching { Platform.screen().current<Any?>() }.getOrNull()

internal fun mutateFeedbackState(block: () -> Unit) {
    if (Snapshot.current.readOnly) Snapshot.global { Snapshot.withMutableSnapshot(block) }
    else Snapshot.withMutableSnapshot(block)
}
