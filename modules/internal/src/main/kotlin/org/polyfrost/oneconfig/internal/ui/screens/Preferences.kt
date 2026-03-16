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

const val PREFERENCES_ID = "oneconfig.json"

@Composable
fun Preferences() {
    val tree = ConfigRegistry.findTree(PREFERENCES_ID)

    DisposableEffect(Unit) {
        ShellState.title = "Preferences"
        onDispose {  }
    }

    if (tree == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No preferences available.", color = LocalTheme.current.textColorSecondary)
        }
        return
    }

    ConfigScreen(tree)
}
