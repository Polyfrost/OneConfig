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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.hudHasPositionDefaults
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.hud.resetHudPosition
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.components.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.screens.HudConfigScreen
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val DeleteButtonColor = Color(0xFFE5484D)

@Composable
fun Settings(hud: Hud? = null, onDeleted: () -> Unit = {}) {
    if (hud == null) return

    LaunchedEffect(hud) { repairHudStaticSize(hud) }

    HudSettingsContent(hud) {
        var useGuiScale by remember { mutableStateOf(hud.useGuiScale) }
        var customScale by remember { mutableStateOf(hud.customScale) }
        var showBackground by remember { mutableStateOf(hud.showBackground) }
        var bgColor by remember { mutableStateOf(Color(hud.bgColor)) }
        var bgChroma by remember { mutableStateOf(hud.bgChroma) }
        var bgChromaSpeed by remember { mutableStateOf(hud.bgChromaSpeed) }
        var bgRadius by remember { mutableStateOf(hud.bgRadius) }
        var mergeBackground by remember { mutableStateOf(hud.mergeBackground) }
        var mergeDiagonally by remember { mutableStateOf(hud.mergeDiagonally) }
        var textColor by remember { mutableStateOf(Color(hud.textColor)) }
        var textChroma by remember { mutableStateOf(hud.textChroma) }
        var textChromaSpeed by remember { mutableStateOf(hud.textChromaSpeed) }
        var showShadow by remember { mutableStateOf(hud.showShadow) }
        var shadowColor by remember { mutableStateOf(Color(hud.shadowColor)) }
        var shadowChroma by remember { mutableStateOf(hud.shadowChroma) }
        var shadowChromaSpeed by remember { mutableStateOf(hud.shadowChromaSpeed) }
        var showInF3 by remember { mutableStateOf(hud.showInF3) }
        var showInTab by remember { mutableStateOf(hud.showInTab) }
        var showInScreens by remember { mutableStateOf(hud.showInScreens) }
        var showInChat by remember { mutableStateOf(hud.showInChat) }
        var locked by remember { mutableStateOf(hud.locked) }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                Section("Position") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ResetHudPositionButton(hud)
                        HudSettingTarget(hud, "locked") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SwitchControl(locked) {
                                    Snapshot.withMutableSnapshot { locked = it; hud.locked = it }
                                }
                                Text("Lock position", color = LocalTheme.current.textColor, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            if (hud is LegacyHudMarker) {
                if (hud.supportsScale) item {
                    Section("Scale") {
                        HudSettingTarget(hud, "customScale") {
                            NumberSpinner(
                                "Scale",
                                "x",
                                customScale,
                                { Snapshot.withMutableSnapshot { customScale = it; hud.customScale = it } },
                                0.25f,
                                4f,
                                0.25f,
                                width = 176.dp
                            )
                        }
                    }
                }
            } else {
                item {
                    Section("GUI Scale") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HudSettingTarget(hud, "useGuiScale") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SwitchControl(useGuiScale) {
                                        Snapshot.withMutableSnapshot { useGuiScale = it; hud.useGuiScale = it }
                                    }
                                    Text("Use Minecraft GUI Scale", color = LocalTheme.current.textColor, fontSize = 14.sp)
                                }
                            }
                            HudSettingTarget(hud, "customScale") {
                                NumberSpinner(
                                    "Custom Scale",
                                    "x",
                                    customScale,
                                    { Snapshot.withMutableSnapshot { customScale = it; hud.customScale = it } },
                                    0.25f,
                                    4f,
                                    0.25f,
                                    width = 176.dp
                                )
                            }
                        }
                    }
                }
            }

            if (hud !is LegacyHudMarker) {
            item {
                Section("Background") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HudSettingTarget(hud, "showBackground") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SwitchControl(showBackground) {
                                    Snapshot.withMutableSnapshot { showBackground = it; hud.showBackground = it }
                                }
                                Text("Show Background", color = LocalTheme.current.textColor, fontSize = 14.sp)
                            }
                        }
                        if (showBackground) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HudSettingTarget(hud, "bgColor") {
                                    ColorButton(
                                        "Background Color", bgColor,
                                        chroma = bgChroma, chromaSpeed = bgChromaSpeed,
                                        onColorChanged = {
                                            Snapshot.withMutableSnapshot { bgColor = it; hud.bgColor = it.toArgb() }
                                        },
                                        onChromaChanged = { en, sp ->
                                            Snapshot.withMutableSnapshot {
                                                bgChroma = en; hud.bgChroma = en
                                                bgChromaSpeed = sp; hud.bgChromaSpeed = sp
                                            }
                                        },
                                    )
                                }
                                HudSettingTarget(hud, "bgRadius") {
                                    NumberSpinner(
                                        "Radius", "px",
                                        bgRadius, { Snapshot.withMutableSnapshot { bgRadius = it; hud.bgRadius = it } },
                                        0f, 32f, 1f, width = 100.dp
                                    )
                                }
                            }
                            // HUDs which draw their own background opt out of merging entirely, so
                            // there is nothing for these switches to control
                            if (hud.canMergeBackground()) {
                                HudSettingTarget(hud, "mergeBackground") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        SwitchControl(mergeBackground) {
                                            Snapshot.withMutableSnapshot { mergeBackground = it; hud.mergeBackground = it }
                                        }
                                        Text("Merge With Neighbours", color = LocalTheme.current.textColor, fontSize = 14.sp)
                                    }
                                }
                                if (mergeBackground) {
                                    HudSettingTarget(hud, "mergeDiagonally") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            SwitchControl(mergeDiagonally) {
                                                Snapshot.withMutableSnapshot { mergeDiagonally = it; hud.mergeDiagonally = it }
                                            }
                                            Text("Merge Diagonally", color = LocalTheme.current.textColor, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Section("Appearance") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HudSettingTarget(hud, "textColor") {
                            ColorButton(
                                "Text Color", textColor,
                                chroma = textChroma, chromaSpeed = textChromaSpeed,
                                onColorChanged = {
                                    Snapshot.withMutableSnapshot { textColor = it; hud.textColor = it.toArgb() }
                                },
                                onChromaChanged = { en, sp ->
                                    Snapshot.withMutableSnapshot {
                                        textChroma = en; hud.textChroma = en
                                        textChromaSpeed = sp; hud.textChromaSpeed = sp
                                    }
                                },
                            )
                        }
                        HudSettingTarget(hud, "showShadow") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SwitchControl(showShadow) {
                                    Snapshot.withMutableSnapshot { showShadow = it; hud.showShadow = it }
                                }
                                Text("Text Shadow", color = LocalTheme.current.textColor, fontSize = 14.sp)
                            }
                        }
                        if (showShadow) {
                            HudSettingTarget(hud, "shadowColor") {
                                ColorButton(
                                    "Shadow Color", shadowColor,
                                    chroma = shadowChroma, chromaSpeed = shadowChromaSpeed,
                                    onColorChanged = {
                                        Snapshot.withMutableSnapshot { shadowColor = it; hud.shadowColor = it.toArgb() }
                                    },
                                    onChromaChanged = { en, sp ->
                                        Snapshot.withMutableSnapshot {
                                            shadowChroma = en; hud.shadowChroma = en
                                            shadowChromaSpeed = sp; hud.shadowChromaSpeed = sp
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                Section("Visibility") {
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
            }

            var tree = hud.tree
            if (tree != null)
                item {
                    HudConfigScreen(tree)
                }

            if (hud.canDelete()) {
                item {
                    DeleteHudButton(hud, onDeleted)
                }
            }
        }
    }
}

@Composable
private fun ResetHudPositionButton(hud: Hud) {
    val theme = LocalTheme.current
    val enabled = hudHasPositionDefaults(hud)
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor by animateColorAsState(
        when {
            !enabled -> theme.borderColor
            isHovered -> Accent.copy(alpha = 0.75f)
            else -> Accent
        },
    )
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
