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

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.PopupMenu
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.radii

private val LOGGER = LogManager.getLogger("OneConfig/HUD")

private val scaleBlob by lazy {
    var sx = 0f
    var sy = 0f
    var st = 1f
    val b = Block(
        size = 20f by 20f,
        radii = 10f.radii(),
        focusable = true,
    ).draggable(
        onStart = {
            sx = polyUI.mouseX
            sy = polyUI.mouseY
            st = scaleX
        },
        onDrag = {
            cur?.let {
                val dx = polyUI.mouseX - sx
                val dy = polyUI.mouseY - sy
                val s = st + (((dx + dy) / (it.width + it.height))).coerceIn(0.5f, 3f)
                it.scaleX = s
                it.scaleY = s
                x = it.x + (it.width * s) - (width / 2f)
                y = it.y + (it.height * s) - (height / 2f)
            }
        },
    ).apply {
        addEventHandler(Event.Mouse.Pressed(0)) {
            // if(!polyUI.inputManager.hasFocused) polyUI.focus(this)
        }
        addEventHandler(Event.Focused.Lost) {
            renders = false
            cur = null
        }
    }.setPalette { brand.fg }
    HudManager.polyUI.master.addChild(b, reposition = false)
    b.renders = false
    b
}

private var cur: Drawable? = null

/**
 * Build a HUD element, turning the given HUD into a representation for the HUD picker screen.
 *
 * The returned element is a [Block] with the given HUD as its only child.
 *
 * The returned element is draggable, and will be added to the screen when dropped.
 */
fun Hud<out Drawable>.buildNew(): Block {
    var tx = 0f
    var ty = 0f
    return get().addDefaultBackground(backgroundColor()).draggable(
        free = true,
        onStart = {
            tx = x - parent.x
            ty = y - parent.y
        },
        onDrag = { snapHandlerNew() },
        onDrop = {
            if (HudManager.panelOpen) {
                // asm: the hud manager is closed when it is dragged enough
                // if it is still open, then don't add
                val p = parent
                x = p.x + tx
                y = p.y + ty
                polyUI.inputManager.recalculate()
                return@draggable
            }
            val hud = this@buildNew.make().build()

            polyUI.master.addChild(hud, reposition = false)
            hud.x = x
            hud.y = y

            x = parent.x + tx
            y = parent.y + ty
            polyUI.inputManager.recalculate()
            if (HudManager.canAutoOpen()) {
                HudManager.toggle()
            }
        },
    )
}

/**
 * Build a HUD element, turning the given HUD into a final HUD element,
 * ready to be placed on the screen.
 */
fun Hud<out Drawable>.build(): Block {
    val freq = updateFrequency()
    if (freq == 0L) LOGGER.warn("update of HUD $this is 0, this is not recommemded!")
    val exe = if (freq < 0L) {
        null
    } else {
        HudManager.polyUI.every(freq) {
            if (update()) {
                get().parent.recalculateChildren()
            }
        }
    }

    return get().addDefaultBackground(backgroundColor()).addScaler().draggable(
        onStart = {
            if (HudManager.panelOpen) HudManager.toggle()
        },
        onDrag = { snapHandler() },
        onDrop = {
            if (!intersects(minMargin, minMargin, polyUI.size.x - (minMargin * 2f), polyUI.size.y - (minMargin * 2f))) {
                LOGGER.warn("cannot place HUD element out of bounds!")
                x = polyUI.size.x / 2f - width / 2f
                y = polyUI.size.y / 2f - height / 2f
            }
            if (HudManager.canAutoOpen()) {
                if (!HudManager.panelOpen) HudManager.toggle()
            }
        },
    ).events {
        Event.Mouse.Clicked(0, amountClicks = 2) then {
            HudManager.openHudEditor(this@build)
        }
        Event.Mouse.Clicked(1) then {
            PopupMenu(
                Text("oneconfig.edithud").withStates(consume = true).onClick {
                    HudManager.openHudEditor(this@build)
                    HudManager.polyUI.unfocus()
                },
                Image("assets/oneconfig/ico/close.svg".image()).setDestructivePalette().withStates(consume = true).onClick {
                    HudManager.polyUI.master.removeChild(this@events.self)
                    HudManager.polyUI.removeExecutor(exe)
                    HudManager.polyUI.unfocus()
                    if (HudManager.panel[2] !== HudManager.hudsPage) HudManager.panel[2] = HudManager.hudsPage
                },
                polyUI = HudManager.polyUI,
                position = Point.Above,
            )
            true
        }
    }
}

