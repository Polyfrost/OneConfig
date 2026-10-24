package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.polyfrost.oneconfig.internal.ui.screens.ModConfig
import org.polyfrost.oneconfig.internal.ui.screens.Mods

@Serializable
data object ModsGraph

@Serializable
data object ModsRoute

@Serializable
data class ModConfigRoute(val id: String, val category: String? = null)

fun NavGraphBuilder.modsGraph() {
    navigation<ModsGraph>(startDestination = ModsRoute) {
        composable<ModsRoute> {
            Mods()
        }
        composable<ModConfigRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ModConfigRoute>()
            ModConfig(route.id, route.category)
        }
    }
}
