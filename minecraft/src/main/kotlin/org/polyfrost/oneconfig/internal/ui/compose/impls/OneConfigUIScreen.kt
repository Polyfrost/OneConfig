package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.mojang.blaze3d.platform.InputConstants
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.backend.PolyCanvas
import org.polyfrost.compose.backend.PolyCompose
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyGlyphPage
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyTessellator
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.internal.ClassPreloader
import org.polyfrost.oneconfig.internal.OneConfig
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.OneConfigInterface
import org.polyfrost.oneconfig.internal.ui.PlayerHeadLoader
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.api.ModCardTypeCollapseStore
import org.polyfrost.oneconfig.internal.ui.api.ModFavorites
import org.polyfrost.oneconfig.internal.ui.api.ModHidden
import org.polyfrost.oneconfig.internal.ui.api.ModSortStore
import org.polyfrost.oneconfig.internal.ui.components.item.ItemCatalog
import org.polyfrost.oneconfig.internal.ui.components.warmIconCache
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.compose.ComposeSupport
import org.polyfrost.oneconfig.internal.ui.compose.sceneDensity
import org.polyfrost.oneconfig.internal.ui.guiCloseAnimationMillis
import org.polyfrost.oneconfig.internal.ui.hud.LegacyHudRenderer
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudEditorViewport
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindGroupCollapseStore
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.navigation.graph.KeybindsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ThemesGraph
import org.polyfrost.oneconfig.internal.ui.shell.HudEditorRoute
import org.polyfrost.oneconfig.internal.ui.shell.SearchFocus
import org.polyfrost.oneconfig.internal.ui.shell.ShellController
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.updateAccent
import org.polyfrost.oneconfig.utils.v1.Multithreading

//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor
//?}

//? if < 1.21.11 && > 1.8.9 {
/*import org.lwjgl.glfw.GLFW
*///?}

