package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph

class ShellController {
    val session = ShellSession()
    val search = ShellSearch()
    val appearance = ShellAppearance()
    val layout = ShellLayout()
    val transitions = ShellTransitions()
    val scroll = ShellScroll()
    val navigation = ShellNavigation(search, transitions)

    companion object {
        @JvmStatic
        val shared: ShellController = ShellController()
    }
}

val LocalShell = staticCompositionLocalOf { ShellController.shared }

val LocalShellNavController = staticCompositionLocalOf<NavHostController> { error("No NavHostController provided") }

class ShellSession : LifecycleOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry.createUnsafe(this)

    override val lifecycle: Lifecycle get() = registry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    var playerName by mutableStateOf("Player")
        private set

    /** PNG bytes for the local player's head avatar or null until loaded */
    var playerHeadPng by mutableStateOf<ByteArray?>(null)
        private set

    var versionLabel by mutableStateOf("")
        private set

    /** Wall-clock time in ms the menu was last closed used by the "Smart reset" opening behavior */
    var lastClosedAt: Long = 0L
        private set

    fun updatePlayerName(name: String) {
        playerName = name
    }

    fun updatePlayerHead(png: ByteArray?) {
        playerHeadPng = png
    }

    fun updateVersionLabel(label: String) {
        versionLabel = label
    }

    fun markClosed(at: Long) {
        lastClosedAt = at
    }

    fun resume() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.CREATED
    }
}

class ShellSearch {
    private val byEntry = mutableStateMapOf<String, String>()

    private var entryId: String? = null

    var query by mutableStateOf("")
        private set

    /** Applied once by the search field then cleared */
    var focusRequest by mutableStateOf<SearchFocus?>(null)
        private set

    /** Mirrors the search field's focus so a reopened or rebuilt menu can put it back */
    var fieldFocused: Boolean = false
        private set

    fun update(text: String) {
        query = text
        entryId?.let { byEntry[it] = text }
    }

    fun clear() = update("")

    fun queryFor(entryId: String?): String =
        if (entryId == null || entryId == this.entryId) query else byEntry[entryId] ?: ""

    fun requestFocus(request: SearchFocus) {
        focusRequest = request
    }

    fun consumeFocusRequest() {
        focusRequest = null
    }

    fun updateFieldFocused(focused: Boolean) {
        fieldFocused = focused
    }

    internal fun bind(entryId: String?, text: String) {
        this.entryId = entryId
        query = text
        if (entryId != null) byEntry[entryId] = text
    }

    internal fun forgetEntries() {
        byEntry.clear()
        entryId = null
    }
}

class ShellAppearance {
    var flipTopOptionOrder by mutableStateOf(false)
        private set

    /** Content-panel background opacity from 0 to 100 mirrored from config so slider changes apply live */
    var pageOpacity by mutableStateOf(88f)
        private set

    /** Sidebar background opacity from 0 to 100 mirrored from config so slider changes apply live */
    var sidebarOpacity by mutableStateOf(80f)
        private set

    /** Accent glow opacity from 0 to 100 for the two blurred circles behind the shell */
    var glowOpacity by mutableStateOf(25f)
        private set

    fun update(flipTopOptionOrder: Boolean, pageOpacity: Float, sidebarOpacity: Float, glowOpacity: Float) {
        this.flipTopOptionOrder = flipTopOptionOrder
        this.pageOpacity = pageOpacity
        this.sidebarOpacity = sidebarOpacity
        this.glowOpacity = glowOpacity
    }
}

class ShellLayout {
    var hudDragging by mutableStateOf(false)
        private set

    /**
     * True while a mod card is being dragged (or settling back) so the content pane can drop its
     * clip and let the overlay card draw outside the grid
     */
    var modDragging by mutableStateOf(false)
        private set

    var shellBounds by mutableStateOf<Rect?>(null)
        private set

