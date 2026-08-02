package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.platform.LocalWindowInfo
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
//? >= 1.21.10
import net.minecraft.client.input.KeyEvent
//? >= 1.21.10
import net.minecraft.client.input.MouseButtonEvent
import org.lwjgl.glfw.GLFW
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.OneConfigInterface
import org.polyfrost.oneconfig.internal.ui.compose.BlurRenderer
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignSession
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudEditorViewport
import org.polyfrost.oneconfig.internal.ui.PlayerHeadLoader
import org.polyfrost.oneconfig.internal.ui.shell.HudEditorRoute
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.api.platform.v1.Platform
import kotlin.math.pow

class OneConfigUIScreen @JvmOverloads constructor(
    private val initialTreeId: String? = null,
    private val initialCategory: String? = null,
    private val initialTree: Tree? = null,
) : ComposeScreen() {
    companion object {
        private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/UI")
        private const val FULLSCREEN_BLUR_RADIUS = 8f
        private const val OPEN_ANIMATION_MS = 250L
        private const val CLOSE_ANIMATION_MS = 300L

        private fun resolveOpeningBehaviorRoute(): Any = when (OneConfigConfig.openingBehavior) {
            0 -> ModsGraph
            1 -> PreferencesGraph
            2 -> ShellState.lastRoute ?: ModsGraph
            3 -> {
                val last = ShellState.lastClosedAt
                val route = ShellState.lastRoute
                // the HUD editor stays restorable for longer than a config page
                val window = if (route === HudEditorRoute) HudDesignSession.restoreWindowMillis()
                    else (OneConfigConfig.timeBeforeReset * 1000f).toLong()
                val withinWindow = last > 0L && System.currentTimeMillis() - last <= window
                if (withinWindow) route ?: ModsGraph else ModsGraph
            }
            else -> ModsGraph
        }

        @JvmStatic
        fun openLastSession() {
            if (resolveOpeningBehaviorRoute() === HudEditorRoute) HudManager.openEditor()
            else Platform.screen().display(OneConfigUIScreen())
        }
    }

    @Volatile private var closeRequested = false
    @Volatile private var closeRequestedAt = 0L
    @Volatile private var openedAt = 0L

    /** The page this screen is showing. Survives the scene being disposed and rebuilt. */
    private var route: Any? = null

    /** True once this screen has been displaced by another and is being shown again. */
    private var resuming = false

    private fun markClosed() {
        ShellState.lastClosedAt = System.currentTimeMillis()
    }

    override fun init() {
        org.polyfrost.oneconfig.internal.OneConfig.dismissFirstLaunchToast()
        ConfigRegistry.loadFrom(ConfigManager.active(), ConfigSource.OC)
        initialTree?.let { ConfigRegistry.registerTree(it, ConfigSource.OC) }

        if (route == null) {
            route = when {
                initialTreeId != null -> ModConfigRoute(initialTreeId, initialCategory)
                else -> resolveOpeningBehaviorRoute().takeIf { it !== HudEditorRoute } ?: ModsGraph
            }
            ShellState.lastRoute = route
        }

        try {
            ShellState.playerName = net.minecraft.client.Minecraft.getInstance().user.name
        } catch (_: Throwable) {
            ShellState.playerName = "Player"
        }
        ShellState.playerHeadPng = null
        ShellState.focusSearchField = OneConfigConfig.instantSearch
        val client = net.minecraft.client.Minecraft.getInstance()
        Thread {
            val head = PlayerHeadLoader.loadLocalPlayerHeadPng() ?: return@Thread
            client.execute { ShellState.playerHeadPng = head }
        }.apply {
            isDaemon = true
            name = "OneConfig-PlayerHead"
            start()
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

        SkiaCtx.suppressInGameHudRender = true
        HudManager.overrideShowInScreens = true
        HudManager.isConfigUiOpen = true

        openedAt = System.currentTimeMillis()
        UiSounds.play(UiSoundEvent.OPEN)
        UiSounds.acquireAmbience()
        super.init()
    }

    override fun removed() {
        // A screen opened over this one removes it and hands it back when it closes, and the scene is rebuilt
        // from scratch in between, so the page has to be carried across by hand.
        ShellState.lastRoute?.takeIf { it !== HudEditorRoute }?.let { route = it }
        resuming = true
        SkiaCtx.suppressInGameHudRender = false
        HudManager.overrideShowInScreens = false
        HudManager.isConfigUiOpen = false
        UiSounds.releaseAmbience()
        try {
            ConfigManager.active().saveAll()
        } catch (t: Throwable) {
            LOGGER.error("Failed to save configs on OneConfig UI close", t)
        }
        super.removed()
    }

    override fun isPauseScreen(): Boolean = OneConfigConfig.pauseGame

    @Suppress("DuplicatedCode")
    //? >= 1.21.10 {
    override fun keyPressed(event: KeyEvent): Boolean {
        val key = event.key
        //? } else {
        /*override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        *///? }
        if (key == InputConstants.KEY_ESCAPE) {
            if (KeybindRecordingBus.consumeEscape()) return true
            if (!closeRequested) {
                closeRequested = true
                closeRequestedAt = System.currentTimeMillis()
                markClosed()
                UiSounds.play(UiSoundEvent.CLOSE)
                requestCloseCallback?.invoke()
            }
            return true
        }
        val toggleKey = OneConfigConfig.oneConfigKeybind.keyCodes?.firstOrNull()
        if (toggleKey != null && key == toggleKey && !KeybindRecordingBus.isRecording) {
            HudManager.openEditor()
            return true
        }
        //? >= 1.21.10 {
        return super.keyPressed(event)
        //? } else {
        /*return super.keyPressed(key, scanCode, modifiers)
        *///? }
    }

    /** Mouse side buttons navigate the page history, like a browser. */
    //? >= 1.21.10 {
    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val button = event.button()
        //? } else {
        /*override fun mouseClicked(x: Double, y: Double, button: Int): Boolean {
        *///? }
        if (!closeRequested && LocalNavController.isReady) {
            when (button) {
                GLFW.GLFW_MOUSE_BUTTON_4 -> {
                    UiSounds.play(UiSoundEvent.CLICK)
                    LocalNavController.wrapper.back()
                    return true
                }
                GLFW.GLFW_MOUSE_BUTTON_5 -> {
                    UiSounds.play(UiSoundEvent.CLICK)
                    LocalNavController.wrapper.forward()
                    return true
                }
            }
        }
        //? >= 1.21.10 {
        return super.mouseClicked(event, doubleClick)
        //? } else {
        /*return super.mouseClicked(x, y, button)
        *///? }
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (closeRequested && System.currentTimeMillis() - closeRequestedAt >= CLOSE_ANIMATION_MS) {
            Platform.screen().close()
            return
        }
        if (client.level == null) {
            HudManager.inWorld = false
            val sw = ctx.guiWidth().toFloat()
            val sh = ctx.guiHeight().toFloat()
            HudManager.guiScreenWidth = sw
            HudManager.guiScreenHeight = sh
            HudManager.prepare(sw, sh)
        }
        HudEditorViewport.update(Platform.screen().viewportWidth(), Platform.screen().viewportHeight())
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
            1f - easeOutExpo((now - closeRequestedAt).toFloat() / CLOSE_ANIMATION_MS)
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

    @Composable
    override fun compose() {
        val initialRoute = route ?: ModsGraph

        val containerSize = LocalWindowInfo.current.containerSize
        OneConfigInterface(
            containerSize.width.toFloat(),
            containerSize.height.toFloat(),
            initialRoute = initialRoute,
            resuming = resuming,
            onCloseRequest = {
                if (!closeRequested) {
                    closeRequested = true
                    closeRequestedAt = System.currentTimeMillis()
                    markClosed()
                    UiSounds.play(UiSoundEvent.CLOSE)
                }
            },
            onCloseReady = { closeRequest ->
                requestCloseCallback = closeRequest
            },
        ) { }
    }
}
