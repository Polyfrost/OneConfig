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
import org.polyfrost.oneconfig.api.hud.v1.Hud
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

    private fun activeLegacyHuds(): List<LegacyHud> =
        HudManager.activeInstances.mapNotNull { hud: Hud ->
            (hud as? LegacyHud)?.takeUnless {
                it.hidden && !HudManager.isEditing && !SkiaCtx.suppressInGameHudRender
            }
        }

    fun render() {
        hasContent = false
        if (failed) return
        if (java.lang.Boolean.getBoolean("oneconfig.disable.legacyHudOffscreen")) return
        val huds = activeLegacyHuds()
        if (huds.isEmpty() && !CompatOverlayRenderer.hasHooks()) return
        if (!SkiaCtx.isReady) return
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return

        try {
            if (!offscreen.resolveTarget(w, h)) return
            val rt = offscreen.target ?: return
            //? if >= 1.21.8 {
            renderRecorded(rt, huds)
            //?} elif >= 1.21.5 {
            /*renderImmediate(rt, huds)
            *///?} else {
            /*renderImmediateLegacy(rt, huds)
            *///?}
            hasContent = true
        } catch (t: Throwable) {
            LOG.warn("Legacy HUD offscreen render failed; disabling", t)
            failed = true
            //? if >= 1.21.5
            GuiTargetRedirect.target = null
        }
    }

    //? if >= 1.21.8 {
    private fun renderRecorded(rt: RenderTarget, huds: List<LegacyHud>) {
        val guiRenderer = (client.gameRenderer as GameRendererAccessor).`oneconfig$getGuiRenderer`()
        val accessor = guiRenderer as GuiRendererAccessor

        val state = GuiRenderState()
        //? if >= 1.21.11 {
        val ext = GuiGraphicsExtractor(client, state, Platform.screen().guiWidth(), Platform.screen().guiHeight())
        //?} else
        //val ext = GuiGraphicsExtractor(client, state)
        renderHuds(ext, huds)

        clearTarget(rt)

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
    /*private fun renderImmediate(rt: RenderTarget, huds: List<LegacyHud>) {
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        clearTarget(rt)
        GuiTargetRedirect.target = rt
        try {
            renderHuds(graphics, huds)
            graphics.flush()
        } finally {
            GuiTargetRedirect.target = null
        }
    }
    *///?} else {
    /*private fun renderImmediateLegacy(rt: RenderTarget, huds: List<LegacyHud>) {
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        clearTarget(rt)
        GuiTargetRedirect.target = rt
        try {
            rt.bindWrite(true)
            renderHuds(graphics, huds)
            graphics.flush()
        } finally {
            GuiTargetRedirect.target = null
            client.mainRenderTarget.bindWrite(true)
        }
    }
    *///?}

    private fun renderHuds(ext: GuiGraphicsExtractor, huds: List<LegacyHud>) {
        for (hud in huds) {
            try {
                val scale = hud.effectiveScale
                hud.renderedW = hud.width * scale
                hud.renderedH = hud.height * scale
                val pose = ext.pose()
                //? if >= 1.21.8 {
                pose.pushMatrix()
                try {
                    pose.translate(hud.x, hud.y)
                    if (scale != 1f) pose.scale(scale, scale)
                    hud.render(ext)
                } finally {
                    pose.popMatrix()
                }
                //?} else {
                /*pose.pushPose()
                try {
                    pose.translate(hud.x.toDouble(), hud.y.toDouble(), 0.0)
                    if (scale != 1f) pose.scale(scale, scale, 1f)
                    hud.render(ext)
                } finally {
                    pose.popPose()
                }
                *///?}
            } catch (t: Throwable) {
                LOG.debug("legacy hud render (record) failed", t)
            }
        }
        if (CompatOverlayRenderer.oneConfigScreenOpen()) CompatOverlayRenderer.render(ext)
    }

    private fun clearTarget(rt: RenderTarget) {
        //? if >= 26.2 {
        val colorTex = rt.colorTexture ?: return
        com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder()
            .clearColorTexture(colorTex, org.joml.Vector4f(0f, 0f, 0f, 0f))
        //? } else if >= 1.21.5 {
        /*val colorTex = rt.colorTexture ?: return
        val encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder()
        encoder.clearColorTexture(colorTex, 0)
        //? if < 1.21.10 {
        /*//1.21.5 does not clear depth, and 1.21.8 clears it only after rendering the before-blur range
        rt.depthTexture?.let { encoder.clearDepthTexture(it, 1.0) }
        *///?}
        *///?} elif >= 1.21.4 {
        /*rt.clear()
        *///?} else {
        /*rt.clear(Minecraft.ON_OSX)
        *///?}
    }

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
