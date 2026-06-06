package org.polyfrost.oneconfig.internal.ui.services

//? if >= 26.2 {
/*import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vulkan.VulkanDevice
import com.mojang.blaze3d.vulkan.VulkanGpuTexture
import net.minecraft.client.Minecraft
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.SurfaceColorFormat
import org.lwjgl.vulkan.VK
import org.lwjgl.vulkan.VK12.*
import org.polyfrost.oneconfig.internal.mixin.blaze3d.GpuDeviceAccessor
import org.slf4j.LoggerFactory

/**
 * Vulkan-backed [VulkanService] using Minecraft 26.2's native Vulkan backend.
 *
 * Replaces [CinnabarVulkanService]: 26.2 ships its own Vulkan renderer in Blaze3D, so the
 * vk handles are pulled straight from the public [VulkanDevice] API instead of Cinnabar's.
 */
class NativeVulkanService private constructor(
    private val vkInstance: Long,
    private val vkPhysDevice: Long,
    private val vkDevice: Long,
    private val vkQueue: Long,
    val queueFamilyIndex: Int,
) : VulkanService {

    override val isVulkan = true

    override fun makeDirectContext(): DirectContext {
        val provider = VK.getFunctionProvider()
        val instanceProcAddr = provider.getFunctionAddress("vkGetInstanceProcAddr")
        val deviceProcAddr = provider.getFunctionAddress("vkGetDeviceProcAddr")
        return DirectContext.makeVulkan(
            vkInstance, vkPhysDevice, vkDevice, vkQueue,
            queueFamilyIndex,
            instanceProcAddr,
            deviceProcAddr,
            VK_API_VERSION_1_2,
        )
    }

    override fun makeBackendRenderTarget(
        width: Int, height: Int,
        vkImageHandle: Long, vkFormat: Int, vkQueueFamily: Int,
    ): BackendRenderTarget = BackendRenderTarget.makeVulkan(
        width, height,
        vkImageHandle,
        VK_IMAGE_TILING_OPTIMAL,
        VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
        vkFormat,
        VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT
                or VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
        /* sampleCount = */ 1,
        /* levelCount  = */ 1,
    )

    override fun makeBackBufferRenderTarget(
        width: Int, height: Int,
        vkImageHandle: Long, vkFormat: Int, vkQueueFamily: Int,
    ): BackendRenderTarget = makeBackendRenderTarget(width, height, vkImageHandle, vkFormat, vkQueueFamily)

    override fun makeOffscreenBRT(
        target: RenderTarget,
        width: Int,
        height: Int,
    ): Pair<BackendRenderTarget, SurfaceColorFormat> {
        val colorTexture = target.colorTexture as? VulkanGpuTexture
            ?: error("Expected VulkanGpuTexture on offscreen TextureTarget, got ${target.colorTexture?.javaClass}")
        val vkFmt = gpuFormatToVkFormat(colorTexture.format)
        return makeBackendRenderTarget(width, height, colorTexture.vkImage(), vkFmt, queueFamilyIndex) to
                SurfaceColorFormat.RGBA_8888
    }

    override fun getMainColorImageInfo(): Triple<Long, Int, Int> = try {
        val renderTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        val colorTexture = renderTarget.colorTexture as? VulkanGpuTexture
            ?: return Triple(0L, 0, 0)
        Triple(colorTexture.vkImage(), gpuFormatToVkFormat(colorTexture.format), queueFamilyIndex)
    } catch (e: Exception) {
        LOG.warn("getMainColorImageInfo failed", e)
        Triple(0L, 0, 0)
    }

    override fun midFrameFlush() {
        // Submit MC's pending command buffer so Skia sees a consistent image state.
        RenderSystem.getDevice().createCommandEncoder().submit()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(NativeVulkanService::class.java)

        private fun gpuFormatToVkFormat(format: GpuFormat): Int = when (format) {
            GpuFormat.RGBA8_UNORM -> VK_FORMAT_R8G8B8A8_UNORM
            GpuFormat.RGBA8_SNORM -> VK_FORMAT_R8G8B8A8_SNORM
            else -> VK_FORMAT_R8G8B8A8_UNORM
        }

        @JvmStatic
        fun tryCreate(): NativeVulkanService? = try {
            val rawDevice = RenderSystem.getDevice()
                ?: return null.also { LOG.warn("RenderSystem.getDevice() returned null") }

            val backend = (rawDevice as GpuDeviceAccessor).`oneconfig$getBackend`()
            val vkDev = backend as? VulkanDevice
                ?: return null.also {
                    LOG.info("GpuDevice backend is not Vulkan ({}); falling back to GL", backend?.javaClass?.name)
                }

            val lwjglDevice = vkDev.vkDevice()
            val vkDevice = lwjglDevice.address()
            val vkPhysDevice = lwjglDevice.physicalDevice.address()
            val vkInstance = vkDev.instance().vkInstance().address()

            val graphicsQueue = vkDev.graphicsQueue()
            val vkQueue = graphicsQueue.vkQueue().address()
            val queueFamily = graphicsQueue.queueFamilyIndex()

            LOG.info(
                "Native VK handles: instance=0x{} physDev=0x{} device=0x{} queue=0x{} family={}",
                vkInstance.toHexString(), vkPhysDevice.toHexString(),
                vkDevice.toHexString(), vkQueue.toHexString(), queueFamily,
            )
            NativeVulkanService(vkInstance, vkPhysDevice, vkDevice, vkQueue, queueFamily)
        } catch (e: Exception) {
            LOG.error("NativeVulkanService.tryCreate() failed", e)
            null
        }
    }
}
*///? }
