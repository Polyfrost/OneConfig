package org.polyfrost.oneconfig.internal.ui.compose

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.LoadingOverlay
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.compose.impls.HudEditorUIScreen
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.slf4j.LoggerFactory

object ComposePreloader {
    private val LOG = LoggerFactory.getLogger(ComposePreloader::class.java)

    @Volatile
    private var gpuWarmed = false

    var stopped = false
        private set
    private var failed = false

    private var menuComplete = false
    private var passStartedNanos = 0L
    private var passes = 0

    private data class Inputs(val width: Int, val height: Int, val configs: Int, val inWorld: Boolean)

    private var manager: ConfigManager? = null
    private var warmed: Inputs? = null
    private var seen: Inputs? = null
    private var seenTicks = 0
    private var passQueued = false
    private var watcher: EventHandler<TickEvent.End>? = null

    fun preloadGpuWarmup() {
        if (gpuWarmed) return
        gpuWarmed = true
        NotificationsManager.ensureInitialized()
        SkiaCtx.queueWarmup(::startupWarmUp)
    }

    fun fail(reason: String, cause: Throwable? = null) {
        if (failed) return
        failed = true
        stopped = true
        LOG.warn("OneConfig UI warm-up failed: $reason.", cause)
        endPass()
        stopWatching()
    }

    private fun endPass() {
        menuComplete = false
        passStartedNanos = 0L
        runCatching { OneConfigUIScreen.endPrewarmShared() }.onFailure { LOG.warn("Failed to end menu warm-up", it) }
        runCatching { HudEditorUIScreen.endPrewarmShared() }.onFailure { LOG.warn("Failed to end HUD editor warm-up", it) }
    }

    private fun releaseHold() {
        stopped = true
        if (!failed && watcher == null) watcher = EventManager.register(TickEvent.End::class.java, Runnable(::onTick))
    }

    private fun stopWatching() {
        watcher?.let { EventManager.INSTANCE.unregister(it) }
        watcher = null
    }

    private fun inputs(): Inputs {
        val manager = manager ?: ConfigManager.active().also { manager = it }
        return Inputs(
            width = Platform.screen().windowWidth(),
            height = Platform.screen().windowHeight(),
            configs = manager.trees().size,
            inWorld = Minecraft.getInstance().level != null,
        )
    }

    private fun step(): Boolean {
        if (passStartedNanos == 0L) passStartedNanos = System.nanoTime()
        if (!menuComplete) {
            menuComplete = OneConfigUIScreen.prewarmShared()
            return false
        }
        if (!HudEditorUIScreen.prewarmShared()) return false
        val tookMs = (System.nanoTime() - passStartedNanos) / 1_000_000
        warmed = inputs()
        passes++
        endPass()
        LOG.info("OneConfig UI warm-up pass {} completed in {} ms ({})", passes, tookMs, warmed)
        return true
    }

    private fun startupWarmUp() {
        if (stopped) return

        //? if >= 26.2 {
        val overlay = Minecraft.getInstance().gui.overlay()
        //?} else
        //val overlay = Minecraft.getInstance().overlay

        if (overlay !is LoadingOverlay) {
            releaseHold()
            return
        }

        try {
            if (SkiaFontRenderer.isReadyForWarmup()) {
                if (step()) releaseHold()
            } else if (SkiaFontRenderer.reloadFailed) {
                fail("font reload produced no usable font")
            }
        } catch (t: Throwable) {
            fail("${t.javaClass.simpleName}: ${t.message}", t)
        }

        if (!stopped) SkiaCtx.queueWarmup(::startupWarmUp)
    }

    private fun onTick() {
        if (passQueued || failed || Platform.screen().current<Any?>() is ComposeScreen) return
        val inputs = inputs()
        if (inputs.width <= 0 || inputs.height <= 0) return
        if (inputs != seen) {
            seen = inputs
            seenTicks = 0
            return
        }
        if (inputs == warmed || ++seenTicks < SETTLE_TICKS) return
        passQueued = true
        SkiaCtx.queueWarmup(::rewarm)
    }

    private fun rewarm() {
        if (failed) return
        val done = try {
            SkiaFontRenderer.isReadyForWarmup() && step()
        } catch (t: Throwable) {
            fail("${t.javaClass.simpleName}: ${t.message}", t)
            return
        }
        if (!done) {
            SkiaCtx.queueWarmup(::rewarm)
            return
        }
        passQueued = false
        if (warmed?.inWorld == true || passes >= MAX_PASSES) stopWatching()
    }

    private const val SETTLE_TICKS = 10

    private const val MAX_PASSES = 8
}
