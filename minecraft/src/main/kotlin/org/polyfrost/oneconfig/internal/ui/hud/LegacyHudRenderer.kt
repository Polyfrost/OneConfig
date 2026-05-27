package org.polyfrost.oneconfig.internal.ui.hud

//~ gui_graphics
import androidx.compose.runtime.snapshots.Snapshot
import net.minecraft.client.gui.GuiGraphics
import org.polyfrost.oneconfig.api.hud.v1.HudManager

object LegacyHudRenderer {
    /*fun renderLive(eventCtx: GuiGraphics) {
        for (hud in HudManager.activeInstances) {
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
            val graphics = eventCtx.graphics ?: continue
            val pose = graphics.pose()
            pose.pushMatrix()
            pose.translate(hud.x, hud.y)
            if (hudScale != 1f) pose.scale(hudScale, hudScale)
            hud.render(eventCtx)
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
    }*/
}
