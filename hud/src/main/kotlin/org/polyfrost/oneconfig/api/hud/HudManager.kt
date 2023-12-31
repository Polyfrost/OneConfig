/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.api.hud

import org.polyfrost.oneconfig.api.hud.collector.ReflectiveHudCollector
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.utils.LinkedList
import org.slf4j.Logger
import org.slf4j.LoggerFactory


// NOTE:
// do not touch! I have not attached the remaining classes for the HUD UI as it is not in a state which I am happy pushing.
// I will push it when I am happy with it.
object HudManager {
    private val huds = LinkedList<Hud<*>>()
    private lateinit var polyUI: PolyUI
    private val collector = ReflectiveHudCollector()

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger("OneConfig HUD API")

    fun register(hud: Hud<*>) {
        huds.add(hud)
    }

    fun init(renderer: Renderer) {
        if (::polyUI.isInitialized) throw IllegalStateException("HudManager already initialised!")
        polyUI = PolyUI(renderer = renderer)

    }

    fun render() {
        if (huds.size == 0) return
        polyUI.render()
    }
}
