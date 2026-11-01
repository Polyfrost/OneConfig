package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudAnchor
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.components.AlignmentPicker
import org.polyfrost.oneconfig.internal.ui.hud.hudHasPositionDefaults
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.hud.resetHudPosition
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudEditorViewport
import org.polyfrost.oneconfig.internal.ui.hud.screens.beginHudMove
import org.polyfrost.oneconfig.internal.ui.hud.screens.designerSelection
import org.polyfrost.oneconfig.internal.ui.hud.screens.endHudMove
import org.polyfrost.oneconfig.internal.ui.hud.screens.moveHudsBy
import org.polyfrost.oneconfig.internal.ui.hud.screens.notifyHudLocked
import org.polyfrost.oneconfig.internal.ui.hud.screens.placementLabel
import org.polyfrost.oneconfig.internal.ui.hud.screens.selectionOrigin
import org.polyfrost.oneconfig.internal.ui.themes.AccentBrush
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Layout(hud: Hud?, query: String) {
    if (hud == null) return

    LaunchedEffect(hud) { repairHudStaticSize(hud) }

    HudSettingsContent(hud) {
        var locked by remember { mutableStateOf(hud.locked) }
        var staticWidth by remember { mutableStateOf(hud.staticWidth) }
        var staticW by remember { mutableStateOf(hud.staticW) }
        var staticH by remember { mutableStateOf(hud.staticH) }
        var alignment by remember { mutableStateOf(hud.alignment) }
        var growthAnchor by remember { mutableStateOf(hud.effectiveGrowthAnchor) }
        val growthAlign = growthAnchor.toAlign() ?: PolyAlign.Center
        var padLeft by remember { mutableStateOf(hud.padLeft) }
        var padRight by remember { mutableStateOf(hud.padRight) }
        var padTop by remember { mutableStateOf(hud.padTop) }
        var padBottom by remember { mutableStateOf(hud.padBottom) }

        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Section("Position", query, "Reset to default position", "Lock position") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResetHudPositionButton(hud)
                    hud.externalPlacement?.takeIf { it.moved || it.scaled }?.let {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(placementLabel(it), color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
                            val resettingFollowsOtherMod = !it.moved && it.scaled
                            if (resettingFollowsOtherMod) {
                                Text("Reset position to follow it", color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
                            }
                        }
                    }
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

            if (hud is LegacyHudMarker) {
                if (hud.supportsScale) Section("Scale", query) {
                    HudSettingTarget(hud, "customScale") {
                        NumberSpinner(
                            hud.customScale,
                            { Snapshot.withMutableSnapshot { hud.customScale = it } },
                            0.25f,
                            4f,
                            0.25f,
                            width = 176.dp,
                            unit = "x",
                            label = "Scale",
                        )
                    }
                }
            } else {
                Section("GUI Scale", query, "Use Minecraft GUI Scale", "Custom Scale") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                hud.customScale,
                                { Snapshot.withMutableSnapshot { hud.customScale = it } },
                                0.25f,
                                4f,
                                0.25f,
                                width = 176.dp,
                                unit = "x",
                                label = "Custom Scale",
                            )
                        }
                    }
                }
            }

            if (hud !is LegacyHudMarker) {
                Section("Size & Alignment", query, "Position", "X", "Y", "Static Size", "Width", "Height", "Alignment", "Growth anchor") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HudPositionFields(hud)

                        HudSettingTarget(hud, "staticWidth") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SwitchControl(staticWidth) {
                                    Snapshot.withMutableSnapshot {
                                        staticWidth = it; hud.staticWidth = it
                                        if (it) {
                                            staticW = hud.staticW
                                            staticH = hud.staticH
                                        } else {
                                            hud.updateAndRecalculate()
                                        }
                                    }
                                }
                                Text("Static Size", color = LocalTheme.current.textColor, fontSize = 14.sp)
                            }
                        }

                        if (staticWidth) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HudSettingTarget(hud, "staticW") {
                                    NumberSpinner(
                                        staticW, { Snapshot.withMutableSnapshot { staticW = it; hud.staticW = it } },
                                        20f, 2000f, 1f, width = 132.dp, unit = "px", label = "Width", scrub = true, math = true
                                    )
                                }
                                HudSettingTarget(hud, "staticH") {
                                    NumberSpinner(
                                        staticH, { Snapshot.withMutableSnapshot { staticH = it; hud.staticH = it } },
                                        8f, 2000f, 1f, width = 132.dp, unit = "px", label = "Height", scrub = true, math = true
                                    )
                                }
                            }
                        }

                        HudSettingTarget(hud, if (staticWidth) "alignment" else "growthAnchor") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AlignmentPicker(if (staticWidth) alignment else growthAlign) {
                                Snapshot.withMutableSnapshot {
                                    if (staticWidth) {
                                        alignment = it
                                        hud.alignment = it
                                    } else {
                                        growthAnchor = HudAnchor.of(it)
                                        hud.setGrowthAnchorKeepingPosition(growthAnchor)
                                    }
                                }
                            }
                            Text(
                                if (staticWidth) "Aligns the content inside the fixed box"
                                else "Pins this corner or edge in place;\nthe HUD only grows away from it",
                                color = LocalTheme.current.textColorSecondary,
                                fontSize = 12.sp
                            )
                        }
                        }
                    }
                }

                Section("Padding", query, "Edge padding") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Edge padding", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HudSettingTarget(hud, "padLeft") {
                                NumberSpinner(
                                    padLeft, { Snapshot.withMutableSnapshot { padLeft = it; hud.padLeft = it } },
                                    0f, 100f, 1f, width = 88.dp, unit = "px", icon = "pad-left"
                                )
                            }
                            HudSettingTarget(hud, "padRight") {
                                NumberSpinner(
                                    padRight, { Snapshot.withMutableSnapshot { padRight = it; hud.padRight = it } },
                                    0f, 100f, 1f, width = 88.dp, unit = "px", icon = "pad-right"
                                )
                            }
                            HudSettingTarget(hud, "padTop") {
                                NumberSpinner(
                                    padTop, { Snapshot.withMutableSnapshot { padTop = it; hud.padTop = it } },
                                    0f, 100f, 1f, width = 88.dp, unit = "px", icon = "pad-top"
                                )
                            }
                            HudSettingTarget(hud, "padBottom") {
                                NumberSpinner(
                                    padBottom, { Snapshot.withMutableSnapshot { padBottom = it; hud.padBottom = it } },
                                    0f, 100f, 1f, width = 88.dp, unit = "px", icon = "pad-bottom"
                                )
                            }
                        }
                    }
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
    val disabledAlpha by animateFloatAsState(if (enabled) 0f else 1f)
    val accentAlpha by animateFloatAsState(
        when {
            !enabled -> 0f
            isHovered -> 0.75f
            else -> 1f
        },
    )
    val textColor = if (enabled) theme.accentTextColor else theme.textColorSecondary

    Box(
        modifier = Modifier
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .background(theme.borderColor.copy(alpha = theme.borderColor.alpha * disabledAlpha), theme.buttonShape)
            .background(AccentBrush, theme.buttonShape, alpha = accentAlpha)
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
private fun HudPositionFields(hud: Hud) {
    HudEditorViewport.observe()
    val selection = designerSelection(hud)
    val origin = selectionOrigin(selection) ?: Offset(hud.x, hud.y)
    val locked = selection.any { it.locked }
    val screenW = HudManager.guiScreenWidth
    val screenH = HudManager.guiScreenHeight
    var moving by remember { mutableStateOf(false) }
    val startMove = {
        if (!moving) {
            moving = true
            beginHudMove(selection)
        }
    }
    val endMove = {
        if (moving) {
            moving = false
            endHudMove()
        }
    }
    Row(
        modifier = if (locked) {
            Modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) notifyHudLocked()
                    }
                }
            }
        } else {
            Modifier
        },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NumberSpinner(
            origin.x, { v -> selectionOrigin(selection)?.let { moveHudsBy(selection, v - it.x, 0f) } },
            -screenW, screenW * 2f, 1f, width = 132.dp, unit = "px", label = "X",
            onEditEnd = endMove, scrub = true, math = true, enabled = !locked,
            onEditStart = startMove, liveValue = { selectionOrigin(selection)?.x ?: origin.x },
        )
        NumberSpinner(
            origin.y, { v -> selectionOrigin(selection)?.let { moveHudsBy(selection, 0f, v - it.y) } },
            -screenH, screenH * 2f, 1f, width = 132.dp, unit = "px", label = "Y",
            onEditEnd = endMove, scrub = true, math = true, enabled = !locked,
            onEditStart = startMove, liveValue = { selectionOrigin(selection)?.y ?: origin.y },
        )
    }
}
