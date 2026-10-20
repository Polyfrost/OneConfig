package org.polyfrost.oneconfig.internal.ui.navigation

@Deprecated("Register a OneConfigPage through OneConfigPages instead. Removed in OneConfig 1.6.0")
data class NavigationRoute(
    val id: String,
    val icon: String,
    val route: Any,
)

@Deprecated("Use PageGroup and OneConfigPages instead. Removed in OneConfig 1.6.0")
class NavigationGroup(
    val id: String,
    @Suppress("DEPRECATION") vararg val routes: NavigationRoute
)

@Suppress("DEPRECATION")
@Deprecated("Use OneConfigPages.all() instead. Removed in OneConfig 1.6.0")
val NavigationGroups: List<NavigationGroup>
    get() = registeredPages()
        .filter { it.showInSidebar }
        .groupBy { it.group }
        .map { (group, pages) ->
            NavigationGroup(
                group.title.fallback,
                *pages.map { NavigationRoute(it.id.substringAfter(':'), it.icon, pageRoute(it)) }.toTypedArray(),
            )
        }
