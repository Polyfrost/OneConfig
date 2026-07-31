package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun ModConfig(id: String, initialCategory: String? = null) {
    val revision = ConfigRegistry.revision
    val config = ConfigRegistry.findById(id)
    val tree = ConfigRegistry.findTree(id)

    DisposableEffect(id, config?.title, config?.authors, config?.credits, config?.version, tree) {
        val title = config?.title?.asRenderText()
        ShellState.title = title
        ShellState.titleInfoForTitle = title
        ShellState.titleAuthors = config?.authors
        ShellState.titleCredits = config?.credits
        ShellState.titleVersion = config?.version
        ShellState.openOriginalScreen = tree?.getMetadata<Runnable>("open_original_screen")
        onDispose {
            ShellState.titleInfoForTitle = null
            ShellState.titleAuthors = null
            ShellState.titleCredits = null
            ShellState.titleVersion = null
            ShellState.openOriginalScreen = null
        }
    }

    val externalOpen = config?.onOpen

    when {
        config == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Unknown mod: $id", color = LocalTheme.current.textColorSecondary)
        }
        tree?.getMetadata<Boolean>("ui_only") == true && externalOpen != null -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("This mod opens its own settings screen.", color = LocalTheme.current.textColorSecondary)
        }
        tree == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This mod manages its own configuration.", color = LocalTheme.current.textColorSecondary)
        }
        else -> key(revision, tree) {
            ConfigScreen(tree, initialCategory, pageKey = id)
        }
    }
}
