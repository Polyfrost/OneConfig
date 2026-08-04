package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.OneConfigInterface
import org.slf4j.LoggerFactory

/**
 * Warms up the Compose UI so the first real screen open doesn't stutter. Renders [OneConfigInterface]
 */
@OptIn(InternalComposeUiApi::class)
object ComposePreloader {
    private val LOG = LoggerFactory.getLogger(ComposePreloader::class.java)

    @Volatile
    private var gpuWarmed = false

    fun preloadGpuWarmup() {
        if (gpuWarmed) return
        gpuWarmed = true
        SkiaCtx.queueWarmup {
            val startNanos = System.nanoTime()
            val w = Platform.screen().windowWidth().takeIf { it > 0 } ?: 1280
            val h = Platform.screen().windowHeight().takeIf { it > 0 } ?: 720
            // Mirror ComposeScreen: created and rendered on the render thread, default coroutineContext.
            val scene = CanvasLayersComposeScene(
                platformContext = ComposeSceneContextImpl.platformContext,
            )
            val surface = Surface.makeRenderTarget(
                SkiaCtx.directContext,
                false,
                ImageInfo.makeN32Premul(w, h),
            )
            try {
                scene.setContent {
                    OneConfigInterface(
                        windowWidth = w.toFloat(),
                        windowHeight = h.toFloat(),
                    )
                }
                scene.size = IntSize(w, h)
                scene.density = Density(1f)
                scene.sendPointerEvent(PointerEventType.Move, position = Offset(50f, 50f))
                scene.render(surface.canvas.asComposeCanvas(), System.nanoTime())
                surface.flushAndSubmit()
                LOG.info("Compose GPU warm-up finished in {} ms", (System.nanoTime() - startNanos) / 1_000_000)
            } catch (e: Throwable) {
                LOG.warn("Compose GPU warm-up failed", e)
            } finally {
                scene.close()
                surface.close()
            }
        }
    }
}
