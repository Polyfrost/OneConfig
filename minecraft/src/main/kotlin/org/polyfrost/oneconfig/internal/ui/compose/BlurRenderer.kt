package org.polyfrost.oneconfig.internal.ui.compose

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import org.jetbrains.skia.*


object BlurRenderer {
    private val client get() = Minecraft.getInstance()
    private val paints = HashMap<Float, Paint>()
    private var cachedSurface: Surface? = null
    private var cachedBackendRenderTarget: BackendRenderTarget? = null
    private var cachedFramebufferId = -1
    private var cachedWidth = -1
    private var cachedHeight = -1

    fun drawBlur() {
        SkiaCtx.queueDraw {
            val target = client.mainRenderTarget
            drawRegion(SkiaCtx.canvas, 0f, 0f, target.width.toFloat(), target.height.toFloat(), 8f)
        }
    }

    fun drawRegion(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, radius: Float) {
        if (width <= 0f || height <= 0f) {
            return
        }

        val target = client.mainRenderTarget
        val sourceSurface = resolveSurface(target, target.width, target.height) ?: return
        sourceSurface.notifyContentWillChange(ContentChangeMode.RETAIN)

        canvas.save()
        canvas.clipRect(Rect.makeXYWH(0f, 0f, width, height))
        canvas.translate(-x, -y)
        sourceSurface.draw(canvas, 0, 0, paintFor(radius))
        canvas.restore()
    }

    private fun resolveSurface(target: RenderTarget, width: Int, height: Int): Surface? {
        //#if MC >= 1.21.5
        //$$ val frameBufferId = getFboId(target) // praying ts works 🙏🙏
        //#else
        val frameBufferId = target.frameBufferId
        //#endif

        if (width <= 0 || height <= 0 || frameBufferId <= 0) {
            return null
        }
        if (cachedSurface != null && cachedFramebufferId == frameBufferId && cachedWidth == width && cachedHeight == height) {
            return cachedSurface
        }

        cachedSurface?.close()
        cachedBackendRenderTarget?.close()

        val backendRenderTarget = BackendRenderTarget.makeGL(
            width,
            height,
            0,
            8,
            frameBufferId,
            FramebufferFormat.GR_GL_RGBA8
        )

        val surface = Surface.makeFromBackendRenderTarget(
            SkiaCtx.directContext,
            backendRenderTarget,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
            null
        )

        if (surface == null) {
            backendRenderTarget.close()
            cachedBackendRenderTarget = null
            cachedSurface = null
            cachedFramebufferId = -1
            cachedWidth = -1
            cachedHeight = -1
            return null
        }

        cachedBackendRenderTarget = backendRenderTarget
        cachedSurface = surface
        cachedFramebufferId = frameBufferId
        cachedWidth = width
        cachedHeight = height
        return surface
    }

    //#if MC >= 1.21.5
    /**
     * Credits: lowercasebtw
     * Taken from: https://discord.com/channels/507304429255393322/807617488313516032/1452333789778018314 (The Fabric Project)
     */
    //$$ fun getFboId(frameBuffer: RenderTarget): Int {
    //$$    val device = RenderSystem.getDevice()
    //$$   if (device !is com.mojang.blaze3d.opengl.GlDevice) {
    //$$       return -1
    //$$   } else {
    //$$       val texture = (frameBuffer.getColorTexture() as com.mojang.blaze3d.opengl.GlTexture?) ?: error("well, someone messed up!")
    //$$       return texture.getFbo((device as com.mojang.blaze3d.opengl.GlDevice).directStateAccess(), frameBuffer.getDepthTexture())
    //$$   }
    //$$ }
    //#endif

    private fun paintFor(radius: Float): Paint = paints.getOrPut(radius) {
        Paint().also { paint ->
            paint.blendMode = BlendMode.SRC
            paint.imageFilter = ImageFilter.makeBlur(radius, radius, FilterTileMode.CLAMP)
        }
    }
}
