package org.polyfrost.oneconfig.api.ui.v1.keybind

import kotlin.experimental.and

open class OneConfigKeybind(
    val keyCodes: IntArray?,
    val mouseBtns: IntArray?,
    val mods: Byte,
    val durationNanos: Long,
    @Transient
    val action: (Boolean) -> Boolean,
) {
    val isBound get() = keyCodes?.isNotEmpty() == true || mouseBtns?.isNotEmpty() == true

    open fun test(downKeys: Set<Int>, downMouse: Set<Int>, currentMods: Byte): Boolean {
        if (mods != KeyModifiers.NONE && (currentMods and mods) != mods) return false
        val keyMatch = keyCodes?.any { it in downKeys } ?: false
        val mouseMatch = mouseBtns?.any { it in downMouse } ?: false
        return keyMatch || mouseMatch
    }
}
