@file:OptIn(ExperimentalOneConfigUiApi::class, InternalOneConfigUiApi::class)
@file:JvmName("HudClickRegions")

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import java.util.IdentityHashMap
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi

@ExperimentalOneConfigUiApi
public fun Hud.clickRegion(id: String, x: Float, y: Float, w: Float, h: Float, onClick: () -> Unit): Boolean =
    HudClickInput.region(this, id, x, y, w, h, onClick)

@ExperimentalOneConfigUiApi
public fun Hud.removeClickRegion(id: String): Boolean = HudClickInput.remove(this, id)

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
@ApiStatus.Internal
public object HudClickInput {
    private class Region(val hud: Hud) {
        @Volatile var x = 0f
        @Volatile var y = 0f
        @Volatile var w = 0f
        @Volatile var h = 0f
        @Volatile var onClick: () -> Unit = {}
        val hovered = mutableStateOf(false)
    }

    private val regions = IdentityHashMap<Hud, LinkedHashMap<String, Region>>()
    @Volatile private var pointerX = Float.NaN
    @Volatile private var pointerY = Float.NaN
    @Volatile private var active = false
    private var consumedPress = false

    internal fun region(hud: Hud, id: String, x: Float, y: Float, w: Float, h: Float, onClick: () -> Unit): Boolean {
        val region = synchronized(regions) { regions.getOrPut(hud) { LinkedHashMap() }.getOrPut(id) { Region(hud) } }
        region.x = x
        region.y = y
        region.w = w
        region.h = h
        region.onClick = onClick
        return region.hovered.value
    }

    internal fun remove(hud: Hud, id: String): Boolean {
        val removed = synchronized(regions) {
            val byId = regions[hud] ?: return false
            val region = byId.remove(id) ?: return false
            if (byId.isEmpty()) regions.remove(hud)
            region
        }
        if (removed.hovered.value) Snapshot.withMutableSnapshot { removed.hovered.value = false }
        return true
    }

    public fun updatePointer(guiX: Float, guiY: Float, active: Boolean) {
        pointerX = guiX
        pointerY = guiY
        this.active = active
        val changed = ArrayList<Region>(0)
        synchronized(regions) {
            if (regions.isEmpty()) return
            val iter = regions.entries.iterator()
            while (iter.hasNext()) {
                val (hud, byId) = iter.next()
                if (HudManager.activeInstances.none { it === hud }) {
                    for (region in byId.values) if (region.hovered.value) changed += region
                    iter.remove()
                    continue
                }
                for (region in byId.values) if (region.hovered.value != hits(region)) changed += region
            }
        }
        if (changed.isEmpty()) return
        Snapshot.withMutableSnapshot {
            for (region in changed) region.hovered.value = !region.hovered.value
        }
    }

    public fun consumeButton(button: Int, pressed: Boolean): Boolean {
        if (button != 0) return false
        if (!pressed) return consumedPress.also { consumedPress = false }
        consumedPress = false
        if (!active || HudManager.isEditing) return false
        val target = topmostHit() ?: return false
        consumedPress = true
        try {
            target.onClick()
        } catch (e: Throwable) {
            if (e is VirtualMachineError) throw e
            HudManager.LOGGER.error("Click handler of HUD ${target.hud.title} failed", e)
        }
        return true
    }

    private fun topmostHit(): Region? {
        val byHud = synchronized(regions) {
            if (regions.isEmpty()) return null
            IdentityHashMap<Hud, List<Region>>(regions.size).also { copy ->
                for ((hud, byId) in regions) copy[hud] = byId.values.toList()
            }
        }
        val order = HudManager.zOrderedInstances()
        for (i in order.indices.reversed()) {
            val list = byHud[order[i]] ?: continue
            for (j in list.indices.reversed()) if (hits(list[j])) return list[j]
        }
        return null
    }

    private fun hits(region: Region): Boolean {
        if (!active || HudManager.isEditing) return false
        if (region.w <= 0f || region.h <= 0f) return false
        val hud = region.hud
        if (!hud.isVisible.value) return false
        val scale = hud.frameEffectiveScale
        if (scale <= 0f) return false
        val px = pointerX
        val py = pointerY
        if (px.isNaN() || py.isNaN()) return false
        val lx = (px - hud.frameX) / scale
        val ly = (py - hud.frameY) / scale
        return lx >= region.x && ly >= region.y && lx < region.x + region.w && ly < region.y + region.h
    }
}
