package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.api.ui.v1.OneConfigUIBridge
import org.polyfrost.oneconfig.internal.ui.poly.ShellScreen

class OneConfigUIBridgeImpl : OneConfigUIBridge {
    override fun createScreen(route: Any?): Any = ShellScreen.forRoute(route)

    override fun open(route: Any?) {
        if (route == null) ShellScreen.openLastSession() else ShellScreen.show(route)
    }
}
