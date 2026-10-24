package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.api.ui.v1.keybind.ConfigKeybind
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle
import org.polyfrost.oneconfig.internal.ui.keybind.collectAllKeybindGroups

class OneConfigUIKeybindBridgeImpl : OneConfigUIKeybindBridge {
    override fun configKeybinds(): List<ConfigKeybind> = collectAllKeybindGroups().flatMap { group ->
        val modTitle = group.modTitle.asRenderText()
        group.entries.mapNotNull { entry ->
            val keybind = entry.prop.get() as? OneConfigKeybind ?: return@mapNotNull null
            val title = entry.prop.localizedTitle().asRenderText().ifBlank { entry.path }
            KeybindInterop.configKeybind(keybind, title, entry.path, group.modId, modTitle)
        }
    }
}
