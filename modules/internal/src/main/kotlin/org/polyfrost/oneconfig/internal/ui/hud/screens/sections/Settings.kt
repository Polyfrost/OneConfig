package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.api.ui.v1.components.NumberField
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyPressMode
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData
import org.polyfrost.oneconfig.internal.ui.components.settings.Dropdown
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindOption
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.hudOptionsCanReset
import org.polyfrost.oneconfig.internal.ui.hud.hudProperty
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

            if (hud.hasVisibilityKeyMode()) VisibilityKeyCard(hud, query)

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
                        SwitchRow(
                            hudPanelText("show_in_f3", "Show in F3 Screen"),
                            showInF3,
                            { Snapshot.withMutableSnapshot { showInF3 = it; hud.showInF3 = it } },
                        )
                    }
                    HudSettingTarget(hud, "showInTab") {
                        SwitchRow(
                            hudPanelText("show_in_tab", "Show in Tab List"),
                            showInTab,
                            { Snapshot.withMutableSnapshot { showInTab = it; hud.showInTab = it } },
                        )
                    }
                    HudSettingTarget(hud, "showInScreens") {
                        SwitchRow(
                            hudPanelText("show_in_screens", "Show in GUIs"),
                            showInScreens,
                            { Snapshot.withMutableSnapshot { showInScreens = it; hud.showInScreens = it } },
                        )
                    }
                    HudSettingTarget(hud, "showInChat") {
                        SwitchRow(
                            hudPanelText("show_in_chat", "Show in Chat"),
                            showInChat,
                            { Snapshot.withMutableSnapshot { showInChat = it; hud.showInChat = it } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisibilityKeyCard(hud: Hud, query: String) {
    val keyProp = hudProperty(hud, "visibilityKeybind")
    if (hudProperty(hud, "visibilityKeyMode") == null) return
    val mode = hud.visibilityKeyMode
    val bound = keyProp == null || hud.visibilityKeybind.isBound

    val title = hudPanelText("visibility_keybind", "Visibility Keybind")
    val keyModeLabel = hudPanelText("key_mode", "Key Mode")
    val holdThresholdLabel = hudPanelText("hold_threshold", "Hold Threshold")
    CardSection(title, query, title, keyModeLabel, holdThresholdLabel) {
        if (keyProp != null) {
            HudSettingTarget(hud, "visibilityKeybind") {
                key(hud.visibilityKeybind) {
                    ControlRow(hudPanelText("key", "Key")) { KeybindOption(KeybindOptionData(keyProp)) }
                }
            }
        }
        if (bound) {
            HudSettingTarget(hud, "visibilityKeyMode") {
                ControlRow(keyModeLabel) {
                    Dropdown(keyModeLabels(), mode.ordinal, onSelect = {
                        Snapshot.withMutableSnapshot { hud.visibilityKeyMode = KeyPressMode.entries[it] }
                    }, width = 176.dp)
                }
            }
            if (mode == KeyPressMode.QuickPress) {
                HudSettingTarget(hud, "visibilityKeyHoldThreshold") {
                    ControlRow(holdThresholdLabel) {
                        NumberField(
                            hud.visibilityKeyHoldThreshold.toFloat(),
                            { Snapshot.withMutableSnapshot { hud.visibilityKeyHoldThreshold = it.toInt() } },
                            range = 50f..1000f,
                            step = 50f,
                            unit = "ms",
                            width = 176.dp,
                        )
                    }
                }
            }
        }
    }
}

private fun keyModeLabels(): List<String> = KeyPressMode.entries.map {
    when (it) {
        KeyPressMode.Held -> translated("oneconfig.preferences.key_mode.held", "Held")
        KeyPressMode.Toggle -> translated("oneconfig.preferences.key_mode.toggle", "Toggle")
        KeyPressMode.QuickPress -> translated("oneconfig.preferences.key_mode.quick_press", "Quick Press")
    }
}
