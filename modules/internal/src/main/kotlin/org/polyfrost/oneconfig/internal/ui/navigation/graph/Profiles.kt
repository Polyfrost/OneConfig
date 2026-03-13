package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import org.polyfrost.oneconfig.internal.ui.screens.Profiles

@Serializable
data object ProfilesGraph

@Serializable
data object ProfilesRoute

fun NavGraphBuilder.profilesGraph() {
    navigation<ProfilesGraph>(startDestination = ProfilesRoute) {
        composable<ProfilesRoute> {
            Profiles()
        }
    }
}