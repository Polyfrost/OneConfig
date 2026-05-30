package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphics
//? >= 1.21.10
import net.minecraft.client.input.KeyEvent
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.OneConfigInterface
import org.polyfrost.oneconfig.internal.ui.compose.BlurRenderer
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.api.platform.v1.Platform

class OneConfigUIScreen @JvmOverloads constructor(
    private val initialTreeId: String? = null,
    private val initialCategory: String? = null,
    private val initialTree: Tree? = null,
) : ComposeScreen() {
    private companion object {
        const val SHELL_BLUR_RADIUS = 48f
        const val CLOSE_ANIMATION_MS = 220L
    }

    @Volatile private var closeRequested = false
    @Volatile private var closeRequestedAt = 0L

    override fun init() {
        ConfigRegistry.loadFrom(ConfigManager.active(), ConfigSource.OC)
        initialTree?.let { ConfigRegistry.registerTree(it, ConfigSource.OC) }

        try {
            ShellState.playerName = net.minecraft.client.Minecraft.getInstance().user.name
        } catch (_: Throwable) {
            ShellState.playerName = "Player"
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

        super.init()
    }

    @Suppress("DuplicatedCode")
    //? >= 1.21.10 {
    override fun keyPressed(event: KeyEvent): Boolean {
        val key = event.key
        //? } else {
        //override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        //? }
        if (key == InputConstants.KEY_ESCAPE) {
            if (!closeRequested) {
                closeRequested = true
                closeRequestedAt = System.currentTimeMillis()
                requestCloseCallback?.invoke()
            }
            return true
        }
        //? >= 1.21.10 {
        return super.keyPressed(event)
        //? } else {
        //return super.keyPressed(key, scanCode, modifiers)
        //? }
    }

    override fun render(ctx: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (closeRequested && System.currentTimeMillis() - closeRequestedAt >= CLOSE_ANIMATION_MS) {
            Platform.screen().close()
            return
        }
        BlurRenderer.drawBlur()
        super.render(ctx, mouseX, mouseY, tickDelta)
    }

    /** Holds a reference to the close-animation trigger from Compose */
    private var requestCloseCallback: (() -> Unit)? = null

    @Composable
    override fun compose() {
        val initialRoute =
            if (initialTreeId != null) ModConfigRoute(initialTreeId, initialCategory)
            else org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph

        OneConfigInterface(
            client.window.screenWidth.toFloat(),
            client.window.screenHeight.toFloat(),
            initialRoute = initialRoute,
            onCloseRequest = {
                closeRequested = true
                closeRequestedAt = 0L
            },
            onCloseReady = { closeRequest ->
                requestCloseCallback = closeRequest
            },
        ) { windowOffset ->
            drawIntoCanvas { canvas ->
                BlurRenderer.drawRegion(
                    canvas.nativeCanvas,
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