package org.polyfrost.oneconfig.internal.ui.services

//? cinnabar {
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import graphics.cinnabar.api.c3d.C3DGpuDevice
import net.minecraft.client.Minecraft
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.SurfaceColorFormat
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import kotlin.jvm.javaClass

/**
 * Vulkan-backed [VulkanService] using Cinnabar's backend
 */
class CinnabarVulkanService private constructor(
    private val c3dDevice: C3DGpuDevice,
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
        val deviceProcAddr   = provider.getFunctionAddress("vkGetDeviceProcAddr")
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
    ): BackendRenderTarget = BackendRenderTarget.Companion.makeVulkan(
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

    override fun makeOffscreenBRT(
        target: RenderTarget,
        width: Int,
        height: Int,
    ): Pair<BackendRenderTarget, SurfaceColorFormat> {
        val colorTexture = target.colorTexture as? Hg3DGpuTexture
            ?: error("Expected Hg3DGpuTexture on offscreen TextureTarget, got ${target.colorTexture?.javaClass}")
        val image = colorTexture.image() as? MercuryImage
            ?: error("Expected MercuryImage, got ${colorTexture.image()?.javaClass}")
        val vkFmt = hgFormatToVkFormat(image.format().toString())
        val colorFormat = when (vkFmt) {
            VK_FORMAT_B8G8R8A8_UNORM, VK_FORMAT_B8G8R8A8_SRGB -> SurfaceColorFormat.BGRA_8888
            else -> SurfaceColorFormat.RGBA_8888
        }
        return makeBackendRenderTarget(width, height, image.vkImage(), vkFmt, queueFamilyIndex) to colorFormat
    }

    override fun midFrameFlush() {
        val encoder = c3dDevice.createCommandEncoder()
        findMethod(encoder, "endCommandBuffers")?.invoke(encoder)
        findMethod(encoder, "flush")?.invoke(encoder)
    }

    override fun getMainColorImageInfo(): Triple<Long, Int, Int> = try {
        val renderTarget = Minecraft.getInstance().mainRenderTarget
        val colorTexture = renderTarget.colorTexture as? Hg3DGpuTexture
            ?: return Triple(0L, 0, 0)
        val image = colorTexture.image() as? MercuryImage
            ?: return Triple(0L, 0, 0)
        val vkImage = image.vkImage()
        val format = image.format()
        Triple(vkImage, hgFormatToVkFormat(format.toString()), queueFamilyIndex)
    } catch (e: Exception) {
        LOG.warn("getMainColorImageInfo failed", e)
        Triple(0L, 0, 0)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CinnabarVulkanService::class.java)

        fun hgFormatToVkFormat(name: String): Int= when (name) {
            "RGBA8_UNORM" -> VK_FORMAT_R8G8B8A8_UNORM
            "RGBA8_SRGB" -> VK_FORMAT_R8G8B8A8_SRGB
            "BGRA8_UNORM" -> VK_FORMAT_B8G8R8A8_UNORM
            "BGRA8_SRGB" -> VK_FORMAT_B8G8R8A8_SRGB
            else -> VK_FORMAT_B8G8R8A8_UNORM
        }

        @JvmStatic
        fun tryCreate(): CinnabarVulkanService? = try {
            // this is a horrid way...

            val rawDevice = RenderSystem.getDevice()
                ?: return null.also { LOG.warn("RenderSystem.getDevice() returned null") }

            val c3dDevice: C3DGpuDevice = rawDevice as? C3DGpuDevice
                ?: findInFields(rawDevice, C3DGpuDevice::class.java)
                ?: return null.also {
                    LOG.warn("Cannot find C3DGpuDevice in ${rawDevice.javaClass.name}")
                }

            val hgDevice = findMethod(c3dDevice, "hgDevice")?.invoke(c3dDevice)
                ?: return null.also { LOG.warn("hgDevice() not found or returned null") }

            val lwjglVkDevice = hgDevice.javaClass.getMethod("vkDevice").invoke(hgDevice)
            val vkDevice = lwjglVkDevice.javaClass.getMethod("address").invoke(lwjglVkDevice) as Long

            val vkPhysDevice = reflectHandle(hgDevice, "vkPhysicalDevice")
                ?: return null.also { LOG.warn("vkPhysicalDevice field not found on ${hgDevice.javaClass.name}") }

            val vkInstance = reflectHandle(hgDevice, "vkInstance")
                ?: return null.also { LOG.warn("vkInstance field not found") }

            val hgQueueTypeClass = Class.forName($$"graphics.cinnabar.api.hg.HgQueue$Type")
            val graphicsType = hgQueueTypeClass.enumConstants.first { (it as Enum<*>).name == "GRAPHICS" }
            val graphicsQueue = findMethod(hgDevice, "queue", hgQueueTypeClass)?.invoke(hgDevice, graphicsType)
                ?: return null.also { LOG.warn("queue(GRAPHICS) not found") }
            val vkQueueObj = graphicsQueue.javaClass.getMethod("vkQueue").invoke(graphicsQueue)
            val vkQueue = vkQueueObj.javaClass.getMethod("address").invoke(vkQueueObj) as Long
            val queueFamily = graphicsQueue.javaClass.getMethod("familyIndex").invoke(graphicsQueue) as Int

            LOG.info(
                "VK handles: instance=0x{} physDev=0x{} device=0x{} queue=0x{} family={}",
                vkInstance.toHexString(), vkPhysDevice.toHexString(),
                vkDevice.toHexString(), vkQueue.toHexString(), queueFamily,
            )
            CinnabarVulkanService(c3dDevice, vkInstance, vkPhysDevice, vkDevice, vkQueue, queueFamily)
        } catch (e: Exception) {
            LOG.error("CinnabarVulkanService.tryCreate() failed", e)
            null
        }

        private fun <T> findInFields(obj: Any, type: Class<T>): T? {
            var cls: Class<*>? = obj.javaClass
            while (cls != null && cls != Any::class.java) {
                for (field in cls.declaredFields) {
                    try {
                        field.isAccessible = true
                        val value = field.get(obj) ?: continue
                        if (type.isInstance(value)) {
                            @Suppress("UNCHECKED_CAST")
                            return value as T
                        }
                    } catch (_: Exception) {}
                }
                cls = cls.superclass
            }
            return null
        }

        private fun findMethod(obj: Any, name: String, vararg params: Class<*>): Method? {
            var cls: Class<*>? = obj.javaClass
            while (cls != null && cls != Any::class.java) {
                try {
                    return cls.getDeclaredMethod(name, *params).also { it.isAccessible = true }
                } catch (_: NoSuchMethodException) {
                    cls = cls.superclass
                }
            }
            return null
        }

        private fun reflectHandle(obj: Any, fieldName: String): Long? {
            var cls: Class<*>? = obj.javaClass
            while (cls != null && cls != Any::class.java) {
                try {
                    val field = cls.getDeclaredField(fieldName).also { it.isAccessible = true }
                    val fieldObj = field.get(obj) ?: return null
                    return fieldObj.javaClass.getMethod("address").invoke(fieldObj) as Long
                } catch (_: NoSuchFieldException) {
                    cls = cls.superclass
                }
            }
            return null
        }
    }
}
//? }