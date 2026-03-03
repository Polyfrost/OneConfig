package org.polyfrost.oneconfig.api.hud.v1.internal

import org.polyfrost.oneconfig.api.config.v1.Properties.ktProperty
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.unit.Vec2

object HudFactory {

    /**
     * Build a HUD element, turning the given HUD into a representation for the HUD picker screen.
     *
     * The returned element is a [Block] with the given HUD as its only child.
     *
     * The returned element is draggable, and will be added to the screen when dropped.
     */
    fun buildForPickerScreen(hud: Hud<*>): Drawable {
        var tx = 0f
        var ty = 0f
        if (!hud.multipleInstancesAllowed() && hud.isReal) {
            return hud.makeAlreadyUsed()
        }
        val o = hud.get().addDefaultBackground(hud.hasBackground(), hud.backgroundColor()).draggable(free = true)
            .onDragStart {
                tx = x - parent.x
                ty = y - parent.y
            }
            .onDrag {
                // closes the hud manager and prepares the hud to be added once it is dragged outside of it
                if (polyUI.mouseX !in (polyUI.size.x - HudManager.panel.width)..polyUI.size.x) {
                    if (HudManager.panelOpen) HudManager.toggle()
                }
                HudScreenOverlay.processSnapping(hud)
            }
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
                val newHud = hud.make()
                val hudDrawable = build(hud)
                val canMultiply = hud.multipleInstancesAllowed()
                if (!canMultiply) {
                    val p = this.parent
                    p.removeChild(this, recalculate = false)
                    p.addChild(hud.makeAlreadyUsed())
                }

                polyUI.master.addChild(hudDrawable, recalculate = false)
                HudManager.activeInstances.add(newHud)
                hudDrawable.x = x
                hudDrawable.y = y

                if (canMultiply) {
                    x = parent.x + tx
                    y = parent.y + ty
                    polyUI.inputManager.recalculate()
                }
                if (HudManager.canAutoOpen()) {
                    HudManager.toggle()
                }
            }
        val min = hud.minimumSize()
        if (min != Vec2.ZERO) o.minimumSize(min)
        addPostInitTreeProperties(hud)
        hud.setup()
        return o
    }

    private fun Hud<*>.makeAlreadyUsed(): Block {
        return Block(Text(title, fontSize = 16f)).withHoverStates().onClick { HudManager.openHudEditor(this@makeAlreadyUsed) }
    }

    /**
     * Build a HUD element, turning the given HUD into a final HUD element,
     * ready to be placed on the screen.
     */
    fun build(hud: Hud<*>): Drawable {
        val freq = hud.updateFrequency()
        if (freq > 0L) {
            HudManager.polyUI.every(freq) {
                if (hud.update()) hud.getBackground()?.recalculate(false)
            }.also { hud.tree.addMetadata("updateTicker", it) }
        }


        val o = hud.get().addDefaultBackground(hud.hasBackground(), hud.backgroundColor()).draggable()
            .onDragStart {
                if (HudManager.panelOpen) HudManager.toggle()
                HudScreenOverlay.closeMenu()
            }
            .onDrag { HudScreenOverlay.processSnapping(hud) }
            .onDragEnd {
                if (!intersects(minMargin, minMargin, polyUI.size.x - (minMargin * 2f), polyUI.size.y - (minMargin * 2f))) {
                    println("cannot place HUD element out of bounds!")
                    x = polyUI.size.x / 2f - width / 2f
                    y = polyUI.size.y / 2f - height / 2f
                }
                if (HudManager.canAutoOpen()) {
                    if (!HudManager.panelOpen) HudManager.toggle()
                }
            }
            .events {
                Event.Mouse.Clicked(0, amountClicks = 2) then {
                    HudManager.openHudEditor(hud)
                }
            }
        HudScreenOverlay.addMenuOnClick(hud)
        val min = hud.minimumSize()
        if (min != Vec2.ZERO) o.minimumSize(min)
        addPostInitTreeProperties(hud)
        hud.setup()
        o.rawRescalePosition = true
        return o
    }

    private fun <T : Drawable> T.addDefaultBackground(add: Boolean, color: PolyColor?) = if (!add) this else Block(
        this,
        alignment = alignHudDefault,
        color = color ?: BLACK_HALF,
    ).radius(5f).namedId("HudBackground")

    private fun <T : Drawable> addPostInitTreeProperties(hud: Hud<T>) {
        if (hud.isReal) {
            val tree = hud.tree
            // asm: add all the background properties to the tree as well (we have to do it here because in make, the background is not be created yet)
            val cmp = hud.getBackground() ?: hud.get()
            tree["alignment"] = ktProperty(cmp::alignment)
            tree["alpha"] = ktProperty(cmp::alpha)
            tree["scaleX"] = ktProperty(cmp::scaleX)
            tree["scaleY"] = ktProperty(cmp::scaleY)
            tree["rotation"] = ktProperty(cmp::rotation)
            tree["skewX"] = ktProperty(cmp::skewX)
            tree["skewY"] = ktProperty(cmp::skewY)
            tree["padding"] = ktProperty(cmp::padding)

            if (cmp is Block) tree["radii"] = ktProperty(cmp::radii)
        }
    }

}