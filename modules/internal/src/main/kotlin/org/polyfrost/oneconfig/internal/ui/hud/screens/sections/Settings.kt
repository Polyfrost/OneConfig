package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.hudOptionsCanReset
import org.polyfrost.oneconfig.internal.ui.hud.resetHudOptions
import org.polyfrost.oneconfig.internal.ui.screens.HudConfigScreen

private val VISIBILITY_OPTIONS = listOf("showInF3", "showInTab", "showInScreens", "showInChat")

/** The HUD's own options from its mod, followed by when it shows */
@Composable
fun Settings(hud: Hud?, query: String) {
    if (hud == null) return

    HudSettingsContent(hud) {
        var showInF3 by remember { mutableStateOf(hud.showInF3) }
        var showInTab by remember { mutableStateOf(hud.showInTab) }
        var showInScreens by remember { mutableStateOf(hud.showInScreens) }
        var showInChat by remember { mutableStateOf(hud.showInChat) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            hud.tree?.let { HudConfigScreen(it, query) }

            if (hud !is LegacyHudMarker) {
                val canReset = remember(showInF3, showInTab, showInScreens, showInChat) {
                    hudOptionsCanReset(hud, VISIBILITY_OPTIONS)
                }
                CardSection(
                    hudPanelText("visibility", "Visibility"), query, hudPanelText("show_in_f3", "Show in F3 Screen"), hudPanelText("show_in_tab", "Show in Tab List"), hudPanelText("show_in_screens", "Show in GUIs"), hudPanelText("show_in_chat", "Show in Chat"),
                    onReset = { resetHudOptions(hud, VISIBILITY_OPTIONS) },
                    canReset = canReset,
                    collapsedByDefault = !canReset,
                ) {
                    HudSettingTarget(hud, "showInF3") {
                        SwitchRow(hudPanelText("show_in_f3", "Show in F3 Screen"), showInF3) {
                            Snapshot.withMutableSnapshot { showInF3 = it; hud.showInF3 = it }
                        }
                    }
                    HudSettingTarget(hud, "showInTab") {
                        SwitchRow(hudPanelText("show_in_tab", "Show in Tab List"), showInTab) {
                            Snapshot.withMutableSnapshot { showInTab = it; hud.showInTab = it }
                        }
                    }
                    HudSettingTarget(hud, "showInScreens") {
                        SwitchRow(hudPanelText("show_in_screens", "Show in GUIs"), showInScreens) {
                            Snapshot.withMutableSnapshot { showInScreens = it; hud.showInScreens = it }
                        }
                    }
                    HudSettingTarget(hud, "showInChat") {
                        SwitchRow(hudPanelText("show_in_chat", "Show in Chat"), showInChat) {
                            Snapshot.withMutableSnapshot { showInChat = it; hud.showInChat = it }
                        }
                    }
                }
            }
        }
    }
}
