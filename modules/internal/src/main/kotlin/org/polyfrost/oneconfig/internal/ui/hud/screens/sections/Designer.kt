package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudAnchor
import org.polyfrost.oneconfig.api.hud.v1.TextHud
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.SelectableIconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.ChromaColorAnimation
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerModel
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerPopup
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.components.settings.colorToHex
import org.polyfrost.oneconfig.internal.ui.components.settings.luminance
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.components.AlignmentPicker
import org.polyfrost.oneconfig.internal.ui.hud.components.Dropdown
import org.polyfrost.oneconfig.internal.ui.hud.components.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.hud.components.NumberSpinnerWithIcon
import org.polyfrost.oneconfig.internal.ui.hud.components.Radio
import org.polyfrost.oneconfig.internal.ui.hud.components.RadioValue
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

enum class PaddingType {
    SpaceBetween,
    SpaceAround,
    SpaceEvenly;
}
enum class AlignType(override val icon: String) : RadioValue {
    Left("align-left"),
    Center("align-center"),
    Right("align-right");
}
enum class CaseType(override val icon: String) : RadioValue {
    Aa("capitalized"),
    AA("uppercase"),
    aa("lowercase");
}
enum class Modifiers(override val icon: String) : RadioValue {
    Bold("bold"),
    Italic("italic");
}


