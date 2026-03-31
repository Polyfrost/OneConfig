package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import org.polyfrost.oneconfig.internal.ui.OneConfigInterface
import org.polyfrost.oneconfig.internal.ui.compose.BlurRenderer
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.api.platform.v1.Platform
import dev.deftu.omnicore.api.client.input.KeyboardModifiers
import dev.deftu.omnicore.api.client.input.OmniKey
import dev.deftu.omnicore.api.client.screen.KeyPressEvent
import org.lwjgl.glfw.GLFW

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

    override fun onInitialize(width: Int, height: Int) {
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

        super.onInitialize(width, height)
    }

    override fun onKeyPress(key: OmniKey, scanCode: Int, typedChar: Char, modifiers: KeyboardModifiers, event: KeyPressEvent): Boolean {
        if (key.code == GLFW.GLFW_KEY_ESCAPE) {
            if (!closeRequested) {
                closeRequested = true
                closeRequestedAt = System.currentTimeMillis()
                requestCloseCallback?.invoke()
            }
            return true
        }
        return super.onKeyPress(key, scanCode, typedChar, modifiers, event)
    }

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (closeRequested && System.currentTimeMillis() - closeRequestedAt >= CLOSE_ANIMATION_MS) {
            Platform.screen().close()
            return
        }
        BlurRenderer.drawBlur()
        super.onRender(ctx, mouseX, mouseY, tickDelta)
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