package org.polyfrost.oneconfig.internal.ui.compose

import com.mojang.blaze3d.pipeline.RenderTarget
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.AbstractTexture
import org.lwjgl.opengl.GL11
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsRenderer
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.internal.ui.compose.opengl.StoredGLState
import org.slf4j.LoggerFactory

//? if >= 26.2 {
import com.mojang.renderpearl.api.GpuFormat
//?}

//? if >= 26.1 {
import com.mojang.renderpearl.api.textures.FilterMode
//?}

//? if >= 1.21.8 {
import com.mojang.renderpearl.api.textures.GpuTextureView
//?}

//? if >= 1.21.5 {
import com.mojang.renderpearl.api.textures.GpuTexture
import net.minecraft.client.renderer.RenderPipelines
//?}

//? if >= 1.21.4 {
import net.minecraft.resources.Identifier
//?}

//? if > 1.8.9 {
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphicsExtractor
//?}

//? if < 26.1 {
/*import org.lwjgl.opengl.GL30
*///?}

//? if = 1.21.5 {
/*import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
*///?}

//? if >= 1.21.4 && < 1.21.8 {
/*import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.TriState
*///?}

//? if = 1.21.4 {
/*import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
*///?}

//? if < 1.21.5 {
/*import com.mojang.blaze3d.platform.GlStateManager
*///?}

//? if < 1.21.4 {
/*import net.minecraft.server.packs.resources.ResourceManager
*///?}

//? if < 1.21.5 && > 1.8.9 {
/*import net.minecraft.resources.ResourceLocation
*///?}

//? if < 1.21.11
//import net.minecraft.client.gui.components.DebugScreenOverlay

//? if >= 1.21.8 && < 1.21.11 {
/*import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.fog.FogRenderer
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
*///?}

//? if < 1.21.8
//import org.polyfrost.oneconfig.internal.ui.withGuiProjection

//? if = 1.8.9
//import net.minecraft.client.render.Window

//? if > 1.8.9 {
internal typealias GuiDraw = (GuiGraphicsExtractor) -> Unit
//?} else
//internal typealias GuiDraw = () -> Unit

object GlHost {
    private val LOG = LoggerFactory.getLogger(GlHost::class.java)

    private val client get() = Minecraft.getInstance()

    @JvmStatic
    val isVulkanModInstalled: Boolean by lazy {
        try {
            ModInfo.loadedMods.any { it.id.equals("vulkanmod", ignoreCase = true) }
        } catch (_: Throwable) {
            runCatching {
                Class.forName("net.vulkanmod.Initializer", false, javaClass.classLoader)
            }.isSuccess
        }
    }

    @JvmField
    //? if = 1.21.1 {
    /*val vanillaPipeline = isVulkanModInstalled
    *///?} else
    val vanillaPipeline = false

    @JvmField
    //? if >= 1.21.11 {
    val legacyGl = false
    //?} else
    //val legacyGl = vanillaPipeline || !isVulkanModInstalled

    fun unavailableReason(): String? {
        //? if >= 1.21.11 {
        return null
        //?} else {
        /*if (legacyGl) return null
        return "OneConfig's UI can't render while VulkanMod is installed, because VulkanMod replaces " +
            "Minecraft's OpenGL renderer. Remove VulkanMod to use the OneConfig UI."
        *///?}
    }

    private val gl = StoredGLState(330)

    private var hudTarget: RenderTarget? = null
    private var hudFrame: GlRenderer? = null

    //? >= 1.21.5 {
    private val HUD_TEXTURE_LOC = Identifier.fromNamespaceAndPath("oneconfig", "hud_skia")
    private var hudTextureWrapper: HudGpuTexture? = null

    private class HudGpuTexture : AbstractTexture() {
        fun setGpuTexture(t: GpuTexture?) {
            this.texture = t
        }

        //? >= 1.21.8 {
        fun setGpuTextureView(v: GpuTextureView?) {
            this.textureView = v
            //? >= 26.1 {
            this.sampler = RenderSystem.getSamplerCache()
                .getClampToEdge(FilterMode.LINEAR)
            //? }
        }
        //? }

        override fun close() {
            this.texture = null
            //? >= 1.21.8
            this.textureView = null
        }
    }
    //?} elif > 1.8.9 {
    /*private val HUD_TEXTURE_LOC = ResourceLocation.fromNamespaceAndPath("oneconfig", "hud_skia")
    private var hudTextureWrapper: HudGlTexture? = null
    //? >= 1.21.4 {
    private class HudGlTexture : AbstractTexture() {
        fun setGlTexId(id: Int) { this.id = id }
        override fun close() { this.id = -1 }
    }
    //? } else {
    /*private class HudGlTexture : AbstractTexture() {
        fun setGlTexId(id: Int) { this.id = id }
        override fun load(manager: ResourceManager) {}
        override fun close() { this.id = -1 }
    }
    *///? }
    *///? }

