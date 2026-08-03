package org.polyfrost.oneconfig.api.ui.v1.keybind

import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper

object KeybindUtils {
    @JvmStatic
    val actionModifier: Byte
        get() = if (DesktopHelper.isMac) KeyModifiers.META else KeyModifiers.CTRL

    /** Whether the platform action modifier is currently pressed */
    fun isActionModifierPressed(modifiers: PointerKeyboardModifiers): Boolean =
        if (DesktopHelper.isMac) modifiers.isMetaPressed else modifiers.isCtrlPressed
}
