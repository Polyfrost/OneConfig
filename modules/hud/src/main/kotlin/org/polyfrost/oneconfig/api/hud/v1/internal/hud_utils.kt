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
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.PopupMenu
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.fastEach
import kotlin.math.sqrt

private val LOGGER = LogManager.getLogger("OneConfig/HUD")

private val scaleBlob by lazy {
    var sx = 0f
    var sy = 0f
    var st = 1f
    val b = Block(
        size = 20f by 20f,
        focusable = true,
    ).radius(10f).draggable().onDragStart {
        sx = polyUI.mouseX
        sy = polyUI.mouseY
        st = cur?.scaleX ?: 1f
    }.onDrag {
        cur?.let {
            val dx = polyUI.mouseX - sx
            val dy = polyUI.mouseY - sy
            val dst = sqrt(dx * dx + dy * dy)
            val init = sqrt(it.width * it.width + it.height * it.height)
            val sign = if (dx + dy < 0f) -1f else 1f
            val s = (st + sign * (dst / init)).coerceIn(0.5f, 3f)

            it.scaleX = s
            it.scaleY = s
            x = it.x + (it.width * s) - (width / 2f)
            y = it.y + (it.height * s) - (height / 2f)
        }
    }.apply {
//        addEventHandler(Event.Mouse.Pressed(0)) {
        // if(!polyUI.inputManager.hasFocused) polyUI.focus(this)
//        }
        on(Event.Focused.Lost) {
            cur = null
        }
    }.setPalette { brand.fg }
    HudManager.polyUI.master.addChild(b, recalculate = false)
    b.renders = false
    b
}

private var cur: Drawable? = null
    set(value) {
        field = value
        scaleBlob.renders = value != null
    }

/**
 * Build a HUD element, turning the given HUD into a representation for the HUD picker screen.
 *
 * The returned element is a [Block] with the given HUD as its only child.
 *
 * The returned element is draggable, and will be added to the screen when dropped.
 */
fun Hud<*>.buildNew(): Drawable {
    var tx = 0f
    var ty = 0f
    if (!multipleInstancesAllowed() && isReal) {
        return makeAlreadyUsed()
    }
    val o = get().addDefaultBackground(hasBackground(), backgroundColor()).draggable(free = true)
        .onDragStart {
            tx = x - parent.x
            ty = y - parent.y
        }
        .onDrag { snapHandlerNew() }
        .onDragEnd {
            if (HudManager.panelOpen) {
                // asm: the hud manager is closed when it is dragged enough
                // if it is still open, then don't add
                val p = parent
                x = p.x + tx
                y = p.y + ty
                polyUI.inputManager.recalculate()
                return@onDragEnd
            }
            val hud = this@buildNew.make().build()
            val canMultiply = this@buildNew.multipleInstancesAllowed()
            if (!canMultiply) {
                val p = this.parent
                p.removeChild(this, recalculate = false)
                p.addChild(this@buildNew.makeAlreadyUsed())
            }

            polyUI.master.addChild(hud, recalculate = false)
            hud.x = x
            hud.y = y

            if (canMultiply) {
                x = parent.x + tx
                y = parent.y + ty
                polyUI.inputManager.recalculate()
            }
            if (HudManager.canAutoOpen()) {
                HudManager.toggle()
            }
        }
    initialize()
    return o
}

fun Hud<*>.makeAlreadyUsed(): Block {
    return Block(Text(title, fontSize = 16f)).withHoverStates().onClick { HudManager.openHudEditor(this@makeAlreadyUsed) }
}

/**
 * Build a HUD element, turning the given HUD into a final HUD element,
 * ready to be placed on the screen.
 */
