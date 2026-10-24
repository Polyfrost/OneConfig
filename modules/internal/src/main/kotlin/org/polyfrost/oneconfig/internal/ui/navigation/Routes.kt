package org.polyfrost.oneconfig.internal.ui.navigation

import org.polyfrost.oneconfig.internal.ui.shell.PolyPage

data class NavigationRoute(
    val id: String,
    val icon: String,
    val route: PolyPage,
    val arg: Any? = null,
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
                *pages.map {
                    val (route, arg) = pageTarget(it)
                    NavigationRoute(it.id.substringAfter(':'), it.icon, route, arg)
                }.toTypedArray(),
            )
        }

val PolyPage.hierarchy: PolyPage
    get() = when (this) {
        PolyPage.ModConfig -> PolyPage.Mods
        PolyPage.ChangelogEntry -> PolyPage.Changelog
        else -> this
    }
