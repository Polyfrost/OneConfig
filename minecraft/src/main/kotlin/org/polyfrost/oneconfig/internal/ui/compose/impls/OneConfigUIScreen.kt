package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
//? >= 1.21.10
import net.minecraft.client.input.KeyEvent
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.OneConfigInterface
import org.polyfrost.oneconfig.internal.ui.compose.BlurRenderer
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.PlayerHeadLoader
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
    private companion object {
        const val SHELL_BLUR_RADIUS = 48f
        const val FULLSCREEN_BLUR_RADIUS = 8f
        const val OPEN_ANIMATION_MS = 250L
        const val CLOSE_ANIMATION_MS = 300L
    }

    @Volatile private var closeRequested = false
    @Volatile private var closeRequestedAt = 0L
    @Volatile private var openedAt = 0L

    private fun markClosed() {
        ShellState.lastClosedAt = System.currentTimeMillis()
    }

    override fun init() {
        ConfigRegistry.loadFrom(ConfigManager.active(), ConfigSource.OC)
        initialTree?.let { ConfigRegistry.registerTree(it, ConfigSource.OC) }

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

        openedAt = System.currentTimeMillis()
        UiSounds.play(UiSoundEvent.OPEN)
        UiSounds.acquireAmbience()
        super.init()
    }

    override fun removed() {
        UiSounds.releaseAmbience()
        super.removed()
    }

    @Suppress("DuplicatedCode")
    //? >= 1.21.10 {
    override fun keyPressed(event: KeyEvent): Boolean {
        val key = event.key
        //? } else {
        /*override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        *///? }
        if (key == InputConstants.KEY_ESCAPE) {
            if (!closeRequested) {
                closeRequested = true
                closeRequestedAt = System.currentTimeMillis()
                markClosed()
                UiSounds.play(UiSoundEvent.CLOSE)
                requestCloseCallback?.invoke()
            }
            return true
        }
        //? >= 1.21.10 {
        return super.keyPressed(event)
        //? } else {
        /*return super.keyPressed(key, scanCode, modifiers)
        *///? }
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (closeRequested && System.currentTimeMillis() - closeRequestedAt >= CLOSE_ANIMATION_MS) {
            Platform.screen().close()
            return
        }
        if (OneConfigConfig.enableBackgroundBlur) BlurRenderer.drawBlur(fullscreenBlurRadius())
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

    /**
     * Resolves the route to open based on the "Opening Behavior" preference:
     * 0 = Mods, 1 = Preferences, 2 = Previous page, 3 = Smart reset (previous page unless idle past "Time before reset").
     */
    private fun resolveOpeningBehaviorRoute(): Any = when (OneConfigConfig.openingBehavior) {
        0 -> ModsGraph
        1 -> PreferencesGraph
        2 -> ShellState.lastRoute ?: ModsGraph
        3 -> {
            val last = ShellState.lastClosedAt
            val withinWindow = last > 0L &&
                    System.currentTimeMillis() - last <= (OneConfigConfig.timeBeforeReset * 1000f).toLong()
            if (withinWindow) ShellState.lastRoute ?: ModsGraph else ModsGraph
        }
        else -> ModsGraph
    }

    /** Holds a reference to the close-animation trigger from Compose */
    private var requestCloseCallback: (() -> Unit)? = null

    @Composable
    override fun compose() {
        val initialRoute = when {
            initialTreeId != null -> ModConfigRoute(initialTreeId, initialCategory)
            else -> resolveOpeningBehaviorRoute()
        }

        OneConfigInterface(
            client.window.screenWidth.toFloat(),
            client.window.screenHeight.toFloat(),
            initialRoute = initialRoute,
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
        ) { windowOffset ->
            if (OneConfigConfig.enableWindowBlur) {
                drawIntoCanvas { canvas ->
                    BlurRenderer.drawRegion(
                        canvas.skiaCanvas,
                        windowOffset.x,
                        windowOffset.y,
                        size.width,
                        size.height,
                        SHELL_BLUR_RADIUS
                    )
                }
            }
        }
    }
}
