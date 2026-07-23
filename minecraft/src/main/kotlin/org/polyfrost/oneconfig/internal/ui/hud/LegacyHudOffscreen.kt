//? if >= 26.1 {
package org.polyfrost.oneconfig.internal.ui.hud

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.renderer.state.gui.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceOrigin
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.slf4j.LoggerFactory

object LegacyHudOffscreen {
    private val LOG = LoggerFactory.getLogger("OneConfig/LegacyHudOffscreen")
    private val client get() = Minecraft.getInstance()

    @Volatile
    @JvmField
    var redirectTarget: RenderTarget? = null

    private var target: TextureTarget? = null
    private var brt: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var lastW = -1
    private var lastH = -1

    @Volatile private var hasContent = false
    @Volatile private var failed = false

    private val blitPaint = Paint()

    init {
        LegacyHudOverlayBridge.painter = { c -> drawInto(c) }
    }

    private fun resolveTarget(w: Int, h: Int): Boolean {
        if (target != null && lastW == w && lastH == h && surface != null) return true
        destroy()
        try {
            //? if >= 26.2 {
            /*val rt = TextureTarget(null, w, h, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)
            *///? } else {
            val rt = TextureTarget(null, w, h, true)
            //? }
            target = rt
            val svc = SkiaCtx.vulkanService ?: return false
            if (!SkiaCtx.isVulkanMode) {
                val fboId = org.polyfrost.oneconfig.internal.ui.RenderTargetFbo.getFboId(rt)
                if (fboId <= 0) {
                    target = null
                    rt.destroyBuffers()
                    return false
                }
            }
            val (b, colorFmt) = svc.makeOffscreenBRT(rt, w, h)
            brt = b
            val origin = if (SkiaCtx.isDeferredComposeBackend) SurfaceOrigin.TOP_LEFT else SurfaceOrigin.BOTTOM_LEFT
            surface = Surface.makeFromBackendRenderTarget(
                SkiaCtx.directContext, b, origin, colorFmt, ColorSpace.sRGB, null,
            )
            if (surface == null) {
                b.close(); brt = null
                return false
            }
            lastW = w; lastH = h
            return true
        } catch (t: Throwable) {
            LOG.warn("Failed to create legacy HUD offscreen target", t)
            destroy()
            return false
        }
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
        val w = client.window.width
        val h = client.window.height
        if (w <= 0 || h <= 0) return

        try {
            if (!resolveTarget(w, h)) return
            val rt = target ?: return
            val guiRenderer = (client.gameRenderer as GameRendererAccessor).`oneconfig$getGuiRenderer`()
            val accessor = guiRenderer as GuiRendererAccessor

            val guiW = client.window.guiScaledWidth
            val guiH = client.window.guiScaledHeight

            val state = GuiRenderState()
            val ext = GuiGraphicsExtractor(client, state, guiW, guiH)
            for (hud in huds) {
                try {
                    val scale = hud.effectiveScale
                    hud.renderedW = hud.width * scale
                    hud.renderedH = hud.height * scale
                    val pose = ext.pose()
                    pose.pushMatrix()
                    pose.translate(hud.x, hud.y)
                    if (scale != 1f) pose.scale(scale, scale)
                    hud.render(ext)
                    pose.popMatrix()
                } catch (t: Throwable) {
                    LOG.debug("legacy hud render (record) failed", t)
                }
            }

            if (CompatOverlayRenderer.oneConfigScreenOpen()) CompatOverlayRenderer.render(ext)

            val colorTex = rt.colorTexture ?: return
            val encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder()
            //? if >= 26.2 {
            /*encoder.clearColorTexture(colorTex, org.joml.Vector4f(0f, 0f, 0f, 0f))
            *///? } else {
            encoder.clearColorTexture(colorTex, 0)
            //? }

            val prevState = accessor.`oneconfig$getRenderState`()
            redirectTarget = rt
            try {
                accessor.`oneconfig$setRenderState`(state)
                //? if >= 26.2 {
                /*guiRenderer.render()
                *///? } else {
                val fog = (client.gameRenderer as GameRendererAccessor).`oneconfig$getFogRenderer`()
                    .getBuffer(net.minecraft.client.renderer.fog.FogRenderer.FogMode.NONE)
                guiRenderer.render(fog)
                //? }
            } finally {
                redirectTarget = null
                accessor.`oneconfig$setRenderState`(prevState)
            }
            hasContent = true
        } catch (t: Throwable) {
            LOG.warn("Legacy HUD offscreen render failed; disabling", t)
            failed = true
            redirectTarget = null
        }
    }

    fun drawInto(canvas: org.jetbrains.skia.Canvas) {
        if (!hasContent) return
        val s = surface ?: return
        try {
            s.notifyContentWillChange(org.jetbrains.skia.ContentChangeMode.RETAIN)
            val pixelRatio = Platform.screen().pixelRatio().coerceAtLeast(0.0001f)
            canvas.save()
            canvas.scale(1f / pixelRatio, 1f / pixelRatio)
            s.draw(canvas, 0, 0, blitPaint)
            canvas.restore()
        } catch (t: Throwable) {
            LOG.debug("legacy hud blit failed", t)
        }
    }

    private fun destroy() {
        surface?.close(); surface = null
        brt?.close(); brt = null
        target?.destroyBuffers(); target = null
        lastW = -1; lastH = -1
    }

    fun invalidate() {
        destroy()
        hasContent = false
    }
}
//? }
