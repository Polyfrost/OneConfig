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
            return try {
                val cls = Class.forName("org.polyfrost.oneconfig.internal.ui.services.CinnabarVulkanService")
                val svc = cls.getDeclaredMethod("tryCreate").invoke(null) as? VulkanService
                if (svc != null) {
                    LOG.info("VK service is available, using Vulkan Skia backend")
                    svc
                } else {
                    error("CinnabarVulkanService.tryCreate() returned null")
                }
            } catch (_: ClassNotFoundException) {
                GLVulkanService
            }
        }
    }
}
