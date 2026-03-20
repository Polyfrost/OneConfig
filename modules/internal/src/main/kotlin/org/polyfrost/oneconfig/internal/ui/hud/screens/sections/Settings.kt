package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.components.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.screens.HudConfigScreen
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Settings(hud: Hud? = null) {
    if (hud == null) return

    var useGuiScale by remember(hud) { mutableStateOf(hud.useGuiScale) }
    var customScale by remember(hud) { mutableStateOf(hud.customScale) }
    var showBackground by remember(hud) { mutableStateOf(hud.showBackground) }
    var bgColor by remember(hud) { mutableStateOf(Color(hud.bgColor)) }
    var bgRadius by remember(hud) { mutableStateOf(hud.bgRadius) }
    var textColor by remember(hud) { mutableStateOf(Color(hud.textColor)) }
    var showShadow by remember(hud) { mutableStateOf(hud.showShadow) }
    var shadowColor by remember(hud) { mutableStateOf(Color(hud.shadowColor)) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Section("GUI Scale") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SwitchControl(useGuiScale) {
                            Snapshot.withMutableSnapshot { useGuiScale = it; hud.useGuiScale = it }
                        }
                        Text("Use Minecraft GUI Scale", color = LocalTheme.current.textColor, fontSize = 14.sp)
                    }
                    if (!useGuiScale) {
                        NumberSpinner(
                            "Custom Scale", "x",
                            customScale, { Snapshot.withMutableSnapshot { customScale = it; hud.customScale = it } },
                            0.25f, 4f, 0.25f, width = 128.dp
                        )
                    }
                }
            }
        }

        item {
            Section("Background") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SwitchControl(showBackground) {
                            Snapshot.withMutableSnapshot { showBackground = it; hud.showBackground = it }
                        }
                        Text("Show Background", color = LocalTheme.current.textColor, fontSize = 14.sp)
                    }
                    if (showBackground) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ColorButton("Background Color", bgColor) {
                                Snapshot.withMutableSnapshot { bgColor = it; hud.bgColor = it.toArgb() }
                            }
                            NumberSpinner(
                                "Radius", "px",
                                bgRadius, { Snapshot.withMutableSnapshot { bgRadius = it; hud.bgRadius = it } },
                                0f, 32f, 1f, width = 100.dp
                            )
                        }
                    }
                }
            }
        }

        item {
            Section("Appearance") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorButton("Text Color", textColor) {
                        Snapshot.withMutableSnapshot { textColor = it; hud.textColor = it.toArgb() }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SwitchControl(showShadow) {
                            Snapshot.withMutableSnapshot { showShadow = it; hud.showShadow = it }
                        }
                        Text("Text Shadow", color = LocalTheme.current.textColor, fontSize = 14.sp)
                    }
                    if (showShadow) {
                        ColorButton("Shadow Color", shadowColor) {
                            Snapshot.withMutableSnapshot { shadowColor = it; hud.shadowColor = it.toArgb() }
                        }
                    }
                }
            }
        }

        item {
            HudConfigScreen(hud.tree)
        }
    }
}
