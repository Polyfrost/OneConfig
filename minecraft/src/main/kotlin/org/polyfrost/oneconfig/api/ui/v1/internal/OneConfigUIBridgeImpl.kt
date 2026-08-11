package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUIBridge
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController

class OneConfigUIBridgeImpl : OneConfigUIBridge {
    override fun createScreen(route: Any?): Any = OneConfigUIScreen.forRoute(route)

    override fun open(route: Any?) {
        if (route == null) {
            OneConfigUIScreen.openLastSession()
            return
        }
        // isReady stays true after the menu closes so the screen check is what tells open from closed
        if (LocalNavController.isReady && Platform.screen().current<Any?>() is OneConfigUIScreen) {
            LocalNavController.wrapper.navigate(route)
        } else {
            Platform.screen().display(OneConfigUIScreen.forRoute(route))
        }
    }
}
