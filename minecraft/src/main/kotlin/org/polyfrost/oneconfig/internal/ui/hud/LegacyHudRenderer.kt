package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.snapshots.Snapshot
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud

object LegacyHudRenderer {
    fun renderLive(graphics: GuiGraphicsExtractor) {
        for (hud in HudManager.activeInstances) {
            if (hud !is LegacyHud || hud.hidden) continue
            if (HudManager.isDebugScreenVisible && !hud.showInF3) continue
            if (HudManager.isTabListVisible && !hud.showInTab) continue
            if (HudManager.isGuiScreenOpen && !hud.showInScreens) continue
            hud.update()
            val hudScale = hud.effectiveScale
            Snapshot.withMutableSnapshot {
                hud.renderedW = hud.width * hudScale
                hud.renderedH = hud.height * hudScale
            }
            //? >= 1.21.8 {
            val pose = graphics.pose()
            pose.pushMatrix()
            pose.translate(hud.x, hud.y)
            if (hudScale != 1f) pose.scale(hudScale, hudScale)
            hud.render(graphics)
            pose.popMatrix()
            //? } else {
            /*val graphics = eventCtx.graphics ?: continue
            val pose = graphics.pose()
            pose.pushPose()
            pose.translate(hud.x.toDouble(), hud.y.toDouble(), 0.0)
            if (hudScale != 1f) pose.scale(hudScale, hudScale, 1f)
            hud.render(eventCtx)
            graphics.flush()
            pose.popPose()
            *///? }
        }
    }
}
