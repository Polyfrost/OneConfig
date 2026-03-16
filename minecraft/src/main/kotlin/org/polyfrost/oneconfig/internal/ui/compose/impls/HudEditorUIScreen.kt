package org.polyfrost.oneconfig.internal.ui.compose.impls

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.Theme
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignStudio
import org.polyfrost.oneconfig.internal.ui.shell.Lifecycle
import org.polyfrost.oneconfig.internal.ui.shell.OCViewModelStoreOwner
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@OptIn(InternalComposeUiApi::class)
class HudEditorUIScreen : ComposeScreen() {
    @Composable
    override fun compose() {
        DisposableEffect(Unit) {
            onDispose {
                if (HudManager.isEditing) {
                    HudManager.toggleEditor()
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalTheme provides Theme,
                LocalLifecycleOwner provides Lifecycle,
                LocalViewModelStoreOwner provides OCViewModelStoreOwner
            ) {
                HudDesignStudio()
            }
        }
    }
}

