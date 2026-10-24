package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf

class ShellController {
    val session = ShellSession()
    val search = ShellSearch()
    val appearance = ShellAppearance()
    val layout = ShellLayout()

    companion object {
        @JvmStatic
        val shared: ShellController = ShellController()
    }
}

val LocalShell = staticCompositionLocalOf { ShellController.shared }

class ShellSession {
    val playerName: String get() = ShellState.playerName

    val playerHeadPng: ByteArray? get() = ShellState.playerHeadPng

    val versionLabel: String get() = ShellState.versionLabel

    val lastClosedAt: Long get() = ShellState.lastClosedAt

    fun updatePlayerName(name: String) {
        ShellState.playerName = name
    }

    fun updatePlayerHead(png: ByteArray?) {
        ShellState.playerHeadPng = png
    }

    fun updateVersionLabel(label: String) {
        ShellState.versionLabel = label
    }

    fun markClosed(at: Long) {
        ShellState.lastClosedAt = at
    }
}

class ShellSearch {
    val query: String get() = ShellState.searchQuery

    fun update(text: String) {
        ShellState.searchQuery = text
    }

    fun clear() = update("")
}

class ShellAppearance {
    val flipTopOptionOrder: Boolean get() = ShellState.flipTopOptionOrder

    val pageOpacity: Float get() = ShellState.pageOpacity

    val sidebarOpacity: Float get() = ShellState.sidebarOpacity

    val glowOpacity: Float get() = ShellState.glowOpacity

    fun update(flipTopOptionOrder: Boolean, pageOpacity: Float, sidebarOpacity: Float, glowOpacity: Float) {
        ShellState.flipTopOptionOrder = flipTopOptionOrder
        ShellState.pageOpacity = pageOpacity
        ShellState.sidebarOpacity = sidebarOpacity
        ShellState.glowOpacity = glowOpacity
    }
}

class ShellLayout {
    val hudDragging: Boolean get() = ShellState.hudDragging

    val modDragging: Boolean get() = ShellState.modDragging

    fun markHudDragging(dragging: Boolean) {
        ShellState.hudDragging = dragging
    }

    fun markModDragging(dragging: Boolean) {
        ShellState.modDragging = dragging
    }
}
