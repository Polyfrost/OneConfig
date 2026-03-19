package org.polyfrost.oneconfig.internal.ui.compose

import net.minecraft.client.Minecraft
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.opengl.GL11
import org.polyfrost.oneconfig.internal.ui.compose.opengl.StoredGLState
import org.polyfrost.oneconfig.internal.ui.services.VulkanService
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

object SkiaCtx {
    private val LOG = LoggerFactory.getLogger(SkiaCtx::class.java)

    lateinit var directContext: DirectContext
    internal var vulkanService: VulkanService? = null

    private val client get() = Minecraft.getInstance()

    val isReady get() = this::directContext.isInitialized
    val isVulkanMode get() = vulkanService?.isVulkan == true

    private val queuedHudDraws = CopyOnWriteArrayList<() -> Unit>()
    private val queuedDraws = CopyOnWriteArrayList<() -> Unit>()

    private val gl = StoredGLState(330)

    private var glHudSurface: Surface? = null
    private var glHudBrt: BackendRenderTarget? = null

    private var glSurface: Surface? = null
    private var glBrt: BackendRenderTarget? = null

    private data class VkSurfaceEntry(val surface: Surface, val brt: BackendRenderTarget)
    private val vkSurfaces = LinkedHashMap<Long, VkSurfaceEntry>()
    private var vkSurfaceWidth = 0
    private var vkSurfaceHeight = 0

    private var currentSurface: Surface? = null
    val canvas get() = currentSurface!!.canvas

    fun init() {
        System.setProperty("skiko.macos.opengl.enabled", "true")
        if (!this::directContext.isInitialized) {
            try {
                val svc = VulkanService.detect()
                vulkanService = svc
                directContext = svc.makeDirectContext()
                LOG.info("SkiaCtx initialized (vulkan={})", svc.isVulkan)
            } catch (e: Exception) {
                LOG.error("SkiaCtx.init() !", e)
                vulkanService = null
            }
        }
    }

    fun queueHudDraw(block: Runnable) {
        queuedHudDraws.add { block.run() }
    }

    fun queueDraw(block: () -> Unit) {
        queuedDraws.add(block)
    }

    fun queueDraw(block: Runnable) {
        queuedDraws.add { block.run() }
    }

    fun drawNow() {
        flush(queuedHudDraws, midFrame = true)
    }

    fun draw() {
        flush(queuedDraws, midFrame = false)
    }

    private fun flush(queue: CopyOnWriteArrayList<() -> Unit>, midFrame: Boolean) {
        if (!this::directContext.isInitialized) return

        val draws = queue.toList()
        queue.clear()
        if (draws.isEmpty()) return

        currentSurface = if (isVulkanMode) {
            resolveVkSurface()
        } else if (midFrame) {
            resolveGLHudSurface()
        } else {
            resolveGLSurface()
        }
        if (currentSurface == null) return

        if (isVulkanMode) {
            vulkanService?.midFrameFlush()
            directContext.resetAll()
        } else {
            gl.capture()
            directContext.resetGLAll()
            GL11.glViewport(0, 0, currentSurface!!.width, currentSurface!!.height)
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
        }

        draws.forEach { it() }

        if (isVulkanMode) {
            directContext.flushAndSubmit(currentSurface!!, false)
        } else {
            directContext.flush()
            gl.restore()
        }

        currentSurface = null
    }

    fun recreateSurface(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (isVulkanMode) {
            invalidateVkSurfaces()
        } else {
            glSurface?.close(); glSurface = null
            glBrt?.close(); glBrt = null
            glHudSurface?.close(); glHudSurface = null
            glHudBrt?.close(); glHudBrt = null
        }
    }

    private fun resolveVkSurface(): Surface? {
        val svc = vulkanService ?: return null
        val (vkImg, vkFmt, queueFamily) = svc.getMainColorImageInfo()
        if (vkImg == 0L) return null

        val w = client.window.width
        val h = client.window.height
        if (w <= 0 || h <= 0) return null

        if (w != vkSurfaceWidth || h != vkSurfaceHeight) {
            invalidateVkSurfaces()
            vkSurfaceWidth = w
            vkSurfaceHeight = h
        }

        return vkSurfaces.getOrPut(vkImg) {
            val brt = svc.makeBackendRenderTarget(w, h, vkImg, vkFmt, queueFamily)
            val colorFmt = when (vkFmt) {
                44, 50 -> SurfaceColorFormat.BGRA_8888
                else   -> SurfaceColorFormat.RGBA_8888
            }
            val surf = Surface.makeFromBackendRenderTarget(
                directContext, brt,
                SurfaceOrigin.BOTTOM_LEFT,
                colorFmt,
                ColorSpace.sRGB,
                null,
            )
            if (surf == null) {
                brt.close()
                LOG.warn("makeFromBackendRenderTarget returned null for VkImage=0x{} fmt={}",
                    java.lang.Long.toHexString(vkImg), vkFmt)
                return null
            }
            LOG.info("Created VK surface #{} for image 0x{} ({}x{} fmt={})",
                vkSurfaces.size + 1, java.lang.Long.toHexString(vkImg), w, h, vkFmt)
            VkSurfaceEntry(surf, brt)
        }.surface
    }

    private fun invalidateVkSurfaces() {
        vkSurfaces.values.forEach { (s, brt) -> s.close(); brt.close() }
        vkSurfaces.clear()
    }

    private fun resolveGLHudSurface(): Surface? {
        val svc = vulkanService ?: return null
        val w = client.window.width
        val h = client.window.height
        if (w <= 0 || h <= 0) return null
        val existing = glHudSurface
        if (existing != null && existing.width == w && existing.height == h) return existing

        glHudSurface?.close(); glHudBrt?.close()
        glHudBrt = svc.makeBackendRenderTarget(w, h)
        glHudSurface = Surface.makeFromBackendRenderTarget(
            directContext, glHudBrt!!,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
            null,
        )
        return glHudSurface
    }

    private fun resolveGLSurface(): Surface? {
        val svc = vulkanService ?: return null
        val w = client.window.width
        val h = client.window.height
        if (w <= 0 || h <= 0) return null
        val existing = glSurface
        if (existing != null && existing.width == w && existing.height == h) return existing

        glSurface?.close(); glBrt?.close()
        glBrt = svc.makeBackBufferRenderTarget(w, h)
        glSurface = Surface.makeFromBackendRenderTarget(
            directContext, glBrt!!,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
            null,
        )
        return glSurface
    }
}
