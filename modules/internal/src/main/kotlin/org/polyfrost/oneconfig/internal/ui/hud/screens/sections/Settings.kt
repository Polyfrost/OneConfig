package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.graphics.toArgb
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
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
import org.polyfrost.oneconfig.internal.ui.hud.components.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.hud.hudHasPositionDefaults
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.hud.resetHudPosition
import org.polyfrost.oneconfig.internal.ui.screens.HudConfigScreen
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val DeleteButtonColor = Color(0xFFE5484D)

@Composable
fun Settings(hud: Hud?, query: String, onDeleted: () -> Unit) {
    if (hud == null) return

    LaunchedEffect(hud) { repairHudStaticSize(hud) }

    HudSettingsContent(hud) {
        Section("Position", query, "Lock position", "Reset to default position") {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ResetHudPositionButton(hud)
                HudSettingTarget(hud, "locked") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SwitchControl(hud.locked) {
                            Snapshot.withMutableSnapshot { hud.locked = it }
                        }
                        Text("Lock position", color = LocalTheme.current.textColor, fontSize = 14.sp)
                    }
                }
            }
        }

        if (hud is LegacyHudMarker) {
            if (hud.supportsScale) {
                Section("Scale", query) {
                    HudSettingTarget(hud, "customScale") {
                        NumberSpinner(
                            "Scale",
                            "x",
                            hud.customScale,
                            { Snapshot.withMutableSnapshot { hud.customScale = it } },
                            0.25f,
                            4f,
                            0.25f,
                            width = 176.dp
                        )
                    }
                }
            }
        } else {
            Section("GUI Scale", query, "Use Minecraft GUI Scale", "Custom Scale") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HudSettingTarget(hud, "useGuiScale") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SwitchControl(hud.useGuiScale) {
                                Snapshot.withMutableSnapshot { hud.useGuiScale = it }
                            }
                            Text("Use Minecraft GUI Scale", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        }
                    }
                    HudSettingTarget(hud, "customScale") {
                        NumberSpinner(
                            "Custom Scale",
                            "x",
                            hud.customScale,
                            { Snapshot.withMutableSnapshot { hud.customScale = it } },
                            0.25f,
                            4f,
                            0.25f,
                            width = 176.dp
                        )
                    }
                }
            }
        }

        if (hud !is LegacyHudMarker) {
            Section(
                "Background", query,
                "Show Background", "Background Color", "Radius",
                "Merge With Neighbours", "Merge Diagonally", "Keep Background When Hidden",
            ) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HudSettingTarget(hud, "showBackground") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SwitchControl(hud.showBackground) {
                                Snapshot.withMutableSnapshot { hud.showBackground = it }
                            }
                            Text("Show Background", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        }
                    }
                    if (hud.showBackground) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HudSettingTarget(hud, "bgColor") {
                                ColorButton(
                                    "Background Color", Color(hud.bgColor),
                                    chroma = hud.bgChroma, chromaSpeed = hud.bgChromaSpeed,
                                    onColorChanged = {
                                        Snapshot.withMutableSnapshot { hud.bgColor = it.toArgb() }
                                    },
                                    onChromaChanged = { en, sp ->
                                        Snapshot.withMutableSnapshot {
                                            hud.bgChroma = en
                                            hud.bgChromaSpeed = sp
                                        }
                                    },
                                )
                            }
                            HudSettingTarget(hud, "bgRadius") {
                                NumberSpinner(
                                    "Radius", "px",
                                    hud.bgRadius, { Snapshot.withMutableSnapshot { hud.bgRadius = it } },
                                    0f, 32f, 1f, width = 112.dp
                                )
                            }
                        }
                        // HUDs drawing their own background opt out of merging so these switches
                        // have nothing to control
                        if (hud.canMergeBackground()) {
                            HudSettingTarget(hud, "mergeBackground") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SwitchControl(hud.mergeBackground) {
                                        Snapshot.withMutableSnapshot { hud.mergeBackground = it }
                                    }
                                    Text("Merge With Neighbours", color = LocalTheme.current.textColor, fontSize = 14.sp)
                                }
                            }
                            if (hud.mergeBackground) {
                                HudSettingTarget(hud, "mergeDiagonally") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        SwitchControl(hud.mergeDiagonally) {
                                            Snapshot.withMutableSnapshot { hud.mergeDiagonally = it }
                                        }
                                        Text("Merge Diagonally", color = LocalTheme.current.textColor, fontSize = 14.sp)
                                    }
                                }
                                HudSettingTarget(hud, "keepBgWhenHidden") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        SwitchControl(hud.keepBgWhenHidden) {
                                            Snapshot.withMutableSnapshot { hud.keepBgWhenHidden = it }
                                        }
                                        Text("Keep Background When Hidden", color = LocalTheme.current.textColor, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Section("Appearance", query, "Text Color", "Text Shadow", "Shadow Color") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HudSettingTarget(hud, "textColor") {
                        ColorButton(
                            "Text Color", Color(hud.textColor),
                            chroma = hud.textChroma, chromaSpeed = hud.textChromaSpeed,
                            onColorChanged = {
                                Snapshot.withMutableSnapshot { hud.textColor = it.toArgb() }
                            },
                            onChromaChanged = { en, sp ->
                                Snapshot.withMutableSnapshot {
                                    hud.textChroma = en
                                    hud.textChromaSpeed = sp
                                }
                            },
                        )
                    }
                    HudSettingTarget(hud, "showShadow") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SwitchControl(hud.showShadow) {
                                Snapshot.withMutableSnapshot { hud.showShadow = it }
                            }
                            Text("Text Shadow", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        }
                    }
                    if (hud.showShadow) {
                        HudSettingTarget(hud, "shadowColor") {
                            ColorButton(
                                "Shadow Color", Color(hud.shadowColor),
                                chroma = hud.shadowChroma, chromaSpeed = hud.shadowChromaSpeed,
                                onColorChanged = {
                                    Snapshot.withMutableSnapshot { hud.shadowColor = it.toArgb() }
                                },
                                onChromaChanged = { en, sp ->
                                    Snapshot.withMutableSnapshot {
                                        hud.shadowChroma = en
                                        hud.shadowChromaSpeed = sp
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Section("Visibility", query, "Show in F3 Screen", "Show in Tab List", "Show in GUIs", "Show in Chat") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HudSettingTarget(hud, "showInF3") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SwitchControl(hud.showInF3) {
                                Snapshot.withMutableSnapshot { hud.showInF3 = it }
                            }
                            Text("Show in F3 Screen", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        }
                    }
                    HudSettingTarget(hud, "showInTab") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SwitchControl(hud.showInTab) {
                                Snapshot.withMutableSnapshot { hud.showInTab = it }
                            }
                            Text("Show in Tab List", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        }
                    }
                    HudSettingTarget(hud, "showInScreens") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SwitchControl(hud.showInScreens) {
                                Snapshot.withMutableSnapshot { hud.showInScreens = it }
                            }
                            Text("Show in GUIs", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        }
                    }
                    HudSettingTarget(hud, "showInChat") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SwitchControl(hud.showInChat) {
                                Snapshot.withMutableSnapshot { hud.showInChat = it }
                            }
                            Text("Show in Chat", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        hud.tree?.let { HudConfigScreen(it, query) }

        if (hud.canDelete() && hudSearchMatches(query, "Delete HUD")) {
            DeleteHudButton(hud, onDeleted)
        }
    }
}

@Composable
private fun ResetHudPositionButton(hud: Hud) {
    val theme = LocalTheme.current
    val enabled = hudHasPositionDefaults(hud)
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor = when {
        !enabled -> theme.borderColor
        isHovered -> Accent.copy(alpha = Accent.alphaF * 0.75f)
        else -> Accent
    }
    val textColor = if (enabled) theme.accentTextColor else theme.textColorSecondary

    Box(
        modifier = Modifier
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .background(bgColor, theme.buttonShape)
            .then(
                if (enabled) {
                    Modifier.onClick(interactionSource) { resetHudPosition(hud) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Reset to default position", color = textColor, fontSize = 13.sp)
    }
}

@Composable
private fun DeleteHudButton(hud: Hud, onDeleted: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor = if (isHovered) DeleteButtonColor.copy(alpha = 0.75f) else DeleteButtonColor

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
