package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.foundation.lazy.grid.LazyGridState
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

enum class SearchFocus {
    SelectAll,

    /** Focuses the search field and leaves its text and cursor alone */
    Focus,
    Unfocus,
}

object Lifecycle : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this)
}

object OCViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

object ShellState {
    var playerName by mutableStateOf("Player")

    /** PNG bytes for the local player's head avatar or null until loaded */
    var playerHeadPng by mutableStateOf<ByteArray?>(null)

    var versionLabel by mutableStateOf("")

    var searchQuery by mutableStateOf("")

    /** Applied once by the search field then cleared */
    var searchFocus by mutableStateOf<SearchFocus?>(null)

    /** Mirrors the search field's focus so a reopened or rebuilt menu can put it back */
    var searchFieldFocused: Boolean = false

    var hudDragging by mutableStateOf(false)

    /**
     * True while a mod card is being dragged (or settling back) so the content pane can drop its
     * clip and let the overlay card draw outside the grid
     */
    var modDragging by mutableStateOf(false)

    var shellBounds by mutableStateOf<Rect?>(null)

    var flipTopOptionOrder by mutableStateOf(false)

    /** Content-panel background opacity from 0 to 100 mirrored from config so slider changes apply live */
    var pageOpacity by mutableStateOf(88f)

    /** Sidebar background opacity from 0 to 100 mirrored from config so slider changes apply live */
    var sidebarOpacity by mutableStateOf(80f)

    /** Accent glow opacity from 0 to 100 for the two blurred circles behind the shell */
    var glowOpacity by mutableStateOf(25f)

    /** Last top-level route navigated to used by the "Previous page" and "Smart reset" opening behaviors */
    var lastRoute: Any? = null

    /** Where each scrollable page was left keyed by page key see [rememberRestorableLazyListState] */
    val scrollAnchors = mutableMapOf<String, ScrollAnchor>()

    val gridStates = mutableMapOf<String, LazyGridState>()

    /** Last selected settings tab per page key restored when a page is reopened or navigated back to */
    val selectedCategories = mutableStateMapOf<String, String>()

    /** Wall-clock time in ms the menu was last closed used by the "Smart reset" opening behavior */
    var lastClosedAt: Long = 0L

    /** Whether the initial page transition on open is animated */
    var animateOpeningPage: Boolean = false

    /** Consumed once per open so the first page transition can be treated specially */
    var initialTransitionConsumed: Boolean = false

    /**
     * True while the menu is composed but has not yet navigated to the page it is opening on
     *
     * The nav host must be composed before it can be navigated so it briefly holds the mod grid and this
     * keeps that placeholder from being drawn
     */
    var awaitingInitialRoute by mutableStateOf(false)

    var openingTransitionTarget: String? = null

    /**
     * True while the most recent navigation was arrow-back so the shared enter/exit transition
     * can mirror the old pop direction. Plain field on purpose: it is only read by NavHost
     * transition lambdas at navigation time, and must not itself trigger recomposition.
     */
    var lastNavWasBack: Boolean = false
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

    /** True once a nav host is attached meaning the OC UI is open and navigable */
    val isReady: Boolean get() = _current?.currentDestination != null

    val wrapper = NavControllerWrapper

    object NavControllerWrapper {
        /**
         * One step of history
         *
         * [categoryKey] and [category] record the settings tab selected on the page at that point so
         * switching tabs inside a page is undoable without touching the nav host
         *
         * [searchQuery] records the page's search when it was left so going back or forward to it brings the
         * search back
         */
        private data class Entry(
            val route: Any,
            val categoryKey: String? = null,
            val category: String? = null,
            val searchQuery: String = "",
        )

        private val backStack = ArrayDeque<Entry>()
        private val forwardStack = ArrayDeque<Entry>()

        // Bumped on every history mutation so composition can observe the (non-snapshot) deques
        // below; the deques stay ArrayDeque and remain the single source of truth.
        private var historyRevisionState by mutableStateOf(0)

        /** Read during composition to subscribe to history changes, then query [canGoBack]/[canGoForward] */
        val historyRevision: Int get() = historyRevisionState

        private fun updateArrows() {
            historyRevisionState++
        }

        fun canGoBack(): Boolean = backStack.isNotEmpty()

