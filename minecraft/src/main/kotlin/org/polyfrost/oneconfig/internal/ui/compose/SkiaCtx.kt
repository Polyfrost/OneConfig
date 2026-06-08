package org.polyfrost.oneconfig.internal.ui.compose

import com.mojang.blaze3d.pipeline.TextureTarget
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
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
    private val queuedWarmups = CopyOnWriteArrayList<() -> Unit>()

    private val gl = StoredGLState(330)

    private var hudTarget: TextureTarget? = null
    private var hudSurface: Surface? = null
    private var hudBrt: BackendRenderTarget? = null

    //? >= 1.21.5 {
    private val HUD_TEXTURE_LOC = Identifier.fromNamespaceAndPath("oneconfig", "hud_skia")
    private var hudTextureWrapper: HudGpuTexture? = null

    private class HudGpuTexture : net.minecraft.client.renderer.texture.AbstractTexture() {
        fun setGpuTexture(t: com.mojang.blaze3d.textures.GpuTexture?) {
            this.texture = t
        }

        //? >= 1.21.8 {
        fun setGpuTextureView(v: com.mojang.blaze3d.textures.GpuTextureView?) {
            this.textureView = v
            //? >= 26.1 {
            this.sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                .getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR)
            //? }
        }
        //? }

        override fun close() {
            this.texture = null
            //? >= 1.21.8
            this.textureView = null
        }
    }
    //? } else {
    /*private val HUD_TEXTURE_LOC = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("oneconfig", "hud_skia")
    private var hudTextureWrapper: HudGlTexture? = null
    //? >= 1.21.4 {
    private class HudGlTexture : net.minecraft.client.renderer.texture.AbstractTexture() {
        fun setGlTexId(id: Int) { this.id = id }
        override fun close() { this.id = -1 }
    }
    //? } else {
    /*private class HudGlTexture : net.minecraft.client.renderer.texture.AbstractTexture() {
        fun setGlTexId(id: Int) { this.id = id }
        override fun load(manager: net.minecraft.server.packs.resources.ResourceManager) {}
        override fun close() { this.id = -1 }
    }
    *///? }
    *///? }

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

    fun queueWarmup(block: () -> Unit) {
        queuedWarmups.add(block)
    }

    private fun runWarmups() {
        if (!this::directContext.isInitialized) return
        if (queuedWarmups.isEmpty()) return
        val warmups = queuedWarmups.toList()
        queuedWarmups.clear()
        val savedFbo = IntArray(1)
        try {
            if (isVulkanMode) {
                vulkanService?.midFrameFlush()
                directContext.resetAll()
            } else {
                gl.capture()
                GL30.glGetIntegerv(GL30.GL_FRAMEBUFFER_BINDING, savedFbo)
                directContext.resetGLAll()
            }

            warmups.forEach { it() }

            if (isVulkanMode) {
                directContext.flush()
            } else {
                directContext.flush()
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFbo[0])
                gl.restore()
            }
        } catch (e: Throwable) {
            LOG.warn("SkiaCtx.runWarmups() error", e)
            if (!isVulkanMode) try {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFbo[0])
                gl.restore()
            } catch (_: Throwable) {
            }
        }
    }

    fun drawNow() {
        if (!this::directContext.isInitialized) return
        val draws = queuedHudDraws.toList()
        queuedHudDraws.clear()
        if (draws.isEmpty()) return
        flushToTarget(draws, resolveHudSurface() ?: return)
    }

    /**
     * Pre-26.1 only: the fullscreen Compose GUI is drawn straight onto the back buffer (see [resolveGLSurface]),
     * so it never reaches the main render target that screenshots read. Called right before a screenshot reads
     * the main render target's colour texture; blits the finished back buffer (which already holds the GUI) into
     * it so the capture matches what is on screen. The HUD is unaffected - it is already blitted into the main RT.
     */
    fun compositeBackBufferForScreenshot(target: com.mojang.blaze3d.pipeline.RenderTarget) {
        //? if >= 26.1 {
         26.1+ draws compose into the main render target already, so no compositing is needed here.
        //? } else {
        /*if (!this::directContext.isInitialized) return
        if (isVulkanMode) return
        if (client.screen !is ComposeScreen) return
        val w = target.width
        val h = target.height
        if (w <= 0 || h <= 0) return
        val drawFbo = org.polyfrost.oneconfig.internal.ui.RenderTargetFbo.getFboId(target)
        if (drawFbo <= 0) return

        val savedRead = IntArray(1)
        val savedDraw = IntArray(1)
        GL30.glGetIntegerv(GL30.GL_READ_FRAMEBUFFER_BINDING, savedRead)
        GL30.glGetIntegerv(GL30.GL_DRAW_FRAMEBUFFER_BINDING, savedDraw)
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0)
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo)
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST)
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, savedRead[0])
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, savedDraw[0])
        }
        *///? }
    }

    fun blitHud(guiGraphics: GuiGraphicsExtractor) {
        val rt = hudTarget ?: return
        val w = rt.width;
        val h = rt.height
        val guiScale = client.window.guiScale.toFloat()

        //? >= 1.21.5 {
        val colorTex = rt.getColorTexture() ?: return
        var wrapper = hudTextureWrapper
        if (wrapper == null) {
            wrapper = HudGpuTexture()
            hudTextureWrapper = wrapper
            client.textureManager.register(HUD_TEXTURE_LOC, wrapper)
        }
        wrapper.setGpuTexture(colorTex)
        //? >= 1.21.8 {
        wrapper.setGpuTextureView(rt.getColorTextureView())
        guiGraphics.pose().pushMatrix()
        guiGraphics.pose().scale(1f / guiScale, 1f / guiScale)
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, HUD_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        guiGraphics.pose().popMatrix()
        //? } else {
        /*guiGraphics.pose().pushPose()
        guiGraphics.pose().scale(1f / guiScale, 1f / guiScale, 1f)
        guiGraphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, HUD_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        guiGraphics.pose().popPose()
        *///? }
        //? } else {
        /*var wrapper = hudTextureWrapper
        if (wrapper == null) {
            wrapper = HudGlTexture()
            hudTextureWrapper = wrapper
            client.textureManager.register(HUD_TEXTURE_LOC, wrapper)
        }
        wrapper.setGlTexId(rt.colorTextureId)
        guiGraphics.pose().pushPose()
        guiGraphics.pose().scale(1f / guiScale, 1f / guiScale, 1f)
        //? >= 1.21.4 {
        guiGraphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, HUD_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        //?} else {
        /*com.mojang.blaze3d.systems.RenderSystem.enableBlend()
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc()
        guiGraphics.blit(HUD_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        com.mojang.blaze3d.systems.RenderSystem.disableBlend()
        *///?}
        guiGraphics.pose().popPose()
        *///? }
    }

    fun draw() {
        if (!this::directContext.isInitialized) return
        runWarmups()
        val draws = queuedDraws.toList()
        queuedDraws.clear()
        if (draws.isEmpty()) return

        currentSurface = if (isVulkanMode) {
            resolveVkSurface()
        } else {
            resolveGLSurface()
        }
        if (currentSurface == null) return

        val savedFbo = IntArray(1)
        try {
            if (isVulkanMode) {
                vulkanService?.midFrameFlush()
                directContext.resetAll()
            } else {
                gl.capture()
                // Skia's draw binds our main-RT FBO via raw glBindFramebuffer, bypassing MC's
                // GlStateManager cache. Save the framebuffer that was bound when the main framebuffer
                // finished rendering and rebind it after, so the following blitToScreen targets the
                // correct framebuffer instead of the one Skia left bound (otherwise the screen flickers).
                GL30.glGetIntegerv(GL30.GL_FRAMEBUFFER_BINDING, savedFbo)
                directContext.resetGLAll()
                GL11.glViewport(0, 0, currentSurface!!.width, currentSurface!!.height)
                GL11.glDisable(GL11.GL_SCISSOR_TEST)
            }

            draws.forEach { it() }

            if (isVulkanMode) {
                directContext.flushAndSubmit(currentSurface!!, false)
            } else {
                directContext.flush()
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFbo[0])
                gl.restore()
            }
        } catch (e: Throwable) {
            LOG.warn("SkiaCtx.draw() error", e)
            if (!isVulkanMode) try {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFbo[0])
                gl.restore()
            } catch (_: Throwable) {
            }
        } finally {
            currentSurface = null
        }
    }

    fun recreateSurface(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (isVulkanMode) {
            invalidateVkSurfaces()
        } else {
            glSurface?.close(); glSurface = null
            glBrt?.close(); glBrt = null
        }
        destroyHudTarget()
    }

    private fun flushToTarget(draws: List<() -> Unit>, surface: Surface) {
        currentSurface = surface
        val savedFbo = IntArray(1)
        try {
            if (isVulkanMode) {
                vulkanService?.midFrameFlush()
                directContext.resetAll()
            } else {
                gl.capture()
                GL30.glGetIntegerv(GL30.GL_FRAMEBUFFER_BINDING, savedFbo)
                directContext.resetGLAll()
                GL11.glViewport(0, 0, surface.width, surface.height)
                GL11.glDisable(GL11.GL_SCISSOR_TEST)
            }

            canvas.clear(Color.TRANSPARENT)
            draws.forEach { it() }

            if (isVulkanMode) {
                directContext.flushAndSubmit(surface, false)
            } else {
                directContext.flush()
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFbo[0])
                gl.restore()
            }
        } catch (e: Throwable) {
            LOG.warn("SkiaCtx.flushToTarget() error", e)
            if (!isVulkanMode) try {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, savedFbo[0])
                gl.restore()
            } catch (_: Throwable) {
            }
        } finally {
            currentSurface = null
        }
    }

    private fun resolveHudSurface(): Surface? {
        val w = client.window.width
        val h = client.window.height
        if (w <= 0 || h <= 0) return null

        var rt = hudTarget
        if (rt == null || rt.width != w || rt.height != h) {
            destroyHudTarget()
            //? if >= 26.2 {
            /*rt = TextureTarget(null, w, h, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)
            *///? } else if >= 1.21.5 {
            rt = TextureTarget(null, w, h, true)
            //? } else if >= 1.21.4 {
            // rt = TextureTarget(w, h, true)
            //? } else {
            /*rt = TextureTarget(w, h, true, Minecraft.ON_OSX)
            *///? }
            hudTarget = rt

            val svc = vulkanService ?: return null
            //? >= 1.21.5 {
            if (!isVulkanMode) {
                val fboId = org.polyfrost.oneconfig.internal.ui.RenderTargetFbo.getFboId(rt)
                if (fboId <= 0) {
                    LOG.warn("SkiaCtx: hud TextureTarget FBO not ready (id={}), retry next frame", fboId)
                    hudTarget = null
                    rt.destroyBuffers()
                    return null
                }
            }
            //? }
            val (brt, colorFmt) = svc.makeOffscreenBRT(rt, w, h)
            hudBrt = brt
            hudSurface = Surface.makeFromBackendRenderTarget(
                directContext, brt,
                SurfaceOrigin.TOP_LEFT,
                colorFmt,
                ColorSpace.sRGB,
                null,
            )
            if (hudSurface == null) {
                LOG.warn("SkiaCtx: hudSurface is null (w={} h={} vk={})", w, h, isVulkanMode)
                brt.close(); hudBrt = null
            }
        }
        return hudSurface
    }

    private fun destroyHudTarget() {
        hudSurface?.close(); hudSurface = null
        hudBrt?.close(); hudBrt = null
        hudTarget?.destroyBuffers()
        hudTarget = null
    }

    private fun resolveGLSurface(): Surface? {
        val svc = vulkanService ?: return null
        val w = client.window.width
        val h = client.window.height
        if (w <= 0 || h <= 0) return null
        val existing = glSurface
        if (existing != null && existing.width == w && existing.height == h) return existing

        glSurface?.close(); glBrt?.close()
        //? if >= 26.1 {
        // 26.1+: SkiaCtx.draw() runs before RenderTarget.blitToScreen (Mixin_SkiaFramePresent), so compose
        // lands in Minecraft's main render target and is seen by the window blit, screenshots and Tracy captures.
        //? if >= 26.2 {
        /*val target = client.gameRenderer.mainRenderTarget()
        *///? } else {
        val target = client.mainRenderTarget
        //? }
        val (brt, colorFmt) = svc.makeOffscreenBRT(target, w, h)
        glBrt = brt
        glSurface = Surface.makeFromBackendRenderTarget(
            directContext, glBrt!!,
            SurfaceOrigin.BOTTOM_LEFT,
            colorFmt,
            ColorSpace.sRGB,
            null,
        )
        //? } else {
        /*// Pre-26.1: SkiaCtx.draw() runs at Window.updateDisplay, which is after the main render target has
        // already been blitted to the back buffer. Draw straight onto the back buffer so compose is visible.
        glBrt = svc.makeBackBufferRenderTarget(w, h)
        glSurface = Surface.makeFromBackendRenderTarget(
            directContext, glBrt!!,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
            null,
        )
        *///? }
        return glSurface
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
                else -> SurfaceColorFormat.RGBA_8888
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
                LOG.warn(
                    "makeFromBackendRenderTarget returned null for VkImage=0x{} fmt={}",
                    java.lang.Long.toHexString(vkImg), vkFmt
                )
                return null
            }
            VkSurfaceEntry(surf, brt)
        }.surface
    }

    private fun invalidateVkSurfaces() {
        vkSurfaces.values.forEach { (s, brt) -> s.close(); brt.close() }
        vkSurfaces.clear()
    }
}
