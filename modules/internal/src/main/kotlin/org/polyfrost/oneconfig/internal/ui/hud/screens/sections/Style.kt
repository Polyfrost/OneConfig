package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.Weight
import org.polyfrost.oneconfig.internal.ui.components.SelectableIconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.settings.ChromaColorAnimation
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPicker
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerModel
import org.polyfrost.oneconfig.internal.ui.components.settings.Dropdown
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.components.settings.Radio
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

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

private enum class ShadowMode(val title: String) {
    Off("Off"),
    Normal("Normal"),
    Full("Full");
}

private enum class MergeMode(val title: String) {
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
    var keepBgWhenHidden by remember { mutableStateOf(hud.keepBgWhenHidden) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardSection(
            "Text", query, "Font", "Size", "Font size", "Modifiers", "Bold", "Italic", "Underline", "Weight", "Case", "Case Type",
            "Color", "Text Color", "Shadow", "Normal", "Full", "Shadow color",
        ) { col1 ->
            GridRow(
                col1,
                first = {
                    HudSettingTarget(hud, "font") {
                        LabeledControl("Font") {
                            Dropdown(Font.entries.map { it.name }, font.ordinal, onSelect = {
                                Snapshot.withMutableSnapshot { font = Font.entries[it]; hud.font = font }
                            }, width = col1)
                        }
                    }
                },
                second = {
                    HudSettingTarget(hud, "textScale") {
                        LabeledControl("Size") {
                            NumberSpinner(
                                hud.textScale * 100f, { Snapshot.withMutableSnapshot { hud.textScale = it / 100f } },
                                25f, 500f, 5f, width = GridCol2, unit = "%"
                            )
                        }
                    }
                },
                third = {
                    HudSettingTarget(hud, "textBold") {
                        LabeledControl("Modifiers") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LocalTheme.current.componentBackground, LocalTheme.current.buttonShape)
                                    .border(1.dp, LocalTheme.current.borderColor, LocalTheme.current.buttonShape)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SelectableIconButton(
                                        Modifiers.Bold.icon,
                                        selected = textBold,
                                        modifier = Modifier.size(18.dp),
                                        onClick = {
                                            Snapshot.withMutableSnapshot {
                                                textBold = !textBold; hud.textBold = textBold
                                            }
                                        }
                                    )
                                    SelectableIconButton(
                                        Modifiers.Italic.icon,
                                        selected = textItalic,
                                        modifier = Modifier.size(18.dp),
                                        onClick = {
                                            Snapshot.withMutableSnapshot {
                                                textItalic = !textItalic; hud.textItalic = textItalic
                                            }
                                        }
                                    )
                                    SelectableIconButton(
                                        Modifiers.Underline.icon,
                                        selected = textUnderline,
                                        modifier = Modifier.size(18.dp),
                                        onClick = {
                                            Snapshot.withMutableSnapshot {
                                                textUnderline = !textUnderline; hud.textUnderline = textUnderline
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
            )
            GridRow(
                col1,
                first = {
                    HudSettingTarget(hud, "textWeight") {
                        LabeledControl("Weight") {
                            Dropdown(Weight.entries.map { it.name }, textWeight.ordinal, onSelect = {
                                Snapshot.withMutableSnapshot { textWeight = Weight.entries[it]; hud.textWeight = textWeight }
                            }, width = col1)
                        }
                    }
                },
                second = {
                    HudSettingTarget(hud, "caseType") {
                        LabeledControl("Case") {
                            Radio(
                                CaseType.entries.map { null }, caseType.ordinal,
                                fillWidth = true, icons = CaseType.entries.map { it.icon },
                            ) {
                                Snapshot.withMutableSnapshot { caseType = CaseType.entries[it]; hud.caseType = it }
                            }
                        }
                    }
                },
                third = {
                    HudSettingTarget(hud, "textColor") {
                        LabeledControl("Text color") {
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
                    LabeledControl("Shadow") {
                        val shadowMode = when {
                            !showShadow -> ShadowMode.Off
                            fullShadow -> ShadowMode.Full
                            else -> ShadowMode.Normal
                        }
                        HudSettingTarget(hud, "showShadow", "fullShadow") {
                            Radio(ShadowMode.entries.map { it.title }, shadowMode.ordinal, fillWidth = true) {
                                val mode = ShadowMode.entries[it]
                                Snapshot.withMutableSnapshot {
                                    showShadow = mode != ShadowMode.Off
                                    hud.showShadow = showShadow
                                    if (showShadow) {
                                        fullShadow = mode == ShadowMode.Full
                                        hud.fullShadow = fullShadow
                                    }
                                }
                            }
                        }
                    }
                },
                third = {
                    if (showShadow) {
                        HudSettingTarget(hud, "shadowColor") {
                            LabeledControl("Shadow color") {
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
            "Background", query, "Show Background", "Radius", "Color", "Background Color", "Merge",
            "Merge with neighbours", "Diagonal", "Keep hidden", "Keep Background When Hidden",
            checked = showBackground,
            onCheckedChange = { Snapshot.withMutableSnapshot { showBackground = it; hud.showBackground = it } },
        ) { col1 ->
            GridRow(
                col1,
                first = {
                    HudSettingTarget(hud, "bgRadius") {
                        LabeledControl("Radius") {
                            NumberSpinner(
                                bgRadius, { Snapshot.withMutableSnapshot { bgRadius = it; hud.bgRadius = it } },
                                0f, 32f, 1f, width = col1, unit = "px"
                            )
                        }
                    }
                },
                third = {
                    HudSettingTarget(hud, "bgColor") {
                        LabeledControl("Color") {
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
            if (hud.canMergeBackground()) {
                GridRow(
                    col1,
                    wide = {
                        LabeledControl("Merge with neighbours") {
                            val mergeMode = when {
                                !mergeBackground -> MergeMode.Off
                                mergeDiagonally -> MergeMode.Diagonal
                                else -> MergeMode.Normal
                            }
                            HudSettingTarget(hud, "mergeBackground", "mergeDiagonally") {
                                Radio(MergeMode.entries.map { it.title }, mergeMode.ordinal, fillWidth = true) {
                                    val mode = MergeMode.entries[it]
                                    Snapshot.withMutableSnapshot {
                                        mergeBackground = mode != MergeMode.Off
                                        hud.mergeBackground = mergeBackground
                                        if (mergeBackground) {
                                            mergeDiagonally = mode == MergeMode.Diagonal
                                            hud.mergeDiagonally = mergeDiagonally
                                        }
                                    }
                                }
                            }
                        }
                    },
                    third = {
                        if (mergeBackground) {
                            HudSettingTarget(hud, "keepBgWhenHidden") {
                                LabeledControl("Keep hidden") {
                                    SwitchControl(keepBgWhenHidden) {
                                        Snapshot.withMutableSnapshot { keepBgWhenHidden = it; hud.keepBgWhenHidden = it }
                                    }
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

/** [ColorPicker] for a HUD colour stored as separate colour, chroma and speed fields */
@Composable
private fun ColorSwatch(
    color: Color,
    chroma: Boolean = false,
    chromaSpeed: Float = 1f,
    onColorChanged: (Color) -> Unit,
    onChromaChanged: (Boolean, Float) -> Unit = { _, _ -> },
) {
    val pickerModel = remember {
        ColorPickerModel(color).apply {
            chromaEnabled = chroma
            this.chromaSpeed = chromaSpeed
        }
    }

    var chromaColor by remember { mutableStateOf(color) }
    ChromaColorAnimation(pickerModel) { chromaColor = it }

    ColorPicker(
        pickerModel,
        if (pickerModel.chromaEnabled) chromaColor else color,
        onColorChanged = { c ->
            onColorChanged(c)
            onChromaChanged(pickerModel.chromaEnabled, pickerModel.chromaSpeed)
        },
        swatch = true,
    )
}
