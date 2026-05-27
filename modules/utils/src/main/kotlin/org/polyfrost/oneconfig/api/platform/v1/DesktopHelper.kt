package org.polyfrost.oneconfig.api.platform.v1

import java.awt.Desktop
import java.net.URI

object DesktopHelper {

    @JvmStatic
    fun browse(uri: URI): Boolean {
        return executeIfDesktop(Desktop.Action.BROWSE) {
            it.browse(uri)
        }
    }

    fun executeIfDesktop(action: Desktop.Action, runnable: (Desktop) -> Unit): Boolean = runCatching {
        if (!Desktop.isDesktopSupported()) return false

        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(action)) return false
        runnable(desktop)
        return true
    }.getOrElse { false }

}