fun Hud<*>.build(): Drawable {
    val freq = updateFrequency()
    if (freq == 0L) LOGGER.warn("update of HUD $this is 0, this is not recommended!")
    val exe = if (freq < 0L) {
        null
    } else {
        HudManager.polyUI.every(freq) {
            if (update()) {
                getBackground()?.recalculate()
            }
        }
    }

    val o = get().addDefaultBackground(hasBackground(), backgroundColor()).addScaler().draggable()
        .onDragStart {
            if (HudManager.panelOpen) HudManager.toggle()
            cur = null
        }
        .onDrag { snapHandler() }
        .onDragEnd {
            if (!intersects(minMargin, minMargin, polyUI.size.x - (minMargin * 2f), polyUI.size.y - (minMargin * 2f))) {
                LOGGER.warn("cannot place HUD element out of bounds!")
                x = polyUI.size.x / 2f - width / 2f
                y = polyUI.size.y / 2f - height / 2f
            }
            if (HudManager.canAutoOpen()) {
                if (!HudManager.panelOpen) HudManager.toggle()
            }
        }
        .events {
            Event.Mouse.Clicked(0, amountClicks = 2) then {
                HudManager.openHudEditor(this@build)
            }
            Event.Mouse.Clicked(1) then {
                PopupMenu(
                    Text("oneconfig.huds.edit").withHoverStates(consume = true).onClick {
                        HudManager.openHudEditor(this@build)
                        HudManager.polyUI.unfocus()
                    },
                    Image("assets/oneconfig/ico/close.svg").setDestructivePalette().withHoverStates(consume = true).onClick {
                        HudManager.polyUI.master.removeChild(this@events.self, recalculate = false)
                        HudManager.polyUI.removeExecutor(exe)
                        HudManager.polyUI.unfocus()
//                    if (HudManager.panel[3] !== HudManager.hudsPage) HudManager.panel[3] = HudManager.hudsPage
                    },
                    polyUI = HudManager.polyUI,
                    position = Point.Above,
                )
                true
            }
        }
    initialize()
    return o
}

private fun <T : Drawable> T.addDefaultBackground(add: Boolean, color: PolyColor?) = if (!add) this else Block(
    this,
    alignment = alignC,
    color = color,
).radius(6f).withBorder().namedId("HudBackground")

private fun Drawable.addScaler(): Drawable {
    this.on(Event.Mouse.Clicked) {
        val sb = scaleBlob
        sb.renders = true
        val vs = visibleSize
        sb.x = x + vs.x - (sb.width / 2f)
        sb.y = y + vs.y - (sb.height / 2f)
        cur = this
        polyUI.focus(scaleBlob)
        return@on false
    }
    return this
}

private fun Component.trySnapX(lx: Float, sw: Float): Boolean {
    val low = lx - snapMargin
    val high = lx + snapMargin
    if (x + (sw / 2f) in low..high) {
        x = lx - (sw / 2f)
        HudManager.slinex = lx
        return true
    }
    if (x in low..high) {
        x = lx
        HudManager.slinex = lx
        return true
    }
    if (x + sw in low..high) {
        x = lx - sw
        HudManager.slinex = lx
        return true
    }
    return false
}

private fun Component.trySnapY(ly: Float, sh: Float): Boolean {
    val low = ly - snapMargin
    val high = ly + snapMargin
    if (y + (sh / 2f) in low..high) {
        y = ly - (sh / 2f)
        HudManager.sliney = ly
        return true
    }
    if (y in low..high) {
        y = ly
        HudManager.sliney = ly
        return true
    }
    if (y + sh in low..high) {
        y = ly - sh
        HudManager.sliney = ly
        return true
    }
    return false
}

/**
 * Method to be used as the `onDrag` handler for HUD elements.
 */
fun Drawable.snapHandler() {
    val vs = visibleSize
    val w = vs.x * scaleX
    val h = vs.y * scaleY
    if (cur === this) {
        scaleBlob.let {
            it.x = x + w - (it.width / 2f)
            it.y = y + h - (it.height / 2f)
        }
    }
    HudManager.slinex = -1f
    HudManager.sliney = -1f
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
        if (it === HudManager.panel || it === scaleBlob) return@fastEach
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

fun Drawable.snapHandlerNew() {
    // closes the hud manager and prepares the hud to be added once it is dragged outside of it
    if (polyUI.mouseX !in (polyUI.size.x - HudManager.panel.width)..polyUI.size.x) {
        if (HudManager.panelOpen) HudManager.toggle()
    }
    snapHandler()
}