        fun canGoForward(): Boolean = forwardStack.isNotEmpty()

        private var currentEntry = Entry(ModsGraph)

        val currentRoute: Any get() = currentEntry.route

        fun reset() {
            backStack.clear()
            forwardStack.clear()
            currentEntry = Entry(ModsGraph)
            updateArrows()
        }

        private fun host(): NavHostController? = if (isReady) current else null

        /** [clearSearch] is off when the navigation only restores a page the user was already on */
        fun navigate(route: Any, clearSearch: Boolean = true) {
            if (route == currentEntry.route) return
            val host = host() ?: return
            forwardStack.clear()
            backStack.addLast(currentEntry.withCurrentSearch())
            currentEntry = Entry(route)
            if (clearSearch) ShellState.searchQuery = ""
            ShellState.lastRoute = route
            ShellState.lastNavWasBack = false
            seedRouteCategory(route)
            updateArrows()
            host.navigate(route)
        }

        /**
         * Records an in-page settings tab switch so back and forward walk tabs the same way they walk pages
         *
         * [pageKey] identifies the page whose tab changed such as a mod id or "preferences"
         */
        fun selectCategory(pageKey: String, category: String) {
            val previous = ShellState.selectedCategories[pageKey]
            if (previous == category) return
            forwardStack.clear()
            backStack.addLast(currentEntry.copy(categoryKey = pageKey, category = previous))
            currentEntry = currentEntry.copy(categoryKey = pageKey, category = category)
            ShellState.selectedCategories[pageKey] = category
            updateArrows()
        }

        fun back() {
            val previous = backStack.lastOrNull() ?: return
            // same page with only the tab changed so restore it in place with no screen transition
            if (previous.route == currentEntry.route) {
                backStack.removeLast()
                forwardStack.addLast(currentEntry.withCurrentSearch())
                currentEntry = previous
                applyCategory(previous)
                updateArrows()
                return
            }
            val host = host() ?: return
            // Route back through navigate() instead of popBackStack(): this NavHost version never
            // plays its pop transitions from a resting state, while the forward enter/exit
            // transition reliably plays (matching the existing forward() path below). The shared
            // transition in Shell mirrors the old pop direction while lastNavWasBack is set.
            // This leaves the host's own back stack growing by an entry per back press. Don't trim it with
            // popUpTo here: popping the outgoing entry drops its content mid-transition, so the page snaps
            // instead of animating. The host is rebuilt every time the menu opens, which bounds the growth
            // to one session; this wrapper's stacks are the real history.
            ShellState.lastNavWasBack = true
            runCatching { host.navigate(previous.route) }.onFailure {
                ShellState.lastNavWasBack = false
                return
            }
            forwardStack.addLast(currentEntry.withCurrentSearch())
            currentEntry = backStack.removeLast()
            applyCategory(currentEntry)
            ShellState.searchQuery = currentEntry.searchQuery
            ShellState.lastRoute = currentEntry.route
            updateArrows()
        }

        fun forward() {
            val next = forwardStack.lastOrNull() ?: return
            val samePage = next.route == currentEntry.route
            val host = if (samePage) null else (host() ?: return)
            forwardStack.removeLast()
            backStack.addLast(currentEntry.withCurrentSearch())
            currentEntry = next
            if (host != null) seedRouteCategory(next.route)
            applyCategory(next)
            if (host == null) {
                updateArrows()
                return
            }
            ShellState.searchQuery = next.searchQuery
            ShellState.lastRoute = next.route
            ShellState.lastNavWasBack = false
            updateArrows()
            host.navigate(next.route)
        }

        /** A route that names a category such as an external deep link overrides the remembered tab */
        private fun seedRouteCategory(route: Any) {
            val modConfig = route as? ModConfigRoute ?: return
            val category = modConfig.category ?: return
            ShellState.selectedCategories[modConfig.id] = category
        }

        /** Restores the tab an entry was recorded with where a null category means the page default */
        private fun applyCategory(entry: Entry) {
            val key = entry.categoryKey ?: return
            if (entry.category == null) ShellState.selectedCategories.remove(key)
            else ShellState.selectedCategories[key] = entry.category
        }

        private fun Entry.withCurrentSearch() = copy(searchQuery = ShellState.searchQuery)
    }
}
