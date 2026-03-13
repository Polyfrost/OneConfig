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

class OneConfigUIScreen @JvmOverloads constructor(
    private val initialTreeId: String? = null,
    private val initialCategory: String? = null,
    private val initialTree: Tree? = null,
) : ComposeScreen() {
    private companion object {
        const val SHELL_BLUR_RADIUS = 24f
    }

    override fun onInitialize(width: Int, height: Int) {
        ConfigRegistry.loadFrom(ConfigManager.active(), ConfigSource.OC)
        initialTree?.let { ConfigRegistry.registerTree(it, ConfigSource.OC) }
        super.onInitialize(width, height)
    }

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        BlurRenderer.drawBlur()
        super.onRender(ctx, mouseX, mouseY, tickDelta)
    }

    @Composable
    override fun compose() {
        val initialRoute =
            if (initialTreeId != null) ModConfigRoute(initialTreeId, initialCategory)
            else org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph

        OneConfigInterface(
            client.window.screenWidth.toFloat(),
            client.window.screenHeight.toFloat(),
            initialRoute = initialRoute
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