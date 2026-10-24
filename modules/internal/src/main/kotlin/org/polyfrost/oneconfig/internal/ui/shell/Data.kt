package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignSession
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus

object HudEditorRoute

enum class PolyPage(val title: String, val icon: String) {
    Mods("Mods", "settings"),
    Profiles("Profiles", "profiles"),
    Keybinds("Keybinds", "keyboard"),
    Themes("Themes", "paintbrush"),
    Preferences("Preferences", "cog"),
    Changelog("Changelog", "refresh"),

    ChangelogEntry("Changelog", "refresh"),
    Credits("Credits", "star"),

    ModConfig("Mods", "settings"),

    Page("", ""),
}

data class ModConfigTarget(val mod: ConfigData, val category: String?)

object ShellState {
    var playerName by mutableStateOf("Player")

    /** PNG bytes for the local player's head avatar or null until loaded */
    var playerHeadPng by mutableStateOf<ByteArray?>(null)

    var versionLabel by mutableStateOf("")

    var hudDragging by mutableStateOf(false)

    var modDragging by mutableStateOf(false)

    var flipTopOptionOrder by mutableStateOf(false)

    /** Content-panel background opacity from 0 to 100 mirrored from config so slider changes apply live */
    var pageOpacity by mutableStateOf(88f)

    /** Sidebar background opacity from 0 to 100 mirrored from config so slider changes apply live */
    var sidebarOpacity by mutableStateOf(80f)

    /** Accent glow opacity from 0 to 100 for the two blurred circles behind the shell */
    var glowOpacity by mutableStateOf(25f)

    var lastPage: Pair<PolyPage, Any?>? = null

    /** Last selected settings tab per page key restored when a page is reopened or navigated back to */
    val selectedCategories = mutableStateMapOf<String, String>()

    /** Wall-clock time in ms the menu was last closed used by the "Smart reset" opening behavior */
    var lastClosedAt: Long = 0L

    var lastWasHudEditor = false

    var lastSearch = ""
    var lastSearchFocused = false

    internal var searchField: TextFieldState? = null

    var searchQuery: String
        get() = searchField?.text ?: ""
        set(value) {
            val field = searchField ?: return
            field.replaceText(value)
            field.moveCaret(value.length)
        }

    @JvmStatic
    fun handleSearchKeybind(pressed: Boolean): Boolean {
        if (KeybindRecordingBus.isRecording) return false
        if (HudDesignSession.handleSearch(pressed)) return true
        if (!pressed || !HudManager.isConfigUiOpen) return false
        val field = searchField ?: return false
        field.requestFocus(selectAll = true)
        return true
    }
}

class ShellNav(start: PolyPage, startArg: Any? = null) {
    private class Entry(val page: PolyPage, val arg: Any?) {
        var search = ""
    }

    private val backStack = mutableStateListOf<Entry>()
    private val forwardStack = mutableStateListOf<Entry>()

    fun canGoBack(): Boolean = backStack.isNotEmpty()

    fun canGoForward(): Boolean = forwardStack.isNotEmpty()

    private var currentEntry by mutableStateOf(Entry(start, startArg))

    val current: PolyPage get() = currentEntry.page

    val arg: Any? get() = currentEntry.arg

    val search: String get() = currentEntry.search

    var currentSearch: () -> String = { "" }

    fun restoreSearch(text: String) {
        currentEntry.search = text
    }

    var direction = 1
        private set

    fun navigate(page: PolyPage, arg: Any? = null) {
        if (page == current && arg == this.arg) return
        forwardStack.clear()
        backStack.add(currentEntry.withCurrentSearch())
        direction = 1
        currentEntry = Entry(page, arg)
    }

    fun tab(from: Any?, to: Any?) {
        currentEntry = Entry(currentEntry.page, from)
        navigate(currentEntry.page, to)
    }

    fun back() {
        val previous = backStack.removeLastOrNull() ?: return
        forwardStack.add(currentEntry.withCurrentSearch())
        direction = -1
        currentEntry = previous
    }

    fun forward() {
        val next = forwardStack.removeLastOrNull() ?: return
        backStack.add(currentEntry.withCurrentSearch())
        direction = 1
        currentEntry = next
    }

    fun discard(page: PolyPage, arg: Any?, fallback: PolyPage) {
        backStack.removeAll { it.page == page && it.arg == arg }
        forwardStack.removeAll { it.page == page && it.arg == arg }
        if (current == page && this.arg == arg) {
            direction = 1
            currentEntry = Entry(fallback, null)
        }
    }

    private fun Entry.withCurrentSearch() = also { it.search = currentSearch() }
}
