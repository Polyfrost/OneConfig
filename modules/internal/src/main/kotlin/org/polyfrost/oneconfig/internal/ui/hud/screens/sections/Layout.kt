package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudAnchor
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.components.settings.RangeSeparatorWidth
import org.polyfrost.oneconfig.internal.ui.components.settings.formatSpinnerValue
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.HudPositionTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.components.AlignmentPicker
import org.polyfrost.oneconfig.internal.ui.hud.hudOptionsCanReset
import org.polyfrost.oneconfig.internal.ui.hud.resetHudOptions
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudEditorViewport
import org.polyfrost.oneconfig.internal.ui.hud.screens.beginHudMove
import org.polyfrost.oneconfig.internal.ui.hud.screens.designerSelection
import org.polyfrost.oneconfig.internal.ui.hud.screens.endHudMove
import org.polyfrost.oneconfig.internal.ui.hud.screens.moveHudsBy
import org.polyfrost.oneconfig.internal.ui.hud.screens.notifyHudLocked
import org.polyfrost.oneconfig.internal.ui.hud.screens.placementLabel
import org.polyfrost.oneconfig.internal.ui.hud.screens.selectionOrigin
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val POSITION_SCALE_OPTIONS = listOf("locked", "useGuiScale", "customScale")
private val SIZE_OPTIONS = listOf("minW", "maxW", "minH", "maxH", "fitContent", "alignment", "growthAnchor")
private val PADDING_OPTIONS = listOf("padLeft", "padRight", "padTop", "padBottom")

