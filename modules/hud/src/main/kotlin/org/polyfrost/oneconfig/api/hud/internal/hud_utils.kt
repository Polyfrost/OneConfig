package org.polyfrost.oneconfig.api.hud.internal

import org.polyfrost.oneconfig.api.hud.Hud
import org.polyfrost.oneconfig.api.hud.HudManager
import org.polyfrost.polyui.PolyUI
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

private val scaleBlob by lazy {
    var sx = 0f
    var sy = 0f
    var s = Float.NaN
    val b = Block(
        size = 20f by 20f,
        radii = 10f.radii(),
        focusable = true,
    ).draggable(
        onStart = {
            sx = polyUI.mouseX
            sy = polyUI.mouseY
            s = Float.NaN
        },
        onDrag = {
            cur?.let {
                val dx = polyUI.mouseX - sx
                val dy = polyUI.mouseY - sy
                s = (1f + ((dx + dy) / (it.width + it.height))).coerceIn(0.5f, 3f)
                if (!s.isNaN()) {
                    it.scaleX = s
                    it.scaleY = s
                    x = it.x + (it.width * s) - (width / 2f)
                    y = it.y + (it.height * s) - (height / 2f)
                }
            }
        },
        onDrop = {
            if (!s.isNaN()) cur?.let {
                // asm: finalize the scaling
                it.rescale(s, s, false)
                it.scaleX = 1f
                it.scaleY = 1f
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
            val p = parent!!
            tx = x - p.x
            ty = y - p.y
        },
        onDrag = { snapHandlerNew() },
        onDrop = {
            if (HudManager.open) {
                // asm: the hud manager is closed when it is dragged enough
                // if it is still open, then don't add
                val p = parent!!
                x = p.x + tx
                y = p.y + ty
                polyUI.inputManager.recalculate()
                return@draggable
            }
            val hud = this@buildNew.clone().build()

            polyUI.master.addChild(hud, reposition = false)
            hud.x = x
            hud.y = y

            val p = parent!!
            x = p.x + tx
            y = p.y + ty
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
    val exe = if (freq < 0L) {
        null
    } else {
        HudManager.polyUI.every(freq) {
            if (update()) {
                get().parent?.recalculateChildren()
            }
        }
    }
    return get().addDefaultBackground(backgroundColor()).addScaler().draggable(
        onStart = {
            if (HudManager.open) HudManager.toggle()
        },
        onDrag = { snapHandler() },
        onDrop = {
            if (!intersects(minMargin, minMargin, polyUI.size.x - (minMargin * 2f), polyUI.size.y - (minMargin * 2f))) {
                PolyUI.LOGGER.warn("cannot place HUD element out of bounds!")
                x = polyUI.size.x / 2f - width / 2f
                y = polyUI.size.y / 2f - height / 2f
            }
            if (HudManager.canAutoOpen()) {
                if (!HudManager.open) HudManager.toggle()
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
                Image("close.svg".image()).setDestructivePalette().withStates(consume = true).onClick {
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
        alignment = alignC,
        children = arrayOf(this),
        radii = 6f.radii(),
    ).withBoarder().namedId("HudBackground").also {
        if (color != null) it.color = color.toAnimatable()
    }
}

private fun Block.addScaler(): Block {
    this.addEventHandler(Event.Mouse.Clicked(0)) {
        val sb = scaleBlob
        sb.renders = true
        sb.x = x + width - (sb.width / 2f)
        sb.y = y + height - (sb.height / 2f)
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
    if (HudManager.open) return

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
        if (HudManager.open) HudManager.toggle()
    }
    snapHandler()
}
