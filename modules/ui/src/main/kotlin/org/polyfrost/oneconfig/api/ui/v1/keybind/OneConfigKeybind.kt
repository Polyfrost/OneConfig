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
        if (!isBound) return false
        if (mods != KeyModifiers.NONE && (currentMods and mods) != mods) return false
        val keysHeld = keyCodes?.all { it in downKeys } ?: true
        val mouseHeld = mouseBtns?.all { it in downMouse } ?: true
        return keysHeld && mouseHeld
    }

    fun conflictsWith(other: OneConfigKeybind): Boolean {
        if (this === other || !isBound || !other.isBound) return false
        return mods == other.mods &&
            sameCodes(keyCodes, other.keyCodes) &&
            sameCodes(mouseBtns, other.mouseBtns)
    }

    private fun sameCodes(a: IntArray?, b: IntArray?): Boolean {
        val sa = a?.toHashSet() ?: emptySet()
        val sb = b?.toHashSet() ?: emptySet()
        return sa == sb
    }
}
