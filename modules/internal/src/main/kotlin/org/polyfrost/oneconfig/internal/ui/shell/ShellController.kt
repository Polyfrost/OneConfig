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
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph

private val LOGGER = LogManager.getLogger("OneConfig/UI")

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

    var playerHeadPng by mutableStateOf<ByteArray?>(null)
        private set

    var versionLabel by mutableStateOf("")
        private set

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

    var focusRequest by mutableStateOf<SearchFocus?>(null)
        private set

    var fieldFocused: Boolean = false
        private set

    fun update(text: String) {
        query = text
        entryId?.let { byEntry[it] = text }
    }

    fun clear() = update("")

    fun queryFor(entryId: String?): String =
        if (entryId == null || this.entryId == null || entryId == this.entryId) query else byEntry[entryId] ?: ""

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

    var pageOpacity by mutableStateOf(88f)
        private set

    var sidebarOpacity by mutableStateOf(80f)
        private set

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
    var awaitingInitialRoute by mutableStateOf(false)
        private set

    var animatePages: Boolean = false
        private set

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
    private data class Entry(
        val route: Any,
        val categoryKey: String? = null,
        val category: String? = null,
        val searchQuery: String = "",
    )

    private var host: NavHostController? = null

    private val backStack = ArrayDeque<Entry>()
    private val forwardStack = ArrayDeque<Entry>()

    private var historyRevisionState by mutableStateOf(0)

    val historyRevision: Int get() = historyRevisionState

    private val selectedCategories = mutableStateMapOf<String, String>()

    var lastRoute: Any? = null
        private set

    private var currentEntry = Entry(ModsGraph)

    val currentRoute: Any get() = currentEntry.route

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

    fun navigate(route: Any, clearSearch: Boolean = true, animate: Boolean = OneConfigConfig.showPageAnimations) {
        if (route == currentEntry.route) return
        val host = readyHost() ?: return
        val query = if (clearSearch) "" else search.query
        transitions.prepare(animate, back = false)
        host.navigate(route)
        forwardStack.clear()
        backStack.addLast(currentEntry.withCurrentSearch())
        currentEntry = Entry(route, searchQuery = query)
        lastRoute = route
        seedRouteCategory(route)
        updateArrows()
        search.bind(host.currentBackStackEntry?.id, query)
    }

    fun discard(route: Any, fallback: Any) {
        val removed = backStack.removeAll { it.route == route } or forwardStack.removeAll { it.route == route }
        if (lastRoute == route) lastRoute = fallback
        if (currentEntry.route != route) {
            if (removed) updateArrows()
            return
        }
        val host = readyHost() ?: return
        transitions.prepare(false, back = false)
        host.navigate(fallback)
        currentEntry = Entry(fallback)
        lastRoute = fallback
        seedRouteCategory(fallback)
        updateArrows()
        search.bind(host.currentBackStackEntry?.id, "")
    }

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
        if (previous.route == currentEntry.route) {
            backStack.removeLast()
            forwardStack.addLast(currentEntry.withCurrentSearch())
            currentEntry = previous
            applyCategory(previous)
            updateArrows()
            return
        }
        val host = readyHost() ?: return
        transitions.prepare(OneConfigConfig.showPageAnimations, back = true)
        runCatching { host.navigate(previous.route) }.onFailure {
            LOGGER.error("Failed to navigate back to {}", previous.route, it)
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

    private fun seedRouteCategory(route: Any) {
        val modConfig = route as? ModConfigRoute ?: return
        val category = modConfig.category ?: return
        selectedCategories[modConfig.id] = category
    }

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
