package org.polyfrost.oneconfig.internal.ui.services

//? vulkanmod {
/*import com.mojang.blaze3d.pipeline.RenderTarget
import net.vulkanmod.gl.VkGlTexture
import net.vulkanmod.vulkan.Renderer
import net.vulkanmod.vulkan.device.DeviceManager
import net.vulkanmod.vulkan.queue.Queue
import net.vulkanmod.vulkan.texture.VulkanImage
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.SurfaceColorFormat
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK
import org.lwjgl.vulkan.VK12.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkImageBlit
import org.lwjgl.vulkan.VkOffset3D
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo
import org.slf4j.LoggerFactory

class VulkanModVulkanService private constructor(
    private val vkInstance: Long,
    private val vkPhysDevice: Long,
    private val vkDevice: Long,
    private val vkQueue: Long,
    private val queueFamilyIndex: Int,
) : VulkanService {

    override val isVulkan = true

    override val usesDeferredCompose = true

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
        1,
        1,
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
        val image = vulkanImageForTarget(target)
            ?: error("VulkanMod: could not resolve VkImage for offscreen RenderTarget ${target.javaClass.name}")
        val vkFmt = image.format
        return makeBackendRenderTarget(width, height, image.id, vkFmt, queueFamilyIndex) to colorFormatFor(vkFmt)
    }

    override fun getMainColorImageInfo(): Triple<Long, Int, Int> = try {
        val image = Renderer.getInstance()?.swapChain?.colorAttachment
            ?: return Triple(0L, 0, 0)
        Triple(image.id, image.format, queueFamilyIndex)
    } catch (e: Exception) {
        LOG.warn("getMainColorImageInfo failed", e)
        Triple(0L, 0, 0)
    }

    private var snapshotImage: VulkanImage? = null
    private var snapHandle = 0L
    private var snapFmt = 0
    private var snapW = -1
    private var snapH = -1
    private var snapLoggedError = false
    private var snapLoggedOk = false

    override fun takeWorldSnapshot(target: RenderTarget) {
        try {
            val renderer = Renderer.getInstance() ?: return logSnapOnce("no renderer")
            val swapChain = renderer.swapChain ?: return logSnapOnce("no swapChain")
            val src = swapChain.colorAttachment ?: return logSnapOnce("no swapchain color")
            val mainPass = renderer.mainPass ?: return logSnapOnce("no mainPass")
            val w = src.width
            val h = src.height
            if (w <= 0 || h <= 0) return logSnapOnce("bad src size ${w}x$h")
            val snap = ensureSnapshot(w, h) ?: return logSnapOnce("ensureSnapshot failed (${w}x$h)")
            val srcId = src.id
            val cmd = Renderer.getCommandBuffer() ?: return logSnapOnce("no command buffer")

            snap.setCurrentLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            mainPass.bindAsTexture()
            transitionLayout(src, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)      // SHADER_READ -> TRANSFER_SRC
            transitionLayout(snap, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)     // UNDEFINED   -> TRANSFER_DST
            blitImage(cmd, srcId, snapHandle, w, h)
            transitionLayout(snap, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            transitionLayout(src, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            mainPass.rebindMainTarget()
            logSnapOk(w, h)
        } catch (e: Exception) {
            if (!snapLoggedError) {
                snapLoggedError = true
                LOG.warn("takeWorldSnapshot failed", e)
            }
        }
    }

    private fun logSnapOk(w: Int, h: Int) {
        if (!snapLoggedOk) {
            snapLoggedOk = true
            LOG.info("World snapshot OK: {}x{} -> snapHandle=0x{}", w, h, snapHandle.toHexString())
        }
    }

    private fun logSnapOnce(msg: String) {
        if (!snapLoggedError) {
            snapLoggedError = true
            LOG.warn("takeWorldSnapshot skipped: {}", msg)
        }
    }

    override fun getBlurSourceImageInfo(): Triple<Long, Int, Int> =
        if (snapshotImage != null && snapHandle != 0L) Triple(snapHandle, snapFmt, queueFamilyIndex)
        else Triple(0L, 0, 0)

    private fun ensureSnapshot(w: Int, h: Int): VulkanImage? {
        snapshotImage?.let { if (snapW == w && snapH == h) return it; it.free() }
        snapshotImage = null
        snapHandle = 0L

        val usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT or
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_TRANSFER_SRC_BIT
        val img = VulkanImage.Builder(w, h)
            .setFormat(VK_FORMAT_R8G8B8A8_UNORM)
            .setMipLevels(1)
            .setUsage(usage)
            .createVulkanImage() ?: return null

        snapshotImage = img
        snapHandle = img.id
        snapFmt = VK_FORMAT_R8G8B8A8_UNORM
        snapW = w
        snapH = h
        return img
    }

    private fun transitionLayout(img: VulkanImage, layout: Int) {
        MemoryStack.stackPush().use { stack ->
            val cmd = Renderer.getCommandBuffer() ?: return
            img.transitionImageLayout(stack, cmd, layout)
        }
    }

    private fun blitImage(cmd: VkCommandBuffer, srcId: Long, dstId: Long, w: Int, h: Int) {
        MemoryStack.stackPush().use { stack ->
            val region = VkImageBlit.calloc(1, stack)
            region.srcOffsets(0, VkOffset3D.calloc(stack).set(0, 0, 0))
            region.srcOffsets(1, VkOffset3D.calloc(stack).set(w, h, 1))
            region.dstOffsets(0, VkOffset3D.calloc(stack).set(0, 0, 0))
            region.dstOffsets(1, VkOffset3D.calloc(stack).set(w, h, 1))
            region.srcSubresource()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(0).baseArrayLayer(0).layerCount(1)
            region.dstSubresource()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(0).baseArrayLayer(0).layerCount(1)
            vkCmdBlitImage(
                cmd,
                srcId, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                dstId, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                region, VK_FILTER_NEAREST,
            )
        }
    }

    override fun midFrameFlush() {
        try {
            Renderer.getInstance()?.flushCmds()
        } catch (e: Exception) {
            LOG.debug("midFrameFlush failed", e)
        }
    }

    private fun colorFormatFor(vkFormat: Int): SurfaceColorFormat = when (vkFormat) {
        VK_FORMAT_B8G8R8A8_UNORM, VK_FORMAT_B8G8R8A8_SRGB -> SurfaceColorFormat.BGRA_8888
        else -> SurfaceColorFormat.RGBA_8888
    }

    private fun vulkanImageForTarget(target: RenderTarget): VulkanImage? {
        val texId = RenderTargetFbo.getColorTexId(target).takeIf { it != 0 && it != -1 } ?: return null
        return VkGlTexture.getTexture(texId)?.vulkanImage
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(VulkanModVulkanService::class.java)

        @JvmStatic
        fun tryCreate(): VulkanModVulkanService? = try {
            val vkDevice = DeviceManager.vkDevice
                ?: return null.also { LOG.info("VulkanMod present but DeviceManager.vkDevice is null; using GL") }
            val physicalDevice = DeviceManager.physicalDevice
                ?: return null.also { LOG.warn("DeviceManager.physicalDevice is null") }
            val instance = physicalDevice.instance
            val graphicsQueue = DeviceManager.getGraphicsQueue()
                ?: return null.also { LOG.warn("VulkanMod getGraphicsQueue() returned null") }

            val instAddr = instance.address()
            val physAddr = physicalDevice.address()
            val devAddr = vkDevice.address()
            //? if >= 1.21.10 {
            val queueAddr = graphicsQueue.vkQueue().address()
            //? } else {
            /*val queueAddr = graphicsQueue.queue().address()
            *///? }
            val family = Queue.getQueueFamilies().graphicsFamily

            LOG.info(
                "VulkanMod VK handles: instance=0x{} physDev=0x{} device=0x{} queue=0x{} family={}",
                instAddr.toHexString(), physAddr.toHexString(), devAddr.toHexString(), queueAddr.toHexString(), family,
            )
            VulkanModVulkanService(instAddr, physAddr, devAddr, queueAddr, family)
        } catch (e: Exception) {
            LOG.error("VulkanModVulkanService.tryCreate() failed", e)
            null
        }
    }
}
*///? }
