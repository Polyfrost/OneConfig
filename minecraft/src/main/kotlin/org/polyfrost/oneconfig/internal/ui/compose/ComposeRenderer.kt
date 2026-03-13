package org.polyfrost.oneconfig.internal.ui.compose

import com.mojang.authlib.minecraft.client.MinecraftClient
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.GLBackendState
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.opengl.GL11
import org.polyfrost.oneconfig.internal.ui.compose.opengl.StoredGLState

class ComposeRenderer {
    lateinit var surface: Surface
    lateinit var buffer: RenderTarget

    val canvas get() = surface.canvas

    private var brt: BackendRenderTarget? = null

    private val gl = StoredGLState(330)
    private val states = arrayOf(
        GLBackendState.BLEND,
        GLBackendState.VERTEX,
        GLBackendState.PIXEL_STORE,
        GLBackendState.TEXTURE_BINDING,
        GLBackendState.MISC
    )
    fun initialize(width: Int, height: Int) {
        buffer = TextureTarget(width, height, false, false)

        brt = BackendRenderTarget.makeGL(
            width,
            height,
            0,
            8,
            buffer.frameBufferId,
            FramebufferFormat.GR_GL_RGBA8
        )

        surface = Surface.makeFromBackendRenderTarget(
            SkiaCtx.directContext,
            brt!!,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
            null
        )!!
    }

    fun render(block: Canvas.() -> Unit) {
        if (!::buffer.isInitialized)
            return

        buffer.clear(false)
        buffer.bindWrite(true)

        gl.capture()
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glClearColor(0f, 0f, 0f, 0f)

        surface.canvas.clear(Color.TRANSPARENT)

        SkiaCtx.directContext.resetGL(*states)
        GL11.glDisable(GL11.GL_ALPHA_TEST)
        canvas.block()

        surface.flushAndSubmit()
        gl.restore()

        buffer.unbindWrite()
        Minecraft.getInstance().mainRenderTarget.bindWrite(true)

        RenderSystem.disableBlend()
    }

    fun render() {
        val mcBuffer = Minecraft.getInstance().mainRenderTarget
        mcBuffer.bindWrite(true)

        RenderSystem.enableBlend()
        buffer.blitToScreen(surface.width, surface.height, false)
    }

}