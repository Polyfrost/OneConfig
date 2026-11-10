package org.polyfrost.oneconfig.internal.ui.navigation

import org.polyfrost.oneconfig.api.ui.v1.page.PageGroup
import org.polyfrost.oneconfig.api.ui.v1.page.PageText

data class NavigationRoute(
    val id: String,
    val icon: String,
    val route: Any,
)

class NavigationGroup(
    val id: String,
    val title: PageText,
    vararg val routes: NavigationRoute
)

val NavigationGroups: List<NavigationGroup>
    get() = registeredPages()
        .filter { it.showInSidebar && it.group != PageGroup.ONECONFIG }
        .groupBy { it.group }
        .map { (group, pages) ->
            NavigationGroup(
                group.id,
                group.title,
                *pages.map { NavigationRoute(it.id.substringAfter(':'), it.icon, pageRoute(it)) }.toTypedArray(),
            )
        }
