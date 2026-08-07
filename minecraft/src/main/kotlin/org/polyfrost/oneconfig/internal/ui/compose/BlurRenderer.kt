package org.polyfrost.oneconfig.internal.ui.compose

//~ main_render_target
import com.mojang.blaze3d.pipeline.RenderTarget
import net.minecraft.client.Minecraft
import org.jetbrains.skia.*
import org.polyfrost.oneconfig.internal.ui.RenderTargetFbo

object BlurRenderer {
    private const val RADIUS_QUANTUM = 0.5f

    private const val DOWNSAMPLE = 4

    private val client get() = Minecraft.getInstance()

    private val paints = HashMap<Int, Paint>()

    private val lowResPaints = HashMap<Int, Paint>()

    private val OPAQUE_ALPHA_PAINT = Paint().apply {
        color = 0xFF000000.toInt()
        blendMode = BlendMode.PLUS
    }

    private val COPY_PAINT = Paint().apply {
        blendMode = BlendMode.SRC
    }

    private var cachedSurface: Surface? = null
    private var cachedBackendRenderTarget: BackendRenderTarget? = null
    private var cachedFramebufferId = -1
    private var cachedWidth = -1
    private var cachedHeight = -1

    private var sourceOrigin = SurfaceOrigin.BOTTOM_LEFT

    private var scratchSurface: Surface? = null
    private var blurSurface: Surface? = null
    private var scratchSource: Surface? = null
    private var scratchOrigin: SurfaceOrigin? = null
    private var scratchWidth = -1
    private var scratchHeight = -1

    private var cachedVkSurface: Surface? = null
    private var cachedVkBRT: BackendRenderTarget? = null
    private var cachedVkImage: Long = 0L
    private var cachedVkWidth = -1
    private var cachedVkHeight = -1

    fun drawBlur(radius: Float = 8f) {
        if (radius < 0.5f) return
        SkiaCtx.queueDraw {
            val target = client.gameRenderer.mainRenderTarget()
            drawRegion(SkiaCtx.canvas, 0f, 0f, target.width.toFloat(), target.height.toFloat(), radius)
        }
    }

