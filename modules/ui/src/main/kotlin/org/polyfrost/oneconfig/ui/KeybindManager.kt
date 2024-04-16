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

package org.polyfrost.oneconfig.ui

import org.polyfrost.oneconfig.api.events.event.KeyInputEvent
import org.polyfrost.oneconfig.api.events.event.TickEvent
import org.polyfrost.oneconfig.api.events.eventHandler
import org.polyfrost.oneconfig.libs.universal.UKeyboard
import org.polyfrost.oneconfig.platform.Platform
import org.polyfrost.polyui.event.InputManager
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.property.Settings

object KeybindManager {
    private val settings = Settings()
    private val keyBinder = KeyBinder(settings)
    val inputManager = InputManager(null, keyBinder, settings)

    @JvmStatic
    val modsMap: Map<Int, Byte>

    @JvmStatic
    val keysMap: Map<Int, Keys>


    init {
        eventHandler { event: KeyInputEvent ->
            // keybindings only work when in game (todo maybe change)?
            if (Platform.getGuiPlatform().currentScreen == null) {
                translateKey(inputManager, event.key, event.character, event.state != 0)
            }
        }.register()
        eventHandler { _: TickEvent.End ->
            keyBinder.update(50_000L, inputManager.mods)
        }.register()

        modsMap = hashMapOf(
            UKeyboard.KEY_LSHIFT to KeyModifiers.LSHIFT.value,
            UKeyboard.KEY_RSHIFT to KeyModifiers.RSHIFT.value,
            UKeyboard.KEY_LCONTROL to KeyModifiers.LCONTROL.value,
            UKeyboard.KEY_RCONTROL to KeyModifiers.RCONTROL.value,
            UKeyboard.KEY_LMENU to KeyModifiers.LALT.value,
            UKeyboard.KEY_RMENU to KeyModifiers.RALT.value,
            UKeyboard.KEY_LMETA to KeyModifiers.LMETA.value,
            UKeyboard.KEY_RMETA to KeyModifiers.RMETA.value
        )

        keysMap = hashMapOf(
            UKeyboard.KEY_F1 to Keys.F1,
            UKeyboard.KEY_F2 to Keys.F2,
            UKeyboard.KEY_F3 to Keys.F3,
            UKeyboard.KEY_F4 to Keys.F4,
            UKeyboard.KEY_F5 to Keys.F5,
            UKeyboard.KEY_F6 to Keys.F6,
            UKeyboard.KEY_F7 to Keys.F7,
            UKeyboard.KEY_F8 to Keys.F8,
            UKeyboard.KEY_F9 to Keys.F9,
            UKeyboard.KEY_F10 to Keys.F10,
            UKeyboard.KEY_F11 to Keys.F11,
            UKeyboard.KEY_F12 to Keys.F12,

            UKeyboard.KEY_ESCAPE to Keys.ESCAPE,
            UKeyboard.KEY_BACKSPACE to Keys.BACKSPACE,
            UKeyboard.KEY_TAB to Keys.TAB,
            UKeyboard.KEY_ENTER to Keys.ENTER,
            UKeyboard.KEY_END to Keys.END,
            UKeyboard.KEY_HOME to Keys.HOME,

            UKeyboard.KEY_LEFT to Keys.LEFT,
            UKeyboard.KEY_UP to Keys.UP,
            UKeyboard.KEY_RIGHT to Keys.RIGHT,
            UKeyboard.KEY_DOWN to Keys.DOWN
        )

    }

    @JvmStatic
    fun registerKeybind(bind: KeyBinder.Bind) {
        keyBinder.add(bind)
    }

    @JvmStatic
    fun translateKey(inputManager: InputManager, key: Int, character: Char, state: Boolean) {
        if (character != '\u0000') {
            inputManager.keyTyped(character)
        }

        if (state) {
            keysMap[key]?.let { inputManager.keyDown(it); return }
            modsMap[key]?.let { inputManager.addModifier(it); return }
            inputManager.keyDown(key)
        } else {
            keysMap[key]?.let { inputManager.keyUp(it); return }
            modsMap[key]?.let { inputManager.removeModifier(it); return }
            inputManager.keyUp(key)
        }

    }
}