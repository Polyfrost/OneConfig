@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.Weight
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.ColorSwatchButton
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.NumberField
import org.polyfrost.oneconfig.api.ui.v1.components.RadioGroup
import org.polyfrost.oneconfig.api.ui.v1.components.Switch
import org.polyfrost.oneconfig.internal.ui.components.settings.Dropdown
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.hudOptionsCanReset
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.hud.resetHudOptions
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val TEXT_OPTIONS = listOf(
    "font", "textScale", "textBold", "textItalic", "textUnderline", "textWeight", "caseType", "textColor", "textChroma",
    "textChromaSpeed", "showShadow", "fullShadow", "shadowColor", "shadowChroma", "shadowChromaSpeed",
)
private val BACKGROUND_OPTIONS = listOf(
    "showBackground", "bgRadius", "bgColor", "bgChroma", "bgChromaSpeed", "mergeBackground", "mergeDiagonally", "mergeAcrossColours",
    "keepBgWhenHidden", "borderEnabled", "borderColor", "borderChroma", "borderChromaSpeed", "borderSize",
)

enum class CaseType(val icon: String) {
    Aa("capitalized"),
    AA("uppercase"),
    aa("lowercase");
}
enum class Modifiers(val icon: String) {
    Bold("bold"),
    Italic("italic"),
    Underline("underline");
}

internal enum class ShadowMode(val title: String) {
    Off("Off"),
    Normal("Normal"),
    Full("Full");
}

internal enum class MergeMode(val title: String) {
    Off("Off"),
    Normal("Normal"),
    Diagonal("Diagonal");
}

