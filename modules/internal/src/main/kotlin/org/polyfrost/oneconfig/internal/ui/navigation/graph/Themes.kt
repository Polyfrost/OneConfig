package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable

@Serializable
data object ThemesGraph

@Serializable
data object ThemesRoute

fun NavGraphBuilder.themesGraph() {
    navigation<ThemesGraph>(startDestination = ThemesRoute) {
        composable<ThemesRoute> {
        }
    }
}