package org.polyfrost.oneconfig.internal.ui.navigation

data class NavigationRoute(
    val id: String,
    val icon: String,
    val route: Any,
)

class NavigationGroup(
    val id: String,
    vararg val routes: NavigationRoute
)

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
