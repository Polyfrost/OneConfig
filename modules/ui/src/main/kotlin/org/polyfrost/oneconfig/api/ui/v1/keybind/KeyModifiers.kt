package org.polyfrost.oneconfig.api.ui.v1.keybind

import kotlin.experimental.and

object KeyModifiers {
    const val NONE: Byte = 0
    const val SHIFT: Byte = 1
    const val CTRL: Byte = 2
    const val ALT: Byte = 4
    const val META: Byte = 8

    fun has(mask: Byte, flag: Byte) = (mask and flag) == flag
}
