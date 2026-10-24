package org.polyfrost.oneconfig.internal.ui.navigation

import org.polyfrost.oneconfig.internal.ui.shell.PolyPage

data class NavigationRoute(
    val id: String,
    val icon: String,
    val route: PolyPage,
)

class NavigationGroup(
    val id: String,
    vararg val routes: NavigationRoute
)

val NavigationGroups = listOf(
    NavigationGroup(
        id = "Mods & Options",
        NavigationRoute(
            id = "mods",
            icon = "settings",
            route = PolyPage.Mods
        ),
        NavigationRoute(
            id = "profiles",
            icon = "profiles",
            route = PolyPage.Profiles
        ),
        NavigationRoute(
            id = "keybinds",
            icon = "keyboard",
            route = PolyPage.Keybinds
        )
    ),
    NavigationGroup(
        id = "Personalization",
        NavigationRoute(
            id = "themes",
            icon = "paintbrush",
            route = PolyPage.Themes
        ),
        NavigationRoute(
            id = "preferences",
            icon = "cog",
            route = PolyPage.Preferences
        ),
    ),
    NavigationGroup(
        id = "OneConfig",
        NavigationRoute(
            id = "changelog",
            icon = "refresh",
            route = PolyPage.Changelog
        ),
        NavigationRoute(
            id = "credits",
            icon = "star",
            route = PolyPage.Credits
        ),
    )
)

val PolyPage.hierarchy: PolyPage
    get() = when (this) {
        PolyPage.ModConfig -> PolyPage.Mods
        PolyPage.ChangelogEntry -> PolyPage.Changelog
        else -> this
    }

fun searchPlaceholder(destination: PolyPage): String {
    val section = NavigationGroups
        .asSequence()
        .flatMap { it.routes.asSequence() }
        .firstOrNull { def ->
            destination.hierarchy == def.route
        }
    return if (section != null) "Search ${section.id}..." else "Search..."
}
