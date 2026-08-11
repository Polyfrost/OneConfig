package org.polyfrost.oneconfig.internal.ui.services

import com.mojang.blaze3d.pipeline.RenderTarget
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.SurfaceColorFormat
import org.slf4j.LoggerFactory

/**
 * Abstraction over GL vs VK rendering backend for skiko
 */
interface VulkanService {
    val isVulkan: Boolean

    /**
     * True only for VulkanMod which defers the GUI to end-of-frame and cannot have its
     * command buffer flushed mid-frame
     */
    val usesDeferredCompose: Boolean get() = false

    val offscreenNeedsPerFrameRewrap: Boolean get() = false

    fun transitionOffscreenForSampling(target: RenderTarget) {}

    fun transitionOffscreenForRendering(target: RenderTarget) {}

    fun restoreMainRTLayout() {}

    fun makeDirectContext(): DirectContext

    fun makeBackendRenderTarget(
        width: Int, height: Int,
        vkImageHandle: Long = 0L,
        vkFormat: Int = 0,
        vkQueueFamily: Int = 0,
    ): BackendRenderTarget

    /**
     * Create a BackendRenderTarget that draws directly to the back buffer
     */
    fun makeBackBufferRenderTarget(
        width: Int, height: Int,
        vkImageHandle: Long = 0L,
        vkFormat: Int = 0,
        vkQueueFamily: Int = 0,
    ): BackendRenderTarget = makeBackendRenderTarget(width, height, vkImageHandle, vkFormat, vkQueueFamily)

    /**
     * Wraps [target]'s color attachment as an offscreen Skia render target
     */
    fun makeOffscreenBRT(
        target: RenderTarget,
        width: Int,
        height: Int,
    ): Pair<BackendRenderTarget, SurfaceColorFormat>

    /**
     * @return (vkImageHandle, vkFormat, queueFamily) for MC's main color render target
     */
    fun getMainColorImageInfo(): Triple<Long, Int, Int> = Triple(0L, 0, 0)

    /**
     * Capture [target] which is MC's main render target holding the world before the GUI composites
     * into a private stable image used as the blur backdrop source
     */
    fun takeWorldSnapshot(target: RenderTarget) {}

    fun getBlurSourceImageInfo(): Triple<Long, Int, Int> = Triple(0L, 0, 0)

    fun midFrameFlush() {}

    companion object {
        private val LOG = LoggerFactory.getLogger(VulkanService::class.java)

        fun detect(): VulkanService {
            // only one of these is compiled per MC version so a missing class or a null
            // tryCreate both fall through to the GL backend
            for (className in listOf(
                "org.polyfrost.oneconfig.internal.ui.services.NativeVulkanService",
                "org.polyfrost.oneconfig.internal.ui.services.CinnabarVulkanService",
                "org.polyfrost.oneconfig.internal.ui.services.VulkanModVulkanService",
            )) {
                try {
                    val cls = Class.forName(className)
                    val svc = cls.getDeclaredMethod("tryCreate").invoke(null) as? VulkanService
                    if (svc != null) {
                        LOG.info("VK service available ({}), using Vulkan Skia backend", className)
                        return svc
                    }
                } catch (_: ClassNotFoundException) {
                    // not present on this version
                } catch (_: LinkageError) {
                    LOG.debug("VK service {} not linkable (mod absent), using GL backend", className)
                } catch (e: Exception) {
                    LOG.warn("VK service {} failed to initialize", className, e)
                }
            }
            return GLVulkanService
        }
    }
}