@Composable
fun Designer(hud: Hud?, query: String) {
    if (hud == null) return

    LaunchedEffect(hud) { repairHudStaticSize(hud) }

    HudSettingsContent(hud) {
    val staticWidth = hud.staticWidth
    val growthAlign = hud.effectiveGrowthAnchor.toAlign() ?: Alignment.Center
    val caseType = CaseType.entries[hud.caseType.coerceIn(
        0,
        CaseType.entries.lastIndex
    )]
    val textAlign = AlignType.entries[hud.textAlign.coerceIn(
        0,
        AlignType.entries.lastIndex
    )]

    Section("Size & Alignment", query, "Static Size", "Width", "Height") {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HudSettingTarget(hud, "staticWidth") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SwitchControl(staticWidth) {
                        Snapshot.withMutableSnapshot {
                            hud.staticWidth = it
                            if (!it) hud.updateAndRecalculate()
                        }
                    }
                    Text("Static Size", color = LocalTheme.current.textColor, fontSize = 14.sp)
                }
            }

            if (staticWidth) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HudSettingTarget(hud, "staticW") {
                        NumberSpinner(
                            "Width", "px",
                            hud.staticW, { Snapshot.withMutableSnapshot { hud.staticW = it } },
                            20f, 2000f, 1f, width = 128.dp
                        )
                    }
                    HudSettingTarget(hud, "staticH") {
                        NumberSpinner(
                            "Height", "px",
                            hud.staticH, { Snapshot.withMutableSnapshot { hud.staticH = it } },
                            8f, 2000f, 1f, width = 128.dp
                        )
                    }
                }
            }

            HudSettingTarget(hud, if (staticWidth) "alignment" else "growthAnchor") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlignmentPicker(if (staticWidth) hud.alignment else growthAlign) {
                    Snapshot.withMutableSnapshot {
                        if (staticWidth) {
                            hud.alignment = it
                        } else {
                            hud.setGrowthAnchorKeepingPosition(HudAnchor.of(it))
                        }
                    }
                }
                Text(
                    if (staticWidth) "Aligns the content inside the fixed box"
                    else "Pins this corner or edge in place;\nthe HUD only grows away from it",
                    modifier = Modifier.weight(1f),
                    color = LocalTheme.current.textColorSecondary,
                    fontSize = 12.sp,
                    softWrap = true,
                )
            }
            }
        }
    }

    Section("Padding", query, "Edge padding") {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Edge padding", color = LocalTheme.current.textColor, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HudSettingTarget(hud, "padLeft") {
                    NumberSpinnerWithIcon(
                        "pad-left", "px",
                        hud.padLeft, { Snapshot.withMutableSnapshot { hud.padLeft = it } }, 0f, 100f, 1f
                    )
                }
                HudSettingTarget(hud, "padRight") {
                    NumberSpinnerWithIcon(
                        "pad-right", "px",
                        hud.padRight, { Snapshot.withMutableSnapshot { hud.padRight = it } }, 0f, 100f, 1f
                    )
                }
                HudSettingTarget(hud, "padTop") {
                    NumberSpinnerWithIcon(
                        "pad-top", "px",
                        hud.padTop, { Snapshot.withMutableSnapshot { hud.padTop = it } }, 0f, 100f, 1f
                    )
                }
                HudSettingTarget(hud, "padBottom") {
                    NumberSpinnerWithIcon(
                        "pad-bottom", "px",
                        hud.padBottom, { Snapshot.withMutableSnapshot { hud.padBottom = it } }, 0f, 100f, 1f
                    )
                }
            }
        }
    }

    Section("Text Options", query, "Font", "Font size", "Modifiers", "Bold", "Italic", "Weight", "Align", "Case Type") {
        Box(
            modifier = Modifier.fillMaxWidth()
                .height(58.dp)
                .background(LocalTheme.current.componentBackground, LocalTheme.current.buttonShape)
                .border(1.dp, LocalTheme.current.borderColor, LocalTheme.current.buttonShape),
            contentAlignment = Alignment.Center
        ) {
            val previewText = run {
                val base = "Hello, OneConfig!"
                val raw = if ((hud as? TextHud)?.brackets == true) "[$base]" else base
                when (caseType.ordinal) {
                    1 -> raw.uppercase()
                    2 -> raw.lowercase()
                    else -> raw
                }
            }
            val density = LocalDensity.current.density / Platform.screen().surfaceRatio()
            val textColor = Color(hud.textColor)
            if (hud.font == Font.Poppins) {
                val fontName = hud.getPoppinsFontName()
                val polyFont = PolyFont(fontName, 14f * hud.textScale / density)
                val metrics = polyFont.metrics
                val textW = polyFont.measureTextWidth(previewText)
                val textH = metrics.descent - metrics.ascent
                val textUnderline = hud.textUnderline
                Canvas(modifier = Modifier.size(textW.dp, textH.dp)) { x, y, _, _ ->
                    val baseline = y - metrics.ascent
                    text(previewText, x, baseline, textColor, polyFont)
                    if (textUnderline) {
                        val underlinePos = metrics.underlinePosition
                        line(x, baseline + underlinePos, x + textW, baseline + underlinePos, textColor, metrics.underlineThickness)
                    }
                }
            } else {
                val mcText = buildString {
                    if (hud.textBold) append("§l")
                    if (hud.textItalic) append("§o")
                    if (hud.textUnderline) append("§n")
                    append(previewText)
                }
                val scale = hud.textScale / density
                val textW = McFontQueue.measureTextWidth(mcText, scale)
                val textH = McFontQueue.measureBlockHeight(mcText.lines().size, scale)
                val showShadow = hud.showShadow
                val fullShadow = hud.fullShadow
                val shadowColor = hud.shadowColor
                Canvas(modifier = Modifier.size(textW.dp, textH.dp)) { x, y, _, _ ->
                    if (showShadow && fullShadow) {
                        mcTextOutline(mcText, x, y, textColor.argb, shadowColor, scale)
                    } else {
                        this.mcText(
                            mcText,
                            x,
                            y,
                            textColor.argb,
                            showShadow,
                            scale
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HudSettingTarget(hud, "font") {
                Dropdown(
                    "Font",
                    hud.font,
                    { Snapshot.withMutableSnapshot { hud.font = it } }
                )
            }
            HudSettingTarget(hud, "textScale") {
                NumberSpinner(
                    "Font size", "align", "px",
                    hud.textScale * 14f, { Snapshot.withMutableSnapshot { hud.textScale = it / 14f } },
                    6f, 64f, 1f, width = 112.dp
                )
            }
            HudSettingTarget(hud, "textBold", "textItalic") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Modifiers", color = LocalTheme.current.textColor, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .background(LocalTheme.current.componentBackground, LocalTheme.current.buttonShape)
                        .border(1.dp, LocalTheme.current.borderColor, LocalTheme.current.buttonShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SelectableIconButton(
                            Modifiers.Bold.icon,
                            selected = hud.textBold,
                            modifier = Modifier.size(18.dp),
                            onClick = {
                                Snapshot.withMutableSnapshot {
                                    hud.textBold = !hud.textBold
                                }
                            }
                        )
                        SelectableIconButton(
                            Modifiers.Italic.icon,
                            selected = hud.textItalic,
                            modifier = Modifier.size(18.dp),
                            onClick = {
                                Snapshot.withMutableSnapshot {
                                    hud.textItalic = !hud.textItalic
                                }
                            }
                        )
                    }
                }
            }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HudSettingTarget(hud, "textWeight") {
                Dropdown(
                    "Weight",
                    hud.textWeight,
                    { Snapshot.withMutableSnapshot { hud.textWeight = it } }
                )
            }
            HudSettingTarget(hud, "textAlign") {
                Radio(
                    "Align",
                    textAlign,
                ) { a -> Snapshot.withMutableSnapshot { hud.textAlign = a.ordinal } }
            }
            HudSettingTarget(hud, "caseType") {
                Radio(
                    "Case Type",
                    caseType,
                ) { c -> Snapshot.withMutableSnapshot { hud.caseType = c.ordinal } }
            }
        }
    }
    }
}