    fun markHudDragging(dragging: Boolean) {
        hudDragging = dragging
    }

    fun markModDragging(dragging: Boolean) {
        modDragging = dragging
    }

    fun updateShellBounds(bounds: Rect?) {
        shellBounds = bounds
    }
}

class ShellTransitions {
    /**
     * True while the menu is composed but has not yet navigated to the page it is opening on
     *
     * The nav host must be composed before it can be navigated so it briefly holds the mod grid and this
     * keeps that placeholder from being drawn
     */
    var awaitingInitialRoute by mutableStateOf(false)
        private set

    var animatePages: Boolean = false
        private set

    /**
     * True while the most recent navigation was arrow-back so the shared enter/exit transition
     * can mirror the old pop direction. Plain field on purpose: it is only read by NavHost
     * transition lambdas at navigation time, and must not itself trigger recomposition.
     */
    var lastNavWasBack: Boolean = false
        private set

    fun animates(): Boolean = !awaitingInitialRoute && animatePages

    fun beginOpening() {
        awaitingInitialRoute = true
        animatePages = false
    }

    fun finishOpening() {
        awaitingInitialRoute = false
    }

    internal fun prepare(animate: Boolean, back: Boolean) {
        animatePages = animate
        lastNavWasBack = back
    }
}

class ShellScroll {
    /** Where each scrollable page was left keyed by page key see [rememberRestorableLazyListState] */
    private val anchors = HashMap<String, ScrollAnchor>()

    private val grids = HashMap<String, LazyGridState>()

    fun anchor(key: String): ScrollAnchor? = anchors[key]

    fun saveAnchor(key: String, anchor: ScrollAnchor) {
        anchors[key] = anchor
    }

    fun grid(key: String): LazyGridState? = grids[key]

    fun attachGrid(key: String, state: LazyGridState) {
        grids[key] = state
    }

    fun detachGrid(key: String, state: LazyGridState) {
        if (grids[key] === state) grids.remove(key)
    }
}

