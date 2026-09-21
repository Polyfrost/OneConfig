package org.polyfrost.oneconfig.internal.ui.services

//~ main_render_target
import net.minecraft.client.Minecraft
import com.mojang.blaze3d.pipeline.RenderTarget
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.SurfaceColorFormat
//? if >= 26.3
import org.lwjgl.sdl.SDLVideo.SDL_GetCurrentVideoDriver
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo
import org.slf4j.LoggerFactory

object GLVulkanService : VulkanService {
    private val LOG = LoggerFactory.getLogger(GLVulkanService::class.java)
    private val client get() = Minecraft.getInstance()
    override val isVulkan = false

    override fun makeDirectContext(): DirectContext {
        if (!isGlxBackend) {
            GLInterfaceFactory.makeDirectContextViaLwjgl()?.let { return it }
            LOG.warn("Assembled GL interface unavailable on a non-GLX backend; falling back to DirectContext.makeGL()")
        }
        return try {
            DirectContext.makeGL()
        } catch (e: Exception) {
            LOG.warn("DirectContext.makeGL() failed; retrying via LWJGL proc loader (SDL/EGL backend?)", e)
            GLInterfaceFactory.makeDirectContextViaLwjgl() ?: throw e
        }
    }

    private val isGlxBackend: Boolean
        get() {
            //? if >= 26.3 {
            val driver = try {
                SDL_GetCurrentVideoDriver()
            } catch (_: Throwable) {
                null
            }
            if (driver != null) return !driver.equals("wayland", ignoreCase = true)
            //?}
            return true
        }

    override fun makeBackendRenderTarget(
        width: Int, height: Int,
        vkImageHandle: Long, vkFormat: Int, vkQueueFamily: Int,
    ): BackendRenderTarget {
        val target = client.gameRenderer.mainRenderTarget()
        //? >= 1.21.5 {
        val frameBufferId = RenderTargetFbo.getFboId(target)
        //? } else
        //val frameBufferId = target.frameBufferId

        //~ if = 1.8.9 '8' -> '0'
        val stencilBits = 8
        return BackendRenderTarget.makeGL(
            width, height, 0, stencilBits, frameBufferId, FramebufferFormat.GR_GL_RGBA8
        )
    }

    override fun makeBackBufferRenderTarget(
        width: Int, height: Int,
        vkImageHandle: Long, vkFormat: Int, vkQueueFamily: Int,
    ): BackendRenderTarget {
        //~ if = 1.8.9 '8' -> '0'
        val stencilBits = 8
        return BackendRenderTarget.makeGL(
            width, height, 0, stencilBits, 0, FramebufferFormat.GR_GL_RGBA8
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

        // 1.8.9 RenderTarget has no stencil attachment
        //~ if = 1.8.9 '8' -> '0'
        val stencilBits = 8
        return BackendRenderTarget.makeGL(width, height, 0, stencilBits, fboId, FramebufferFormat.GR_GL_RGBA8) to
                SurfaceColorFormat.RGBA_8888
    }

}
