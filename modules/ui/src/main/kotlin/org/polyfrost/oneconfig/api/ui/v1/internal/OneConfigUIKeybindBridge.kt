package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.keybind.ConfigKeybind
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind

@ExperimentalOneConfigUiApi
public interface OneConfigUIKeybindBridge {
    public fun configKeybinds(): List<ConfigKeybind>
}

@ExperimentalOneConfigUiApi
public object KeybindInterop {
    @JvmStatic
    public fun configKeybind(keybind: OneConfigKeybind, title: String, path: String, modId: String, modTitle: String): ConfigKeybind =
        ConfigKeybind(keybind, title, path, modId, modTitle)
}

@ExperimentalOneConfigUiApi
private val loadedKeybindBridge: OneConfigUIKeybindBridge? by lazy {
    loadBridge(OneConfigUIKeybindBridge::class.java, "config keybinds cannot be listed")
}

@ExperimentalOneConfigUiApi
internal fun keybindBridge(): OneConfigUIKeybindBridge? = loadedKeybindBridge
