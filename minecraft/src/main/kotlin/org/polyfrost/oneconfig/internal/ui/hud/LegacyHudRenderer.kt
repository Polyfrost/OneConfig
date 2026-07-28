package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.snapshots.Snapshot
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud

object LegacyHudRenderer {
    fun renderLive(graphics: GuiGraphicsExtractor) {
        renderLiveHuds(graphics)
        if (CompatOverlayRenderer.oneConfigScreenOpen()) CompatOverlayRenderer.render(graphics)
    }

    private fun renderLiveHuds(graphics: GuiGraphicsExtractor) {
        for (hud in HudManager.activeInstances) {
            if (hud !is LegacyHud) continue
            if (hud.hidden && !HudManager.isEditing) continue
            if (HudManager.isDebugScreenVisible && !hud.showInF3) continue
            if (HudManager.isTabListVisible && !hud.showInTab) continue
            if (HudManager.isGuiScreenOpen && !hud.showInScreens && !HudManager.overrideShowInScreens) continue
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
            pose.translate(hud.x, hud.y)
            if (hudScale != 1f) pose.scale(hudScale, hudScale)
            hud.render(graphics)
            pose.popMatrix()
            //? } else {
            /*val pose = graphics.pose()
            pose.pushPose()
            pose.translate(hud.x.toDouble(), hud.y.toDouble(), 0.0)
            if (hudScale != 1f) pose.scale(hudScale, hudScale, 1f)
            hud.render(graphics)
            pose.popPose()
            *///? }
        }
    }
}
