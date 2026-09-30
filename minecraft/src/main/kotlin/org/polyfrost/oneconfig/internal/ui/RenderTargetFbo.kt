package org.polyfrost.oneconfig.internal.ui

import com.mojang.blaze3d.pipeline.RenderTarget

//? if >= 26.2 {
import com.mojang.renderpearl.backend.opengl.FrameBufferAttachment
//?}

//? if >= 26.1 {
import org.polyfrost.oneconfig.internal.mixin.blaze3d.GlDeviceAccessor
import org.polyfrost.oneconfig.internal.mixin.blaze3d.GpuDeviceAccessor
//?}

//? if >= 1.21.5 {
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.backend.opengl.GlTexture
//?}

//? if >= 1.21.5 && < 26.1 {
/*import com.mojang.renderpearl.backend.opengl.GlDevice
*///?}

//? if < 1.21.5 {
/*import net.minecraft.client.Minecraft
*///?}

/**
 * Credits to lowercasebtw and taken from The Fabric Project
 * https://discord.com/channels/507304429255393322/807617488313516032/1452333789778018314
 */
object RenderTargetFbo {
    //? if >= 26.2 {
    fun getFboId(frameBuffer: RenderTarget): Int {
        val device = RenderSystem.getDevice()
        val backend = (device as GpuDeviceAccessor).`oneconfig$getBackend`()
        if (backend !is GlDeviceAccessor) return -1
        val dsa = backend.`oneconfig$getDirectStateAccess`()
        val color = frameBuffer.colorTexture as? GlTexture ?: return -1
        val depth = frameBuffer.depthTexture as? GlTexture
        return backend.`oneconfig$getFrameBufferCache`().getFbo(dsa, listOf<FrameBufferAttachment>(color), depth)
    }
    //? } else if >= 26.1 {
    /*fun getFboId(frameBuffer: RenderTarget): Int {
        val device = RenderSystem.getDevice()
        val backend = (device as GpuDeviceAccessor).`oneconfig$getBackend`()
        if (backend !is GlDeviceAccessor) return -1
        val dsa = backend.`oneconfig$getDirectStateAccess`()
        val texture = frameBuffer.colorTexture as? GlTexture ?: return -1
        return texture.getFbo(dsa, frameBuffer.depthTexture)
    }
    *///? } else if >= 1.21.5 {
    /*fun getFboId(frameBuffer: RenderTarget): Int {
        val device = RenderSystem.getDevice()
        if (device !is GlDevice) {
            return -1
        } else {
            val texture = frameBuffer.colorTexture as? GlTexture
                ?: return -1
            return texture.getFbo(device.directStateAccess(), frameBuffer.depthTexture)
        }
    }
    *///? } else {
    /*fun getFboId(frameBuffer: RenderTarget): Int {
        return frameBuffer.frameBufferId
    }
    *///? }

    //? if >= 1.21.5 {
    fun getColorTexId(frameBuffer: RenderTarget): Int =
        (frameBuffer.colorTexture as? GlTexture)?.glId() ?: -1
    //? } else {
    /*fun getColorTexId(frameBuffer: RenderTarget): Int = frameBuffer.colorTextureId
    *///? }

    // Creating, clearing, and destroying a render target leaves framebuffer 0 bound, and 1.21.1 GUI draws do not rebind.
    //? if < 1.21.5
    //fun restoreMainTarget() = Minecraft.getInstance().mainRenderTarget?.bindWrite(true)
}
