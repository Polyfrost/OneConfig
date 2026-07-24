package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavHostController
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph

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

    var openOriginalScreen by mutableStateOf<Runnable?>(null)

    var playerName by mutableStateOf("Player")

    /** PNG bytes for the local player's head avatar, or null until loaded. */
    var playerHeadPng by mutableStateOf<ByteArray?>(null)

    var versionLabel by mutableStateOf("")

    var searchQuery by mutableStateOf("")

    var globalSearchActive by mutableStateOf(false)

    var focusSearchField by mutableStateOf(false)

    var showSearchField by mutableStateOf(false)

    var hudDragging by mutableStateOf(false)

    var shellBounds by mutableStateOf<Rect?>(null)

    var flipTopOptionOrder by mutableStateOf(false)

    /** Last top-level route navigated to, used by the "Previous page" / "Smart reset" opening behaviors. */
    var lastRoute: Any? = null

    /** Last selected settings category (tab) per mod id, restored when a mod's config is reopened. */
    val selectedCategories = HashMap<String, String>()

    /** Wall-clock time (ms) the menu was last closed, used by the "Smart reset" opening behavior. */
    var lastClosedAt: Long = 0L

    /** When true, the initial page transition on open is animated (driven by "Show opening page animation"). */
    var animateOpeningPage: Boolean = false

    /** Consumed once per open so the first page transition can be treated specially. */
    var initialTransitionConsumed: Boolean = false

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
    val isReady: Boolean get() = _current != null

    val wrapper = NavControllerWrapper

    object NavControllerWrapper {
        private val backStack = ArrayDeque<Any>()
        private val forwardStack = ArrayDeque<Any>()

        private var currentRoute: Any = ModsGraph

        fun reset() {
            backStack.clear()
            forwardStack.clear()
            currentRoute = ModsGraph
        }

        fun navigate(route: Any) {
            forwardStack.clear()
            backStack.addLast(currentRoute)
            currentRoute = route
            ShellState.searchQuery = ""
            ShellState.globalSearchActive = false
            ShellState.showSearchField = false
            ShellState.lastRoute = route
            current.navigate(route)
        }

        fun back() {
            if (backStack.isEmpty()) return
            if (current.popBackStack()) {
                forwardStack.addLast(currentRoute)
                currentRoute = backStack.removeLast()
                ShellState.lastRoute = currentRoute
                ShellState.globalSearchActive = false
                ShellState.showSearchField = false
            }
        }

        fun forward() {
            val next = forwardStack.removeLastOrNull() ?: return
            backStack.addLast(currentRoute)
            currentRoute = next
            ShellState.lastRoute = next
            ShellState.globalSearchActive = false
            ShellState.showSearchField = false
            current.navigate(next)
        }
    }
}
