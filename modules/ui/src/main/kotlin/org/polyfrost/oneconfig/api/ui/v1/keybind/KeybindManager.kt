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

import dev.deftu.omnicore.client.OmniChat
import dev.deftu.omnicore.client.OmniKeyboard
import dev.deftu.omnicore.common.OmniLoader
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.events.WindowFocusEvent
import org.polyfrost.polyui.Settings
import org.polyfrost.polyui.input.InputManager
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.utils.Int2IntMap

@Suppress("UnstableApiUsage")
object KeybindManager {
    private val LOGGER = LogManager.getLogger("OneConfig/Keybinds")
    private val settings = Settings()
    private val keyBinder = KeyBinder(settings)
    val inputManager = InputManager(null, keyBinder, settings)

    @JvmStatic
    val modsMap: Int2IntMap

    @JvmStatic
    val keysMap: Map<Int, Keys>


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

        val m = Int2IntMap(8)
        m[OmniKeyboard.KEY_LSHIFT] = KeyModifiers.LSHIFT.value.toInt()
        m[OmniKeyboard.KEY_RSHIFT] = KeyModifiers.RSHIFT.value.toInt()
        m[OmniKeyboard.KEY_LCONTROL] = KeyModifiers.LCONTROL.value.toInt()
        m[OmniKeyboard.KEY_RCONTROL] = KeyModifiers.RCONTROL.value.toInt()
        m[OmniKeyboard.KEY_LMENU] = KeyModifiers.LALT.value.toInt()
        m[OmniKeyboard.KEY_RMENU] = KeyModifiers.RALT.value.toInt()
        m[OmniKeyboard.KEY_LMETA] = KeyModifiers.LMETA.value.toInt()
        m[OmniKeyboard.KEY_RMETA] = KeyModifiers.RMETA.value.toInt()
        modsMap = m

        keysMap = hashMapOf(
            OmniKeyboard.KEY_F1 to Keys.F1,
            OmniKeyboard.KEY_F2 to Keys.F2,
            OmniKeyboard.KEY_F3 to Keys.F3,
            OmniKeyboard.KEY_F4 to Keys.F4,
            OmniKeyboard.KEY_F5 to Keys.F5,
            OmniKeyboard.KEY_F6 to Keys.F6,
            OmniKeyboard.KEY_F7 to Keys.F7,
            OmniKeyboard.KEY_F8 to Keys.F8,
            OmniKeyboard.KEY_F9 to Keys.F9,
            OmniKeyboard.KEY_F10 to Keys.F10,
            OmniKeyboard.KEY_F11 to Keys.F11,
            OmniKeyboard.KEY_F12 to Keys.F12,

            OmniKeyboard.KEY_ESCAPE to Keys.ESCAPE,
            OmniKeyboard.KEY_BACKSPACE to Keys.BACKSPACE,
            OmniKeyboard.KEY_TAB to Keys.TAB,
            OmniKeyboard.KEY_ENTER to Keys.ENTER,
            OmniKeyboard.KEY_END to Keys.END,
            OmniKeyboard.KEY_HOME to Keys.HOME,

            OmniKeyboard.KEY_LEFT to Keys.LEFT,
            OmniKeyboard.KEY_UP to Keys.UP,
            OmniKeyboard.KEY_RIGHT to Keys.RIGHT,
            OmniKeyboard.KEY_DOWN to Keys.DOWN
        )
    }

    @JvmStatic
    fun registerKeybind(bind: KeyBinder.Bind): KeyBinder.Bind {
        keyBinder.add(bind)
        return bind
    }

    @JvmStatic
    fun builder() = OCKeybindHelper()

    @JvmStatic
    fun translateKey(inputManager: InputManager, key: Int, char: Char, down: Boolean) {
        // fix for modified characters not being sent in glfwCharCallback as glfwSetCharModsCallback is deprecated
        // for more info (see PolyUI/nanovg-impl/GLFWWindow)
        if (!char.isValid() && key < 255 && inputManager.mods > 1.toByte() && down && OmniLoader.paddedMinecraftVersion > 11300) {
            inputManager.keyTyped((key + 32).toChar())
        }
        try {
            val k = keysMap[key]
            if (k != null) {
                if (down) inputManager.keyDown(k)
                else inputManager.keyUp(k)
                return
            }

            val m = modsMap[key].toByte()
            if (m != 0.toByte()) {
                if (down) inputManager.addModifier(m)
                else inputManager.removeModifier(m)
                return
            }

            if (char.isValid()) {
                if (down) {
                    if (key == 0) {
                        inputManager.keyTyped(char)
                        return
                    }
                    inputManager.keyDown(char.lowercaseChar().code)
                }
                else inputManager.keyUp(char.lowercaseChar().code)
            } else {
                if (down) inputManager.keyDown(key)
                else inputManager.keyUp(key)
            }
        } catch (t: Throwable) {
            LOGGER.error("Failed to process input key=$key, char=$char, down=$down", t)
        }
    }

    private fun Char.isValid() = this != '\u0000' && !this.isISOControl() && this.isDefined()
}