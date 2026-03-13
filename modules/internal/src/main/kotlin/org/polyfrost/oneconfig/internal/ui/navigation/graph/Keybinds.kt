package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable

@Serializable
data object KeybindsGraph

@Serializable
data object KeybindsRoute

fun NavGraphBuilder.keybindsGraph() {
    navigation<KeybindsGraph>(startDestination = KeybindsRoute) {
        composable<KeybindsRoute> {
        }
    }
}