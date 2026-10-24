package org.polyfrost.oneconfig.internal.ui.shell

import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignSession
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus

object HudEditorRoute

enum class SearchFocus {
    SelectAll,

    Focus,
    Unfocus,
}

object ShellState {
    var searchQuery: String
        get() = ShellController.shared.search.query
        set(value) {
            ShellController.shared.search.update(value)
        }

    @JvmStatic
    fun handleSearchKeybind(pressed: Boolean): Boolean {
        if (KeybindRecordingBus.isRecording) return false
        if (HudDesignSession.handleSearch(pressed)) return true
        if (!pressed || !HudManager.isConfigUiOpen) return false
        ShellController.shared.search.requestFocus(SearchFocus.SelectAll)
        return true
    }
}