@Composable
fun Style(hud: Hud?, query: String) {
    if (hud == null) return

    LaunchedEffect(hud) { repairHudStaticSize(hud) }

    HudSettingsContent(hud) {
    var font by remember { mutableStateOf(hud.font) }
    var textBold by remember { mutableStateOf(hud.textBold) }
    var textItalic by remember { mutableStateOf(hud.textItalic) }
    var textUnderline by remember { mutableStateOf(hud.textUnderline) }
    var textWeight by remember { mutableStateOf(hud.textWeight) }
    var caseType by remember {
        mutableStateOf(
            CaseType.entries[hud.caseType.coerceIn(
                0,
                CaseType.entries.lastIndex
            )]
        )
    }
    var textColor by remember { mutableStateOf(Color(hud.textColor)) }
    var showShadow by remember { mutableStateOf(hud.showShadow) }
    var fullShadow by remember { mutableStateOf(hud.fullShadow) }
    var textChroma by remember { mutableStateOf(hud.textChroma) }
    var textChromaSpeed by remember { mutableStateOf(hud.textChromaSpeed) }
    var shadowColor by remember { mutableStateOf(Color(hud.shadowColor)) }
    var shadowChroma by remember { mutableStateOf(hud.shadowChroma) }
    var shadowChromaSpeed by remember { mutableStateOf(hud.shadowChromaSpeed) }
    var showBackground by remember { mutableStateOf(hud.showBackground) }
    var bgColor by remember { mutableStateOf(Color(hud.bgColor)) }
    var bgChroma by remember { mutableStateOf(hud.bgChroma) }
    var bgChromaSpeed by remember { mutableStateOf(hud.bgChromaSpeed) }
    var bgRadius by remember { mutableStateOf(hud.bgRadius) }
    var mergeBackground by remember { mutableStateOf(hud.mergeBackground) }
    var mergeDiagonally by remember { mutableStateOf(hud.mergeDiagonally) }
    var mergeAcrossColours by remember { mutableStateOf(hud.mergeAcrossColours) }
    var keepBgWhenHidden by remember { mutableStateOf(hud.keepBgWhenHidden) }
    var borderEnabled by remember { mutableStateOf(hud.borderEnabled) }
    var borderSize by remember { mutableStateOf(hud.borderSize) }
    var borderColor by remember { mutableStateOf(Color(hud.borderColor)) }
    var borderChroma by remember { mutableStateOf(hud.borderChroma) }
    var borderChromaSpeed by remember { mutableStateOf(hud.borderChromaSpeed) }
    val hasBorderOptions = hud.hasBuiltInBorderOptions && hud.tree?.getProp("borderEnabled") != null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardSection(
            hudPanelText("text", "Text"), query, hudPanelText("font", "Font"), hudPanelText("size", "Size"), "Font size", hudPanelText("modifiers", "Modifiers"), "Bold", "Italic", "Underline", hudPanelText("weight", "Weight"), hudPanelText("case", "Case"), "Case Type",
            hudPanelText("color", "Color"), "Text Color", hudPanelText("shadow", "Shadow"), hudPanelText("mode.normal", "Normal"), hudPanelText("mode.full", "Full"), hudPanelText("shadow_color", "Shadow color"),
            onReset = { resetHudOptions(hud, TEXT_OPTIONS) },
            canReset = hudOptionsCanReset(hud, TEXT_OPTIONS),
        ) { col1 ->
            GridRow(
                col1,
                first = {
                    HudSettingTarget(hud, "font") {
                        LabeledControl(hudPanelText("font", "Font")) {
                            Dropdown(Font.entries.map { it.name }, font.ordinal, onSelect = {
                                Snapshot.withMutableSnapshot { font = Font.entries[it]; hud.font = font }
                            }, width = col1)
                        }
                    }
                },
                second = {
                    HudSettingTarget(hud, "textScale") {
                        LabeledControl(hudPanelText("size", "Size")) {
                            NumberField(
                                hud.textScale * 100f, { Snapshot.withMutableSnapshot { hud.textScale = it / 100f } },
                                range = 25f..500f, step = 5f, roundToDecimals = false, fractionDigits = 2, unit = "%", width = GridCol2,
                                fontSize = 13.sp,
                            )
                        }
                    }
                },
                third = {
                    HudSettingTarget(hud, "textBold") {
                        LabeledControl(hudPanelText("modifiers", "Modifiers")) {
                            ModifierButtons(
                                listOf(
                                    ModifierToggle(Modifiers.Bold.icon, hudPanelText("bold", "Bold"), textBold) {
                                        Snapshot.withMutableSnapshot { textBold = !textBold; hud.textBold = textBold }
                                    },
                                    ModifierToggle(Modifiers.Italic.icon, hudPanelText("italic", "Italic"), textItalic) {
                                        Snapshot.withMutableSnapshot { textItalic = !textItalic; hud.textItalic = textItalic }
                                    },
                                    ModifierToggle(Modifiers.Underline.icon, hudPanelText("underline", "Underline"), textUnderline) {
                                        Snapshot.withMutableSnapshot { textUnderline = !textUnderline; hud.textUnderline = textUnderline }
                                    },
                                ),
                            )
                        }
                    }
                },
            )
            GridRow(
                col1,
                first = {
                    HudSettingTarget(hud, "textWeight") {
                        LabeledControl(hudPanelText("weight", "Weight")) {
                            Dropdown(Weight.entries.map { it.name }, textWeight.ordinal, onSelect = {
                                Snapshot.withMutableSnapshot { textWeight = Weight.entries[it]; hud.textWeight = textWeight }
                            }, width = col1)
                        }
                    }
                },
                second = {
                    HudSettingTarget(hud, "caseType") {
                        LabeledControl(hudPanelText("case", "Case")) {
                            RadioGroup(
                                caseType, CaseType.entries, { case ->
                                    Snapshot.withMutableSnapshot { caseType = case; hud.caseType = case.ordinal }
                                },
                                label = { "" },
                                orientation = Orientation.Horizontal,
                                fillWidth = true,
                                icon = { it.icon },
                            )
                        }
                    }
                },
                third = {
                    HudSettingTarget(hud, "textColor") {
                        LabeledControl(hudPanelText("text_color", "Text color")) {
                            ColorSwatch(
                                textColor,
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
                    }
                },
            )
            GridRow(
                col1,
                wide = {
                    LabeledControl(hudPanelText("shadow", "Shadow")) {
                        val shadowMode = when {
                            !showShadow -> ShadowMode.Off
                            fullShadow -> ShadowMode.Full
                            else -> ShadowMode.Normal
                        }
                        HudSettingTarget(hud, "showShadow", "fullShadow") {
                            RadioGroup(
                                shadowMode, ShadowMode.entries,
                                label = { hudPanelText("mode.${it.name.lowercase()}", it.title) },
                                orientation = Orientation.Horizontal,
                                fillWidth = true,
                                onSelect = { mode ->
                                    Snapshot.withMutableSnapshot {
                                        showShadow = mode != ShadowMode.Off
                                        hud.showShadow = showShadow
                                        if (showShadow) {
                                            fullShadow = mode == ShadowMode.Full
                                            hud.fullShadow = fullShadow
                                        }
                                    }
                                },
                            )
                        }
                    }
                },
                third = {
                    FadingCell(showShadow) {
                        HudSettingTarget(hud, "shadowColor") {
                            LabeledControl(hudPanelText("shadow_color", "Shadow color")) {
                                ColorSwatch(
                                    shadowColor,
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
                },
            )
        }

        CardSection(
            hudPanelText("background", "Background"), query, "Show Background", hudPanelText("radius", "Radius"), hudPanelText("color", "Color"), "Background Color", "Merge",
            hudPanelText("merge", "Merge with neighbours"), hudPanelText("mode.diagonal", "Diagonal"), hudPanelText("merge_colours", "Merge with different colours"), hudPanelText("keep_hidden", "Keep hidden"), "Keep Background When Hidden", hudPanelText("border", "Border"), hudPanelText("border_size", "Border size"),
            hudPanelText("border_color", "Border color"),
            checked = showBackground,
            onCheckedChange = { Snapshot.withMutableSnapshot { showBackground = it; hud.showBackground = it } },
            onReset = { resetHudOptions(hud, BACKGROUND_OPTIONS) },
            canReset = hudOptionsCanReset(hud, BACKGROUND_OPTIONS),
        ) { col1 ->
            GridRow(
                col1,
                first = {
                    HudSettingTarget(hud, "bgRadius") {
                        LabeledControl(hudPanelText("radius", "Radius")) {
                            NumberField(
                                bgRadius, { Snapshot.withMutableSnapshot { bgRadius = it; hud.bgRadius = it } },
                                range = 0f..32f, roundToDecimals = false, fractionDigits = 2, unit = "px", width = col1,
                                fontSize = 13.sp,
                            )
                        }
                    }
                },
                third = {
                    HudSettingTarget(hud, "bgColor") {
                        LabeledControl(hudPanelText("color", "Color")) {
                            ColorSwatch(
                                bgColor,
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
                    }
                },
            )
            if (hasBorderOptions) {
                GridRow(
                    col1,
                    first = {
                        HudSettingTarget(hud, "borderEnabled") {
                            LabeledControl(hudPanelText("border", "Border")) {
                                Switch(borderEnabled, {
                                    Snapshot.withMutableSnapshot { borderEnabled = it; hud.borderEnabled = it }
                                }, stateLabel = true)
                            }
                        }
                    },
                    second = {
                        FadingCell(borderEnabled) {
                            HudSettingTarget(hud, "borderSize") {
                                LabeledControl(hudPanelText("border_size", "Border size")) {
                                    NumberField(
                                        borderSize, { Snapshot.withMutableSnapshot { borderSize = it; hud.borderSize = it } },
                                        range = 0f..10f, step = 0.5f, roundToDecimals = false, fractionDigits = 2, unit = "px", width = GridCol2,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    },
                    third = {
                        FadingCell(borderEnabled) {
                            HudSettingTarget(hud, "borderColor", "borderChroma", "borderChromaSpeed") {
                                LabeledControl(hudPanelText("border_color", "Border color")) {
                                    ColorSwatch(
                                        borderColor,
                                        chroma = borderChroma, chromaSpeed = borderChromaSpeed,
                                        onColorChanged = {
                                            Snapshot.withMutableSnapshot { borderColor = it; hud.borderColor = it.toArgb() }
                                        },
                                        onChromaChanged = { en, sp ->
                                            Snapshot.withMutableSnapshot {
                                                borderChroma = en; hud.borderChroma = en
                                                borderChromaSpeed = sp; hud.borderChromaSpeed = sp
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
            if (hud.canMergeBackground()) {
                GridRow(
                    col1,
                    wide = {
                        LabeledControl(hudPanelText("merge", "Merge with neighbours")) {
                            val mergeMode = when {
                                !mergeBackground -> MergeMode.Off
                                mergeDiagonally -> MergeMode.Diagonal
                                else -> MergeMode.Normal
                            }
                            HudSettingTarget(hud, "mergeBackground", "mergeDiagonally") {
                                RadioGroup(
                                    mergeMode, MergeMode.entries,
                                    label = { hudPanelText("mode.${it.name.lowercase()}", it.title) },
                                    orientation = Orientation.Horizontal,
                                    fillWidth = true,
                                    onSelect = { mode ->
                                        Snapshot.withMutableSnapshot {
                                            mergeBackground = mode != MergeMode.Off
                                            hud.mergeBackground = mergeBackground
                                            if (mergeBackground) {
                                                mergeDiagonally = mode == MergeMode.Diagonal
                                                hud.mergeDiagonally = mergeDiagonally
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    },
                    third = {
                        FadingCell(mergeBackground) {
                            HudSettingTarget(hud, "keepBgWhenHidden") {
                                LabeledControl(hudPanelText("keep_hidden", "Keep hidden")) {
                                    Switch(keepBgWhenHidden, {
                                        Snapshot.withMutableSnapshot { keepBgWhenHidden = it; hud.keepBgWhenHidden = it }
                                    }, stateLabel = true)
                                }
                            }
                        }
                    },
                )
                GridRow(
                    col1,
                    wide = {
                        FadingCell(mergeBackground) {
                            HudSettingTarget(hud, "mergeAcrossColours") {
                                LabeledControl(hudPanelText("merge_colours", "Merge with different colours")) {
                                    Switch(mergeAcrossColours, {
                                        Snapshot.withMutableSnapshot { mergeAcrossColours = it; hud.mergeAcrossColours = it }
                                    }, stateLabel = true)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
    }
}

internal class ModifierToggle(val icon: String, val label: String, val selected: Boolean, val onToggle: () -> Unit)

/** A row of icon switches that light up while on */
@Composable
internal fun ModifierButtons(toggles: List<ModifierToggle>) {
    val theme = LocalTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.componentBackground, theme.buttonShape)
            .border(1.dp, theme.borderColor, theme.buttonShape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (toggle in toggles) {
                IconButton(
                    toggle.icon,
                    toggle.label,
                    modifier = Modifier.size(28.dp),
                    size = ControlSize.Large,
                    selected = toggle.selected,
                    onClick = toggle.onToggle,
                )
            }
        }
    }
}

/** [ColorPicker] for a HUD colour stored as separate colour, chroma and speed fields */
@Composable
internal fun ColorSwatch(
    color: Color,
    chroma: Boolean = false,
    chromaSpeed: Float = 1f,
    onColorChanged: (Color) -> Unit,
    onChromaChanged: (Boolean, Float) -> Unit = { _, _ -> },
) {
    ColorSwatchButton(
        PolyColor(color.toArgb(), chroma, chromaSpeed),
        onColorChanged = { picked ->
            onColorChanged(Color(picked.rawArgb))
            onChromaChanged(picked.chroma, picked.chromaSpeed)
        },
        chromaCapable = true,
    )
}