class ShellNavigation internal constructor(
    private val search: ShellSearch,
    private val transitions: ShellTransitions,
) {
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

    private var host: NavHostController? = null

    private val backStack = ArrayDeque<Entry>()
    private val forwardStack = ArrayDeque<Entry>()

    // Bumped on every history mutation so composition can observe the (non-snapshot) deques
    // below; the deques stay ArrayDeque and remain the single source of truth.
    private var historyRevisionState by mutableStateOf(0)

    /** Read during composition to subscribe to history changes, then query [canGoBack]/[canGoForward] */
    val historyRevision: Int get() = historyRevisionState

    /** Last selected settings tab per page key restored when a page is reopened or navigated back to */
    private val selectedCategories = mutableStateMapOf<String, String>()

    /** Last top-level route navigated to used by the "Previous page" and "Smart reset" opening behaviors */
    var lastRoute: Any? = null
        private set

    private var currentEntry = Entry(ModsGraph)

    val currentRoute: Any get() = currentEntry.route

    /** True once a nav host is attached meaning the OC UI is open and navigable */
    val isReady: Boolean get() = host?.currentDestination != null

    fun attach(controller: NavHostController) {
        if (host === controller) return
        host = controller
        reset()
    }

    fun detach(controller: NavHostController) {
        if (host === controller) host = null
    }

    fun recordLastRoute(route: Any?) {
        lastRoute = route
    }

    fun selectedCategory(pageKey: String): String? = selectedCategories[pageKey]

    fun canGoBack(): Boolean = backStack.isNotEmpty()

    fun canGoForward(): Boolean = forwardStack.isNotEmpty()

    private fun updateArrows() {
        historyRevisionState++
    }

    fun reset() {
        backStack.clear()
        forwardStack.clear()
        currentEntry = Entry(ModsGraph)
        search.forgetEntries()
        updateArrows()
    }

    private fun readyHost(): NavHostController? = if (isReady) host else null

    /** [clearSearch] is off when the navigation only restores a page the user was already on */
    fun navigate(route: Any, clearSearch: Boolean = true, animate: Boolean = OneConfigConfig.showPageAnimations) {
        if (route == currentEntry.route) return
        val host = readyHost() ?: return
        val query = if (clearSearch) "" else search.query
        forwardStack.clear()
        backStack.addLast(currentEntry.withCurrentSearch())
        currentEntry = Entry(route, searchQuery = query)
        lastRoute = route
        transitions.prepare(animate, back = false)
        seedRouteCategory(route)
        updateArrows()
        host.navigate(route)
        search.bind(host.currentBackStackEntry?.id, query)
    }

    /**
     * Records an in-page settings tab switch so back and forward walk tabs the same way they walk pages
     *
     * [pageKey] identifies the page whose tab changed such as a mod id or "preferences"
     */
    fun selectCategory(pageKey: String, category: String) {
        val previous = selectedCategories[pageKey]
        if (previous == category) return
        forwardStack.clear()
        backStack.addLast(currentEntry.copy(categoryKey = pageKey, category = previous))
        currentEntry = currentEntry.copy(categoryKey = pageKey, category = category)
        selectedCategories[pageKey] = category
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
        val host = readyHost() ?: return
        // Route back through navigate() instead of popBackStack(): this NavHost version never
        // plays its pop transitions from a resting state, while the forward enter/exit
        // transition reliably plays (matching the existing forward() path below). The shared
        // transition in Shell mirrors the old pop direction while lastNavWasBack is set.
        // This leaves the host's own back stack growing by an entry per back press. Don't trim it with
        // popUpTo here: popping the outgoing entry drops its content mid-transition, so the page snaps
        // instead of animating. The host is rebuilt every time the menu opens, which bounds the growth
        // to one session; this wrapper's stacks are the real history.
        transitions.prepare(OneConfigConfig.showPageAnimations, back = true)
        runCatching { host.navigate(previous.route) }.onFailure {
            transitions.prepare(OneConfigConfig.showPageAnimations, back = false)
            return
        }
        forwardStack.addLast(currentEntry.withCurrentSearch())
        currentEntry = backStack.removeLast()
        applyCategory(currentEntry)
        search.bind(host.currentBackStackEntry?.id, currentEntry.searchQuery)
        lastRoute = currentEntry.route
        updateArrows()
    }

    fun forward() {
        val next = forwardStack.lastOrNull() ?: return
        val samePage = next.route == currentEntry.route
        val host = if (samePage) null else (readyHost() ?: return)
        forwardStack.removeLast()
        backStack.addLast(currentEntry.withCurrentSearch())
        currentEntry = next
        if (host != null) seedRouteCategory(next.route)
        applyCategory(next)
        if (host == null) {
            updateArrows()
            return
        }
        lastRoute = next.route
        transitions.prepare(OneConfigConfig.showPageAnimations, back = false)
        updateArrows()
        host.navigate(next.route)
        search.bind(host.currentBackStackEntry?.id, next.searchQuery)
    }

    /** A route that names a category such as an external deep link overrides the remembered tab */
    private fun seedRouteCategory(route: Any) {
        val modConfig = route as? ModConfigRoute ?: return
        val category = modConfig.category ?: return
        selectedCategories[modConfig.id] = category
    }

    /** Restores the tab an entry was recorded with where a null category means the page default */
    private fun applyCategory(entry: Entry) {
        val key = entry.categoryKey ?: return
        if (entry.category == null) selectedCategories.remove(key)
        else selectedCategories[key] = entry.category
    }

    private fun Entry.withCurrentSearch() = copy(searchQuery = search.query)
}

@Composable
fun pageSearchQuery(): String {
    val entry = LocalLifecycleOwner.current as? NavBackStackEntry
    return LocalShell.current.search.queryFor(entry?.id)
}
