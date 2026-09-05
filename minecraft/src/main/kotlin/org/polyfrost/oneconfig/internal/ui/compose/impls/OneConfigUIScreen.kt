package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
//? if < 1.21.11
//import org.lwjgl.glfw.GLFW
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.OneConfigInterface
import org.polyfrost.oneconfig.internal.ui.components.warmIconCache
import org.polyfrost.oneconfig.internal.ui.guiCloseAnimationMillis
import org.polyfrost.oneconfig.internal.ui.compose.BlurRenderer
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.KeybindsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ThemesGraph
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignSession
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudEditorViewport
import org.polyfrost.oneconfig.internal.ui.PlayerHeadLoader
import org.polyfrost.oneconfig.internal.ui.shell.HudEditorRoute
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.OneConfig
import kotlin.math.pow

class OneConfigUIScreen @JvmOverloads constructor(
    private val initialTreeId: String? = null,
    private val initialCategory: String? = null,
    private val initialTree: Tree? = null,
) : ComposeScreen() {
    private var initialRoute: Any? = null

    override val retainsScene: Boolean get() = this === sharedScreen

    companion object {
        private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/UI")

        private var sharedScreen: OneConfigUIScreen? = null

        private fun shared(): OneConfigUIScreen =
            sharedScreen ?: OneConfigUIScreen().also { sharedScreen = it }

        @JvmStatic
        fun forRoute(route: Any?): OneConfigUIScreen = shared().also { it.initialRoute = route }

        @JvmStatic
        fun open(): OneConfigUIScreen = shared().also { it.initialRoute = null }
        @JvmStatic
        fun resume(): OneConfigUIScreen = shared().also {
            it.initialRoute = null
            it.resumeNext = true
        }
        private const val FULLSCREEN_BLUR_RADIUS = 8f
        private const val OPEN_ANIMATION_MS = 250L

        /** Serialized so two closes in quick succession cannot write the same files at once */
        private val SAVE_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
            Thread(r, "OneConfig-ConfigSave").apply { isDaemon = true }
        }

        private val savePending = java.util.concurrent.atomic.AtomicBoolean(false)

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

        private fun resolveOpeningBehaviorRoute(): OpeningRoute = resolveRoute().let {
            // "Reopen HUD editor" is off by default so the editor is never restored as a page
            if (it.route === HudEditorRoute && !OneConfigConfig.restoreHudEditor) OpeningRoute(ModsGraph) else it
        }

        private fun resolveRoute(): OpeningRoute = when (OneConfigConfig.openingBehavior) {
            0 -> OpeningRoute(ModsGraph)
            1 -> OpeningRoute(PreferencesGraph)
            2 -> ShellState.lastRoute?.let { OpeningRoute(it, restored = true) } ?: OpeningRoute(ModsGraph)
            3 -> {
                val last = ShellState.lastClosedAt
                val route = ShellState.lastRoute
                // the HUD editor stays restorable for longer than a config page
                val window = if (route === HudEditorRoute) HudDesignSession.restoreWindowMillis()
                    else (OneConfigConfig.timeBeforeReset * 1000f).toLong()
                val withinWindow = last > 0L && System.currentTimeMillis() - last <= window
                if (withinWindow && route != null) OpeningRoute(route, restored = true) else OpeningRoute(ModsGraph)
            }
            else -> OpeningRoute(ModsGraph)
        }

        @JvmStatic
        fun prewarmShared(): Boolean = shared().runPrewarm()

        @JvmStatic
        fun endPrewarmShared() {
            sharedScreen?.endPrewarm()
        }

        private const val PREWARM_FRAME_BUDGET = 1

        private const val PREWARM_OPEN_FRAME = 1

        private const val PREWARM_FOCUS_FRAME = 2

        private val PREWARM_SCROLL_FRAMES = 5..17
        private const val PREWARM_RESTORE_FRAME = 18

        private val PREWARM_ROUTES = listOf(PreferencesGraph, ThemesGraph, KeybindsGraph, ModsGraph)
        private const val PREWARM_FRAMES_PER_PAGE = 2
        private val PREWARM_PAGE_FRAMES = PREWARM_RESTORE_FRAME + 1..
            PREWARM_RESTORE_FRAME + PREWARM_ROUTES.size * PREWARM_FRAMES_PER_PAGE

        private val PREWARM_FORGET_FRAME = PREWARM_PAGE_FRAMES.last + 1
        private val PREWARM_CLOSE_FRAME = PREWARM_FORGET_FRAME + 1

        private val PREWARM_FRAMES = PREWARM_CLOSE_FRAME + 1

        private const val MOD_GRID_KEY = "mods"

        @JvmStatic
        fun openLastSession() {
            if (resolveOpeningBehaviorRoute().route === HudEditorRoute) HudManager.openEditor()
            else Platform.screen().display(open())
        }
    }

    @Volatile private var closeRequested = false
    @Volatile private var closeRequestedAt = 0L
    @Volatile private var closeAnimationMs = 0L
    @Volatile private var openedAt = 0L

    private fun beginClose() {
        if (prewarming || closeRequested) return
        closeRequested = true
        closeRequestedAt = System.currentTimeMillis()
        closeAnimationMs = guiCloseAnimationMillis()
        UiSounds.play(UiSoundEvent.CLOSE)
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
        return true
    }

    /** The page this screen is showing which survives the scene being disposed and rebuilt */
    private var route: Any? by mutableStateOf(null)

    /** True once this screen has been displaced by another and is being shown again */
    private var resuming by mutableStateOf(false)

    /** True when [route] is a page being put back rather than a page being opened */
    private var restoring by mutableStateOf(false)

    private var resumeNext = false

    private var openRevision by mutableIntStateOf(0)

    private var prewarming = false

    @Volatile private var everOpened = false

    private fun runPrewarm(): Boolean {
        if (everOpened || Platform.screen().current<Any?>() === this) return true
        prewarming = true
        return try {
            ConfigRegistry.loadFrom(ConfigManager.active(), ConfigSource.OC)
            warmIconCache(ConfigRegistry.modCardConfigs.mapNotNull { it.icon })
            var restoreTo = 0
            prewarm(PREWARM_FRAMES, PREWARM_FRAME_BUDGET) { frame ->
                when (frame) {
                    PREWARM_OPEN_FRAME -> requestOpenCallback?.invoke()
                    PREWARM_FOCUS_FRAME -> ShellState.focusSearchField = true
                    PREWARM_CLOSE_FRAME -> {
                        ShellState.focusSearchField = false
                        ShellState.searchFieldFocused = false
                        ShellState.searchQuery = ""
                        requestCloseCallback?.invoke()
                    }
                    PREWARM_RESTORE_FRAME -> scrollModGrid(restoreTo)
                    in PREWARM_PAGE_FRAMES -> {
                        val step = frame - PREWARM_PAGE_FRAMES.first
                        if (step % PREWARM_FRAMES_PER_PAGE == 0) {
                            warmRoute(PREWARM_ROUTES[step / PREWARM_FRAMES_PER_PAGE])
                        }
                    }
                    PREWARM_FORGET_FRAME -> LocalNavController.wrapper.reset()
                    in PREWARM_SCROLL_FRAMES -> {
                        val grid = ShellState.gridStates[MOD_GRID_KEY] ?: return@prewarm
                        if (frame == PREWARM_SCROLL_FRAMES.first) restoreTo = grid.firstVisibleItemIndex
                        val last = (grid.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                        val step = frame - PREWARM_SCROLL_FRAMES.first
                        val span = PREWARM_SCROLL_FRAMES.last - PREWARM_SCROLL_FRAMES.first
                        scrollModGrid(last * step / span)
                    }
                }
            }
        } catch (t: Throwable) {
            LOGGER.warn("OneConfig UI warm-up failed; the first open will build the UI instead", t)
            false
        } finally {
            prewarming = false
        }
    }

    private fun scrollModGrid(index: Int) {
        runCatching { ShellState.gridStates[MOD_GRID_KEY]?.requestScrollToItem(index) }
    }

    private fun warmRoute(route: Any) {
        runCatching { if (LocalNavController.isReady) LocalNavController.wrapper.navigate(route) }
    }

    private fun markClosed() {
        ShellState.lastClosedAt = System.currentTimeMillis()
    }

    override fun init() {
        OneConfig.dismissFirstLaunchToast()
        ConfigRegistry.loadFrom(ConfigManager.active(), ConfigSource.OC)
        initialTree?.let { ConfigRegistry.registerTree(it, ConfigSource.OC) }

        val isResume = resumeNext
        resumeNext = false
        val (target, targetRestoring) = when {
            isResume -> (ShellState.lastRoute?.takeIf { it !== HudEditorRoute } ?: ModsGraph) to true
            initialRoute != null -> initialRoute to false
            initialTreeId != null -> ModConfigRoute(initialTreeId, initialCategory) to false
            else -> {
                val opening = resolveOpeningBehaviorRoute()
                val resolved = opening.route.takeIf { it !== HudEditorRoute } ?: ModsGraph
                resolved to (opening.restored && resolved === opening.route)
            }
        }
        route = target
        restoring = targetRestoring
        resuming = isResume
        openRevision++
        ShellState.lastRoute = target

        try {
            ShellState.playerName = net.minecraft.client.Minecraft.getInstance().user.name
        } catch (_: Throwable) {
            ShellState.playerName = "Player"
        }
        ShellState.focusSearchField = OneConfigConfig.instantSearch
        ShellState.searchFieldFocused = false
        val client = net.minecraft.client.Minecraft.getInstance()
        val cachedHead = PlayerHeadLoader.cachedLocalPlayerHeadPng(client)
        if (cachedHead != null) {
            ShellState.playerHeadPng = cachedHead
        } else {
            Thread {
                runCatching {
                    val head = PlayerHeadLoader.loadLocalPlayerHeadPng(client) ?: return@runCatching
                    client.execute { ShellState.playerHeadPng = head }
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
                ShellState.versionLabel = "${parts[1].replaceFirstChar { it.uppercase() }} ${parts[0]}"
            } else {
                ShellState.versionLabel = loaderStr
            }
        } catch (_: Throwable) {
            ShellState.versionLabel = "OneConfig"
        }

        //? if < 1.21.8 {
        /*// Compose normally creates its surface after the HUD pass.
        // Create the surface now to avoid drawing the HUD twice.
        SkiaCtx.prepareComposeSurface()
        *///?}

        SkiaCtx.suppressInGameHudRender = true
        HudManager.overrideShowInScreens = true
        HudManager.isConfigUiOpen = true

        closeRequested = false
        closeRequestedAt = 0L
        closeAnimationMs = 0L

        everOpened = true
        openedAt = System.currentTimeMillis()
        UiSounds.play(UiSoundEvent.OPEN)
        UiSounds.acquireAmbience()
        super.init()

        requestOpenCallback?.invoke()
    }

    /**
     * A scene that failed mid-frame is thrown away and rebuilt while the menu stays open so the
     * rebuild is treated as coming back from another screen which stops the GUI hanging
     */
    override fun onSceneRebuilding() {
        ShellState.lastRoute?.takeIf { it !== HudEditorRoute }?.let { route = it }
        resuming = true
        restoring = true
        if (ShellState.searchFieldFocused) ShellState.focusSearchField = true
    }

    override fun removed() {
        SkiaCtx.suppressInGameHudRender = false
        HudManager.overrideShowInScreens = false
        HudManager.isConfigUiOpen = false
        ShellState.shellBounds = null
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
        //~ if < 1.21.11 'InputConstants.MOUSE_BUTTON_4' -> 'GLFW.GLFW_MOUSE_BUTTON_4'
        if (button >= InputConstants.MOUSE_BUTTON_4 &&
            KeybindManager.isTriggeredByMouse(OneConfigConfig.oneConfigKeybind, button)
        ) {
            return handleOneConfigKeybind()
        }
        if (!closeRequested && LocalNavController.isReady) {
            when (button) {
                //~ if < 1.21.11 'InputConstants.MOUSE_BUTTON_4' -> 'GLFW.GLFW_MOUSE_BUTTON_4'
                InputConstants.MOUSE_BUTTON_4 -> {
                    if (KeybindManager.hasTriggeredMouseBind(button)) return true
                    UiSounds.play(UiSoundEvent.CLICK)
                    LocalNavController.wrapper.back()
                    return true
                }
                //~ if < 1.21.11 'InputConstants.MOUSE_BUTTON_5' -> 'GLFW.GLFW_MOUSE_BUTTON_5'
                InputConstants.MOUSE_BUTTON_5 -> {
                    if (KeybindManager.hasTriggeredMouseBind(button)) return true
                    UiSounds.play(UiSoundEvent.CLICK)
                    LocalNavController.wrapper.forward()
                    return true
                }
            }
        }
        return false
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        // tcdcommons-based screens like Better Statistics Screen render their parent by hand each frame
        // and that would queue a fullscreen blur which smears over the popup so bail unless we are current
        if (Platform.screen().current<Any?>() !== this) return
        if (closeRequested && System.currentTimeMillis() - closeRequestedAt >= closeAnimationMs) {
            markClosed()
            //? if < 1.21.8
            //renderBackground(ctx, mouseX, mouseY, tickDelta)
            Platform.screen().close()
            //? if >= 1.21.8 {
            // This frame skipped normal HUD rendering because OneConfig was open.
            // Closing removes the Compose copy as well, so add the normal HUD back.
            OneConfig.render(ctx, tickDelta)
            SkiaCtx.blitHud(ctx)
            //?} else {
            /*if (closeAnimationMs <= 0L) {
                SkiaCtx.discardComposeFrame()
                OneConfig.render(ctx, tickDelta)
                SkiaCtx.blitHud(ctx)
            }
            *///?}
            return
        }
        if (client.level == null) {
            HudManager.inWorld = false
            val sw = Platform.screen().guiWidth().toFloat()
            val sh = Platform.screen().guiHeight().toFloat()
            HudManager.guiScreenWidth = sw
            HudManager.guiScreenHeight = sh
            HudManager.prepare(sw, sh)
        }
        HudEditorViewport.update(Platform.screen().windowWidth(), Platform.screen().windowHeight())
        if (OneConfigConfig.enableBackgroundBlur) {
            //? if >= 1.21.10 {
            if (SkiaCtx.isDeferredComposeBackend) {
                ctx.nextStratum()
                ctx.blurBeforeThisStratum()
                SkiaCtx.requestBlurSnapshot()
            }
            //? }
            BlurRenderer.drawBlur(fullscreenBlurRadius())
        }
        //~ if >= 26.1 'render' -> 'extractRenderState'
        super.extractRenderState(ctx, mouseX, mouseY, tickDelta)
    }

    private fun fullscreenBlurRadius(): Float {
        val now = System.currentTimeMillis()
        val progress = if (closeRequested) {
            if (closeAnimationMs <= 0L) 0f
            else 1f - easeOutExpo((now - closeRequestedAt).toFloat() / closeAnimationMs)
        } else {
            (now - openedAt).toFloat() / OPEN_ANIMATION_MS
        }
        return FULLSCREEN_BLUR_RADIUS * progress.coerceIn(0f, 1f)
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
            resuming = resuming,
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
