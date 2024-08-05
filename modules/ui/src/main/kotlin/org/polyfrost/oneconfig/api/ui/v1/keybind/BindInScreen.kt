package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.input.Modifiers

class BindInScreen(
    unmappedKeys: IntArray? = null, keys: Array<Keys>? = null,
    mouse: IntArray? = null,
    mods: Modifiers = Modifiers(0),
    durationNanos: Long = 0L,
    action: (Boolean) -> Boolean
) : KeyBinder.Bind(unmappedKeys, keys, mouse, mods, durationNanos, action)