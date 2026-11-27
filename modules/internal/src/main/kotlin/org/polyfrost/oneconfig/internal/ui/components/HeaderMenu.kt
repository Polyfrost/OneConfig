package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry

internal object HeaderMenus {
    private class Slot(val entryId: String?, val entries: () -> List<MenuEntry>)

    private var slots by mutableStateOf(emptyList<Slot>())

    fun entriesFor(entryId: String?): (() -> List<MenuEntry>)? =
        slots.lastOrNull { it.entryId == entryId }?.entries

    fun register(entryId: String?, entries: () -> List<MenuEntry>): Any {
        val slot = Slot(entryId, entries)
        slots = slots + slot
        return slot
    }

    fun unregister(token: Any) {
        slots = slots.filterNot { it === token }
    }
}

@Composable
internal fun HeaderMenu(entries: () -> List<MenuEntry>) {
    val entryId = (LocalLifecycleOwner.current as? NavBackStackEntry)?.id
    val latest by rememberUpdatedState(entries)
    DisposableEffect(entryId) {
        val token = HeaderMenus.register(entryId) { latest() }
        onDispose { HeaderMenus.unregister(token) }
    }
}
