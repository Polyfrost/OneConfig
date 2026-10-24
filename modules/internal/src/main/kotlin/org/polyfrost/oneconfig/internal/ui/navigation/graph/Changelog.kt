package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.polyfrost.oneconfig.internal.ui.screens.Changelog

@Serializable
data object ChangeLogGraph

@Serializable
data object ChangeLogRoute

@Serializable
data class ChangeLogEntryRoute(val idx: Int)

fun NavGraphBuilder.changeLogGraph() {
    navigation<ChangeLogGraph>(startDestination = ChangeLogRoute) {
        composable<ChangeLogRoute> {
            Changelog()
        }
        composable<ChangeLogEntryRoute> { route ->
            val data = route.toRoute<ChangeLogEntryRoute>()
            Changelog(data.idx)
        }
    }
}