package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.polyfrost.oneconfig.api.platform.v1.Platform
import kotlin.experimental.and

object KeyModifiers {
    const val NONE: Byte = 0
    const val SHIFT: Byte = 1
    const val CTRL: Byte = 2
    const val ALT: Byte = 4
    const val META: Byte = 8

    /**
     * The modifier flag a key code stands for, or [NONE] when it is not a modifier
     *
     * Codes are platform input codes (GLFW key syms up to 26.2, SDL scancodes from 26.3) so they are
     * resolved through the platform rather than hardcoded
     */
    @JvmStatic
    fun of(key: Int): Byte = BY_KEY[key] ?: NONE

    fun has(mask: Byte, flag: Byte) = (mask and flag) == flag

    private val BY_KEY: Map<Int, Byte> by lazy {
        val keys = Platform.compatibility().keys()
        mapOf(
            keys.keyLeftShift to SHIFT,
            keys.keyRightShift to SHIFT,
            keys.keyLeftControl to CTRL,
            keys.keyRightControl to CTRL,
            keys.keyLeftAlt to ALT,
            keys.keyRightAlt to ALT,
            keys.keyLeftSuper to META,
            keys.keyRightSuper to META,
        )
    }
}
