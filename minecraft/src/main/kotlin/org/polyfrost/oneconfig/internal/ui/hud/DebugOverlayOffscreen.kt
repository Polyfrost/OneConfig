package org.polyfrost.oneconfig.internal.ui.hud

import com.mojang.blaze3d.pipeline.TextureTarget
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
//? } else if >= 1.21.8 {
/*import net.minecraft.client.gui.render.state.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
*///? }
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceOrigin
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.slf4j.LoggerFactory

/**
 * Minecraft draws the F3 debug overlay into the GUI while the OneConfig screen is open, which puts it
 * *underneath* the Compose UI and inside the blur backdrop (the blur samples the main render target).
 *
 * To keep F3 legible and on top, the vanilla overlay is cancelled (see Mixin_DebugOverlayAboveUi),
 * recorded into an offscreen target here, and blitted through Skia after the Compose frame.
 */
object DebugOverlayOffscreen {
    private val LOG = LoggerFactory.getLogger("OneConfig/DebugOverlayOffscreen")
    private val client get() = Minecraft.getInstance()

    private var target: TextureTarget? = null
    private var brt: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var lastW = -1
    private var lastH = -1

    @Volatile private var hasContent = false
    @Volatile private var failed = false

    @Volatile private var capturing = false

    private val blitPaint = Paint()

    init {
        SkiaCtx.setPostComposeRenderer { drawInto(SkiaCtx.canvas) }
    }

    private fun active(): Boolean =
        !failed &&
            SkiaCtx.isReady &&
            Platform.screen().current<Screen>() is ComposeScreen &&
            client.debugOverlay.showDebugScreen()

    /** Called from the debug overlay mixin: hide the vanilla (under-UI, blurred) copy. */
    fun shouldSuppressVanilla(): Boolean = !capturing && active()

    fun render() {
        hasContent = false
        if (!active()) return
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return

        try {
            if (!resolveTarget(w, h)) return
            val rt = target ?: return
            //? if >= 1.21.8 {
            renderRecorded(rt)
            //? } else if >= 1.21.5 {
            /*renderImmediate(rt)
            *///? } else {
            /*renderImmediateLegacy(rt)
            *///? }
            hasContent = true
        } catch (t: Throwable) {
            LOG.warn("Debug overlay offscreen render failed; disabling", t)
            failed = true
            capturing = false
            //? if >= 1.21.5
            GuiTargetRedirect.target = null
        }
    }

    //? if >= 1.21.8 {
    private fun renderRecorded(rt: TextureTarget) {
        val guiRenderer = (client.gameRenderer as GameRendererAccessor).`oneconfig$getGuiRenderer`()
        val accessor = guiRenderer as GuiRendererAccessor

        val state = GuiRenderState()
        //? if >= 1.21.11 {
        val ext = GuiGraphicsExtractor(client, state, Platform.screen().guiWidth(), Platform.screen().guiHeight())
        //? } else
        /*val ext = GuiGraphicsExtractor(client, state)*/
        capturing = true
        try {
            //~ if >= 26.1 'render' -> 'extractRenderState'
            client.debugOverlay.extractRenderState(ext)
        } finally {
            capturing = false
        }

        clearTarget(rt)

        val prevState = accessor.`oneconfig$getRenderState`()
        GuiTargetRedirect.target = rt
        try {
            accessor.`oneconfig$setRenderState`(state)
            //? if >= 26.2 {
            /*guiRenderer.render()
            *///? } else {
            val fog = (client.gameRenderer as GameRendererAccessor).`oneconfig$getFogRenderer`()
                .getBuffer(net.minecraft.client.renderer.fog.FogRenderer.FogMode.NONE)
            guiRenderer.render(fog)
            //? }
        } finally {
            GuiTargetRedirect.target = null
            accessor.`oneconfig$setRenderState`(prevState)
        }
    }
    //? } else if >= 1.21.5 {

    /*
    private fun renderImmediate(rt: TextureTarget) {
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        clearTarget(rt)
        capturing = true
        GuiTargetRedirect.target = rt
        try {
            client.debugOverlay.render(graphics)
            graphics.flush()
        } finally {
            GuiTargetRedirect.target = null
            capturing = false
        }
    }
    *///? } else {

    /*
    private fun renderImmediateLegacy(rt: TextureTarget) {
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        clearTarget(rt)
        capturing = true
        rt.bindWrite(true)
        try {
            client.debugOverlay.render(graphics)
            graphics.flush()
        } finally {
            client.mainRenderTarget.bindWrite(true)
            capturing = false
        }
    }
    *///? }

    private fun clearTarget(rt: TextureTarget) {
        //? if >= 26.2 {
        /*val colorTex = rt.colorTexture ?: return
        com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder()
            .clearColorTexture(colorTex, org.joml.Vector4f(0f, 0f, 0f, 0f))
        *///? } else if >= 1.21.5 {
        val colorTex = rt.colorTexture ?: return
        com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder()
            .clearColorTexture(colorTex, 0)
        //? } else if >= 1.21.4 {
        /*rt.clear()
        *///? } else {
        /*rt.clear(Minecraft.ON_OSX)
        *///? }
    }

    private fun resolveTarget(w: Int, h: Int): Boolean {
        if (target != null && lastW == w && lastH == h && surface != null) return true
        destroy()
        try {
            //? if >= 26.2 {
            /*val rt = TextureTarget(null, w, h, true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM)
            *///? } else if >= 1.21.5 {
            val rt = TextureTarget(null, w, h, true)
            //? } else if >= 1.21.4 {
            /*val rt = TextureTarget(w, h, true)
            *///? } else {
            /*val rt = TextureTarget(w, h, true, Minecraft.ON_OSX)
            *///? }
            target = rt
            //? if < 1.21.5
            /*rt.setClearColor(0f, 0f, 0f, 0f)*/
            val svc = SkiaCtx.vulkanService ?: return false
            if (!SkiaCtx.isVulkanMode) {
                //? if >= 1.21.5 {
                val fboId = org.polyfrost.oneconfig.internal.ui.RenderTargetFbo.getFboId(rt)
                //? } else
                /*val fboId = rt.frameBufferId*/
                if (fboId <= 0) {
                    target = null
                    rt.destroyBuffers()
                    return false
                }
            }
            val (b, colorFmt) = svc.makeOffscreenBRT(rt, w, h)
            brt = b
            val origin = if (SkiaCtx.isDeferredComposeBackend) SurfaceOrigin.TOP_LEFT else SurfaceOrigin.BOTTOM_LEFT
            surface = Surface.makeFromBackendRenderTarget(
                SkiaCtx.directContext, b, origin, colorFmt, ColorSpace.sRGB, null,
            )
            if (surface == null) {
                b.close(); brt = null
                return false
            }
            lastW = w; lastH = h
            return true
        } catch (t: Throwable) {
            LOG.warn("Failed to create debug overlay offscreen target", t)
            destroy()
            return false
        }
    }

    private fun drawInto(canvas: org.jetbrains.skia.Canvas) {
        if (!hasContent) return
        val s = surface ?: return
        try {
            s.notifyContentWillChange(org.jetbrains.skia.ContentChangeMode.RETAIN)
            s.draw(canvas, 0, 0, blitPaint)
        } catch (t: Throwable) {
            LOG.debug("debug overlay blit failed", t)
        }
    }

    private fun destroy() {
        surface?.close(); surface = null
        brt?.close(); brt = null
        target?.destroyBuffers(); target = null
        lastW = -1; lastH = -1
    }

    fun invalidate() {
        destroy()
        hasContent = false
    }
}
