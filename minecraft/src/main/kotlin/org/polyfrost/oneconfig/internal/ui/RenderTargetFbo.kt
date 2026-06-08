package org.polyfrost.oneconfig.internal.ui

//? if >= 26.1 {
import com.mojang.blaze3d.opengl.GlTexture
import org.polyfrost.oneconfig.internal.mixin.blaze3d.GlDeviceAccessor
import org.polyfrost.oneconfig.internal.mixin.blaze3d.GpuDeviceAccessor
//? }
//? if >= 26.2 {
/*import com.mojang.blaze3d.opengl.FrameBufferAttachment
*///? }
import com.mojang.blaze3d.pipeline.RenderTarget
//? if >= 1.21.5 {
import com.mojang.blaze3d.systems.RenderSystem
//? }

/**
 * Credits: lowercasebtw
 * Taken from: https://discord.com/channels/507304429255393322/807617488313516032/1452333789778018314 (The Fabric Project)
 */
object RenderTargetFbo {
    //? if >= 26.2 {
    /*fun getFboId(frameBuffer: RenderTarget): Int {
        val device = RenderSystem.getDevice()
        val backend = (device as GpuDeviceAccessor).`oneconfig$getBackend`()
        if (backend !is GlDeviceAccessor) return -1
        val dsa = backend.`oneconfig$getDirectStateAccess`()
        val color = frameBuffer.colorTexture as? GlTexture ?: return -1
        val depth = frameBuffer.depthTexture as? GlTexture
        return backend.`oneconfig$getFrameBufferCache`().getFbo(dsa, listOf<FrameBufferAttachment>(color), depth)
    }
    *///? } else if >= 26.1 {
    fun getFboId(frameBuffer: RenderTarget): Int {
        val device = RenderSystem.getDevice()
        val backend = (device as GpuDeviceAccessor).`oneconfig$getBackend`()
        if (backend !is GlDeviceAccessor) return -1
        val dsa = backend.`oneconfig$getDirectStateAccess`()
        val texture = frameBuffer.colorTexture as? GlTexture ?: return -1
        return texture.getFbo(dsa, frameBuffer.depthTexture)
    }
    //? } else if >= 1.21.5 {
    /*fun getFboId(frameBuffer: RenderTarget): Int {
        val device = RenderSystem.getDevice()
        if (device !is com.mojang.blaze3d.opengl.GlDevice) {
            return -1
        } else {
            val texture = frameBuffer.colorTexture as? com.mojang.blaze3d.opengl.GlTexture
                ?: return -1
            return texture.getFbo(device.directStateAccess(), frameBuffer.depthTexture)
        }
    }
    *///? } else {
    /*fun getFboId(frameBuffer: RenderTarget): Int {
        return frameBuffer.frameBufferId
    }
    *///? }
}
