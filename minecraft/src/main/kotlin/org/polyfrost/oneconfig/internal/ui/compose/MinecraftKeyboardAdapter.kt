package org.polyfrost.oneconfig.internal.ui.compose

import com.mojang.blaze3d.platform.InputConstants
import org.polyfrost.oneconfig.api.platform.v1.Platform
//? if >= 26.3
//import org.lwjgl.sdl.SDLKeycode.*

import java.awt.event.KeyEvent

internal object MinecraftKeyboardAdapter {
    private val keys get() = Platform.compatibility().keys()

    // AWT collapses left/right modifiers to one key code so the side is preserved via key location
    // and Compose can still tell left shift from right shift
    fun keyLocation(bindingKey: Int): Int  = when (bindingKey) {
        InputConstants.KEY_RSHIFT, InputConstants.KEY_RCONTROL, InputConstants.KEY_RALT, keys.keyRightSuper -> KeyEvent.KEY_LOCATION_RIGHT
        InputConstants.KEY_LSHIFT, InputConstants.KEY_LCONTROL, InputConstants.KEY_LALT, keys.keyLeftSuper -> KeyEvent.KEY_LOCATION_LEFT
        InputConstants.KEY_NUMPADENTER -> KeyEvent.KEY_LOCATION_NUMPAD
        else -> KeyEvent.KEY_LOCATION_STANDARD
    }

    fun toAwtKeyCode(shortcutKey: Int): Int = when (shortcutKey) {
        //? if >= 26.3 {
        /*SDLK_BACKSPACE -> KeyEvent.VK_BACK_SPACE
        SDLK_TAB -> KeyEvent.VK_TAB
        SDLK_RETURN, SDLK_KP_ENTER -> KeyEvent.VK_ENTER
        SDLK_ESCAPE -> KeyEvent.VK_ESCAPE
        SDLK_DELETE -> KeyEvent.VK_DELETE
        SDLK_RIGHT -> KeyEvent.VK_RIGHT
        SDLK_LEFT -> KeyEvent.VK_LEFT
        SDLK_DOWN -> KeyEvent.VK_DOWN
        SDLK_UP -> KeyEvent.VK_UP
        SDLK_PAGEUP -> KeyEvent.VK_PAGE_UP
        SDLK_PAGEDOWN -> KeyEvent.VK_PAGE_DOWN
        SDLK_HOME -> KeyEvent.VK_HOME
        SDLK_END -> KeyEvent.VK_END
        SDLK_INSERT -> KeyEvent.VK_INSERT
        SDLK_CAPSLOCK -> KeyEvent.VK_CAPS_LOCK
        SDLK_LSHIFT, SDLK_RSHIFT -> KeyEvent.VK_SHIFT
        SDLK_LCTRL, SDLK_RCTRL -> KeyEvent.VK_CONTROL
        SDLK_LGUI, SDLK_RGUI -> KeyEvent.VK_META
        SDLK_LALT, SDLK_RALT -> KeyEvent.VK_ALT
        in SDLK_F1..SDLK_F12 -> KeyEvent.VK_F1 + (shortcutKey - SDLK_F1)
        else -> if ((shortcutKey and (SDLK_SCANCODE_MASK or SDLK_EXTENDED_MASK)) == 0) {
            KeyEvent.getExtendedKeyCodeForChar(shortcutKey)
        } else {
            KeyEvent.VK_UNDEFINED
        }
        *///?} else {
        InputConstants.KEY_BACKSPACE -> KeyEvent.VK_BACK_SPACE
        InputConstants.KEY_TAB -> KeyEvent.VK_TAB
        InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> KeyEvent.VK_ENTER
        InputConstants.KEY_ESCAPE -> KeyEvent.VK_ESCAPE
        InputConstants.KEY_DELETE -> KeyEvent.VK_DELETE
        InputConstants.KEY_RIGHT -> KeyEvent.VK_RIGHT
        InputConstants.KEY_LEFT -> KeyEvent.VK_LEFT
        InputConstants.KEY_DOWN -> KeyEvent.VK_DOWN
        InputConstants.KEY_UP -> KeyEvent.VK_UP
        InputConstants.KEY_PAGEUP -> KeyEvent.VK_PAGE_UP
        InputConstants.KEY_PAGEDOWN -> KeyEvent.VK_PAGE_DOWN
        InputConstants.KEY_HOME -> KeyEvent.VK_HOME
        InputConstants.KEY_END -> KeyEvent.VK_END
        InputConstants.KEY_INSERT -> KeyEvent.VK_INSERT
        InputConstants.KEY_CAPSLOCK -> KeyEvent.VK_CAPS_LOCK
        InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> KeyEvent.VK_SHIFT
        InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> KeyEvent.VK_CONTROL
        keys.keyLeftSuper, keys.keyRightSuper -> KeyEvent.VK_META
        InputConstants.KEY_LALT, InputConstants.KEY_RALT -> KeyEvent.VK_ALT
        in InputConstants.KEY_F1..InputConstants.KEY_F12 -> KeyEvent.VK_F1 + (shortcutKey - InputConstants.KEY_F1)
        in InputConstants.KEY_0..InputConstants.KEY_9 -> KeyEvent.VK_0 + (shortcutKey - InputConstants.KEY_0)
        in InputConstants.KEY_A..InputConstants.KEY_Z -> KeyEvent.VK_A + (shortcutKey - InputConstants.KEY_A)
        else -> KeyEvent.VK_UNDEFINED
        //?}
    }
}