/** Whether a HUD panel block described by [labels] is kept by the panel's search [query] */
internal fun hudSearchMatches(query: String, vararg labels: String) =
    query.isEmpty() || labels.any { searchMatches(it, query) }

/** Hidden unless [query] matches [title] or one of the [labels] of the controls inside */
@Composable
fun Section(title: String, query: String, vararg labels: String, content: @Composable () -> Unit) {
    if (!hudSearchMatches(query, title, *labels)) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), color = LocalTheme.current.textColorSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
internal fun ColorButton(
    label: String,
    color: Color,
    chroma: Boolean = false,
    chromaSpeed: Float = 1f,
    onColorChanged: (Color) -> Unit,
    onChromaChanged: (Boolean, Float) -> Unit = { _, _ -> },
) {
    var expanded by remember { mutableStateOf(false) }
    val pickerModel = remember {
        ColorPickerModel(color).apply {
            chromaEnabled = chroma
            this.chromaSpeed = chromaSpeed
        }
    }
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    var displayColor by remember { mutableStateOf(color) }
    ChromaColorAnimation(pickerModel) { displayColor = it }
    val shownColor = if (pickerModel.chromaEnabled) displayColor else color
    val textColor = animateColorAsState(if (shownColor.luminance() > 0.6f) Color.Black else Color.White)
    val borderColor = animateColorAsState(
        if (isHovered || expanded) theme.textColor.copy(0.3f) else theme.borderColor
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = theme.textColor, fontSize = 14.sp)
        Box {
            Row(
                modifier = Modifier
                    .onClick(interactionSource) { expanded = !expanded }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .background(shownColor, theme.sideBarNavigationEntryShape)
                    .border(1.dp, borderColor, theme.sideBarNavigationEntryShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon("paintbrush", color = textColor, modifier = Modifier.size(14.dp))
                Text("#${colorToHex(shownColor)}", color = textColor, fontSize = 13.sp)
            }
            if (expanded) {
                Popup(
                    alignment = Alignment.TopEnd,
                    offset = DpOffset(0.dp, 44.dp),
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    ColorPickerPopup(
                        model = pickerModel,
                        onColorChanged = { c ->
                            displayColor = c
                            onColorChanged(c)
                            onChromaChanged(pickerModel.chromaEnabled, pickerModel.chromaSpeed)
                        },
                        onClose = { expanded = false },
                    )
                }
            }
        }
    }
}