class OneConfigUIScreen @JvmOverloads constructor(
    private val initialTreeId: String? = null,
    private val initialCategory: String? = null,
    private val initialTree: Tree? = null,
) : ComposeScreen() {
    private var initialRoute: Any? = null

    override val retainsScene: Boolean get() = this === sharedScreen

    // over the blur, legacy HUDs stay as sharp as the HUD previews next to them
    override val underlay get() = LegacyHudRenderer.liveDraw

    override fun blurAlpha(): Float {
        if (!OneConfigConfig.enableBackgroundBlur) return 0f
        val now = System.currentTimeMillis()
        val progress = if (closeRequested) {
            if (closeAnimationMs <= 0L) 0f
            else 1f - easeOutExpo((now - closeRequestedAt).toFloat() / closeAnimationMs)
        } else {
            (now - openedAt).toFloat() / OPEN_ANIMATION_MS
        }
        return progress.coerceIn(0f, 1f)
    }

    companion object {
        private val LOGGER = LogManager.getLogger("OneConfig/UI")

        private val shell: ShellController = ShellController.shared

        private var sharedScreen: OneConfigUIScreen? = null

        private fun shared(): OneConfigUIScreen =
            sharedScreen ?: run { ClassPreloader.recordFirstOpen(); dropPrerasteredLater(); OneConfigUIScreen() }.also { sharedScreen = it }

        @JvmStatic
        fun forRoute(route: Any?): OneConfigUIScreen = shared().also {
            it.initialRoute = route
            it.resumeNext = false
        }

        @JvmStatic
        fun open(): OneConfigUIScreen = shared().also {
            it.initialRoute = null
            it.resumeNext = false
        }

        /** For returning from a HUD editor opened from the OneConfig menu */
        @JvmStatic
        fun resume(): OneConfigUIScreen = shared().also {
            it.initialRoute = null
            it.resumeNext = true
        }

        private const val OPEN_ANIMATION_MS = 250L

        /** Serialized so two closes in quick succession cannot write the same files at once */
        private val SAVE_EXECUTOR = Executors.newSingleThreadExecutor { r ->
            Thread(r, "OneConfig-ConfigSave").apply { isDaemon = true }
        }

        private val savePending = AtomicBoolean(false)

        private fun scheduleSave() {
            if (!savePending.compareAndSet(false, true)) return
            SAVE_EXECUTOR.execute {
                savePending.set(false)
                try {
                    ConfigManager.active().saveAll()
                } catch (t: Throwable) {
                    LOGGER.error("Failed to save configs on OneConfig UI close", t)
                }
            }
        }

        /** [restored] marks a route that puts the user back where they were rather than opening a fixed page */
        private data class OpeningRoute(val route: Any, val restored: Boolean = false)

        private fun resolveOpeningRoute(): OpeningRoute {
            val route = shell.navigation.lastRoute
            // "Reopen HUD editor" is off by default so the editor is never restored as a page
            if (route == null || !OneConfigConfig.remembersPageClosedAt(shell.session.lastClosedAt) ||
                (route === HudEditorRoute && !OneConfigConfig.restoreHudEditor)) return OpeningRoute(ModsGraph)
            return OpeningRoute(route, restored = true)
        }

        private const val WARM_FRAMES = 6

        private const val WARM_STOP_WAIT_MS = 250L

        private const val PRERASTER_DROP_DELAY_S = 5L

        private val warmLock = ReentrantLock()

        @Volatile
        private var warmStopped = false

        @JvmStatic
        fun stopWarmUp() {
            if (warmStopped) return
            warmStopped = true
            val start = System.nanoTime()
            if (!warmLock.tryLock(WARM_STOP_WAIT_MS, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("The OneConfig UI warm-up did not stop within {}ms", WARM_STOP_WAIT_MS)
                return
            }
            warmLock.unlock()
            val waited = (System.nanoTime() - start) / 1_000_000L
            if (waited > 0) LOGGER.info("Waited {}ms for the OneConfig UI warm-up to stop", waited)
        }

        private fun dropPrerasteredLater() {
            Multithreading.schedule({
                LOGGER.info("Dropped {} unused prerasterized glyphs", PolyGlyphPage.dropPrerastered())
            }, PRERASTER_DROP_DELAY_S, TimeUnit.SECONDS)
        }

        @JvmStatic
        fun prepareWarmUp(): Runnable? {
            if (java.lang.Boolean.getBoolean("oneconfig.debug.noWarmCompose") || sharedScreen != null || !ComposeSupport.isAvailable) return null
            val w = Platform.screen().windowWidth()
            val h = Platform.screen().windowHeight()
            if (w <= 0 || h <= 0) return null
            ModFavorites.contains("")
            ModHidden.contains("")
            ModSortStore.selectedId
            ModCardTypeCollapseStore.isCollapsed("")
            KeybindGroupCollapseStore.isCollapsed("")
            val density = sceneDensity()
            val surface = Platform.screen().viewportWidth().toFloat() / w
            return Runnable { warmUp(w, h, density, surface) }
        }

        private fun warmUp(w: Int, h: Int, density: Float, surface: Float) {
            warmLock.lock()
            try {
                if (!warmStopped) warmFrames(w, h, density, surface)
            } finally {
                warmLock.unlock()
            }
        }

        @OptIn(InternalComposeUiApi::class)
        private fun warmFrames(w: Int, h: Int, density: Float, surface: Float) {
            val start = System.nanoTime()
            val thread = Thread.currentThread()
            val queue = ConcurrentLinkedQueue<Runnable>()
            val dispatcher = object : CoroutineDispatcher() {
                override fun isDispatchNeeded(context: CoroutineContext) = Thread.currentThread() !== thread
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    queue += block
                }
            }
            fun drain() {
                while (true) (queue.poll() ?: return).run()
            }
            val failure = AtomicReference<Throwable?>()
            var frames = 0
            try {
                PolyCompose.install()
                val recomposer = FrameRecomposer(dispatcher + CoroutineExceptionHandler { _, t -> failure.compareAndSet(null, t) }) {}
                val scope = SingleComposeSceneRenderingScope {}
                val scene = try {
                    CanvasLayersComposeScene(
                        frameRecomposer = recomposer,
                        platformContext = WarmPlatformContext(IntSize(w, h)),
                        invalidateLayout = scope::onSceneInvalidation,
                        invalidateDraw = scope::onSceneInvalidation,
                    )
                } catch (t: Throwable) {
                    recomposer.close()
                    throw t
                }
                val shell = ShellController(warming = true)
                try {
                    scene.density = Density(density)
                    scene.size = IntSize(w, h)
                    shell.session.resume()
                    if (OneConfigConfig.instantSearch) shell.search.requestFocus(SearchFocus.SelectAll)
                    scene.setContent { OneConfigInterface(w.toFloat(), h.toFloat(), shell = shell) }
                    val renderer = WarmRenderer()
                    val canvas = PolyCanvas(renderer)
                    var nanos = System.nanoTime()
                    while (frames < WARM_FRAMES && !warmStopped) {
                        drain()
                        recomposer.performFrame(nanos)
                        scene.measureAndLayout()
                        scene.sendPointerEvent(PointerEventType.Move, Offset(1f, 1f))
                        renderer.save()
                        renderer.scale(surface, surface)
                        with(scope) { scene.render(recomposer, canvas, nanos) }
                        renderer.restore()
                        failure.get()?.let { throw it }
                        nanos += 16_666_667L
                        frames++
                    }
                } finally {
                    try {
                        scene.close()
                        recomposer.close()
                        shell.session.stop()
                        drain()
                    } catch (t: Throwable) {
                        LOGGER.debug("Ignoring failure while closing the OneConfig UI warm-up scene", t)
                    }
                }
                LOGGER.info("Warmed the OneConfig UI ({} frames) in {}ms", frames, (System.nanoTime() - start) / 1_000_000L)
            } catch (t: Throwable) {
                LOGGER.warn("Failed to warm the OneConfig UI after {} frames; it will start cold", frames, t)
            }
        }

        @JvmStatic
        fun openLastSession() {
            if (resolveOpeningRoute().route === HudEditorRoute) HudManager.openEditor()
            else Platform.screen().display(open())
        }
    }

    @Volatile private var closeRequested = false
    @Volatile private var closeRequestedAt = 0L
    @Volatile private var closeAnimationMs = 0L
    @Volatile private var openedAt = 0L

    private fun beginClose() {
        if (closeRequested) return
        closeRequested = true
        closeRequestedAt = System.currentTimeMillis()
        closeAnimationMs = guiCloseAnimationMillis()
        UiSounds.play(UiSoundEvent.CLOSE)
        UiSounds.releaseAmbience()
    }

    private fun cancelClose(): Boolean {
        if (!closeRequested) return false

        // Resume the opening blur animation from current blur intensity
        val now = System.currentTimeMillis()
        val blurProgress = if (closeAnimationMs <= 0L) 0f
            else 1f - easeOutExpo((now - closeRequestedAt).toFloat() / closeAnimationMs)
        openedAt = now - (blurProgress.coerceIn(0f, 1f) * OPEN_ANIMATION_MS).toLong()

        closeRequested = false
        requestOpenCallback?.invoke()
        UiSounds.play(UiSoundEvent.OPEN)
        UiSounds.acquireAmbience()
        return true
    }

    /** The page this screen is showing which survives the scene being disposed and rebuilt */
    private var route: Any? by mutableStateOf(null)

    /** True when [route] is a page being put back rather than a page being opened */
    private var restoring by mutableStateOf(false)

    private var resumeNext = false

    private var openRevision by mutableIntStateOf(0)

    @Volatile private var everOpened = false

    private fun markClosed() {
        shell.session.markClosed(System.currentTimeMillis())
    }

    override fun init() {
        OneConfig.dismissFirstLaunchToast()
        updateAccent()
        ConfigRegistry.loadFrom(ConfigManager.active(), ConfigSource.OC)
        initialTree?.let { ConfigRegistry.registerTree(it, ConfigSource.OC) }

        val isResume = resumeNext
        resumeNext = false
        val (target, targetRestoring) = when {
            isResume -> (shell.navigation.lastRoute?.takeIf { it !== HudEditorRoute } ?: ModsGraph) to true
            initialRoute != null -> initialRoute to false
            initialTreeId != null -> ModConfigRoute(initialTreeId, initialCategory) to false
            else -> {
                val opening = resolveOpeningRoute()
                val resolved = opening.route.takeIf { it !== HudEditorRoute } ?: ModsGraph
                resolved to (opening.restored && resolved === opening.route)
            }
        }
        route = target
        restoring = targetRestoring
        openRevision++
        shell.navigation.recordLastRoute(target)

        try {
            shell.session.updatePlayerName(Minecraft.getInstance().user.name)
        } catch (_: Throwable) {
            shell.session.updatePlayerName("Player")
        }
        val keepFocus = if (shell.search.fieldFocused) SearchFocus.Focus else SearchFocus.Unfocus
        shell.search.requestFocus(when {
            isResume -> keepFocus
            OneConfigConfig.instantSearch -> SearchFocus.SelectAll
            targetRestoring -> keepFocus
            else -> SearchFocus.Unfocus
        })
        val client = Minecraft.getInstance()
        val cachedHead = PlayerHeadLoader.cachedLocalPlayerHeadPng(client)
        if (cachedHead != null) {
            shell.session.updatePlayerHead(cachedHead)
        } else {
            Thread {
                runCatching {
                    val head = PlayerHeadLoader.loadLocalPlayerHeadPng(client) ?: return@runCatching
                    client.execute { shell.session.updatePlayerHead(head) }
                }.onFailure { LOGGER.warn("Failed to load player head", it) }
            }.apply {
                isDaemon = true
                name = "OneConfig-PlayerHead"
                start()
            }
        }
        try {
            val loaderStr = Platform.loader().loaderString
            val parts = loaderStr.split("-", limit = 2)
            if (parts.size == 2) {
                shell.session.updateVersionLabel("${parts[1].replaceFirstChar { it.uppercase() }} ${parts[0]}")
            } else {
                shell.session.updateVersionLabel(loaderStr)
            }
        } catch (_: Throwable) {
            shell.session.updateVersionLabel("OneConfig")
        }

        HudManager.overrideShowInScreens = true
        HudManager.isConfigUiOpen = true

        closeRequested = false
        closeRequestedAt = 0L
        closeAnimationMs = 0L

        everOpened = true
        openedAt = System.currentTimeMillis()
        UiSounds.play(UiSoundEvent.OPEN)
        UiSounds.acquireAmbience()
        shell.session.resume()
        super.init()
        requestOpenCallback?.invoke()
    }

    /**
     * A scene that failed mid-frame is thrown away and rebuilt while the menu stays open so the
     * rebuild is treated as coming back from another screen which stops the GUI hanging
     */
    override fun onSceneRebuilding() {
        shell.navigation.lastRoute?.takeIf { it !== HudEditorRoute }?.let { route = it }
        restoring = true
        if (shell.search.fieldFocused) shell.search.requestFocus(SearchFocus.Focus)
    }

    override fun removed() {
        HudManager.overrideShowInScreens = false
        HudManager.isConfigUiOpen = false
        shell.layout.updateShellBounds(null)
        shell.session.stop()
        UiSounds.releaseAmbience()
        // writing every registered tree hitches and Minecraft only re-grabs the cursor once this returns
        scheduleSave()
        super.removed()
    }

    override fun isPauseScreen(): Boolean = OneConfigConfig.pauseGame

    private fun handleOneConfigKeybind(): Boolean {
        if (closeRequested) return cancelClose()
        if (OneConfigConfig.keybindClosesGui) {
            OneConfigConfig.notifyKeybindClosedGui()
            beginClose()
            requestCloseCallback?.invoke()
        } else {
            HudManager.openEditor()
        }
        return true
    }

    override fun handleKeyPressed(key: Int, modifiers: Int): Boolean {
        if (key == InputConstants.KEY_ESCAPE) {
            if (!closeRequested) {
                beginClose()
                requestCloseCallback?.invoke()
            }
            return true
        }
        val toggleKey = OneConfigConfig.oneConfigKeybind.keyCodes?.firstOrNull()
        if (toggleKey != null && key == toggleKey && !KeybindRecordingBus.isRecording) {
            return handleOneConfigKeybind()
        }
        return false
    }

    /** Mouse side buttons navigate the page history like a browser */
    override fun handleMouseClicked(button: Int): Boolean {
        if (KeybindRecordingBus.isRecording) return false
        // Only side and extra mouse buttons can trigger the OneConfig keybind
        //~ if < 1.21.11 && > 1.8.9 'InputConstants.MOUSE_BUTTON_4' -> 'GLFW.GLFW_MOUSE_BUTTON_4'
        if (button >= InputConstants.MOUSE_BUTTON_4 &&
            KeybindManager.isTriggeredByMouse(OneConfigConfig.oneConfigKeybind, button)
        ) {
            return handleOneConfigKeybind()
        }
        if (!closeRequested && shell.navigation.isReady) {
            when (button) {
                //~ if < 1.21.11 && > 1.8.9 'InputConstants.MOUSE_BUTTON_4' -> 'GLFW.GLFW_MOUSE_BUTTON_4'
                InputConstants.MOUSE_BUTTON_4 -> {
                    if (KeybindManager.hasTriggeredMouseBind(button)) return true
                    UiSounds.play(UiSoundEvent.CLICK)
                    shell.navigation.back()
                    return true
                }
                //~ if < 1.21.11 && > 1.8.9 'InputConstants.MOUSE_BUTTON_5' -> 'GLFW.GLFW_MOUSE_BUTTON_5'
                InputConstants.MOUSE_BUTTON_5 -> {
                    if (KeybindManager.hasTriggeredMouseBind(button)) return true
                    UiSounds.play(UiSoundEvent.CLICK)
                    shell.navigation.forward()
                    return true
                }
            }
        }
        return false
    }

    //? if > 1.8.9 {
    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
    //?} else
    //override fun render(mouseX: Int, mouseY: Int, tickDelta: Float) {
        // tcdcommons-based screens like Better Statistics Screen render their parent by hand each frame
        // and that would queue a fullscreen blur which smears over the popup so bail unless we are current
        if (Platform.screen().current<Any?>() !== this) return
        if (closeRequested && System.currentTimeMillis() - closeRequestedAt >= closeAnimationMs) {
            markClosed()
            //? if < 1.21.8 && > 1.8.9
            //renderBackground(ctx, mouseX, mouseY, tickDelta)
            Platform.screen().close()
            return
        }
        if (client.level == null) {
            HudManager.inWorld = false
            val sw = Platform.screen().guiWidth().toFloat()
            val sh = Platform.screen().guiHeight().toFloat()
            HudManager.guiScreenWidth = sw
            HudManager.guiScreenHeight = sh
            HudManager.prepare(sw, sh)
            ItemCatalog.renderHudIcons()
        }
        HudEditorViewport.update(Platform.screen().windowWidth(), Platform.screen().windowHeight())
        //? if > 1.8.9 {
        //~ if >= 26.1 'render' -> 'extractRenderState'
        super.extractRenderState(ctx, mouseX, mouseY, tickDelta)
        //?} else
        //super.render(mouseX, mouseY, tickDelta)
    }

    private fun easeOutExpo(progress: Float): Float {
        return if (progress >= 1f) 1f else 1f - 2f.pow(-10f * progress)
    }

    /** Holds a reference to the close-animation trigger from Compose */
    private var requestCloseCallback: (() -> Unit)? = null
    private var requestOpenCallback: (() -> Unit)? = null

    @Composable
    override fun compose() {
        val initialRoute = route ?: ModsGraph

        val containerSize = LocalWindowInfo.current.containerSize
        OneConfigInterface(
            containerSize.width.toFloat(),
            containerSize.height.toFloat(),
            initialRoute = initialRoute,
            restoring = restoring,
            openRevision = openRevision,
            onCloseRequest = { beginClose() },
            onCloseReady = { closeRequest ->
                requestCloseCallback = closeRequest
            },
            onOpenReady = { openRequest ->
                requestOpenCallback = openRequest
            },
        ) { }
    }
}

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
private class WarmPlatformContext(size: IntSize) : PlatformContext by PlatformContext.Empty() {
    override val windowInfo: WindowInfo = object : WindowInfo {
        override val isWindowFocused: Boolean get() = true
        override val containerSize: IntSize = size
    }
}

private class WarmRenderer : PolyTessellator() {
    override val cpuOnly: Boolean get() = true

    override fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {}

    override fun bitmapTexture(bitmap: PolyBitmap): Any = bitmap

    override fun live(draw: (PolyRenderer) -> Unit) = draw(this)
}
