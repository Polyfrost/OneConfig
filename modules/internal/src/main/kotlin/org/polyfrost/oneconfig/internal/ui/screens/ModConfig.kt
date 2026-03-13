package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun ModConfig(id: String, initialCategory: String? = null) {
    val config = ConfigRegistry.findById(id)
    val tree = ConfigRegistry.findTree(id)

    DisposableEffect(id) {
        ShellState.title = config?.title
        onDispose { ShellState.title = null }
    }

    when {
        config == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Unknown mod: $id", color = LocalTheme.current.textColorSecondary)
        }
        tree == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This mod manages its own configuration.", color = LocalTheme.current.textColorSecondary)
        }
        else -> ConfigScreen(tree, initialCategory)
    }
}
