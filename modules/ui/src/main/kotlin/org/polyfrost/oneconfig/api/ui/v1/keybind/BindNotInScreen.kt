package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.polyfrost.oneconfig.api.platform.v1.Platform

class BindNotInScreen(
    keyCodes: IntArray?,
    mouseBtns: IntArray?,
    mods: Byte,
    durationNanos: Long,
    action: (Boolean) -> Boolean,
) : OneConfigKeybind(keyCodes, mouseBtns, mods, durationNanos, action) {
    override fun test(downKeys: Set<Int>, downMouse: Set<Int>, currentMods: Byte): Boolean {
        return super.test(downKeys, downMouse, currentMods) && Platform.screen().current<Any?>() == null
    }
}
