package org.polyfrost.oneconfig.internal.ui

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceOrigin
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.slf4j.LoggerFactory

//? if >= 26.2 {
import com.mojang.renderpearl.api.GpuFormat
import org.joml.Vector4f
//?}

//? if >= 1.21.5 {
import com.mojang.blaze3d.systems.RenderSystem
//?}

//? if < 1.21.4 {
/*import net.minecraft.client.Minecraft
*///?}

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
        val current = target
        if (current != null && lastW == w && lastH == h && surface != null) {
            if (SkiaCtx.vulkanService?.offscreenNeedsPerFrameRewrap != true) return true
            releaseSurface()
            if (runCatching { makeSurface(current, w, h) }.getOrDefault(false)) return true
        }
        destroy()
        if (SkiaCtx.vulkanService == null) return false
        try {
            //? if >= 26.3 {
            val rt = TextureTarget(
                null, w, h,
                GpuFormat.RGBA8_UNORM,
                GpuFormat.D32_FLOAT,
            )
            //?} else if >= 26.2 {
            /*val rt = TextureTarget(null, w, h, true, GpuFormat.RGBA8_UNORM)
            *///?} else if >= 1.21.5 {
            /*val rt = TextureTarget(null, w, h, true)
            *///?} else if >= 1.21.4 {
            /*val rt = TextureTarget(w, h, true)
            *///?} else {
            /*val rt = TextureTarget(w, h, true, Minecraft.ON_OSX)
            *///?}
            target = rt
            //? if < 1.21.5 {
            /*rt.setClearColor(0f, 0f, 0f, 0f)
            RenderTargetFbo.restoreMainTarget()
            *///?}
            if (!SkiaCtx.isVulkanMode && RenderTargetFbo.getFboId(rt) <= 0) {
                destroy()
                return false
            }
            if (!makeSurface(rt, w, h)) {
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

    private fun makeSurface(rt: RenderTarget, w: Int, h: Int): Boolean {
        val svc = SkiaCtx.vulkanService ?: return false
        val (b, colorFmt) = svc.makeOffscreenBRT(rt, w, h)
        brt = b
        val origin = if (SkiaCtx.isDeferredComposeBackend) SurfaceOrigin.TOP_LEFT else SurfaceOrigin.BOTTOM_LEFT
        surface = SkiaCtx.withIsolatedGl {
            Surface.makeFromBackendRenderTarget(
                SkiaCtx.directContext, b, origin, colorFmt, ColorSpace.sRGB, null,
            )
        }
        return surface != null
    }

    private fun releaseSurface() {
        surface?.close(); surface = null
        brt?.close(); brt = null
    }

    fun clearTarget() {
        val rt = target ?: return
        //? if >= 26.2 {
        val colorTex = rt.colorTexture ?: return
        RenderSystem.getDevice().createCommandEncoder()
            .clearColorTexture(colorTex, Vector4f(0f, 0f, 0f, 0f))
        //? } else if >= 1.21.5 {
        /*val colorTex = rt.colorTexture ?: return
        val encoder = RenderSystem.getDevice().createCommandEncoder()
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
        //? if < 1.21.5
        //RenderTargetFbo.restoreMainTarget()
    }

    fun ensureSubmitted() {
        if (target == null) return
        try {
            SkiaCtx.vulkanService?.midFrameFlush()
        } catch (t: Throwable) {
            LOG.debug("Offscreen flush failed", t)
        }
    }

    fun destroy() {
        releaseSurface()
        target?.destroyBuffers(); target = null
        //? if < 1.21.5
        //RenderTargetFbo.restoreMainTarget()
        lastW = -1; lastH = -1
    }

    fun dispose() {
        destroy()
        live.remove(this)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("OneConfig/SkiaOffscreenTarget")

        private val live = ArrayList<SkiaOffscreenTarget>(2)

        fun destroyAll() {
            for (t in live) t.destroy()
        }
    }
}
