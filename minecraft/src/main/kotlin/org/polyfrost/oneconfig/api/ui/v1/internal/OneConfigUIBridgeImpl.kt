package org.polyfrost.oneconfig.api.ui.v1.internal

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUIBridge
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
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

    override fun isOneConfigScreen(screen: Any?): Boolean = screen is OneConfigUIScreen
}
