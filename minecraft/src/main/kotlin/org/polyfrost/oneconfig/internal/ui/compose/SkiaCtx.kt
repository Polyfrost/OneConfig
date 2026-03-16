package org.polyfrost.oneconfig.internal.ui.compose

import net.minecraft.client.Minecraft
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.opengl.GL11
import org.polyfrost.oneconfig.internal.ui.compose.opengl.StoredGLState
import java.util.concurrent.CopyOnWriteArrayList

object SkiaCtx {
    lateinit var surface: Surface
    lateinit var directContext: DirectContext
    val canvas get() = surface.canvas

    private var brt: BackendRenderTarget? = null

    private val client get() = Minecraft.getInstance()

    val isReady get() = this::surface.isInitialized && this::directContext.isInitialized

    private val queuedDraws = CopyOnWriteArrayList<() -> Unit>()
    private val gl = StoredGLState(330)

    fun draw() {
        ensureSurface()
        if (!this::surface.isInitialized) {
            queuedDraws.clear()
            return
        }

        val draws = queuedDraws.toList()
        queuedDraws.clear()
        draws.forEach { it() }
    }

    fun queueDraw(block: () -> Unit) {
        queuedDraws.add {
            beginFrame()
            block()
            endFrame()
        }
    }

    fun queueDraw(block: Runnable) {
        queuedDraws.add {
            beginFrame()
            block.run()
            endFrame()
        }
    }

    fun init() {
        System.setProperty("skiko.macos.opengl.enabled", "true")
        if (!this::directContext.isInitialized) {
            directContext = DirectContext.makeGL()
        }
    }

    fun recreateSurface(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            return
        }

        if (!this::directContext.isInitialized) return
        init()
        if (this::surface.isInitialized && surface.width == width && surface.height == height) {
            return
        }
        createSurface(width, height)
    }

    private fun ensureSurface() {
        val width = client.window.width
        val height = client.window.height
        if (width <= 0 || height <= 0) {
            return
        }

        if (!this::surface.isInitialized || surface.width != width || surface.height != height) {
            recreateSurface(width, height)
        }
    }

    private fun createSurface(width: Int, height: Int) {
        brt?.close()

        if(this::surface.isInitialized) surface.close()

        brt = BackendRenderTarget.makeGL(
            width,
            height,
            0,
            8,
            0,
            FramebufferFormat.GR_GL_RGBA8
        )

        surface = Surface.makeFromBackendRenderTarget(
            directContext,
            brt!!, SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB,
            null
        )!!
    }

    fun beginFrame() {
        ensureSurface()
        gl.capture()
        directContext.resetGLAll()
        GL11.glViewport(0, 0, surface.width, surface.height)
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    fun endFrame() {
        directContext.flush()
        gl.restore()
    }
}