private fun Drawable.addDefaultBackground(color: PolyColor?): Block {
    return Block(
        this,
        at = this.x by this.y,
        alignment = alignC,
        radii = 6f.radii(),
    ).withBoarder().namedId("HudBackground").also {
        if (color != null) it.color = color.toAnimatable()
    }
}

private fun Block.addScaler(): Block {
    this.addEventHandler(Event.Mouse.Clicked(0)) {
        val sb = scaleBlob
        sb.renders = true
        sb.x = x + (width * scaleX) - (sb.width / 2f)
        sb.y = y + (height * scaleY) - (sb.height / 2f)
        cur = this
    }
    return this
}

private fun Drawable.trySnapX(lx: Float): Boolean {
    val low = lx - snapMargin
    val high = lx + snapMargin
    if (x + (width / 2f) in low..high) {
        x = lx - (width / 2f)
        HudManager.slinex = lx
        return true
    }
    if (x in low..high) {
        x = lx
        HudManager.slinex = lx
        return true
    }
    if (x + width in low..high) {
        x = lx - width
        HudManager.slinex = lx
        return true
    }
    return false
}

private fun Drawable.trySnapY(ly: Float): Boolean {
    val low = ly - snapMargin
    val high = ly + snapMargin
    if (y + (height / 2f) in low..high) {
        y = ly - (height / 2f)
        HudManager.sliney = ly
        return true
    }
    if (y in low..high) {
        y = ly
        HudManager.sliney = ly
        return true
    }
    if (y + height in low..high) {
        y = ly - height
        HudManager.sliney = ly
        return true
    }
    return false
}

/**
 * Method to be used as the `onDrag` handler for HUD elements.
 */
fun Drawable.snapHandler() {
    HudManager.slinex = -1f
    HudManager.sliney = -1f
    if (HudManager.panelOpen) return

    // asm: process screen edge snaps + center snap
    // checking center snaps first seems to make it easier to use
    var hran = trySnapX(polyUI.size.x / 2f) ||
            trySnapX(1f) ||
            trySnapX(polyUI.size.x - 1f)

    var vran = trySnapY(polyUI.size.y / 2f) ||
            trySnapY(1f) ||
            trySnapY(polyUI.size.y - 1f)

    // yipee!
    if (hran && vran) return

    // expensive!
    polyUI.master.children?.fastEach {
        if (it === this) return@fastEach
        if (it === HudManager.panel) return@fastEach
        if (!it.renders) return@fastEach

        if (!hran) {
            hran = trySnapX(it.x + (it.width / 2f)) ||
                    trySnapX(it.x) ||
                    trySnapX(it.x + it.width)
        }
        if (!vran) {
            vran = trySnapY(it.y + (it.height / 2f)) ||
                    trySnapY(it.y) ||
                    trySnapY(it.y + it.height)
        }

        // YIPEEE!
        if (hran && vran) return
    }
}

fun Drawable.snapHandlerNew() {
    // closes the hud manager and prepares the hud to be added once it is dragged outside of it
    if (polyUI.mouseX !in (polyUI.size.x - HudManager.panel.width)..polyUI.size.x) {
        if (HudManager.panelOpen) HudManager.toggle()
    }
    snapHandler()
}
