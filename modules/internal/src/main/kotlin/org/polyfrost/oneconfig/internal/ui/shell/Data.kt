package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavHostController
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph

object HudEditorRoute

object Lifecycle : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this)
}
object OCViewModelStoreOwner: ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
object ShellState {
    var title by mutableStateOf<String?>(null)
    var titleInfoForTitle by mutableStateOf<String?>(null)
    var titleAuthors by mutableStateOf<String?>(null)
    var titleCredits by mutableStateOf<String?>(null)
    var titleVersion by mutableStateOf<String?>(null)

    var openOriginalScreen by mutableStateOf<Runnable?>(null)

    var playerName by mutableStateOf("Player")

    /** PNG bytes for the local player's head avatar, or null until loaded. */
    var playerHeadPng by mutableStateOf<ByteArray?>(null)

    var versionLabel by mutableStateOf("")

    var searchQuery by mutableStateOf("")

    var globalSearchActive by mutableStateOf(false)

    var focusSearchField by mutableStateOf(false)

    /**
     * Whether the search field currently holds focus. Mirrored here so a discarded composition
     * can recover the state.
     */
    var searchFieldFocused: Boolean = false

    var showSearchField by mutableStateOf(false)

    var hudDragging by mutableStateOf(false)

    var shellBounds by mutableStateOf<Rect?>(null)

    var flipTopOptionOrder by mutableStateOf(false)

    /** Content-panel background opacity (0-100), mirrored from config so slider changes apply live. */
    var pageOpacity by mutableStateOf(88f)

    /** Sidebar background opacity (0-100), mirrored from config so slider changes apply live. */
    var sidebarOpacity by mutableStateOf(80f)

    /** Accent glow opacity (0-100) for the two blurred circles behind the shell, mirrored from config. */
    var glowOpacity by mutableStateOf(25f)

    /** Last top-level route navigated to, used by the "Previous page" / "Smart reset" opening behaviors. */
    var lastRoute: Any? = null

    /** Where each scrollable page was left, by page key. See [rememberRestorableLazyListState]. */
    val scrollAnchors = mutableMapOf<String, ScrollAnchor>()

    /** Last selected settings category (tab) per page key, restored when a page is reopened or navigated back to. */
    val selectedCategories = mutableStateMapOf<String, String>()

    /** Wall-clock time (ms) the menu was last closed, used by the "Smart reset" opening behavior. */
    var lastClosedAt: Long = 0L

    /** When true, the initial page transition on open is animated (driven by "Show opening page animation"). */
    var animateOpeningPage: Boolean = false

    /** Consumed once per open so the first page transition can be treated specially. */
    var initialTransitionConsumed: Boolean = false

    /**
     * True while the menu is composed but has not yet navigated to the page it is opening on. The nav host has
     * to be composed before it can be navigated, so it briefly holds the mod grid; this keeps that placeholder
     * from being drawn.
     */
    var awaitingInitialRoute by mutableStateOf(false)

    var openingTransitionTarget: String? = null
}
object LocalNavController {
    private var _current: NavHostController? = null

    var current: NavHostController
        get() = _current!!
        set(value) {
            if (_current !== value) {
                _current = value
                wrapper.reset()
            }
        }

    /** True once a nav host is attached, i.e. the OC UI is open and navigable. */
    val isReady: Boolean get() = _current?.currentDestination != null

    val wrapper = NavControllerWrapper

    object NavControllerWrapper {
        /**
         * One step of history. [categoryKey] / [category] record the settings tab selected on the page at
         * that point, so switching tabs inside a page is undoable without touching the nav host.
         */
        private data class Entry(
            val route: Any,
            val categoryKey: String? = null,
            val category: String? = null,
        )

        private val backStack = ArrayDeque<Entry>()
        private val forwardStack = ArrayDeque<Entry>()

        private var currentEntry = Entry(ModsGraph)

        fun reset() {
            backStack.clear()
            forwardStack.clear()
            currentEntry = Entry(ModsGraph)
        }

        private fun host(): NavHostController? = if (isReady) current else null

        /** [clearSearch] is off when the navigation only restores a page the user was already on. */
        fun navigate(route: Any, clearSearch: Boolean = true) {
            val host = host() ?: return
            forwardStack.clear()
            backStack.addLast(currentEntry)
            currentEntry = Entry(route)
            if (clearSearch) {
                ShellState.searchQuery = ""
                ShellState.globalSearchActive = false
                ShellState.showSearchField = false
            }
            ShellState.lastRoute = route
            seedRouteCategory(route)
            host.navigate(route)
        }

        /**
         * Records an in-page settings tab switch, so back/forward walks tabs the same way it walks pages.
         * [pageKey] identifies the page whose tab changed (mod id, "preferences", ...).
         */
        fun selectCategory(pageKey: String, category: String) {
            val previous = ShellState.selectedCategories[pageKey]
            if (previous == category) return
            forwardStack.clear()
            backStack.addLast(currentEntry.copy(categoryKey = pageKey, category = previous))
            currentEntry = currentEntry.copy(categoryKey = pageKey, category = category)
            ShellState.selectedCategories[pageKey] = category
        }

        fun back() {
            val previous = backStack.lastOrNull() ?: return
            // Same page, only the tab changed: restore it in place, no screen transition.
            if (previous.route == currentEntry.route) {
                backStack.removeLast()
                forwardStack.addLast(currentEntry)
                currentEntry = previous
                applyCategory(previous)
                return
            }
            val host = host() ?: return
            if (host.popBackStack()) {
                forwardStack.addLast(currentEntry)
                currentEntry = backStack.removeLast()
                applyCategory(currentEntry)
                ShellState.lastRoute = currentEntry.route
                ShellState.globalSearchActive = false
                ShellState.showSearchField = false
            }
        }

        fun forward() {
            val next = forwardStack.lastOrNull() ?: return
            val samePage = next.route == currentEntry.route
            val host = if (samePage) null else (host() ?: return)
            forwardStack.removeLast()
            backStack.addLast(currentEntry)
            currentEntry = next
            if (host != null) seedRouteCategory(next.route)
            applyCategory(next)
            if (host == null) return
            ShellState.lastRoute = next.route
            ShellState.globalSearchActive = false
            ShellState.showSearchField = false
            host.navigate(next.route)
        }

        /** A route that names a category (e.g. an external deep link) overrides the remembered tab. */
        private fun seedRouteCategory(route: Any) {
            val modConfig = route as? ModConfigRoute ?: return
            val category = modConfig.category ?: return
            ShellState.selectedCategories[modConfig.id] = category
        }

        /** Restores the tab an entry was recorded with; a null category means "page default". */
        private fun applyCategory(entry: Entry) {
            val key = entry.categoryKey ?: return
            if (entry.category == null) ShellState.selectedCategories.remove(key)
            else ShellState.selectedCategories[key] = entry.category
        }
    }
}
