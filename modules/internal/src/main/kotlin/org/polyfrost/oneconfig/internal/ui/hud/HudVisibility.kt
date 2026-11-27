package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.oneconfig.api.hud.v1.Hud

/**
 * The editor's show/hide action
 *
 * Ends any keybind flip on the targets, then shows or hides all of them for good
 */
fun toggleHudVisibility(huds: List<Hud>) {
    if (huds.isEmpty()) return
    val hide = huds.any { !it.effectivelyHidden }
    Snapshot.withMutableSnapshot {
        for (hud in huds) {
            hud.resetVisibilityKey()
            hud.hidden = hide
        }
    }
}
