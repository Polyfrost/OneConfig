package org.polyfrost.oneconfig.api.platform.v1

import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Taken from UniversalCraft under LGPL-3.0.
 *
 * [LICENSE](https://github.com/SparkUniverse/UniversalCraft/blob/master/LICENSE)
 */
object DesktopHelper {

    @JvmStatic
    var isLinux: Boolean = false
        private set

    @JvmStatic
    var isXdg: Boolean = false
        private set

    @JvmStatic
    var isKde: Boolean = false
        private set

    @JvmStatic
    var isGnome: Boolean = false
        private set

    @JvmStatic
    var isMac: Boolean = false
        private set

    @JvmStatic
    var isWindows: Boolean = false
        private set

    init {
        val osName = try {
            System.getProperty("os.name")
        } catch (_: SecurityException) {
            null
        }

        isLinux = osName != null && (osName.startsWith("Linux") || osName.startsWith("LINUX"))
        isMac = osName != null && osName.startsWith("Mac")
        isWindows = osName != null && osName.startsWith("Windows")

        if (isLinux) {
            System.getenv("XDG_SESSION_ID")?.let { isXdg = it.isNotEmpty() }
            System.getenv("GDMSESSION")?.lowercase()?.let {
                isGnome = "gnome" in it
                isKde = "kde" in it
            }
        } else {
            isXdg = false
            isKde = false
            isGnome = false
        }
    }

    @JvmStatic
    fun browse(uri: URI): Boolean = browseDesktop(uri) || openSystemSpecific(uri.toString())

    @JvmStatic
    fun open(file: File): Boolean = openDesktop(file) || openSystemSpecific(file.path)

    @JvmStatic
    fun edit(file: File): Boolean = editDesktop(file) || openSystemSpecific(file.path)

    @Suppress("unused")
    @Deprecated("Use browse(URI), open(File), edit(File) instead", ReplaceWith("browse(uri) || open(file) || edit(file)"),
        DeprecationLevel.HIDDEN)
    fun executeIfDesktop(action: Desktop.Action, runnable: (Desktop) -> Unit) = false

    private fun openSystemSpecific(file: String): Boolean {
        return when {
            isLinux -> listOf("xdg-open", "kde-open", "gnome-open").any { runCommand(it, file, checkExitStatus = true) }
            isMac -> runCommand("open", file)
            isWindows -> runCommand("rundll32", "url.dll,FileProtocolHandler", file)
            else -> false
        }
    }

    private fun browseDesktop(uri: URI): Boolean {
        return if (!Desktop.isDesktopSupported()) false else try {
            if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) return false
            Desktop.getDesktop().browse(uri)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun openDesktop(file: File): Boolean {
        return if (!Desktop.isDesktopSupported()) false else try {
            if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) return false
            Desktop.getDesktop().open(file)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun editDesktop(file: File): Boolean {
        return if (!Desktop.isDesktopSupported()) false else try {
            if (!Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) return false
            Desktop.getDesktop().edit(file)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun runCommand(vararg command: String, checkExitStatus: Boolean = false): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(command) ?: return false
            if (checkExitStatus) {
                if (process.waitFor(3, TimeUnit.SECONDS)) {
                    process.exitValue() == 0
                } else {
                    true
                }
            } else {
                process.isAlive
            }
        } catch (_: IOException) {
            false
        }
    }

}
