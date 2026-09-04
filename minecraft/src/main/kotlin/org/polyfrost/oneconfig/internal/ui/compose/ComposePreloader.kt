package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.OneConfigInterface
import org.slf4j.LoggerFactory

/**
 * Warms up the Compose UI by rendering [OneConfigInterface] so the first screen open doesn't stutter
 */
@OptIn(InternalComposeUiApi::class)
object ComposePreloader {
    private val LOG = LoggerFactory.getLogger(ComposePreloader::class.java)

    @Volatile
    private var gpuWarmed = false

    fun preloadGpuWarmup() {
        if (gpuWarmed) return
        gpuWarmed = true
        NotificationsManager.ensureInitialized()
        SkiaCtx.queueWarmup {
            val startNanos = System.nanoTime()
            val w = Platform.screen().windowWidth().takeIf { it > 0 } ?: 1280
            val h = Platform.screen().windowHeight().takeIf { it > 0 } ?: 720
            var recomposer: FrameRecomposer? = null
            var scene: ComposeScene? = null
            var surface: Surface? = null
            try {
                val renderScope = SingleComposeSceneRenderingScope {}
                val liveRecomposer = FrameRecomposer(RenderThreadDispatcher).also { recomposer = it }
                val liveScene = CanvasLayersComposeScene(
                    frameRecomposer = liveRecomposer,
                    platformContext = ComposeSceneContextImpl.platformContext,
                ).also { scene = it }
                val liveSurface = Surface.makeRenderTarget(
                    SkiaCtx.directContext,
                    false,
                    ImageInfo.makeN32Premul(w, h),
                ).also { surface = it }
                liveScene.setContent {
                    OneConfigInterface(
                        windowWidth = w.toFloat(),
                        windowHeight = h.toFloat(),
                    )
                }
                liveScene.size = IntSize(w, h)
                liveScene.density = Density(1f)
                liveScene.sendPointerEvent(PointerEventType.Move, position = Offset(50f, 50f))
                with(renderScope) {
                    liveScene.render(liveRecomposer, liveSurface.canvas.asComposeCanvas(), System.nanoTime())
                }
                liveSurface.flushAndSubmit()
                LOG.info("Compose GPU warm-up finished in {} ms", (System.nanoTime() - startNanos) / 1_000_000)
            } catch (e: Throwable) {
                LOG.warn("Compose GPU warm-up failed", e)
            } finally {
                closeQuietly(scene)
                closeQuietly(recomposer)
                closeQuietly(surface)
            }
        }
    }

    private fun closeQuietly(closeable: AutoCloseable?) {
        try {
            closeable?.close()
        } catch (t: Throwable) {
            LOG.debug("Ignoring failure while closing a Compose warm-up resource", t)
        }
    }
}
