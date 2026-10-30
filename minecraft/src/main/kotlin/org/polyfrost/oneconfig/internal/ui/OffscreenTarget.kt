package org.polyfrost.oneconfig.internal.ui

import com.mojang.blaze3d.pipeline.RenderTarget
import org.slf4j.LoggerFactory

//? if >= 26.2 {
import com.mojang.renderpearl.api.GpuFormat
import org.joml.Vector4f
//?}

//? if >= 26.1 {
import org.polyfrost.oneconfig.internal.mixin.blaze3d.GlDeviceAccessor
import org.polyfrost.oneconfig.internal.mixin.blaze3d.GpuDeviceAccessor
//?}

//? if >= 1.21.5 && < 26.1 {
/*import com.mojang.renderpearl.backend.opengl.GlDevice
*///?}

//? if >= 1.21.5 {
import com.mojang.blaze3d.systems.RenderSystem
//?}

//? if > 1.8.9 {
import com.mojang.blaze3d.pipeline.TextureTarget
//?}

//? if < 1.21.4 {
/*import net.minecraft.client.Minecraft
*///?}

class OffscreenTarget {
    init {
        live += this
    }

    var target: RenderTarget? = null
        private set
    private var lastW = -1
    private var lastH = -1

    fun resolve(w: Int, h: Int): Boolean {
        if (target != null && lastW == w && lastH == h) return true
        destroy()
        try {
            //? if < 1.21.5
            //val bound = RenderTargetFbo.saveBoundTarget()
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
            *///?} elif > 1.8.9 {
            /*val rt = TextureTarget(w, h, true, Minecraft.ON_OSX)
            *///?} else
            //val rt = RenderTarget(w, h, true)
            target = rt
            //? if < 1.21.5 {
            /*rt.setClearColor(0f, 0f, 0f, 0f)
            RenderTargetFbo.restoreBoundTarget(bound)
            *///?}
            if (!GpuBackend.isVulkan && RenderTargetFbo.getFboId(rt) <= 0) {
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

    fun clear() {
        val rt = target ?: return
        //? if < 1.21.5
        //val bound = RenderTargetFbo.saveBoundTarget()
        //? if >= 26.2 {
        val colorTex = rt.colorTexture ?: return
        RenderSystem.getDevice().createCommandEncoder()
            .clearColorTexture(colorTex, Vector4f(0f, 0f, 0f, 0f))
        //? } else if >= 1.21.5 {
        /*val colorTex = rt.colorTexture ?: return
        val encoder = RenderSystem.getDevice().createCommandEncoder()
        encoder.clearColorTexture(colorTex, 0)
        //? if < 1.21.10 {
        /*        rt.depthTexture?.let { encoder.clearDepthTexture(it, 1.0) }
        *///?}
        *///?} elif >= 1.21.4 {
        /*rt.clear()
        *///?} elif > 1.8.9 {
        /*rt.clear(Minecraft.ON_OSX)
        *///?} else
        //rt.clear()
        //? if < 1.21.5
        //RenderTargetFbo.restoreBoundTarget(bound)
    }

    fun destroy() {
        //? if < 1.21.5
        //val bound = RenderTargetFbo.saveBoundTarget()
        target?.destroyBuffers(); target = null
        //? if < 1.21.5
        //RenderTargetFbo.restoreBoundTarget(bound)
        lastW = -1; lastH = -1
    }

    fun dispose() {
        destroy()
        live.remove(this)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("OneConfig/OffscreenTarget")

        private val live = ArrayList<OffscreenTarget>(2)

        fun destroyAll() {
            for (t in live) t.destroy()
        }
    }
}

object GpuBackend {
    val isVulkan: Boolean by lazy {
        //? if >= 26.1 {
        (RenderSystem.getDevice() as GpuDeviceAccessor).`oneconfig$getBackend`() !is GlDeviceAccessor
        //?} else if >= 1.21.5 {
        /*RenderSystem.getDevice() !is GlDevice
        *///?} else
        //false
    }
}

interface TextureRenderer {
    fun texture(target: RenderTarget, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, alpha: Float)
}
