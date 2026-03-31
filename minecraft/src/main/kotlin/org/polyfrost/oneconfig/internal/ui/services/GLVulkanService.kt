package org.polyfrost.oneconfig.internal.ui.services

import net.minecraft.client.Minecraft
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.SurfaceColorFormat

object GLVulkanService : VulkanService {
    private val client get() = Minecraft.getInstance()
    override val isVulkan = false

    override fun makeDirectContext(): DirectContext = DirectContext.makeGL()

    override fun makeBackendRenderTarget(
        width: Int, height: Int,
        vkImageHandle: Long, vkFormat: Int, vkQueueFamily: Int,
    ): BackendRenderTarget {
        val target = client.mainRenderTarget
        //#if MC >= 1.21.5 && MC <= 1.21.11
        //$$ val frameBufferId = getFboId(target)
        //#else
        //#if MC > 1.21.11
        //$$ val frameBufferId = -1
        //#else
        val frameBufferId = target.frameBufferId
        //#endif
        //#endif

        return BackendRenderTarget.makeGL(
            width, height, 0, 8, frameBufferId, FramebufferFormat.GR_GL_RGBA8
        )
    }

    override fun makeBackBufferRenderTarget(
        width: Int, height: Int,
        vkImageHandle: Long, vkFormat: Int, vkQueueFamily: Int,
    ): BackendRenderTarget {
        return BackendRenderTarget.makeGL(
            width, height, 0, 8, 0, FramebufferFormat.GR_GL_RGBA8
        )
    }

    override fun makeOffscreenBRT(
        target: RenderTarget,
        width: Int,
        height: Int,
    ): Pair<BackendRenderTarget, SurfaceColorFormat> {
        //#if MC >= 1.21.5 && MC <= 1.21.11
        //$$ val fboId = getFboId(target)
        //#else
        //#if MC > 1.21.11
        //$$ val fboId = target.frameBufferId
        //#else
        val fboId = target.frameBufferId
        //#endif
        //#endif
        return BackendRenderTarget.makeGL(width, height, 0, 8, fboId, FramebufferFormat.GR_GL_RGBA8) to
                SurfaceColorFormat.RGBA_8888
    }

    //#if MC >= 1.21.5 && MC <= 1.21.11
    /**
     * Credits: lowercasebtw
     * Taken from: https://discord.com/channels/507304429255393322/807617488313516032/1452333789778018314 (The Fabric Project)
     */
    //$$ fun getFboId(frameBuffer: RenderTarget): Int {
    //$$    val device = RenderSystem.getDevice()
    //$$   if (device !is com.mojang.blaze3d.opengl.GlDevice) {
    //$$       return -1
    //$$   } else {
    //$$       val texture = (frameBuffer.getColorTexture() as com.mojang.blaze3d.opengl.GlTexture?) ?: error("well, someone messed up!")
    //$$       return texture.getFbo((device as com.mojang.blaze3d.opengl.GlDevice).directStateAccess(), frameBuffer.getDepthTexture())
    //$$   }
    //$$ }
    //#endif
}