@Composable
fun Layout(hud: Hud?, query: String) {
    if (hud == null) return

    HudSettingsContent(hud) {
        var locked by remember { mutableStateOf(hud.locked) }
        var growthAnchor by remember { mutableStateOf(hud.effectiveGrowthAnchor) }
        val growthAlign = growthAnchor.toAlign() ?: PolyAlign.Center
        var padLeft by remember { mutableStateOf(hud.padLeft) }
        var padRight by remember { mutableStateOf(hud.padRight) }
        var padTop by remember { mutableStateOf(hud.padTop) }
        var padBottom by remember { mutableStateOf(hud.padBottom) }

        // opens with a field per side when the sides already differ
        var perSide by remember { mutableStateOf(padLeft != padRight || padTop != padBottom) }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val scalable = hud !is LegacyHudMarker || hud.supportsScale
            CardSection(
                if (scalable) "Position & Scale" else "Position", query,
                "X", "Y", "Lock position", "Reset to default position",
                *if (scalable) arrayOf("Scale", "Follow GUI scale", "Use Minecraft GUI Scale") else emptyArray(),
                onReset = { resetHudOptions(hud, POSITION_SCALE_OPTIONS, position = true) },
                canReset = hudOptionsCanReset(hud, POSITION_SCALE_OPTIONS, position = true),
            ) { col1 ->
                HudPositionFields(hud, col1) {
                    if (scalable) {
                        HudSettingTarget(hud, "customScale") {
                            LabeledControl("Scale") {
                                NumberSpinner(
                                    hud.customScale, { Snapshot.withMutableSnapshot { hud.customScale = it } },
                                    0.25f, 4f, 0.25f, width = GridCol3, unit = "x"
                                )
                            }
                        }
                    }
                }
                hud.externalPlacement?.takeIf { it.moved || it.scaled }?.let {
                    val secondary = LocalTheme.current.textColorSecondary
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(placementLabel(it), color = secondary, fontSize = 12.sp)
                        if (!it.moved && it.scaled) Text("Reset position to follow it", color = secondary, fontSize = 12.sp)
                    }
                }
                HudSettingTarget(hud, "locked") {
                    SwitchRow("Lock position", locked) {
                        Snapshot.withMutableSnapshot { locked = it; hud.locked = it }
                    }
                }
                if (hud !is LegacyHudMarker) {
                    HudSettingTarget(hud, "useGuiScale") {
                        SwitchRow("Follow GUI scale", hud.useGuiScale) {
                            Snapshot.withMutableSnapshot { setFollowGuiScale(hud, it) }
                        }
                    }
                }
            }

            if (hud !is LegacyHudMarker) {
                CardSection(
                    "Size & Alignment", query, "Width", "Height", "Min width", "Max width", "Min height", "Max height",
                    "Scale content to fit", "Anchor", "Content alignment", "Alignment", "Growth anchor",
                    onReset = { resetHudOptions(hud, SIZE_OPTIONS) },
                    canReset = hudOptionsCanReset(hud, SIZE_OPTIONS),
                ) { col1 ->
                    // the limits are read from the HUD every time so a canvas resize drag is reflected right away
                    val capped = hud.maxW > 0f || hud.maxH > 0f
                    val limited = hud.hasSizeLimits
                    val pinned = hud.isFixedW && hud.isFixedH

                    SizeRangeRow(hud, "width", 20f, hud.minW, hud.maxW, { hud.minW = it }, { hud.maxW = it })
                    SizeRangeRow(hud, "height", 8f, hud.minH, hud.maxH, { hud.minH = it }, { hud.maxH = it })

                    if (capped) {
                        HudSettingTarget(hud, "fitContent") {
                            SwitchRow("Scale content to fit", hud.fitContent) {
                                Snapshot.withMutableSnapshot { hud.fitContent = it }
                            }
                        }
                    }

                    if (limited) {
                        HudSettingTarget(hud, "alignment") {
                            LabeledControl("Content alignment") {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AlignmentPicker(hud.alignment) {
                                        Snapshot.withMutableSnapshot { hud.alignment = it }
                                    }
                                    Text(
                                        "Aligns the content inside the size limits",
                                        color = LocalTheme.current.textColorSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (!pinned) {
                        HudSettingTarget(hud, "growthAnchor") {
                            LabeledControl("Anchor") {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AlignmentPicker(growthAlign) {
                                        Snapshot.withMutableSnapshot {
                                            growthAnchor = HudAnchor.of(it)
                                            hud.setGrowthAnchorKeepingPosition(growthAnchor)
                                        }
                                    }
                                    Text(
                                        "Pins this corner or edge in place;\nthe HUD only grows away from it",
                                        color = LocalTheme.current.textColorSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                CardSection(
                    "Padding", query, "Horizontal", "Vertical", "Per side", "Left", "Right", "Top", "Bottom",
                    onReset = { resetHudOptions(hud, PADDING_OPTIONS) },
                    canReset = hudOptionsCanReset(hud, PADDING_OPTIONS),
                ) { col1 ->
                    val perSideSwitch: @Composable () -> Unit = {
                        LabeledControl("Per side") {
                            SwitchControl(perSide) {
                                perSide = it
                                // leaving per side mode evens the sides out so the two fields show what is applied
                                if (!it) Snapshot.withMutableSnapshot {
                                    padRight = padLeft; hud.padRight = padLeft
                                    padBottom = padTop; hud.padBottom = padTop
                                }
                            }
                        }
                    }

                    RowWithFold(
                        perSide,
                        row = {
                            GridRow(
                                col1,
                                first = {
                                    if (perSide) {
                                        HudSettingTarget(hud, "padLeft") { PadSpinner("Left", padLeft, col1) { padLeft = it; hud.padLeft = it } }
                                    } else {
                                        PadSpinner("Horizontal", padLeft, col1) {
                                            padLeft = it; hud.padLeft = it
                                            padRight = it; hud.padRight = it
                                        }
                                    }
                                },
                                second = {
                                    if (perSide) {
                                        HudSettingTarget(hud, "padTop") { PadSpinner("Top", padTop, GridCol2) { padTop = it; hud.padTop = it } }
                                    } else {
                                        PadSpinner("Vertical", padTop, GridCol2) {
                                            padTop = it; hud.padTop = it
                                            padBottom = it; hud.padBottom = it
                                        }
                                    }
                                },
                                third = perSideSwitch,
                            )
                        },
                        folded = {
                            GridRow(
                                col1,
                                first = { HudSettingTarget(hud, "padRight") { PadSpinner("Right", padRight, col1) { padRight = it; hud.padRight = it } } },
                                second = { HudSettingTarget(hud, "padBottom") { PadSpinner("Bottom", padBottom, GridCol2) { padBottom = it; hud.padBottom = it } } },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PadSpinner(label: String, value: Float, width: Dp, onChange: (Float) -> Unit) {
    LabeledControl(label) {
        NumberSpinner(value, { Snapshot.withMutableSnapshot { onChange(it) } }, 0f, 100f, 1f, width = width, unit = "px")
    }
}

/** X and Y of the selection's top left corner, moving every selected HUD together, with [third] in the last column */
@Composable
private fun HudPositionFields(hud: Hud, col1: Dp, third: @Composable () -> Unit) {
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
    // pressing a locked field explains why it won't change
    val lockNotice = if (locked) {
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
    }
    GridRow(
        col1,
        first = {
            HudPositionTarget(hud, lockNotice) {
                LabeledControl("X") {
                    NumberSpinner(
                        origin.x, { v -> selectionOrigin(selection)?.let { moveHudsBy(selection, v - it.x, 0f) } },
                        -screenW, screenW * 2f, 1f, width = col1, unit = "px",
                        onEditEnd = endMove, scrub = true, math = true, enabled = !locked,
                        onEditStart = startMove, liveValue = { selectionOrigin(selection)?.x ?: origin.x },
                    )
                }
            }
        },
        second = {
            HudPositionTarget(hud, lockNotice) {
                LabeledControl("Y") {
                    NumberSpinner(
                        origin.y, { v -> selectionOrigin(selection)?.let { moveHudsBy(selection, 0f, v - it.y) } },
                        -screenH, screenH * 2f, 1f, width = GridCol2, unit = "px",
                        onEditEnd = endMove, scrub = true, math = true, enabled = !locked,
                        onEditStart = startMove, liveValue = { selectionOrigin(selection)?.y ?: origin.y },
                    )
                }
            }
        },
        third = third,
    )
}

/** Keeps [hud] the same size on screen */
private fun setFollowGuiScale(hud: Hud, follow: Boolean) {
    if (hud.useGuiScale == follow) return
    val gui = Platform.compatibility().options().guiScale.takeIf { it > 0f } ?: 1f
    if (follow) hud.customScale *= hud.fixedGuiScale / gui else hud.fixedGuiScale = gui
    hud.useGuiScale = follow
}

/**
 * One axis' size limits as a minimum and maximum, where 0 is no limit and reads as None
 *
 * Giving both ends the same value pins the axis to that size
 */
@Composable
private fun SizeRangeRow(
    hud: Hud,
    axis: String,
    floor: Float,
    min: Float,
    max: Float,
    onMin: (Float) -> Unit,
    onMax: (Float) -> Unit,
) {
    val id = if (axis == "width") "W" else "H"
    val none: (Float) -> String = { if (it <= 0f) "None" else formatSpinnerValue(it, 1f) }
    GridRow(
        GridCol3,
        wide = {
            LabeledControl(axis.replaceFirstChar { it.uppercase() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HudSettingTarget(hud, "min$id") {
                        NumberSpinner(
                            min, { v -> Snapshot.withMutableSnapshot { onMin(v); hud.updateAndRecalculate() } },
                            0f, 2000f, 1f, width = GridCol3, format = none, unit = "px", scrub = true, math = true, label = "Min",
                        )
                    }
                    Box(Modifier.width(RangeSeparatorWidth), contentAlignment = Alignment.Center) {
                        Text("-", color = LocalTheme.current.textColorSecondary, fontSize = 13.sp)
                    }
                    HudSettingTarget(hud, "max$id") {
                        NumberSpinner(
                            max, { v -> Snapshot.withMutableSnapshot { onMax(v); hud.updateAndRecalculate() } },
                            0f, 2000f, 1f, width = GridCol3, format = none, unit = "px", scrub = true, math = true, label = "Max",
                        )
                    }
                }
            }
        },
    )
}
