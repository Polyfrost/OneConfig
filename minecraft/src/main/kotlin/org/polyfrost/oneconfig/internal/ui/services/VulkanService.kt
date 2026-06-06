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

    fun makeDirectContext(): DirectContext

    /**
     * Create a BackendRenderTarget for the given dimensions
     */
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
     * Create a [BackendRenderTarget] that wraps [target]'s color attachment for use as an
     * offscreen Skia render target.
     */
    fun makeOffscreenBRT(
        target: RenderTarget,
        width: Int,
        height: Int,
    ): Pair<BackendRenderTarget, SurfaceColorFormat>

    /**
     * @return (vkImageHandle, vkFormat, queueFamily) for MC's main color render target.
     */
    fun getMainColorImageInfo(): Triple<Long, Int, Int> = Triple(0L, 0, 0)

    /**
     * Flush any pending GPU commands recorded since the last flush.
     */
    fun midFrameFlush() {}

    companion object {
        private val LOG = LoggerFactory.getLogger(VulkanService::class.java)

        fun detect(): VulkanService {
            // Try each backend-specific Vulkan service in order; only one is compiled per MC version
            // (NativeVulkanService on >= 26.2, CinnabarVulkanService on the Cinnabar builds). A missing
            // class or GL-mode (tryCreate returns null) both fall through to the GL backend.
            for (className in listOf(
                "org.polyfrost.oneconfig.internal.ui.services.NativeVulkanService",
                "org.polyfrost.oneconfig.internal.ui.services.CinnabarVulkanService",
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
                } catch (e: Exception) {
                    LOG.warn("VK service {} failed to initialize", className, e)
                }
            }
            return GLVulkanService
        }
    }
}
