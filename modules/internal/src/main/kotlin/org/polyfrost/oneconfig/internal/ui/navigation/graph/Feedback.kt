package org.polyfrost.oneconfig.internal.ui.navigation.graph

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import kotlinx.serialization.Serializable

@Serializable
data object FeedbackGraph

@Serializable
data object FeedbackRoute

fun NavGraphBuilder.feedbackGraph() {
    navigation<FeedbackGraph>(startDestination = FeedbackRoute) {
        composable<FeedbackRoute> {
        }
    }
}