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

package org.polyfrost.oneconfig.api.hud.v1.internal

import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.polyui.color.rgba
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.rescaleYToPolyUIInstance
import kotlin.math.sqrt

object HudScreenOverlay {
    private val snapLineColor = rgba(170, 170, 170, 0.8f)
    private var snapLineX = -1f
    private var snapLineY = -1f

    private val menu by lazy {
        val b = Block(
            Image("assets/oneconfig/ico/cog.svg").withHoverStates().onClick { HudManager.openHudEditor(cur ?: return@onClick) },
            Image("assets/oneconfig/ico/trash.svg").withHoverStates().setPalette { state.danger }.onClick {
                val cur = cur ?: return@onClick
                HudManager.removeHud(cur)
                closeMenu()
            },
            alignment = Align(padEdges = Vec2(10f, 8f), padBetween = Vec2(14f, 8f))
        ).withBorder(2f)
        HudManager.polyUI.master.addChild(b, recalculate = false)
        b.renders = false
        b
    }

    private val scaleBlob: Image by lazy {
        var sx = 0f
        var sy = 0f
        var st = 1f
        val b = Image(
            "assets/oneconfig/hud/selector_curve.svg".image(),
            focusable = true,
        ).draggable().onDragStart {
            menu.renders = false
            sx = polyUI.mouseX
            sy = polyUI.mouseY
            st = cur?.get()?.scaleX ?: 1f
        }.onDrag {
            cur?.get()?.let {
                val dx = polyUI.mouseX - sx
                val dy = polyUI.mouseY - sy
                val dst = sqrt(dx * dx + dy * dy)
                val init = sqrt(it.width * it.width + it.height * it.height)
                val sign = if (dx + dy < 0f) -1f else 1f
                val s = (st + sign * (dst / init)).coerceIn(0.5f, 3f)

                it.scaleX = s
                it.scaleY = s
                (cur?.getBackground() ?: it).let {
                    val (w, h) = it.visibleSize
                    x = it.x + w - (width / 2f)
                    y = it.y + h - (height / 2f)
                }
            }
            Unit
        }.onDragEnd {
            cur?.getBackground()?.let {
                menu.x = it.x + (it.visibleSize.x / 2f) - (menu.width / 2f)
                menu.y = it.y - menu.height - 8f.rescaleYToPolyUIInstance(polyUI)
            }
            menu.renders = true
        }
        HudManager.polyUI.master.addChild(b, recalculate = false)
        b.renders = false
        b
    }

    private var cur: Hud<*>? = null
        set(value) {
            value?.getBackground()?.let {
                it.borderWidth = 1f
                it.borderColor = it.polyUI.colors.brand.fg.normal
            }
            field?.getBackground()?.let {
                it.borderWidth = 0f
            }
            field = value
            scaleBlob.renders = value != null
            menu.renders = value != null
        }

    fun closeMenu() {
        cur = null
    }

    fun addMenuOnClick(hud: Hud<*>) {
        (hud.getBackground() ?: hud.get()).onClick {
            val sb = scaleBlob
            sb.renders = true
            val vs = visibleSize
            sb.x = x + vs.x - (sb.width / 2f)
            sb.y = y + vs.y - (sb.height / 2f)
            val menu = menu
            if (!menu.initialized) menu.setup(polyUI)
            menu.renders = true
            menu.x = x + vs.x / 2f - (menu.width / 2f)
            menu.y = y - menu.height - 8f.rescaleYToPolyUIInstance(polyUI)
            cur = hud
            polyUI.focus(scaleBlob)
            return@onClick false
        }
    }

    /**
     * Method to be used as the `onDrag` handler for HUD elements.
     */
    fun processSnapping(hud: Hud<*>) {
        hud.get().apply {
            val vs = visibleSize
            val w = vs.x * scaleX
            val h = vs.y * scaleY
            if (cur?.get() === this) {
                (cur?.getBackground() ?: this).apply {
                    scaleBlob.let {
                        it.x = x + w - (it.width / 2f)
                        it.y = y + h - (it.height / 2f)
                    }
                }
            }
            snapLineX = -1f
            snapLineY = -1f
            if (HudManager.panelOpen) return

            // asm: process screen edge snaps + center snap
            // checking center snaps first seems to make it easier to use
            var hran = trySnapX(polyUI.size.x / 2f, w) ||
                    trySnapX(1f, w) ||
                    trySnapX(polyUI.size.x - 1f, w)

            var vran = trySnapY(polyUI.size.y / 2f, h) ||
                    trySnapY(1f, h) ||
                    trySnapY(polyUI.size.y - 1f, h)

            // yipee!
            if (hran && vran) return

            // expensive!
            polyUI.master.children?.fastEach {
                if (it === this) return@fastEach
                if (it === HudManager.panel || it === scaleBlob || it === menu) return@fastEach
                if (!it.renders) return@fastEach
                if (it !is Drawable) return@fastEach
                val ivs = it.visibleSize
                val iw = ivs.x * it.scaleX
                val ih = ivs.y * it.scaleY

                if (!hran) {
                    hran = trySnapX(it.x + (iw / 2f), w) ||
                            trySnapX(it.x, w) ||
                            trySnapX(it.x + iw, w)
                }
                if (!vran) {
                    vran = trySnapY(it.y + (ih / 2f), h) ||
                            trySnapY(it.y, h) ||
                            trySnapY(it.y + ih, h)
                }

                // YIPEEE!
                if (hran && vran) return
            }
        }
    }

    fun addSnapLinesRenderer(component: Component) {
        component.addOperation {
            val polyUI = component.polyUI
            if (polyUI.mouseDown) {
                val renderer = polyUI.renderer
                if (snapLineX != -1f) renderer.line(snapLineX, 0f, snapLineX, polyUI.size.y, snapLineColor, 1f)
                if (snapLineY != -1f) renderer.line(0f, snapLineY, polyUI.size.x, snapLineY, snapLineColor, 1f)
            } else {
                snapLineX = -1f
                snapLineY = -1f
            }

        }
    }

    private fun Component.trySnapX(lx: Float, sw: Float): Boolean {
        val low = lx - snapMargin
        val high = lx + snapMargin
        if (x + (sw / 2f) in low..high) {
            x = lx - (sw / 2f)
            snapLineX = lx
            return true
        }
        if (x in low..high) {
            x = lx
            snapLineX = lx
            return true
        }
        if (x + sw in low..high) {
            x = lx - sw
            snapLineX = lx
            return true
        }
        return false
    }

    private fun Component.trySnapY(ly: Float, sh: Float): Boolean {
        val low = ly - snapMargin
        val high = ly + snapMargin
        if (y + (sh / 2f) in low..high) {
            y = ly - (sh / 2f)
            snapLineY = ly
            return true
        }
        if (y in low..high) {
            y = ly
            snapLineY = ly
            return true
        }
        if (y + sh in low..high) {
            y = ly - sh
            snapLineY = ly
            return true
        }
        return false
    }
}