package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import org.polyfrost.oneconfig.internal.ui.components.Header
import org.polyfrost.oneconfig.internal.ui.components.Sidebar
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.navigation
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Shell(
    windowWidth: Float,
    windowHeight: Float,
    backdrop: DrawScope.(Offset) -> Unit = {}
) {
    var windowOffset by remember { mutableStateOf(Offset.Zero) }

    Row(
        modifier = Modifier.onGloballyPositioned {
            windowOffset = it.positionInRoot()
        }.sizeIn(
            1391.dp, 700.dp, 1391.dp, 700.dp
        ).clip(LocalTheme.current.backgroundShape)
            .border(1.dp, LocalTheme.current.borderColor, LocalTheme.current.backgroundShape)
            .drawBehind {
                backdrop(windowOffset)
            }
    ) {
        Sidebar()

        Column(
            modifier = Modifier
                .weight(1f)
                .background(LocalTheme.current.pageBackground),
        ) {
            Column(
                modifier = Modifier.weight(1f)
                    .padding(horizontal = 25.dp, vertical = 19.dp),
                verticalArrangement = Arrangement.spacedBy(19.dp)
            ) {
                Header()
                NavHost(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    navController = LocalNavController.current,
                    startDestination = ModsGraph,

                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },

                    popEnterTransition = { fadeIn() },
                    popExitTransition = { fadeOut() }
                ) {
                    navigation()
                }
            }
        }
    }
}
