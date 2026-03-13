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
}
object LocalNavController {
    lateinit var current: NavHostController

    val wrapper = NavControllerWrapper

    object NavControllerWrapper {
        private val forwardStack = ArrayDeque<Any>()

        fun navigate(route: Any) {
            forwardStack.clear()
            current.navigate(route)
        }

        fun back() {
            val current = NavigationGroups.map { it.routes }.toTypedArray().flatten()
                .find { current.currentDestination?.hasRoute(it.route::class) == true }
                ?.route

            if (this@LocalNavController.current.popBackStack()) {
                current?.let { forwardStack.addLast(it) }
            }
        }

        fun forward() {
            val next = forwardStack.removeLastOrNull() ?: return
            current.navigate(next)
        }
    }
}