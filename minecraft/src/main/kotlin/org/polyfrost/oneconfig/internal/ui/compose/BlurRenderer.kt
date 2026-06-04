package org.polyfrost.oneconfig.internal.ui.compose

import com.mojang.blaze3d.pipeline.RenderTarget
import net.minecraft.client.Minecraft
import org.jetbrains.skia.*
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo

object BlurRenderer {
    private val client get() = Minecraft.getInstance()
    private val paints = HashMap<Float, Paint>()

    private val OPAQUE_ALPHA_PAINT = Paint().apply {
        color = 0xFF000000.toInt()
        blendMode = BlendMode.PLUS
    }

    private var cachedSurface: Surface? = null
    private var cachedBackendRenderTarget: BackendRenderTarget? = null
    private var cachedFramebufferId = -1
    private var cachedWidth = -1
    private var cachedHeight = -1

    private var cachedVkSurface: Surface? = null
    private var cachedVkBRT: BackendRenderTarget? = null
    private var cachedVkImage: Long = 0L
    private var cachedVkWidth = -1
    private var cachedVkHeight = -1

    fun drawBlur(radius: Float = 8f) {
        if (radius < 0.5f) return
        SkiaCtx.queueDraw {
            val target = client.mainRenderTarget
            drawRegion(SkiaCtx.canvas, 0f, 0f, target.width.toFloat(), target.height.toFloat(), radius)
        }
    }

    fun drawRegion(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, radius: Float) {
        if (width <= 0f || height <= 0f) return

        val target = client.mainRenderTarget
        val sourceSurface = resolveSurface(target, target.width, target.height) ?: return
        sourceSurface.notifyContentWillChange(ContentChangeMode.RETAIN)

        canvas.save()
        canvas.clipRect(Rect.makeXYWH(0f, 0f, width, height))
        canvas.translate(-x, -y)
        sourceSurface.draw(canvas, 0, 0, paintFor(radius))
        canvas.restore()

        // BlendMode.SRC above copies the source's alpha verbatim. On 1.21.11 and below Minecraft's
        // main render target leaves that alpha < 1, which is harmless when drawing straight to the
        // opaque back buffer, but makes the backdrop turn transparent the moment Compose composites
        // it through an offscreen layer (the open/close fade uses one). Force the region opaque while
        // preserving its colour so the backdrop survives layer compositing on every version.
        canvas.save()
        canvas.clipRect(Rect.makeXYWH(0f, 0f, width, height))
        canvas.drawRect(Rect.makeXYWH(0f, 0f, width, height), OPAQUE_ALPHA_PAINT)
        canvas.restore()
    }

    private fun resolveSurface(target: RenderTarget, width: Int, height: Int): Surface? {
        if (SkiaCtx.isVulkanMode) return resolveVkSurface(target, width, height)

        //? >= 1.21.5 {
        val frameBufferId = RenderTargetFbo.getFboId(target)
        //? } else
        //val frameBufferId = target.frameBufferId

        if (width <= 0 || height <= 0 || frameBufferId <= 0) return null
        if (cachedSurface != null && cachedFramebufferId == frameBufferId && cachedWidth == width && cachedHeight == height) {
            return cachedSurface
        }

        cachedSurface?.close()
        cachedBackendRenderTarget?.close()

        val backendRenderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, frameBufferId, FramebufferFormat.GR_GL_RGBA8)
        val surface = Surface.makeFromBackendRenderTarget(
            SkiaCtx.directContext, backendRenderTarget,
            SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB, null,
        )

        if (surface == null) {
            backendRenderTarget.close()
            cachedBackendRenderTarget = null; cachedSurface = null
            cachedFramebufferId = -1; cachedWidth = -1; cachedHeight = -1
            return null
        }

        cachedBackendRenderTarget = backendRenderTarget
        cachedSurface = surface
        cachedFramebufferId = frameBufferId; cachedWidth = width; cachedHeight = height
        return surface
    }

    private fun resolveVkSurface(target: RenderTarget, width: Int, height: Int): Surface? {
        val svc = SkiaCtx.vulkanService ?: return null
        val (vkImg, vkFmt, queueFamily) = svc.getMainColorImageInfo()
        if (vkImg == 0L || width <= 0 || height <= 0) return null

        if (cachedVkSurface != null && cachedVkImage == vkImg && cachedVkWidth == width && cachedVkHeight == height) {
            return cachedVkSurface
        }

        cachedVkSurface?.close()
        cachedVkBRT?.close()

        val brt = svc.makeBackendRenderTarget(width, height, vkImg, vkFmt, queueFamily)
        val surface = Surface.makeFromBackendRenderTarget(
            SkiaCtx.directContext, brt,
            SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.sRGB, null,
        )

        if (surface == null) {
            brt.close()
            cachedVkBRT = null; cachedVkSurface = null
            cachedVkImage = 0L; cachedVkWidth = -1; cachedVkHeight = -1
            return null
        }

        cachedVkBRT = brt; cachedVkSurface = surface
        cachedVkImage = vkImg; cachedVkWidth = width; cachedVkHeight = height
        return surface
    }

    private fun paintFor(radius: Float): Paint = paints.getOrPut(radius) {
        Paint().also { paint ->
            paint.blendMode = BlendMode.SRC
            paint.imageFilter = ImageFilter.makeBlur(radius, radius, FilterTileMode.CLAMP)
        }
    }
}
