package org.polyfrost.oneconfig.internal.ui

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceOrigin
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.slf4j.LoggerFactory

/** Owns a Minecraft render target and its Skia surface, cached by size. */
class SkiaOffscreenTarget {
    private val LOG = LoggerFactory.getLogger("OneConfig/SkiaOffscreenTarget")

    var target: RenderTarget? = null
        private set
    private var brt: BackendRenderTarget? = null
    var surface: Surface? = null
        private set
    private var lastW = -1
    private var lastH = -1

    fun resolveTarget(w: Int, h: Int): Boolean {
        if (target != null && lastW == w && lastH == h && surface != null) return true
        destroy()
        try {
            //? if >= 26.2 {
            val rt = TextureTarget(null, w, h, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)
            //?} else if >= 1.21.5 {
            /*val rt = TextureTarget(null, w, h, true)
            *///?} else if >= 1.21.4 {
            /*val rt = TextureTarget(w, h, true)
            *///?} else {
            /*val rt = TextureTarget(w, h, true, net.minecraft.client.Minecraft.ON_OSX)
            *///?}
            target = rt
            //? if < 1.21.5
            /*rt.setClearColor(0f, 0f, 0f, 0f)*/
            val svc = SkiaCtx.vulkanService ?: return false
            if (!SkiaCtx.isVulkanMode) {
                //? if >= 1.21.5 {
                val fboId = RenderTargetFbo.getFboId(rt)
                //? } else
                /*val fboId = rt.frameBufferId*/
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
            LOG.warn("Failed to create offscreen target", t)
            destroy()
            return false
        }
    }

    fun destroy() {
        surface?.close(); surface = null
        brt?.close(); brt = null
        target?.destroyBuffers(); target = null
        lastW = -1; lastH = -1
    }
}
