package org.polyfrost.oneconfig.internal.ui.hud

import com.mojang.blaze3d.pipeline.RenderTarget
//? if > 1.8.9 {
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
//? } else if >= 1.21.8 {
/*import net.minecraft.client.gui.render.state.GuiRenderState
import org.polyfrost.oneconfig.internal.mixin.render.GameRendererAccessor
import org.polyfrost.oneconfig.internal.mixin.render.GuiRendererAccessor
*///? }
import org.jetbrains.skia.Paint
//?} else {
/*import net.minecraft.client.Minecraft
import net.minecraft.client.render.Window
import net.minecraft.client.gui.components.DebugScreenOverlay
*///?}
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.SkiaOffscreenTarget
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.slf4j.LoggerFactory

//? if > 1.8.9 {
/**
 * Minecraft draws the F3 debug overlay into the GUI while the OneConfig screen is open which puts it
 * *underneath* the Compose UI and inside the blur backdrop
 *
 * The vanilla overlay is cancelled in Mixin_DebugOverlayAboveUi then recorded into an offscreen target
 * here and blitted through Skia after the Compose frame
 */
object DebugOverlayOffscreen {
    private val LOG = LoggerFactory.getLogger("OneConfig/DebugOverlayOffscreen")
    private val client get() = Minecraft.getInstance()

    private val offscreen = SkiaOffscreenTarget()

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
            Platform.screen().current<Any?>() is ComposeScreen &&
            client.debugOverlay.showDebugScreen()

    /** Called from the debug overlay mixin to hide the vanilla under-UI blurred copy */
    fun shouldSuppressVanilla(): Boolean = !capturing && active()

    fun render() {
        hasContent = false
        if (!active()) return
        val w = Platform.screen().viewportWidth()
        val h = Platform.screen().viewportHeight()
        if (w <= 0 || h <= 0) return

        try {
            if (!offscreen.resolveTarget(w, h)) return
            val rt = offscreen.target ?: return
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
            GuiTargetRedirect.target = null
        }
    }

    //? if >= 1.21.8 {
    private fun renderRecorded(rt: RenderTarget) {
        val guiRenderer = (client.gameRenderer as GameRendererAccessor).`oneconfig$getGuiRenderer`()
        val accessor = guiRenderer as GuiRendererAccessor

        val state = GuiRenderState()
        //? if >= 1.21.11 {
        val ext = GuiGraphicsExtractor(client, state, Platform.screen().guiWidth(), Platform.screen().guiHeight())
        //? } else
        //val ext = GuiGraphicsExtractor(client, state)
        capturing = true
        try {
            //~ if >= 26.1 'render' -> 'extractRenderState'
            client.debugOverlay.extractRenderState(ext)
        } finally {
            capturing = false
        }

        offscreen.clearTarget()

        val prevState = accessor.`oneconfig$getRenderState`()
        GuiTargetRedirect.target = rt
        try {
            accessor.`oneconfig$setRenderState`(state)
            //? if >= 26.2 {
            guiRenderer.render()
            //? } else {
            /*val fog = (client.gameRenderer as GameRendererAccessor).`oneconfig$getFogRenderer`()
                .getBuffer(net.minecraft.client.renderer.fog.FogRenderer.FogMode.NONE)
            guiRenderer.render(fog)
            *///? }
        } finally {
            GuiTargetRedirect.target = null
            accessor.`oneconfig$setRenderState`(prevState)
        }
    }
    //? } else if >= 1.21.5 {
    /*private fun renderImmediate(rt: RenderTarget) {
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        offscreen.clearTarget()
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
    /*private fun renderImmediateLegacy(rt: RenderTarget) {
        val graphics = GuiGraphicsExtractor(client, client.renderBuffers().bufferSource())
        offscreen.clearTarget()
        capturing = true
        GuiTargetRedirect.target = rt
        try {
            rt.bindWrite(true)
            client.debugOverlay.render(graphics)
            graphics.flush()
        } finally {
            GuiTargetRedirect.target = null
            client.mainRenderTarget.bindWrite(true)
            capturing = false
        }
    }
    *///? }

    private fun drawInto(canvas: org.jetbrains.skia.Canvas) {
        if (!hasContent) return
        val s = offscreen.surface ?: return
        try {
            s.notifyContentWillChange(org.jetbrains.skia.ContentChangeMode.RETAIN)
            s.draw(canvas, 0, 0, blitPaint)
        } catch (t: Throwable) {
            LOG.debug("debug overlay blit failed", t)
        }
    }
}
//?} else {
/*/**
 * Minecraft draws the F3 debug overlay into the GUI while the OneConfig screen is open which puts it
 * *underneath* the Compose UI and inside the blur backdrop
 *
 * The vanilla overlay is cancelled in Mixin_DebugOverlayAboveUi, then its overlay and window are stored
 * here and replayed into the main target after Skia draws in Mixin_SkiaFrame
 */
object DebugOverlayOffscreen {
    private val LOG = LoggerFactory.getLogger("OneConfig/DebugOverlayOffscreen")
    private val client get() = Minecraft.getInstance()

    private var deferredOverlay: DebugOverlay? = null
    private var deferredWindow: Window? = null

    @Volatile private var capturing = false
    @Volatile private var failed = false

    private fun active(): Boolean =
        !failed && SkiaCtx.isReady && Platform.screen().current<Any?>() is ComposeScreen

    /** Called from the debug overlay mixin to hide the vanilla under-UI blurred copy */
    fun shouldSuppressVanilla(overlay: DebugOverlay, window: Window): Boolean {
        if (capturing || !active()) return false
        deferredOverlay = overlay
        deferredWindow = window
        return true
    }

    fun render() {
        val overlay = deferredOverlay ?: return
        val window = deferredWindow ?: return
        val rt = client.mainRenderTarget
        deferredOverlay = null
        deferredWindow = null
        capturing = true
        var bound = false
        try {
            rt.bindWrite(true)
            bound = true
            overlay.render(window)
        } catch (t: Throwable) {
            LOG.warn("Debug overlay replay failed; disabling", t)
            failed = true
        } finally {
            if (bound) rt.unbindWrite()
            capturing = false
        }
    }
}
*///?}
