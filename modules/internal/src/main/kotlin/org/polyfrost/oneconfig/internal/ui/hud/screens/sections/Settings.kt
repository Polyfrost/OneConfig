package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.screens.HudConfigScreen
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val DeleteButtonColor = Color(0xFFE5484D)

/** The HUD's own options from its mod, followed by when it shows */
@Composable
fun Settings(hud: Hud?, query: String, onDeleted: () -> Unit) {
    if (hud == null) return

    HudSettingsContent(hud) {
        var showInF3 by remember { mutableStateOf(hud.showInF3) }
        var showInTab by remember { mutableStateOf(hud.showInTab) }
        var showInScreens by remember { mutableStateOf(hud.showInScreens) }
        var showInChat by remember { mutableStateOf(hud.showInChat) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            hud.tree?.let { HudConfigScreen(it, query) }

            if (hud !is LegacyHudMarker) {
                Section("Visibility", query, "Show in F3 Screen", "Show in Tab List", "Show in GUIs", "Show in Chat") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HudSettingTarget(hud, "showInF3") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SwitchControl(showInF3) {
                                    Snapshot.withMutableSnapshot { showInF3 = it; hud.showInF3 = it }
                                }
                                Text("Show in F3 Screen", color = LocalTheme.current.textColor, fontSize = 14.sp)
                            }
                        }
                        HudSettingTarget(hud, "showInTab") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SwitchControl(showInTab) {
                                    Snapshot.withMutableSnapshot { showInTab = it; hud.showInTab = it }
                                }
                                Text("Show in Tab List", color = LocalTheme.current.textColor, fontSize = 14.sp)
                            }
                        }
                        HudSettingTarget(hud, "showInScreens") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SwitchControl(showInScreens) {
                                    Snapshot.withMutableSnapshot { showInScreens = it; hud.showInScreens = it }
                                }
                                Text("Show in GUIs", color = LocalTheme.current.textColor, fontSize = 14.sp)
                            }
                        }
                        HudSettingTarget(hud, "showInChat") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SwitchControl(showInChat) {
                                    Snapshot.withMutableSnapshot { showInChat = it; hud.showInChat = it }
                                }
                                Text("Show in Chat", color = LocalTheme.current.textColor, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            if (hud.canDelete() && hudSearchMatches(query, "Delete HUD")) DeleteHudButton(hud, onDeleted)
        }
    }
}

@Composable
private fun DeleteHudButton(hud: Hud, onDeleted: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor by animateColorAsState(
        if (isHovered) DeleteButtonColor.copy(alpha = 0.75f) else DeleteButtonColor,
    )

    Box(
        modifier = Modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .background(bgColor, theme.buttonShape)
            .onClick(interactionSource) {
                Snapshot.withMutableSnapshot {
                    HudManager.removeHud(hud, delete = true)
                }
                onDeleted()
            }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon("trash", color = theme.accentTextColor, modifier = Modifier.size(14.dp))
            Text("Delete HUD", color = theme.accentTextColor, fontSize = 13.sp)
        }
    }
}
