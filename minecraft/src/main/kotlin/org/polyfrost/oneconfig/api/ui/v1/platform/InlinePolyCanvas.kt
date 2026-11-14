@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import net.minecraft.client.Minecraft
import org.polyfrost.compose.backend.PolyCanvas
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.FramebufferRenderEvent
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor
//?}

//? if >= 1.21.11 {
import org.polyfrost.oneconfig.internal.ui.compose.BlazeRenderer
import org.polyfrost.oneconfig.internal.ui.compose.GlyphAtlas
//?} else {
/*import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo
import org.polyfrost.oneconfig.internal.ui.compose.GlHost
import org.polyfrost.oneconfig.internal.ui.compose.GlRenderer
import org.polyfrost.oneconfig.internal.ui.compose.opengl.StoredGLState
*///?}

//? if >= 1.21.8 && < 1.21.11 {
/*import com.mojang.blaze3d.pipeline.RenderTarget
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.Identifier
import org.polyfrost.oneconfig.internal.ui.OffscreenTarget
*///?}

//? if = 1.8.9 {
/*import org.polyfrost.oneconfig.api.platform.v1.Platform
*///?}

@ExperimentalOneConfigUiApi
object InlinePolyCanvas {
    private val surfaces = ArrayList<InlineCanvasSurface>()
    private var used = 0
    private var frame = 0L

    init {
        EventManager.register(FramebufferRenderEvent.End::class.java) { _ -> endFrame() }
    }

    //? if > 1.8.9 {
    @JvmStatic
    fun draw(graphics: GuiGraphicsExtractor?, block: (PolyCanvas) -> Unit): Boolean = next().draw(graphics, block)
    //?} else {
    /*
    @JvmStatic
    fun draw(block: (PolyCanvas) -> Unit): Boolean = next().draw(block)
    *///?}

    private fun next(): InlineCanvasSurface {
        val surface = surfaces.getOrNull(used) ?: InlineCanvasSurface("pool_${surfaces.size}").also(surfaces::add)
        used++
        surface.lastUsedFrame = frame
        return surface
    }

    private fun endFrame() {
        frame++
        used = 0
        for (surface in surfaces) {
            if (frame - surface.lastUsedFrame > IDLE_FRAMES) surface.close()
        }
    }

    private const val IDLE_FRAMES = 120
}

internal class InlineCanvasSurface(val name: String) {
    var lastUsedFrame = 0L

    //? if >= 1.21.11 {
    fun draw(ctx: GuiGraphicsExtractor?, block: (PolyCanvas) -> Unit): Boolean {
        if (ctx == null) return false
        val renderer = BlazeRenderer(Minecraft.getInstance().window.guiScale.toFloat())
        block(PolyCanvas(renderer))
        for (op in renderer.finish()) op(ctx)
        GlyphAtlas.uploadDirty()
        return true
    }

    fun close() {}
    //?} elif >= 1.21.8 {
    /*private val textureId = Identifier.fromNamespaceAndPath("oneconfig", "inline_canvas/" + pathSafe(name))
    private val offscreen = OffscreenTarget()
    private var texture: TargetTexture? = null

    private class TargetTexture : AbstractTexture() {
        fun set(target: RenderTarget) {
            texture = target.colorTexture
            textureView = target.colorTextureView
        }

        override fun close() {
            texture = null
            textureView = null
        }
    }

    fun draw(ctx: GuiGraphicsExtractor?, block: (PolyCanvas) -> Unit): Boolean {
        if (ctx == null || !GlHost.legacyGl) return false
        val mc = Minecraft.getInstance()
        val w = mc.window.width
        val h = mc.window.height
        if (w <= 0 || h <= 0 || !offscreen.resolve(w, h)) return false
        val target = offscreen.target ?: return false
        val fbo = RenderTargetFbo.getFboId(target)
        if (!drawGl(fbo, w, h, topDown = true, clear = true, block)) return false
        val tex = texture ?: TargetTexture().also {
            texture = it
            mc.textureManager.register(textureId, it)
        }
        tex.set(target)
        val guiScale = mc.window.guiScale.toFloat()
        ctx.pose().pushMatrix()
        try {
            ctx.pose().scale(1f / guiScale, 1f / guiScale)
            ctx.blit(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA, textureId, 0, 0, 0f, 0f, w, h, w, h)
        } finally {
            ctx.pose().popMatrix()
        }
        return true
    }

    fun close() {
        offscreen.destroy()
        if (texture != null) {
            texture = null
            Minecraft.getInstance().textureManager.release(textureId)
        }
    }

    private fun pathSafe(raw: String): String =
        raw.lowercase().map { if (it in 'a'..'z' || it in '0'..'9' || it in "_-./") it else '_' }.joinToString("")
    *///?} elif > 1.8.9 {
    /*fun draw(ctx: GuiGraphicsExtractor?, block: (PolyCanvas) -> Unit): Boolean {
        if (!GlHost.legacyGl) return false
        ctx?.flush()
        val mc = Minecraft.getInstance()
        //? if = 1.21.1 {
        /^if (GlHost.vanillaPipeline) {
            val renderer = GlRenderer()
            block(PolyCanvas(renderer))
            mc.mainRenderTarget.bindWrite(true)
            renderer.drawVanilla(mc.window.width, mc.window.height, topDown = false)
            return true
        }
        ^///?}
        return drawGl(RenderTargetFbo.getFboId(mc.mainRenderTarget), mc.window.width, mc.window.height, topDown = false, clear = false, block)
    }

    fun close() {}
    *///?} else {
    /*fun draw(block: (PolyCanvas) -> Unit): Boolean {
        if (!GlHost.legacyGl) return false
        val fbo = RenderTargetFbo.getFboId(Minecraft.getInstance().mainRenderTarget)
        return drawGl(fbo, Platform.screen().viewportWidth(), Platform.screen().viewportHeight(), topDown = false, clear = false, block)
    }

    fun close() {}
    *///?}

    //? if < 1.21.11 {
    /*private val glState = StoredGLState(330)

    private fun drawGl(fbo: Int, w: Int, h: Int, topDown: Boolean, clear: Boolean, block: (PolyCanvas) -> Unit): Boolean {
        if (fbo <= 0 || w <= 0 || h <= 0) return false
        glState.capture()
        try {
            val renderer = GlRenderer()
            block(PolyCanvas(renderer))
            renderer.draw(fbo, w, h, topDown, clear)
        } finally {
            glState.restore()
        }
        return true
    }
    *///?}
}
