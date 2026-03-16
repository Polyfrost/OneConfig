package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.internal.ui.screens.HudConfigScreen

@Composable
fun Settings(hud: Hud? = null) = hud?.let { h ->
    HudConfigScreen(h.tree)
}