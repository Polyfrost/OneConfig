package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import org.polyfrost.oneconfig.internal.ui.screens.Credits

@Serializable
data object CreditsGraph

@Serializable
data object CreditsRoute

fun NavGraphBuilder.creditsGraph() {
    navigation<CreditsGraph>(startDestination = CreditsRoute) {
        composable<CreditsRoute> {
            Credits()
        }
    }
}
