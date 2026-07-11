package org.polyfrost.oneconfig.internal.ui.navigation

import androidx.navigation.NavGraphBuilder
import org.polyfrost.oneconfig.internal.ui.navigation.graph.changeLogGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.creditsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.feedbackGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.keybindsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.modsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.preferencesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.profilesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.themesGraph

fun NavGraphBuilder.navigation() {
    modsGraph()
    profilesGraph()
    keybindsGraph()
    themesGraph()
    preferencesGraph()
    changeLogGraph()
    feedbackGraph()
    creditsGraph()
}