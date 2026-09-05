package org.polyfrost.oneconfig.internal.ui.hud

import com.mojang.blaze3d.pipeline.RenderTarget
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
//?} elif >= 1.21.8 {
/*import net.minecraft.client.gui.render.state.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
*///?}
import org.jetbrains.skia.Paint
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.SkiaOffscreenTarget
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.slf4j.LoggerFactory

object LegacyHudOffscreen {
    private val LOG = LoggerFactory.getLogger("OneConfig/LegacyHudOffscreen")
    private val client get() = Minecraft.getInstance()

    private val offscreen = SkiaOffscreenTarget()

    @Volatile private var hasContent = false
    @Volatile private var failed = false

    private val blitPaint = Paint()

    init {
        LegacyHudOverlayBridge.painter = { c -> drawInto(c) }
    }

    fun render(): Boolean {
        hasContent = false
        if (failed) return false
        if (java.lang.Boolean.getBoolean("oneconfig.disable.legacyHudOffscreen")) return false
        if (HudManager.activeInstances.none { it is LegacyHud } && !CompatOverlayRenderer.hasHooks()) return true
        if (!SkiaCtx.isReady) return false
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return false

        try {
            if (!offscreen.resolveTarget(w, h)) return false
            val rt = offscreen.target ?: return false
            //? if >= 1.21.8 {
            renderRecorded(rt)
            //?} elif >= 1.21.5 {
            /*renderImmediate(rt)
            *///?} else {
            /*renderImmediateLegacy(rt)
            *///?}
            hasContent = true
            return true
        } catch (t: Throwable) {
            LOG.warn("Legacy HUD offscreen render failed; disabling", t)
            failed = true
            GuiTargetRedirect.target = null
            return false
        }
    }

    //? if >= 1.21.8 {
    private fun renderRecorded(rt: RenderTarget) {
        val guiRenderer = (client.gameRenderer as GameRendererAccessor).`oneconfig$getGuiRenderer`()
        val accessor = guiRenderer as GuiRendererAccessor

        val state = GuiRenderState()
        //? if >= 1.21.11 {
        val ext = GuiGraphicsExtractor(client, state, Platform.screen().guiWidth(), Platform.screen().guiHeight())
        //?} else
        //val ext = GuiGraphicsExtractor(client, state)
        LegacyHudRenderer.renderLive(ext)

        offscreen.clearTarget()

        val prevState = accessor.`oneconfig$getRenderState`()
        GuiTargetRedirect.target = rt
        try {
            accessor.`oneconfig$setRenderState`(state)
            //? if >= 26.2 {
            guiRenderer.render()
            //?} else {
            /*val fog = (client.gameRenderer as GameRendererAccessor).`oneconfig$getFogRenderer`()
                .getBuffer(net.minecraft.client.renderer.fog.FogRenderer.FogMode.NONE)
            guiRenderer.render(fog)
            *///?}
        } finally {
            GuiTargetRedirect.target = null
            accessor.`oneconfig$setRenderState`(prevState)
        }
    }
    //?} elif >= 1.21.5 {
    /*private fun renderImmediate(rt: RenderTarget) {
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        offscreen.clearTarget()
        GuiTargetRedirect.target = rt
        try {
            LegacyHudRenderer.renderLive(graphics)
            graphics.flush()
        } finally {
            GuiTargetRedirect.target = null
        }
    }
    *///?} else {
    /*private fun renderImmediateLegacy(rt: RenderTarget) {
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        offscreen.clearTarget()
        GuiTargetRedirect.target = rt
        try {
            rt.bindWrite(true)
            LegacyHudRenderer.renderLive(graphics)
            graphics.flush()
        } finally {
            GuiTargetRedirect.target = null
            client.mainRenderTarget.bindWrite(true)
        }
    }
    *///?}

    fun drawInto(canvas: org.jetbrains.skia.Canvas) {
        if (!hasContent) return
        val s = offscreen.surface ?: return
        try {
            s.notifyContentWillChange(org.jetbrains.skia.ContentChangeMode.RETAIN)
            val surfaceRatio = Platform.screen().surfaceRatio().coerceAtLeast(0.0001f)
            canvas.save()
            canvas.scale(1f / surfaceRatio, 1f / surfaceRatio)
            s.draw(canvas, 0, 0, blitPaint)
            canvas.restore()
        } catch (t: Throwable) {
            LOG.debug("legacy hud blit failed", t)
        }
    }
}