    //? if >= 1.21.4 && < 1.21.8 {
    /*private val premulRenderTypes = HashMap<Identifier, RenderType>()

    private fun premulGuiTextured(loc: Identifier): RenderType = premulRenderTypes.getOrPut(loc) {
        //? if >= 1.21.5 {
        /*RenderType.create(
            "oneconfig_gui_textured_premultiplied",
            786432,
            premulGuiPipeline,
            RenderType.CompositeState.builder()
                .setTextureState(RenderStateShard.TextureStateShard(loc, TriState.FALSE, false))
                .createCompositeState(false),
        )
        *///? } else {
        RenderType.create(
            "oneconfig_gui_textured_premultiplied",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            786432,
            RenderType.CompositeState.builder()
                .setTextureState(RenderStateShard.TextureStateShard(loc, TriState.FALSE, false))
                .setShaderState(RenderStateShard.POSITION_TEXTURE_COLOR_SHADER)
                .setTransparencyState(premulTransparency)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .createCompositeState(false),
        )
        //? }
    }

    //? if >= 1.21.5 {
    /*private val premulGuiPipeline by lazy {
        RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation("pipeline/oneconfig_gui_textured_premultiplied")
                .withBlend(
                    BlendFunction(
                        SourceFactor.ONE,
                        DestFactor.ONE_MINUS_SRC_ALPHA,
                        SourceFactor.ONE,
                        DestFactor.ONE_MINUS_SRC_ALPHA,
                    )
                )
                .build()
        )
    }
    *///? } else {
    private val premulTransparency = RenderStateShard.TransparencyStateShard(
        "oneconfig_premultiplied_transparency",
        {
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            )
        },
        {
            RenderSystem.disableBlend()
            RenderSystem.defaultBlendFunc()
        },
    )
    //? }
    *///? }

    //? if > 1.8.9 {
    fun blitHud(guiGraphics: GuiGraphicsExtractor) {
        //? if = 1.21.1 {
        /*if (vanillaPipeline) {
            val frame = hudFrame ?: return
            guiGraphics.flush()
            runCatching { frame.drawVanilla(Platform.screen().viewportWidth(), Platform.screen().viewportHeight(), topDown = false) }
                .onFailure { LOG.warn("GlHost.blitHud() error", it) }
            return
        }
        *///?}
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
        guiGraphics.pose().pushMatrix()
        try {
            guiGraphics.pose().scale(1f / guiScale, 1f / guiScale)
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, HUD_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        } finally {
            guiGraphics.pose().popMatrix()
        }
        //? } else {
        /*guiGraphics.pose().pushPose()
        try {
            guiGraphics.pose().scale(1f / guiScale, 1f / guiScale, 1f)
            guiGraphics.blit(::premulGuiTextured, HUD_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        } finally {
            guiGraphics.pose().popPose()
        }
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
        try {
        guiGraphics.pose().scale(1f / guiScale, 1f / guiScale, 1f)
        //? >= 1.21.4 {
        guiGraphics.blit(::premulGuiTextured, HUD_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        //?} else {
        /*RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
        )
        guiGraphics.blit(HUD_TEXTURE_LOC, 0, 0, 0f, 0f, w, h, w, h)
        RenderSystem.disableBlend()
        RenderSystem.defaultBlendFunc()
        *///?}
        } finally {
            guiGraphics.pose().popPose()
        }
        *///? }
    }
    //?} else {
    /*fun blitHud() {
        val rt = hudTarget ?: return
        val w = rt.width
        val h = rt.height

        val depthTest = GlStateManager.DEPTH.state.enabled
        val depthMask = GlStateManager.DEPTH.mask
        val alphaTest = GlStateManager.ALPHA_TEST.state.enabled
        val lighting = GlStateManager.LIGHTING.enabled
        val unit = GlStateManager.TEXTURES[GlStateManager.texture]
        val texture2d = unit.state.enabled
        val texture = unit.texture
        val blendState = GlStateManager.BLEND
        val blend = blendState.state.enabled
        val srcRgb = blendState.sfactorRGB
        val dstRgb = blendState.dfactorRGB
        val srcAlpha = blendState.sfactorAlpha
        val dstAlpha = blendState.dfactorAlpha
        val c = GlStateManager.COLOR
        val color = floatArrayOf(c.r, c.g, c.b, c.a)
        GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT)
        val fbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING)
        try {
            GlStateManager.enableBlend()
            GlStateManager.blendFuncSeparate(
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
            )
            GlStateManager.matrixMode(GL11.GL_PROJECTION)
            GlStateManager.pushMatrix()
            GlStateManager.matrixMode(GL11.GL_MODELVIEW)
            GlStateManager.pushMatrix()
            try {
                rt.draw(w, h, false)
            } finally {
                GlStateManager.matrixMode(GL11.GL_PROJECTION)
                GlStateManager.popMatrix()
                GlStateManager.matrixMode(GL11.GL_MODELVIEW)
                GlStateManager.popMatrix()
            }
        } finally {
            if (depthTest) GlStateManager.enableDepthTest() else GlStateManager.disableDepthTest()
            GlStateManager.depthMask(depthMask)
            if (alphaTest) GlStateManager.enableAlphaTest() else GlStateManager.disableAlphaTest()
            if (lighting) GlStateManager.enableLighting() else GlStateManager.disableLighting()
            if (texture2d) GlStateManager.enableTexture() else GlStateManager.disableTexture()
            if (blend) GlStateManager.enableBlend() else GlStateManager.disableBlend()
            GlStateManager.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
            GlStateManager.bindTexture(texture)
            GlStateManager.color4f(color[0], color[1], color[2], color[3])
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
            GL11.glPopAttrib()
        }
    }
    *///?}

    private var glScreen: ((RenderContext) -> Unit)? = null
    private var glScreenDirty = false
    private var glBlurAlpha = 0f
    private var glUnderlay: GuiDraw? = null
    private var screenFrame: GlRenderer? = null

    fun screen(dirty: Boolean, blurAlpha: Float, underlay: GuiDraw?, paint: (RenderContext) -> Unit) {
        glScreen = paint
        glBlurAlpha = blurAlpha
        glUnderlay = underlay
        if (dirty) glScreenDirty = true
    }

    private var drawnBeforeBlit = false

    /**
     * Whether vanilla GUI can be drawn between the blur and the OneConfig screen, which the fallback below
     * can't do since it draws after the blit
     */
    var drawsGuiOverScreen = true
        private set

    //? if < 1.21.11 {
    /*private var drawingGui = false

    //~ if = 1.8.9 'DebugScreenOverlay' -> 'DebugOverlay'
    private var deferredDebugScreen: DebugScreenOverlay? = null

    /** Holds back the F3 overlay while a OneConfig screen will be drawn over it, to draw it on top instead */
    //~ if = 1.8.9 'DebugScreenOverlay' -> 'DebugOverlay'
    fun deferDebugScreen(overlay: DebugScreenOverlay): Boolean {
        if (drawingGui || !drawsGuiOverScreen || Platform.screen().current<Any?>() !is ComposeScreen) return false
        deferredDebugScreen = overlay
        return true
    }
    *///?}

    // VulkanMod also draws here: before 0.5.3 it submits the frame right before Window.updateDisplay
    fun drawBeforeBlit() {
        if (!legacyGl) return
        drawnBeforeBlit = true
        //? if < 1.21.11 {
        /*draw(RenderTargetFbo.getFboId(client.mainRenderTarget))
        *///?}
    }

    fun drawAtPresent() {
        if (drawnBeforeBlit) {
            drawnBeforeBlit = false
            return
        }
        draw(0)
    }

    private fun draw(fbo: Int) {
        if (!legacyGl) return
        drawsGuiOverScreen = drawnBeforeBlit
        val paint = glScreen
        if (paint == null) {
            screenFrame = null
            GlBlur.release()
        } else {
            glScreen = null
            if (glBlurAlpha > 0f && !vanillaPipeline) drawBlur(fbo, glBlurAlpha)
            //? if < 1.21.11
            //if (drawsGuiOverScreen) glUnderlay?.let { drawGui(it) }
            val reuse = screenFrame?.takeUnless { glScreenDirty }
            glScreenDirty = false
            screenFrame = drawGl(fbo, reuse, true, paint)
        }
        if (NotificationsManager.count > 0) drawGl(fbo, null) {
            NotificationsRenderer.prepare(Platform.screen().guiWidth().toFloat(), Platform.screen().guiHeight().toFloat())
            NotificationsRenderer.draw(it)
        }
        //? if < 1.21.11 {
        /*deferredDebugScreen?.let { overlay ->
            deferredDebugScreen = null
            //~ if = 1.8.9 'overlay.render(it)' -> 'overlay.render(Window(client))'
            drawGui { overlay.render(it) }
        }
        *///?}
    }

    //? if < 1.21.11 {
    /*private fun drawGui(draw: GuiDraw) {
        drawingGui = true
        try {
            //? if >= 1.21.8 {
            val gameRenderer = client.gameRenderer as GameRendererAccessor
            val renderer = gameRenderer.`oneconfig$getGuiRenderer`()
            val state = GuiRenderState()
            draw(GuiGraphicsExtractor(client, state))
            val accessor = renderer as GuiRendererAccessor
            val previous = accessor.`oneconfig$getRenderState`()
            accessor.`oneconfig$setRenderState`(state)
            try {
                renderer.render(gameRenderer.`oneconfig$getFogRenderer`().getBuffer(FogRenderer.FogMode.NONE))
            } finally {
                accessor.`oneconfig$setRenderState`(previous)
            }
            //?} else {
            /*withGuiProjection(Platform.screen().guiWidth(), Platform.screen().guiHeight()) {
                // depth left by the GUI pass would hide this behind HUD elements
                //? if = 1.21.5 {
                client.mainRenderTarget.depthTexture?.let { RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(it, 1.0) }
                //?} else {
                /*client.mainRenderTarget.bindWrite(true)
                //? if >= 1.21.4 {
                GlStateManager._clear(GL11.GL_DEPTH_BUFFER_BIT)
                //?} elif > 1.8.9 {
                /*GlStateManager._clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX)
                *///?} else
                //GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT)
                *///?}
                //? if > 1.8.9 {
                val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
                draw(graphics)
                graphics.flush()
                //?} else
                //draw()
                //? if < 1.21.5
                //client.mainRenderTarget.unbindWrite()
            }
            *///?}
        } catch (e: Throwable) {
            LOG.warn("GlHost.drawGui() error", e)
        } finally {
            drawingGui = false
        }
    }
    *///?}

    private fun drawBlur(fbo: Int, alpha: Float) {
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return
        gl.capture()
        try {
            GlBlur.draw(fbo, w, h, alpha, client.isPaused)
        } catch (e: Throwable) {
            LOG.warn("GlHost.drawBlur() error", e)
        } finally {
            gl.restore()
        }
    }

    private fun drawGl(fbo: Int, cached: GlRenderer?, kept: Boolean = false, draw: (RenderContext) -> Unit): GlRenderer? {
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0 || Platform.screen().guiWidth() <= 0) return null
        //? if = 1.21.1 {
        /*if (vanillaPipeline) return try {
            (cached ?: GlRenderer().also { draw(RenderContext(it)) }).also {
                client.mainRenderTarget.bindWrite(true)
                it.drawVanilla(w, h, topDown = false)
            }
        } catch (e: Throwable) {
            LOG.warn("GlHost.drawGl() error", e)
            null
        }
        *///?}
        gl.capture()
        try {
            val renderer = cached ?: GlRenderer().also { draw(RenderContext(it)) }
            renderer.draw(fbo, w, h, topDown = false, clear = false, kept = kept)
            return renderer
        } catch (e: Throwable) {
            LOG.warn("GlHost.drawGl() error", e)
            return null
        } finally {
            gl.restore()
        }
    }

    private var oversizeReported = false

    private fun maxTextureSize(): Int =
        //? if >= 26.2 {
        RenderSystem.getDevice().deviceInfo.limits().maxTextureSize()
        //? } else if >= 1.21.5 {
        /*RenderSystem.getDevice().maxTextureSize
        *///? } else if > 1.8.9 {
        /*RenderSystem.maxSupportedTextureSize()
        *///? } else
        //Minecraft.getMaxTextureSize()

    private var maxTextureSizeCache = 0

    private fun cachedMaxTextureSize(): Int {
        if (maxTextureSizeCache == 0) {
            maxTextureSizeCache = runCatching { maxTextureSize() }.getOrNull()
                ?.takeIf { it > 0 }
                ?: Int.MAX_VALUE
        }
        return maxTextureSizeCache
    }

    private fun viewportFitsTexture(w: Int, h: Int): Boolean {
        val max = cachedMaxTextureSize()
        if (w <= max && h <= max) {
            oversizeReported = false
            return true
        }
        if (!oversizeReported) {
            oversizeReported = true
            LOG.warn("GlHost: viewport {}x{} is past the max texture size ({}); skipping the HUD target", w, h, max)
            destroyHudTarget()
        }
        return false
    }

    private var hudStable: GlRenderer? = null
    private var hudVolatile: GlRenderer? = null
    private val stableLayer = GlRenderer.Layer(coalesce = true)
    private val volatileLayer = GlRenderer.Layer()

    private fun reuse(renderer: GlRenderer?): GlRenderer {
        if (renderer == null || !renderer.matches()) return GlRenderer()
        renderer.reset()
        return renderer
    }

    fun drawHudGl(draw: (RenderContext, RenderContext?) -> Unit) {
        //? if = 1.21.1 {
        /*if (vanillaPipeline) {
            hudFrame = runCatching { GlRenderer().also { draw(RenderContext(it), null) } }
                .onFailure { LOG.warn("GlHost.drawHudGl() error", it) }
                .getOrNull()
            return
        }
        *///?}
        val rt = resolveHudTarget() ?: return
        val fbo = RenderTargetFbo.getFboId(rt)
        if (fbo <= 0) return
        gl.capture()
        try {
            val stable = reuse(hudStable).also { hudStable = it }
            val volatile = reuse(hudVolatile).also { hudVolatile = it }
            draw(RenderContext(stable), RenderContext(volatile))
            stableLayer.take(stable)
            volatileLayer.take(volatile)
            //~ if = 1.8.9 'topDown = true' -> 'topDown = false'
            GlRenderer.Layer.draw(fbo, rt.width, rt.height, topDown = true, clear = true, stableLayer, volatileLayer)
        } catch (e: Throwable) {
            LOG.warn("GlHost.drawHudGl() error", e)
        } finally {
            gl.restore()
        }
    }

    private fun resolveHudTarget(): RenderTarget? {
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return null
        if (!viewportFitsTexture(w, h)) return null

        var rt = hudTarget
        val needNewTarget = rt == null || rt.width != w || rt.height != h
        if (needNewTarget) {
            if (System.currentTimeMillis() - allocFailedAt < ALLOC_RETRY_COOLDOWN_MS) return null
            destroyHudTarget()
            //? if < 1.21.5
            //val bound = RenderTargetFbo.saveBoundTarget()
            rt = try {
                //? if >= 26.3 {
                TextureTarget(
                    null, w, h,
                    GpuFormat.RGBA8_UNORM,
                    GpuFormat.D32_FLOAT,
                )
                //? } else if >= 26.2 {
                /*TextureTarget(null, w, h, true, GpuFormat.RGBA8_UNORM)
                *///? } else if >= 1.21.5 {
                /*TextureTarget(null, w, h, true)
                *///? } else if >= 1.21.4 {
                /*TextureTarget(w, h, true)
                *///? } elif > 1.8.9 {
                /*TextureTarget(w, h, true, Minecraft.ON_OSX)
                *///? } else
                //RenderTarget(w, h, false)
            } catch (e: Throwable) {
                onAllocFailure(HUD_TARGET, w, h, e)
                return null
            }
            hudTarget = rt
            //? if < 1.21.5
            //RenderTargetFbo.restoreBoundTarget(bound)

            //? >= 1.21.5 {
            val fboId = RenderTargetFbo.getFboId(rt)
            if (fboId <= 0) {
                LOG.warn("GlHost: hud TextureTarget FBO not ready (id={}), retry next frame", fboId)
                hudTarget = null
                rt.destroyBuffers()
                return null
            }
            //? }
        }
        return rt
    }

    private fun destroyHudTarget() {
        hudTarget?.let { target ->
            //? if < 1.21.5
            //val bound = RenderTargetFbo.saveBoundTarget()
            target.destroyBuffers()
            //? if < 1.21.5
            //RenderTargetFbo.restoreBoundTarget(bound)
        }
        hudTarget = null
    }

    private var allocFailedAt = 0L
    private var allocReported = false

    private const val ALLOC_RETRY_COOLDOWN_MS = 2000L
    private const val HUD_TARGET = "hud"


    private fun onAllocFailure(what: String, w: Int, h: Int, error: Throwable) {
        allocFailedAt = System.currentTimeMillis()
        destroyHudTarget()
        LOG.error("GlHost: failed to allocate the {}x{} {} target; skipping offscreen frames", w, h, what, error)
        if (!allocReported) {
            allocReported = true
            runCatching {
                Platform.screen().showMessage(
                    "OneConfig couldn't allocate GPU memory for its UI (${w}x$h). " +
                        "Lower your resolution or render scale, or close other GPU-heavy programs."
                )
            }
        }
    }

    fun recreateSurface(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            destroyHudTarget()
            hudFrame = null
        }
    }
}
