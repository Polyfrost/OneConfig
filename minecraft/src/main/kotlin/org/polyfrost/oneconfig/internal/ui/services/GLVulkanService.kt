package org.polyfrost.oneconfig.internal.ui.services

//~ main_render_target
import net.minecraft.client.Minecraft
import com.mojang.blaze3d.pipeline.RenderTarget
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.SurfaceColorFormat
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo
import org.slf4j.LoggerFactory

object GLVulkanService : VulkanService {
    private val LOG = LoggerFactory.getLogger(GLVulkanService::class.java)
    private val client get() = Minecraft.getInstance()
    override val isVulkan = false

    override fun makeDirectContext(): DirectContext =
        try {
            DirectContext.makeGL()
        } catch (e: Exception) {
            LOG.warn("DirectContext.makeGL() failed; retrying via LWJGL proc loader (SDL/EGL backend?)", e)
            GLInterfaceFactory.makeDirectContextViaLwjgl() ?: throw e
        }

    override fun makeBackendRenderTarget(
        width: Int, height: Int,
        vkImageHandle: Long, vkFormat: Int, vkQueueFamily: Int,
    ): BackendRenderTarget {
        val target = client.mainRenderTarget
        //? >= 1.21.5 {
        val frameBufferId = RenderTargetFbo.getFboId(target)
        //? } else
        //val frameBufferId = target.frameBufferId

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
        //? >= 1.21.5 {
        val fboId = RenderTargetFbo.getFboId(target)
        //? } else
        //val fboId = target.frameBufferId

        return BackendRenderTarget.makeGL(width, height, 0, 8, fboId, FramebufferFormat.GR_GL_RGBA8) to
                SurfaceColorFormat.RGBA_8888
    }

}
