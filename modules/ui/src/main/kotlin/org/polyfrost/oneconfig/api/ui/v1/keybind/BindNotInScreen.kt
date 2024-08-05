package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.input.Modifiers

class BindNotInScreen(
    unmappedKeys: IntArray? = null, keys: Array<Keys>? = null,
    mouse: IntArray? = null,
    mods: Modifiers = Modifiers(0),
    durationNanos: Long = 0L,
    action: (Boolean) -> Boolean
) : KeyBinder.Bind(unmappedKeys, keys, mouse, mods, durationNanos, action) {
    override fun test(c: ArrayList<Int>, k: ArrayList<Keys>, m: ArrayList<Int>, mods: Byte, deltaTimeNanos: Long, down: Boolean): Boolean {
        return super.test(c, k, m, mods, deltaTimeNanos, down) && Platform.screen().current<Any?>() == null
    }
}