package org.polyfrost.oneconfig.internal.ui.compose

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.LoadingOverlay
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.internal.ui.compose.impls.HudEditorUIScreen
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.slf4j.LoggerFactory

object ComposePreloader {
    private val LOG = LoggerFactory.getLogger(ComposePreloader::class.java)

    @Volatile
    private var gpuWarmed = false
    private var menuComplete = false
    private var warmupStartedNanos = 0L
    var stopped = false
        private set

    fun preloadGpuWarmup() {
        if (gpuWarmed) return
        gpuWarmed = true
        NotificationsManager.ensureInitialized()
        SkiaCtx.queueWarmup(::warmUp)
    }

    fun failStartup(reason: String, cause: Throwable? = null) {
        if (stopped) return
        LOG.warn("OneConfig startup warm-up failed: $reason.", cause)
        finish()
    }

    private fun finish() {
        stopped = true
        OneConfigUIScreen.endPrewarmShared()
        HudEditorUIScreen.endPrewarmShared()
    }

    private fun warmUp() {
        if (stopped) return

        //? if >= 26.2 {
        val overlay = Minecraft.getInstance().gui.overlay()
        //?} else
        //val overlay = Minecraft.getInstance().overlay

        if (overlay !is LoadingOverlay) {
            finish()
            return
        }

        try {
            if (SkiaFontRenderer.isReadyForWarmup()) {
                if (warmupStartedNanos == 0L) warmupStartedNanos = System.nanoTime()
                if (!menuComplete) {
                    menuComplete = OneConfigUIScreen.prewarmShared()
                } else if (HudEditorUIScreen.prewarmShared()) {
                    finish()
                    LOG.info("OneConfig UI warm-up completed in {} ms", (System.nanoTime() - warmupStartedNanos) / 1_000_000)
                }
            }
        } catch (t: Throwable) {
            failStartup("${t.javaClass.simpleName}: ${t.message}", t)
        }

        if (!stopped) SkiaCtx.queueWarmup(::warmUp)
    }
}
