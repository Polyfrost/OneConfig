package org.polyfrost.oneconfig.internal.ui.hud

import androidx.compose.runtime.snapshots.Snapshot
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud

object LegacyHudRenderer {
    fun renderLive(eventCtx: OmniRenderingContext) {
        if (!eventCtx.areGraphicsAvailable) return
        for (hud in HudManager.activeInstances) {
            if (hud !is LegacyHud || hud.hidden) continue
            hud.update()
            val hudScale = hud.effectiveScale
            Snapshot.withMutableSnapshot {
                hud.renderedW = hud.width * hudScale
                hud.renderedH = hud.height * hudScale
            }
            //#if MC >= 1.21.8
            //$$ val graphics = eventCtx.graphics ?: continue
            //$$ val pose = graphics.pose()
            //$$ pose.pushMatrix()
            //$$ pose.translate(hud.x, hud.y)
            //$$ if (hudScale != 1f) pose.scale(hudScale, hudScale)
            //$$ hud.render(eventCtx)
            //$$ pose.popMatrix()
            //#else
            val graphics = eventCtx.graphics ?: continue
            val pose = graphics.pose()
            pose.pushPose()
            pose.translate(hud.x.toDouble(), hud.y.toDouble(), 0.0)
            if (hudScale != 1f) pose.scale(hudScale, hudScale, 1f)
            hud.render(eventCtx)
            graphics.flush()
            pose.popPose()
            //#endif
        }
    }
}
