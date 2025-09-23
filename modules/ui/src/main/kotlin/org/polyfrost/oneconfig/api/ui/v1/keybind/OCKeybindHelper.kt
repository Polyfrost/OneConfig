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

import dev.deftu.omnicore.api.client.input.OmniKey
import org.polyfrost.polyui.input.KeybindHelper
import org.polyfrost.polyui.input.PolyBind
import org.polyfrost.polyui.utils.nullIfEmpty

/**
 * Java builder-style helper for creating keybinds.
 */
@Suppress("UnstableApiUsage")
class OCKeybindHelper : KeybindHelper<OCKeybindHelper>() {
    private var inScreens = false

    override fun build(): PolyBind {
        val func = func ?: throw IllegalStateException("Function must be set")
        return if (!inScreens) BindNotInScreen(
            unmappedKeys.nullIfEmpty()?.toIntArray(),
            keys.ifEmpty { null }?.toTypedArray(),
            mouse.nullIfEmpty()?.toIntArray(),
            mods, duration, func
        ) else super.build()
    }

    fun keys(vararg keys: OmniKey): OCKeybindHelper {
        for (key in keys) {
            this.unmappedKeys.add(key.code)
        }
        return this
    }

    fun inScreens(): OCKeybindHelper {
        inScreens = true
        return this
    }

    fun register() = build().register()

    fun PolyBind.register() = KeybindManager.registerKeybind(this)

    companion object {
        @JvmStatic
        fun builder() = OCKeybindHelper()
    }
}