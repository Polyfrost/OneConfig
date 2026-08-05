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
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
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

    /**
     * True only for backends needing the deferred snapshot/blit compose path (VulkanMod)
     */
    val isDeferredComposeBackend get() = vulkanService?.usesDeferredCompose == true

    val isVulkanModInstalled: Boolean by lazy {
        try {
            ModInfo.loadedMods.any { it.id.equals("vulkanmod", ignoreCase = true) }
        } catch (_: Throwable) {
            runCatching {
                Class.forName("net.vulkanmod.Initializer", false, javaClass.classLoader)
            }.isSuccess
        }
    }

    fun unavailableReason(): String? {
        if (isReady) return null
        return if (isVulkanModInstalled) {
            "OneConfig's UI can't render while VulkanMod is installed, because VulkanMod replaces " +
                "Minecraft's OpenGL renderer. Remove VulkanMod to use the OneConfig UI."
        } else {
            "OneConfig's UI failed to initialize and can't be opened. Please check your logs and report this."
        }
    }

    private val queuedHudDraws = CopyOnWriteArrayList<() -> Unit>()
    private val queuedDraws = CopyOnWriteArrayList<() -> Unit>()
    private val queuedWarmups = CopyOnWriteArrayList<() -> Unit>()

    private val gl = StoredGLState(330)

    private var hudTarget: TextureTarget? = null
    private var hudSurface: Surface? = null
    private var hudBrt: BackendRenderTarget? = null

    private var composeTarget: TextureTarget? = null
    private var composeSurface: Surface? = null
    private var composeBrt: BackendRenderTarget? = null

    private var hudNeedsSamplingTransition = false
    private var composeNeedsSamplingTransition = false

    private var hudRealIsGeneral = false
    private var composeRealIsGeneral = false

    @Volatile
    private var composeActive = false
    @Volatile
    private var composeDirty = false
    @Volatile
    private var composeRender: (() -> Unit)? = null

    fun submitComposeFrame(dirty: Boolean, render: Runnable) {
        composeActive = true
        if (dirty || composeRender == null) {
            composeRender = { render.run() }
            composeDirty = true
        }
    }

    fun clearComposeFrame() {
        composeActive = false
        composeDirty = false
        composeRender = null
    }

    //? >= 1.21.5 {
    private val HUD_TEXTURE_LOC = Identifier.fromNamespaceAndPath("oneconfig", "hud_skia")
    private val COMPOSE_TEXTURE_LOC = Identifier.fromNamespaceAndPath("oneconfig", "compose_skia")
    private var hudTextureWrapper: HudGpuTexture? = null
    private var composeTextureWrapper: HudGpuTexture? = null

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
    private val COMPOSE_TEXTURE_LOC = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("oneconfig", "compose_skia")
    private var hudTextureWrapper: HudGlTexture? = null
    private var composeTextureWrapper: HudGlTexture? = null
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
                if (isVulkanModInstalled) {
                    LOG.error(
                        "SkiaCtx.init() failed: VulkanMod is installed.", e
                    )
                } else {
                    LOG.error("SkiaCtx.init() !", e)
                }
                vulkanService = null
            }
        }
    }

    fun queueHudDraw(block: Runnable) {
        queuedHudDraws.add { block.run() }
    }

    fun setNotifRenderer(block: Runnable?) {
        notifRender = block?.let { r -> { r.run() } }
    }

    @Volatile
    private var notifRender: (() -> Unit)? = null

    fun setPostComposeRenderer(block: Runnable?) {
        postComposeRender = block?.let { r -> { r.run() } }
    }

    @Volatile
    private var postComposeRender: (() -> Unit)? = null

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
        val surface = resolveHudSurface() ?: return
        if (hudRealIsGeneral) hudTarget?.let { vulkanService?.transitionOffscreenForRendering(it) }
        flushToTarget(draws, surface)
        hudNeedsSamplingTransition = true
    }

    @Volatile
    private var blurSnapshotRequested = false

    fun requestBlurSnapshot() {
        blurSnapshotRequested = true
    }

    fun consumeBlurSnapshotRequest(): Boolean {
        val requested = blurSnapshotRequested
        blurSnapshotRequested = false
        return requested
    }

    fun takeWorldSnapshotIfNeeded() {
        if (!this::directContext.isInitialized || !isDeferredComposeBackend) return
        //? if >= 26.2 {
        /*vulkanService?.takeWorldSnapshot(client.gameRenderer.mainRenderTarget())
        *///? } else {
        vulkanService?.takeWorldSnapshot(client.mainRenderTarget)
        //? }
    }

    fun drawComposeBlit(ctx: GuiGraphicsExtractor, block: Runnable) {
        if (!this::directContext.isInitialized) return
        //? if < 1.21.10 {
        /*takeWorldSnapshotIfNeeded()
        *///? }
        val queued = queuedDraws.toList()
        queuedDraws.clear()
        val post = postComposeRender
        val draws = if (post != null) queued + { block.run() } + post else queued + { block.run() }
        val surface = resolveComposeSurface() ?: return
        if (composeRealIsGeneral) composeTarget?.let { vulkanService?.transitionOffscreenForRendering(it) }
        flushToTarget(draws, surface)
        composeNeedsSamplingTransition = true
        blitCompose(ctx)
    }

    fun blitComposeCached(ctx: GuiGraphicsExtractor): Boolean {
        if (!this::directContext.isInitialized) return false
        if (composeTarget == null || composeSurface == null) return false
        blitCompose(ctx)
        return true
    }

    /**
     * Pre-26.1 only: the fullscreen Compose GUI is drawn straight onto the back buffer (see [resolveGLSurface]),
     * so it never reaches the main render target that screenshots read. Called right before a screenshot reads
     * the main render target's colour texture; blits the finished back buffer (which already holds the GUI) into
     * it so the capture matches what is on screen. The HUD is unaffected - it is already blitted into the main RT.
     */
    fun compositeBackBufferForScreenshot(target: com.mojang.blaze3d.pipeline.RenderTarget) {
        //26.1+ draws compose into the main render target already, so no compositing is needed here.
        //? if < 26.1 {
        /*if (!this::directContext.isInitialized) return
        if (isVulkanMode) return
        if (Platform.screen().current<Any>() !is ComposeScreen) return
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

    @Volatile
    @JvmField
    var suppressInGameHudRender = false

    fun blitHud(guiGraphics: GuiGraphicsExtractor) {
        val rt = hudTarget ?: return
        val w = rt.width
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
        if (hudNeedsSamplingTransition) {
            vulkanService?.transitionOffscreenForSampling(rt)
            hudNeedsSamplingTransition = false
            hudRealIsGeneral = true
        }
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

    private fun blitCompose(guiGraphics: GuiGraphicsExtractor) {
        val rt = composeTarget ?: return
        val w = rt.width
        val h = rt.height
        val guiScale = client.window.guiScale.toFloat()

        //? >= 1.21.5 {
        val colorTex = rt.getColorTexture() ?: return
        var wrapper = composeTextureWrapper
        if (wrapper == null) {
            wrapper = HudGpuTexture()
            composeTextureWrapper = wrapper
            client.textureManager.register(COMPOSE_TEXTURE_LOC, wrapper)
        }
        wrapper.setGpuTexture(colorTex)
        //? >= 1.21.8 {
        wrapper.setGpuTextureView(rt.getColorTextureView())
        if (composeNeedsSamplingTransition) {
            vulkanService?.transitionOffscreenForSampling(rt)
            composeNeedsSamplingTransition = false
            composeRealIsGeneral = true
        }
        guiGraphics.pose().pushMatrix()
        guiGraphics.pose().scale(1f / guiScale, 1f / guiScale)
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, COMPOSE_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        guiGraphics.pose().popMatrix()
        //? } else {
        /*guiGraphics.pose().pushPose()
        guiGraphics.pose().scale(1f / guiScale, 1f / guiScale, 1f)
        guiGraphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, COMPOSE_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        guiGraphics.pose().popPose()
        *///? }
        //? } else {
        /*var wrapper = composeTextureWrapper
        if (wrapper == null) {
            wrapper = HudGlTexture()
            composeTextureWrapper = wrapper
            client.textureManager.register(COMPOSE_TEXTURE_LOC, wrapper)
        }
        wrapper.setGlTexId(rt.colorTextureId)
        guiGraphics.pose().pushPose()
        guiGraphics.pose().scale(1f / guiScale, 1f / guiScale, 1f)
        //? >= 1.21.4 {
        guiGraphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, COMPOSE_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        //?} else {
        /*com.mojang.blaze3d.systems.RenderSystem.enableBlend()
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc()
        guiGraphics.blit(COMPOSE_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
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
        val notifDraw = if (NotificationsManager.count > 0) notifRender else null
        val wantCompose = composeActive && !isVulkanMode
        if (draws.isEmpty() && notifDraw == null && !wantCompose) return

        val mainSurface = if (isVulkanMode) resolveVkSurface() else resolveGLSurface()
        if (mainSurface == null) return
        currentSurface = mainSurface

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
                GL11.glViewport(0, 0, mainSurface.width, mainSurface.height)
                GL11.glDisable(GL11.GL_SCISSOR_TEST)
            }

//            val profiling = GpuProfiler.enabled && !isVulkanMode
//            val sections = profiling && GpuProfiler.perSection
//            if (profiling) GpuProfiler.frameBegin()

            draws.forEach { it() }        // blur backdrop onto the main RT (samples the live world)
//            if (sections) { directContext.flush(); GpuProfiler.mark("blur") }

            if (wantCompose) {
                val cs = resolveComposeSurface()
                if (cs != null) {
                    val block = composeRender
                    if (composeDirty && block != null) {
                        currentSurface = cs
                        GL11.glViewport(0, 0, cs.width, cs.height)
                        cs.canvas.clear(Color.TRANSPARENT)
                        block()
                        composeDirty = false
                        currentSurface = mainSurface
                        GL11.glViewport(0, 0, mainSurface.width, mainSurface.height)
//                        if (sections) { directContext.flush(); GpuProfiler.mark("compose.render") }
                    }
                    cs.draw(mainSurface.canvas, 0, 0, null)
//                    if (sections) { directContext.flush(); GpuProfiler.mark("compose.blit") }
                }
            }

            postComposeRender?.invoke()

            notifDraw?.invoke()
//            if (sections) { directContext.flush(); GpuProfiler.mark("notif") }

            if (isVulkanMode) {
                directContext.flushAndSubmit(mainSurface, false)
                vulkanService?.restoreMainRTLayout()
            } else {
                directContext.flush()
//                if (profiling) {
//                    GpuProfiler.mark(if (sections) "flush.tail" else "draw")
//                    GpuProfiler.frameEnd()
//                }
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
        destroyComposeTarget()
    }

    private fun flushToTarget(draws: List<() -> Unit>, surface: Surface) {
        currentSurface = surface
        val savedFbo = IntArray(1)
        try {
            if (isVulkanMode) {
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
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return null

        var rt = hudTarget
        val needNewTarget = rt == null || rt.width != w || rt.height != h
        if (needNewTarget) {
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
        }

        if (needNewTarget || hudSurface == null) {
            hudSurface?.close(); hudSurface = null
            hudBrt?.close(); hudBrt = null
            hudRealIsGeneral = false
            val svc = vulkanService ?: return null
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

    private var composeAllocFailedAt = 0L
    private var composeAllocReported = false

    private const val ALLOC_RETRY_COOLDOWN_MS = 2000L

    private fun resolveComposeSurface(): Surface? {
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return null

        var rt = composeTarget
        val needNewTarget = rt == null || rt.width != w || rt.height != h
        if (needNewTarget) {
            if (System.currentTimeMillis() - composeAllocFailedAt < ALLOC_RETRY_COOLDOWN_MS) return null
            destroyComposeTarget()
            rt = try {
                //? if >= 26.2 {
                /*TextureTarget(null, w, h, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)
                *///? } else if >= 1.21.5 {
                TextureTarget(null, w, h, true)
                //? } else if >= 1.21.4 {
                // TextureTarget(w, h, true)
                //? } else {
                /*TextureTarget(w, h, true, Minecraft.ON_OSX)
                *///? }
            } catch (e: Throwable) {
                onComposeAllocFailure(w, h, e)
                return null
            }
            composeTarget = rt

            //? >= 1.21.5 {
            if (!isVulkanMode) {
                val fboId = org.polyfrost.oneconfig.internal.ui.RenderTargetFbo.getFboId(rt)
                if (fboId <= 0) {
                    LOG.warn("SkiaCtx: compose TextureTarget FBO not ready (id={}), retry next frame", fboId)
                    composeTarget = null
                    rt.destroyBuffers()
                    return null
                }
            }
            //? }
        }

        if (needNewTarget || composeSurface == null) {
            composeSurface?.close(); composeSurface = null
            composeBrt?.close(); composeBrt = null
            composeRealIsGeneral = false
            val svc = vulkanService ?: return null
            val brt = try {
                svc.makeOffscreenBRT(rt!!, w, h)
            } catch (e: Throwable) {
                destroyComposeTarget()
                onComposeAllocFailure(w, h, e)
                return null
            }
            composeBrt = brt.first
            val composeOrigin = if (isVulkanMode) SurfaceOrigin.TOP_LEFT else SurfaceOrigin.BOTTOM_LEFT
            composeSurface = Surface.makeFromBackendRenderTarget(
                directContext, brt.first,
                composeOrigin,
                brt.second,
                ColorSpace.sRGB,
                null,
            )
            if (composeSurface == null) {
                LOG.warn("SkiaCtx: composeSurface is null (w={} h={} vk={})", w, h, isVulkanMode)
                brt.first.close(); composeBrt = null
            }
        }
        return composeSurface
    }

    private fun onComposeAllocFailure(w: Int, h: Int, error: Throwable) {
        composeAllocFailedAt = System.currentTimeMillis()
        destroyComposeTarget()
        destroyHudTarget()
        if (isVulkanMode) invalidateVkSurfaces()
        runCatching { directContext.flush() }
        LOG.error("SkiaCtx: failed to allocate the {}x{} compose target; skipping compose frames", w, h, error)
        if (!composeAllocReported) {
            composeAllocReported = true
            runCatching {
                Platform.screen().showMessage(
                    "OneConfig couldn't allocate GPU memory for its UI (${w}x$h). " +
                        "Lower your resolution or render scale, or close other GPU-heavy programs."
                )
            }
        }
    }

    private fun destroyComposeTarget() {
        composeSurface?.close(); composeSurface = null
        composeBrt?.close(); composeBrt = null
        composeTarget?.destroyBuffers()
        composeTarget = null
    }

    private fun resolveGLSurface(): Surface? {
        val svc = vulkanService ?: return null
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
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

        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return null

        if (w != vkSurfaceWidth || h != vkSurfaceHeight || svc.offscreenNeedsPerFrameRewrap) {
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
