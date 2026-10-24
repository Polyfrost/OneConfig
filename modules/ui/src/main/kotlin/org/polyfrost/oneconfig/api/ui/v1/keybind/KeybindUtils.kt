package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper

object KeybindUtils {
    @JvmStatic
    val actionModifier: Byte
        get() = if (DesktopHelper.isMac) KeyModifiers.META else KeyModifiers.CTRL

    @JvmStatic
    fun isActionModifierPressed(modifiers: Int): Boolean =
        modifiers and (if (DesktopHelper.isMac) KeyEvent.SUPER else KeyEvent.CONTROL) != 0
}