    fun drawRegion(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, radius: Float) {
        if (width <= 0f || height <= 0f) return

        val target = client.gameRenderer.mainRenderTarget()
        val srcWidth = target.width
        val srcHeight = target.height
        val sourceSurface = resolveSurface(target, srcWidth, srcHeight) ?: return
        sourceSurface.notifyContentWillChange(ContentChangeMode.RETAIN)

        if (!drawRegionDownsampled(canvas, x, y, width, height, radius, sourceSurface, srcWidth, srcHeight)) {
            drawRegionFullRes(canvas, x, y, width, height, radius, sourceSurface)
        }

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

    private fun drawRegionDownsampled(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        source: Surface,
        srcWidth: Int,
        srcHeight: Int,
    ): Boolean {
        if (srcWidth <= 0 || srcHeight <= 0) return false
        val lowWidth = ceilDiv(srcWidth, DOWNSAMPLE)
        val lowHeight = ceilDiv(srcHeight, DOWNSAMPLE)
        if (lowWidth <= 0 || lowHeight <= 0) return false
        if (!ensureScratch(source, lowWidth, lowHeight)) return false
        val scratch = scratchSurface ?: return false
        val blurred = blurSurface ?: return false

        val scaleX = lowWidth.toFloat() / srcWidth
        val scaleY = lowHeight.toFloat() / srcHeight

        val scratchCanvas = scratch.canvas
        scratchCanvas.save()
        scratchCanvas.scale(scaleX, scaleY)
        source.draw(scratchCanvas, 0, 0, SamplingMode.LINEAR, COPY_PAINT)
        scratchCanvas.restore()

        scratch.draw(blurred.canvas, 0, 0, lowResPaintFor(radius))

        canvas.save()
        canvas.clipRect(Rect.makeXYWH(0f, 0f, width, height))
        canvas.translate(-x, -y)
        canvas.scale(1f / scaleX, 1f / scaleY)
        blurred.draw(canvas, 0, 0, SamplingMode.LINEAR, COPY_PAINT)
        canvas.restore()
        return true
    }

    private fun drawRegionFullRes(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        source: Surface,
    ) {
        canvas.save()
        canvas.clipRect(Rect.makeXYWH(0f, 0f, width, height))
        canvas.translate(-x, -y)
        source.draw(canvas, 0, 0, paintFor(radius))
        canvas.restore()
    }

    private fun ensureScratch(source: Surface, width: Int, height: Int): Boolean {
        val origin = sourceOrigin
        if (scratchSurface != null && blurSurface != null && scratchSource === source &&
            scratchOrigin == origin && scratchWidth == width && scratchHeight == height
        ) return true

        releaseScratch()

        val info = ImageInfo.makeN32Premul(width, height)
        val scratch = runCatching {
            Surface.makeRenderTarget(SkiaCtx.directContext, false, info, 0, origin, null)
        }.getOrNull() ?: return false
        val blurred = runCatching {
            Surface.makeRenderTarget(SkiaCtx.directContext, false, info, 0, origin, null)
        }.getOrNull()
        if (blurred == null) {
            scratch.close()
            return false
        }

        scratchSurface = scratch
        blurSurface = blurred
        scratchSource = source
        scratchOrigin = origin
        scratchWidth = width
        scratchHeight = height
        return true
    }

    private fun releaseScratch() {
        scratchSurface?.close(); scratchSurface = null
        blurSurface?.close(); blurSurface = null
        scratchSource = null
        scratchOrigin = null
        scratchWidth = -1
        scratchHeight = -1
    }

    private fun ceilDiv(value: Int, divisor: Int) = (value + divisor - 1) / divisor

    private fun resolveSurface(target: RenderTarget, width: Int, height: Int): Surface? {
        if (SkiaCtx.isVulkanMode) return resolveVkSurface(target, width, height)
        sourceOrigin = SurfaceOrigin.BOTTOM_LEFT

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
        if (width <= 0 || height <= 0) return null

        val deferred = svc.usesDeferredCompose
        val origin = if (deferred) SurfaceOrigin.TOP_LEFT else SurfaceOrigin.BOTTOM_LEFT
        sourceOrigin = origin
        val (vkImg, vkFmt, queueFamily) =
            if (deferred) svc.getBlurSourceImageInfo() else svc.getMainColorImageInfo()
        if (vkImg == 0L) return null

        if (cachedVkSurface != null && cachedVkImage == vkImg && cachedVkWidth == width && cachedVkHeight == height) {
            return cachedVkSurface
        }

        cachedVkSurface?.close()
        cachedVkBRT?.close()

        val brt = svc.makeBackendRenderTarget(width, height, vkImg, vkFmt, queueFamily)
        val colorFmt = when (vkFmt) {
            44, 50 -> SurfaceColorFormat.BGRA_8888
            else -> SurfaceColorFormat.RGBA_8888
        }
        val surface = Surface.makeFromBackendRenderTarget(
            SkiaCtx.directContext, brt,
            origin, colorFmt, ColorSpace.sRGB, null,
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

    private fun paintFor(radius: Float): Paint = paintIn(paints, radius, 1)

    private fun lowResPaintFor(radius: Float): Paint = paintIn(lowResPaints, radius, DOWNSAMPLE)

    private fun paintIn(cache: HashMap<Int, Paint>, radius: Float, divisor: Int): Paint {
        val key = (radius / RADIUS_QUANTUM).toInt().coerceAtLeast(1)
        return cache.getOrPut(key) {
            val sigma = key * RADIUS_QUANTUM / divisor
            Paint().also { paint ->
                paint.blendMode = BlendMode.SRC
                paint.imageFilter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP)
            }
        }
    }
}
