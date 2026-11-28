package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.snapshots.Snapshot
import java.util.Collections
import java.util.WeakHashMap
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import org.polyfrost.compose.minecraft.render.gl.GuiDraw

//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor
//?}

//? if = 1.8.9 {
/*import com.mojang.blaze3d.platform.GlStateManager
*///?}

private val LOGGER = LogManager.getLogger("OneConfig/HUD-Render")

object LegacyHudRenderer {
    /**
     * Reused across frames to hold the HUDs drawn this frame because HUD callbacks may add to or remove
     * from [HudManager.activeInstances] which must never be iterated while they run
     */
    private val frame = ArrayList<LegacyHud>()

    /**
     * HUDs that already logged a failure since a throwing HUD usually throws every frame and would flood
     * the log at framerate
     *
     * Weak so a removed HUD can be collected
     */
    private val reportedFailures =
        Collections.newSetFromMap(WeakHashMap<LegacyHud, Boolean>())

    private fun reportFailure(hud: LegacyHud, e: Throwable) {
        if (!reportedFailures.add(hud)) return
        LOGGER.error(
            "Failed to render HUD ${hud.title}; it was dropped from this frame and further failures " +
                "from it will not be logged",
            e,
        )
    }

    private val drawLive: GuiDraw = ::renderLive

    /** [renderLive], or null when it has nothing to draw */
    internal val liveDraw: GuiDraw?
        get() = drawLive.takeIf { CompatOverlayRenderer.hasHooks() || hasLegacyHud() }

    private fun hasLegacyHud(): Boolean {
        val instances = HudManager.activeInstances
        for (i in instances.indices) if (instances[i] is LegacyHud) return true
        return false
    }

    //~ if = 1.8.9 '(graphics: GuiGraphicsExtractor)' -> '()'
    fun renderLive(graphics: GuiGraphicsExtractor) {
        //~ if = 1.8.9 'renderLiveHuds(graphics)' -> 'renderLiveHuds()'
        renderLiveHuds(graphics)
        //~ if = 1.8.9 '.render(graphics)' -> '.render()'
        if (CompatOverlayRenderer.oneConfigScreenOpen() && !HudManager.isHiddenByMenu) CompatOverlayRenderer.render(graphics)
    }

    //~ if = 1.8.9 '(graphics: GuiGraphicsExtractor)' -> '()'
    private fun renderLiveHuds(graphics: GuiGraphicsExtractor) {
        frame.clear()
        val instances = HudManager.activeInstances
        for (i in instances.indices) {
            val hud = instances[i]
            if (hud !is LegacyHud) continue
            if (passesFilters(hud)) frame.add(hud) else setShown(hud, false)
        }
        for (i in frame.indices) {
            val hud = frame[i]
            try {
                HudManager.updateIfDue(hud)
                val shown = HudManager.isPreviewing(hud) || hud.shouldShow()
                setShown(hud, shown)
                if (!shown) continue
                val hudScale = hud.frameScale
                val (mw, mh) = hud.frameMinimumSize()
                val w = mw * hudScale
                val h = mh * hudScale
                if (hud.renderedW != w || hud.renderedH != h) {
                    Snapshot.withMutableSnapshot {
                        hud.renderedW = w
                        hud.renderedH = h
                    }
                }
                hud.settlePin()
                //? if >= 1.21.8 {
                val pose = graphics.pose()
                pose.pushMatrix()
                try {
                    pose.translate(hud.frameX, hud.frameY)
                    if (hudScale != 1f) pose.scale(hudScale, hudScale)
                    hud.render(graphics)
                } finally {
                    pose.popMatrix()
                }
                //? } elif > 1.8.9 {
                /*val pose = graphics.pose()
                pose.pushPose()
                try {
                    pose.translate(hud.frameX.toDouble(), hud.frameY.toDouble(), 0.0)
                    if (hudScale != 1f) pose.scale(hudScale, hudScale, 1f)
                    hud.render(graphics)
                } finally {
                    pose.popPose()
                }
                *///?} else {
                /*GlStateManager.pushMatrix()
                try {
                    GlStateManager.translatef(hud.x, hud.y, 0f)
                    if (hudScale != 1f) GlStateManager.scalef(hudScale, hudScale, 1f)
                    hud.render()
                } finally {
                    GlStateManager.popMatrix()
                }
                *///?}
            } catch (e: Throwable) {
                reportFailure(hud, e)
            }
        }
        frame.clear()
    }

    private fun passesFilters(hud: LegacyHud): Boolean {
        if (HudManager.isHiddenByMenu) return false
        if (HudManager.isPreviewing(hud)) return true
        if (hud.effectivelyHidden && !hud.drawsWhileHidden()) return false
        if (HudManager.isGuiHidden || HudManager.allHudsHiddenByKey) return false
        if (HudManager.isDebugScreenVisible && !hud.showInF3) return false
        if (HudManager.isTabListVisible && !hud.showInTab) return false
        if (!HudManager.overrideShowInScreens && !HudManager.isConfigUiOpen) {
            if (HudManager.isChatScreenOpen) return hud.showInChat
            if (HudManager.isGuiScreenOpen) return hud.showInScreens
        }
        return true
    }

    private fun setShown(hud: LegacyHud, shown: Boolean) {
        if (hud.isVisible.value != shown) Snapshot.withMutableSnapshot { hud.isVisible.value = shown }
    }
}
