package org.polyfrost.oneconfig.internal.ui

import com.mojang.blaze3d.pipeline.RenderTarget
//? if > 1.8.9
import com.mojang.blaze3d.pipeline.TextureTarget
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceOrigin
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.slf4j.LoggerFactory

/** Owns a Minecraft render target and its Skia surface, cached by size. */
class SkiaOffscreenTarget {
    init {
        live += this
    }

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
        val svc = SkiaCtx.vulkanService ?: return false
        try {
            //? if >= 26.2 {
            val rt = TextureTarget(null, w, h, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)
            //?} else if >= 1.21.5 {
            /*val rt = TextureTarget(null, w, h, true)
            *///?} else if >= 1.21.4 {
            /*val rt = TextureTarget(w, h, true)
            *///?} elif > 1.8.9 {
            /*val rt = TextureTarget(w, h, true, net.minecraft.client.Minecraft.ON_OSX)
            *///?} else
            //val rt = RenderTarget(w, h, false)
            target = rt
            //? if < 1.21.5
            //rt.setClearColor(0f, 0f, 0f, 0f)
            if (!SkiaCtx.isVulkanMode && RenderTargetFbo.getFboId(rt) <= 0) {
                destroy()
                return false
            }
            val (b, colorFmt) = svc.makeOffscreenBRT(rt, w, h)
            brt = b
            val origin = if (SkiaCtx.isDeferredComposeBackend) SurfaceOrigin.TOP_LEFT else SurfaceOrigin.BOTTOM_LEFT
            surface = Surface.makeFromBackendRenderTarget(
                SkiaCtx.directContext, b, origin, colorFmt, ColorSpace.sRGB, null,
            )
            if (surface == null) {
                destroy()
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

    fun clearTarget() {
        val rt = target ?: return
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
        *///?} elif > 1.8.9 {
        /*rt.clear(net.minecraft.client.Minecraft.ON_OSX)
        *///?} else
        //rt.clear()
    }

    fun destroy() {
        surface?.close(); surface = null
        brt?.close(); brt = null
        target?.destroyBuffers(); target = null
        lastW = -1; lastH = -1
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("OneConfig/SkiaOffscreenTarget")

        private val live = ArrayList<SkiaOffscreenTarget>(2)

        fun destroyAll() {
            for (t in live) t.destroy()
        }
    }
}
