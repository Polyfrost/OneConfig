package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import org.polyfrost.oneconfig.internal.ui.screens.Hotwire
import org.polyfrost.oneconfig.internal.ui.screens.HotwireAdd

@Serializable
data object HotwireGraph

@Serializable
data object HotwireRoute

@Serializable
data object HotwireAddRoute

fun NavGraphBuilder.hotwireGraph() {
    navigation<HotwireGraph>(startDestination = HotwireRoute) {
        composable<HotwireRoute> {
            Hotwire()
        }
        composable<HotwireAddRoute> {
            HotwireAdd()
        }
    }
}
