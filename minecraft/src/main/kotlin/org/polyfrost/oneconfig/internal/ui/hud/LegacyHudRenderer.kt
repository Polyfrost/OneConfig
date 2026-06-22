package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.snapshots.Snapshot
//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor
//?} else
//import net.minecraft.client.render.platform.GlStateManager
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud

private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/HUD-Render")

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
        java.util.Collections.newSetFromMap(java.util.WeakHashMap<LegacyHud, Boolean>())

    private fun reportFailure(hud: LegacyHud, e: Throwable) {
        if (!reportedFailures.add(hud)) return
        LOGGER.error(
            "Failed to render HUD ${hud.title}; it was dropped from this frame and further failures " +
                "from it will not be logged",
            e,
        )
    }

    //~ if = 1.8.9 '(graphics: GuiGraphicsExtractor)' -> '()'
    fun renderLive(graphics: GuiGraphicsExtractor) {
        //~ if = 1.8.9 'renderLiveHuds(graphics)' -> 'renderLiveHuds()'
        renderLiveHuds(graphics)
        //~ if = 1.8.9 '.render(graphics)' -> '.render()'
        if (CompatOverlayRenderer.oneConfigScreenOpen()) CompatOverlayRenderer.render(graphics)
    }

    //~ if = 1.8.9 '(graphics: GuiGraphicsExtractor)' -> '()'
    private fun renderLiveHuds(graphics: GuiGraphicsExtractor) {
        frame.clear()
        for (hud in HudManager.activeInstances) {
            if (hud !is LegacyHud) continue
            if (!HudManager.isEditing) {
                if (hud.hidden) continue
                if (HudManager.isGuiHidden) continue
                if (HudManager.isDebugScreenVisible && !hud.showInF3) continue
                if (HudManager.isTabListVisible && !hud.showInTab) continue
                if (!HudManager.overrideShowInScreens) {
                    if (HudManager.isChatScreenOpen) {
                        if (!hud.showInChat) continue
                    } else if (HudManager.isGuiScreenOpen && !hud.showInScreens) continue
                }
            }
            frame.add(hud)
        }
        for (hud in frame) {
            try {
                HudManager.updateIfDue(hud)
                val hudScale = hud.effectiveScale
                val (mw, mh) = hud.frameMinimumSize()
                val w = mw * hudScale
                val h = mh * hudScale
                if (hud.renderedW != w || hud.renderedH != h) {
                    Snapshot.withMutableSnapshot {
                        hud.renderedW = w
                        hud.renderedH = h
                    }
                }
                //? >= 1.21.8 {
                val pose = graphics.pose()
                pose.pushMatrix()
                try {
                    pose.translate(hud.x, hud.y)
                    if (hudScale != 1f) pose.scale(hudScale, hudScale)
                    hud.render(graphics)
                } finally {
                    pose.popMatrix()
                }
                //? } elif > 1.8.9 {
                /*val pose = graphics.pose()
                pose.pushPose()
                try {
                    pose.translate(hud.x.toDouble(), hud.y.toDouble(), 0.0)
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
}
