package org.polyfrost.oneconfig.api.ui.v1.internal

import java.util.concurrent.atomic.AtomicBoolean
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUIBridge
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPage
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPages
import org.polyfrost.oneconfig.internal.OneConfig
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.polyfrost.oneconfig.internal.ui.navigation.PageArguments
import org.polyfrost.oneconfig.internal.ui.navigation.PageRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.pageRoute
import org.polyfrost.oneconfig.internal.ui.navigation.resolveOpenTarget
import org.polyfrost.oneconfig.internal.ui.shell.ShellController

private val LOGGER = LogManager.getLogger("OneConfig/UI")

class OneConfigUIBridgeImpl : OneConfigUIBridge {
    override fun createScreen(route: Any?): Any = OneConfigUIScreen.forRoute(resolveOpenTarget(route))

    override fun open(route: Any?) {
        val target = resolveOpenTarget(route)
        if (target == null) {
            OneConfigUIScreen.openLastSession()
            return
        }
        // isReady stays true after the menu closes so the screen check is what tells open from closed
        val navigation = ShellController.shared.navigation
        if (navigation.isReady && Platform.screen().current<Any?>() is OneConfigUIScreen) {
            try {
                navigation.navigate(target)
            } catch (t: Throwable) {
                LOGGER.error("Cannot open {} in the OneConfig UI, opening the mods page instead", target, t)
                navigation.navigate(ModsGraph)
            }
        } else {
            Platform.screen().display(OneConfigUIScreen.forRoute(target))
        }
    }

    @OptIn(ExperimentalOneConfigUiApi::class)
    override fun openPage(page: OneConfigPage, argument: Any?) {
        if (argument != null && OneConfigPages.byId(page.id) === page && pageRoute(page) == PageRoute(page.id)) {
            PageArguments.offer(page.id, argument)
        }
        open(page)
    }

    @OptIn(ExperimentalOneConfigUiApi::class)
    override fun takePageArgument(): Any? = PageArguments.take()

    override fun isOneConfigScreen(screen: Any?): Boolean = screen is OneConfigUIScreen

    private val startupChecked = AtomicBoolean(false)

    @OptIn(ExperimentalOneConfigUiApi::class)
    @Suppress("SENSELESS_COMPARISON")
    override fun ensureInitialized() {
        if (Minecraft.getInstance() == null || !startupChecked.compareAndSet(false, true)) return
        try {
            Class.forName(ComposeScreen::class.java.name, true, ComposeScreen::class.java.classLoader)
        } catch (t: Throwable) {
            LOGGER.warn("Failed to arm the OneConfig screen input handlers", t)
        }
        Minecraft.getInstance().schedule(Runnable { finishSkippedStartup() })
    }

    private fun finishSkippedStartup() {
        if (OneConfig.isInitialized()) return
        LOGGER.warn("OneConfig did not start with the game; starting it now so its screens work")
        try {
            OneConfig.INSTANCE.onInitializeClient()
        } catch (t: Throwable) {
            LOGGER.error("OneConfig failed to start late", t)
        }
    }
}
