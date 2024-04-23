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

package org.polyfrost.oneconfig.ui.v1.keybind

import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.input.Modifiers
import java.util.function.BooleanSupplier
import kotlin.experimental.ExperimentalTypeInference

/**
 * Java builder-style helper for creating keybinds.
 */
@OptIn(ExperimentalTypeInference::class)
class KeybindHelper {
    private var duration = 0L
    private var keys = ArrayList<Keys>(2)
    private var mods: Modifiers = Modifiers(0)
    private var unmappedKeys = ArrayList<Int>(2)
    private var mouse = ArrayList<Int>(2)
    private var func: (() -> Boolean)? = null

    fun build(): KeyBinder.Bind {
        val func = func ?: throw IllegalStateException("Function must be set")
        return KeyBinder.Bind(
            unmappedKeys.nullIfEmpty()?.toIntArray(),
            keys.nullIfEmpty()?.toTypedArray(),
            mouse.nullIfEmpty()?.toIntArray(),
            mods, duration, func
        )
    }

    fun register() = build().register()

    fun keys(vararg keys: Keys): KeybindHelper {
        this.keys.addAll(keys)
        return this
    }

    fun mods(mods: Byte): KeybindHelper {
        this.mods = Modifiers(mods)
        return this
    }

    fun mods(vararg mods: KeyModifiers): KeybindHelper {
        var b = 0
        for(mod in mods) {
            b = b or mod.value.toInt()
        }
        this.mods = Modifiers(b.toByte())
        return this
    }

    fun chars(vararg chars: Char): KeybindHelper {
        this.unmappedKeys.addAll(chars.map { it.code })
        return this
    }

    fun keys(vararg keys: Int): KeybindHelper {
        for (key in keys) {
            this.unmappedKeys.add(key)
        }
        return this
    }

    @OverloadResolutionByLambdaReturnType
    fun does(func: () -> Boolean): KeybindHelper {
        this.func = func
        return this
    }

    @OverloadResolutionByLambdaReturnType
    fun does(func: Runnable): KeybindHelper {
        this.func = { func.run(); true }
        return this
    }

    @OverloadResolutionByLambdaReturnType
    fun does(func: BooleanSupplier): KeybindHelper {
        this.func = { func.asBoolean }
        return this
    }


    fun duration(duration: Long): KeybindHelper {
        this.duration = duration
        return this
    }

    fun KeyBinder.Bind.register() = KeybindManager.registerKeybind(this)

    private fun <T> ArrayList<T>.nullIfEmpty() = if (this.isEmpty()) null else this


    companion object {
        @JvmStatic
        fun builder(): KeybindHelper {
            return KeybindHelper()
        }
    }
}