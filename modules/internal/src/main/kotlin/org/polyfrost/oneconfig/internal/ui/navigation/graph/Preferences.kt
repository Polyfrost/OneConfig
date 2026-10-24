package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable
import org.polyfrost.oneconfig.internal.ui.screens.Preferences

@Serializable
data object PreferencesGraph

@Serializable
data object PreferencesRoute

fun NavGraphBuilder.preferencesGraph() {
    navigation<PreferencesGraph>(startDestination = PreferencesRoute) {
        composable<PreferencesRoute> {
            Preferences()
        }
    }
}
