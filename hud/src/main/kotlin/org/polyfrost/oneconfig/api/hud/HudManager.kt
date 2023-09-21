/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.oneconfig.api.hud

import org.polyfrost.oneconfig.api.hud.collector.ReflectiveHudCollector
import org.polyfrost.oneconfig.api.hud.elements.InferringCComponent
import org.polyfrost.oneconfig.api.hud.properties.ICComponentProperties
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.utils.fastEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HudManager {
    private val huds = ArrayList<Hud>()
    private lateinit var polyUI: PolyUI
    private val collector = ReflectiveHudCollector()

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger("OneConfig HUD API")

    fun register(hud: Hud) {
        huds.add(hud)
    }

    fun init(renderer: Renderer) {
        if (::polyUI.isInitialized) throw IllegalStateException("HudManager already initialised!")
        polyUI = PolyUI(renderer = renderer)
        polyUI.master.propertyManager.addPropertyType(InferringCComponent::class, ICComponentProperties())
        huds.fastEach {
            it.init(collector.collect(it), polyUI)
        }
    }

    fun render() {
        if (huds.size == 0) return
        polyUI.render()
        huds.fastEach {
            it.renderCustoms()
        }
    }
}
