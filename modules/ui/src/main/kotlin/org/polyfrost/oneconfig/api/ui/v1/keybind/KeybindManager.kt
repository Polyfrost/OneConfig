/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.ui.v1.keybind

import dev.deftu.omnicore.api.client.input.OmniKeys
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.events.WindowFocusEvent
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.Settings
import org.polyfrost.polyui.input.InputManager
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.input.PolyBind

@Suppress("UnstableApiUsage")
object KeybindManager {
    private val LOGGER = LogManager.getLogger("OneConfig/Keybinds")
    private val settings = Settings()
    private val keyBinder = KeyBinder(settings)
    val inputManager = InputManager(null, keyBinder, settings)


    init {
        eventHandler { (key, char, state): KeyInputEvent ->
            if (state == 2) return@eventHandler
            translateKey(inputManager, key, char, state == 1)
        }
        eventHandler { _: TickEvent.End ->
            keyBinder.update(50_000L, inputManager.mods, true)
        }

        // asm: this is an old fix which will be kept so that in the (rare) event that the keybind system fails for whatever reason,
        // the user can try to fix it by opening a screen and trying again, and it should fix the issue.
        eventHandler { (screen): ScreenOpenEvent ->
            if (screen == null) {
                inputManager.drop()
                keyBinder.release()
            }
        }
        eventHandler { _: WindowFocusEvent.Lost ->
            // clear all modifiers
            inputManager.drop()
            keyBinder.release()
        }
    }

    @JvmStatic
    fun registerKeybind(bind: PolyBind?): PolyBind? {
        if (bind != null) keyBinder.add(bind)
        return bind
    }

    @JvmStatic
    fun builder() = OCKeybindHelper()

    @JvmStatic
    fun translateKey(inputManager: InputManager, keyCode: Int, char: Char, down: Boolean) {
        try {
            if (char.isValid() && inputManager.mods <= 2) { // asm: only allow no mod or shift mod for char input
                if (down) inputManager.keyTyped(char.code)
            }
            val mod = when (keyCode) {
                OmniKeys.KEY_LEFT_SHIFT.code -> KeyModifiers.LSHIFT
                OmniKeys.KEY_LEFT_CONTROL.code -> if (PolyUI.isOnMac) KeyModifiers.LMETA else KeyModifiers.LPRIMARY
//                OmniKeys.KEY_LMENU -> KeyModifiers.LSECONDARY
//                OmniKeys.KEY_LMETA -> if (PolyUI.isOnMac) KeyModifiers.LPRIMARY else KeyModifiers.LSECONDARY
                OmniKeys.KEY_RIGHT_SHIFT.code -> KeyModifiers.RSHIFT
                OmniKeys.KEY_RIGHT_CONTROL.code -> if (PolyUI.isOnMac) KeyModifiers.RMETA else KeyModifiers.RPRIMARY
//                OmniKeys.KEY_RIGHT_MENU -> KeyModifiers.RSECONDARY
//                OmniKeys.KEY_RIGHT_META -> if (PolyUI.isOnMac) KeyModifiers.RPRIMARY else KeyModifiers.RSECONDARY
                else -> null
            }

            if (mod != null) {
                if (down) {
                    inputManager.addModifier(mod.value)
                } else {
                    inputManager.removeModifier(mod.value)
                }
                return
            }


            val key = when (keyCode) {
                OmniKeys.KEY_F1.code -> Keys.F1
                OmniKeys.KEY_F2.code -> Keys.F2
                OmniKeys.KEY_F3.code -> Keys.F3
                OmniKeys.KEY_F4.code -> Keys.F4
                OmniKeys.KEY_F5.code -> Keys.F5
                OmniKeys.KEY_F6.code -> Keys.F6
                OmniKeys.KEY_F7.code -> Keys.F7
                OmniKeys.KEY_F8.code -> Keys.F8
                OmniKeys.KEY_F9.code -> Keys.F9
                OmniKeys.KEY_F10.code -> Keys.F10
                OmniKeys.KEY_F11.code -> Keys.F11
                OmniKeys.KEY_F12.code -> Keys.F12

                OmniKeys.KEY_ESCAPE.code -> Keys.ESCAPE

                OmniKeys.KEY_ENTER.code -> Keys.ENTER
                OmniKeys.KEY_TAB.code -> Keys.TAB
                OmniKeys.KEY_BACKSPACE.code -> Keys.BACKSPACE
                OmniKeys.KEY_DELETE.code -> Keys.DELETE
                OmniKeys.KEY_HOME.code -> Keys.HOME
                OmniKeys.KEY_END.code -> Keys.END

                OmniKeys.KEY_RIGHT.code -> Keys.RIGHT
                OmniKeys.KEY_LEFT.code -> Keys.LEFT
                OmniKeys.KEY_DOWN.code -> Keys.DOWN
                OmniKeys.KEY_UP.code -> Keys.UP

                OmniKeys.KEY_C.code -> Keys.C
                OmniKeys.KEY_V.code -> Keys.V
                OmniKeys.KEY_X.code -> Keys.X
                OmniKeys.KEY_Z.code -> Keys.Z
                OmniKeys.KEY_A.code -> Keys.A
                OmniKeys.KEY_S.code -> Keys.S
                OmniKeys.KEY_P.code -> Keys.P
                OmniKeys.KEY_I.code -> Keys.I
                OmniKeys.KEY_R.code -> Keys.R
                OmniKeys.KEY_MINUS.code -> Keys.MINUS
                OmniKeys.KEY_EQUAL.code -> Keys.EQUALS

                else -> Keys.UNKNOWN
            }
            if (key != Keys.UNKNOWN) {
                if (down) inputManager.keyDown(key)
                else inputManager.keyUp(key)
            }
            if (down) inputManager.keyDown(keyCode, -1)
            else inputManager.keyUp(keyCode, -1)
        } catch (t: Throwable) {
            LOGGER.error("Failed to process input key=$keyCode, char=$char, down=$down", t)
        }
    }

    private fun Char.isValid() = this != '\u0000' && !this.isISOControl() && this.isDefined()
}