package org.polyfrost.oneconfig.internal.ui.navigation

import org.polyfrost.oneconfig.internal.ui.navigation.graph.ChangeLogGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.FeedbackGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.KeybindsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ProfilesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ThemesGraph

data class NavigationRoute(
    val id: String,
    val icon: String,
    val route: Any,
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
            route = ModsGraph
        ),
        NavigationRoute(
            id = "profiles",
            icon = "profiles",
            route = ProfilesGraph
        ),
        NavigationRoute(
            id = "keybinds",
            icon = "keyboard",
            route = KeybindsGraph
        )
    ),
    NavigationGroup(
        id = "Personalization",
        NavigationRoute(
            id = "themes",
            icon = "paintbrush",
            route = ThemesGraph
        ),
        NavigationRoute(
            id = "preferences",
            icon = "cog",
            route = PreferencesGraph
        ),
    ),
    NavigationGroup(
        id = "OneConfig",
        NavigationRoute(
            id = "changelog",
            icon = "refresh",
            route = ChangeLogGraph
        ),
        NavigationRoute(
            id = "feedback",
            icon = "text",
            route = FeedbackGraph
        ),
    )
)