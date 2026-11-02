package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * The editor's show/hide action
 *
 * A HUD its visibility keybind is temporarily showing or hiding goes back to its configured visibility,
 * every other HUD is shown or hidden for good
 */
fun toggleHudVisibility(huds: List<Hud>) {
    if (huds.isEmpty()) return
    val hide = huds.any { !it.effectivelyHidden }
    Snapshot.withMutableSnapshot {
        for (hud in huds) {
            if (hud.visibilityKeyActive) hud.resetVisibilityKey() else hud.hidden = hide
        }
    }
}
