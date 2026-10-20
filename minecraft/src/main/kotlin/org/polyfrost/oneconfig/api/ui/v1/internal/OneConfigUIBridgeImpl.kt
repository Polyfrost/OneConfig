package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUIBridge
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.polyfrost.oneconfig.internal.ui.navigation.resolveOpenTarget
import org.polyfrost.oneconfig.internal.ui.shell.ShellController

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
            navigation.navigate(target)
        } else {
            Platform.screen().display(OneConfigUIScreen.forRoute(target))
        }
    }

    override fun isOneConfigScreen(screen: Any?): Boolean = screen is OneConfigUIScreen
}
