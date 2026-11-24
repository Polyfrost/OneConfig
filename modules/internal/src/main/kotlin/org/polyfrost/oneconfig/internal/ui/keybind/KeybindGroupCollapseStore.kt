package org.polyfrost.oneconfig.internal.ui.keybind

import org.polyfrost.oneconfig.internal.ui.api.CollapseStore

object KeybindGroupCollapseStore : CollapseStore("keybind-collapsed", "keybind groups") {
    override fun defaultCollapsed(id: String) = id.startsWith("minecraft-keybinds.")
}
