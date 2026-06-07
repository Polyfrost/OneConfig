package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import org.polyfrost.oneconfig.internal.ui.navigation.NavigationGroups
import kotlin.collections.flatten

object Lifecycle : LifecycleOwner {
    override val lifecycle = LifecycleRegistry(this)
}
object OCViewModelStoreOwner: ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
object ShellState {
    var title by mutableStateOf<String?>(null)

    var playerName by mutableStateOf("Player")

    /** PNG bytes for the local player's head avatar, or null until loaded. */
    var playerHeadPng by mutableStateOf<ByteArray?>(null)

    var versionLabel by mutableStateOf("")

    var searchQuery by mutableStateOf("")

    var globalSearchActive by mutableStateOf(false)

    var focusSearchField by mutableStateOf(false)

    var showSearchField by mutableStateOf(false)

    /** True when the account footer should show an unread-notification indicator. */
    var hasUnreadNotifications by mutableStateOf(false)

    /** Last top-level route navigated to, used by the "Previous page" / "Smart reset" opening behaviors. */
    var lastRoute: Any? = null

    /** Wall-clock time (ms) the menu was last closed, used by the "Smart reset" opening behavior. */
    var lastClosedAt: Long = 0L

    /** When true, the initial page transition on open is animated (driven by "Show opening page animation"). */
    var animateOpeningPage: Boolean = false

    /** Consumed once per open so the first page transition can be treated specially. */
    var initialTransitionConsumed: Boolean = false
}
object LocalNavController {
    lateinit var current: NavHostController

    val wrapper = NavControllerWrapper

    object NavControllerWrapper {
        private val forwardStack = ArrayDeque<Any>()

        fun navigate(route: Any) {
            forwardStack.clear()
            ShellState.searchQuery = ""
            ShellState.globalSearchActive = false
            ShellState.showSearchField = false
            ShellState.lastRoute = route
            current.navigate(route)
        }

        fun back() {
            val current = NavigationGroups.map { it.routes }.toTypedArray().flatten()
                .find { current.currentDestination?.hasRoute(it.route::class) == true }
                ?.route

            if (this@LocalNavController.current.popBackStack()) {
                current?.let { forwardStack.addLast(it) }
                ShellState.globalSearchActive = false
                ShellState.showSearchField = false
            }
        }

        fun forward() {
            val next = forwardStack.removeLastOrNull() ?: return
            ShellState.lastRoute = next
            ShellState.globalSearchActive = false
            ShellState.showSearchField = false
            current.navigate(next)
        }
    }
}
