package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.compose.backend.drawPoly
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.TextHud
import org.polyfrost.oneconfig.api.hud.v1.Weight
import org.polyfrost.oneconfig.internal.ui.components.SelectableIconButton
import org.polyfrost.oneconfig.internal.ui.components.ShellPopup
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerModel
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerPopup
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.components.Dropdown
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
fun Style(hud: Hud?, query: String) {
    if (hud == null) return

    LaunchedEffect(hud) { repairHudStaticSize(hud) }

    HudSettingsContent(hud) {
    var font by remember { mutableStateOf(hud.font) }
    var textScale by remember { mutableStateOf(hud.textScale) }
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
    var textAlign by remember {
        mutableStateOf(
            AlignType.entries[hud.textAlign.coerceIn(
                0,
                AlignType.entries.lastIndex
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

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Section(
            "Text", query, "Font", "Font size", "Modifiers", "Bold", "Italic", "Weight", "Align", "Case Type",
            "Text Color", "Text Shadow", "Full Shadow", "Shadow Color",
        ) {
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
                val density = LocalDensity.current.density
                if (font == Font.Poppins) {
                    val fontName = hud.getPoppinsFontName()
                    val polyFont = PolyFont(fontName, 14f * textScale)
                    val metrics = polyFont.metrics
                    val previewLines = previewText.lines()
                    val lineHeight = metrics.spacing
                    val textW = previewLines.maxOf { polyFont.measureTextWidth(it) }
                    val textH = (previewLines.size - 1) * lineHeight + (metrics.descent - metrics.ascent)
                    val argb = textColor.toArgb()
                    Canvas(modifier = Modifier.size((textW / density).dp, (textH / density).dp)) {
                        drawPoly { ctx ->
                            val r = ctx.renderer
                            previewLines.forEachIndexed { index, lineText ->
                                if (lineText.isEmpty()) return@forEachIndexed
                                val baseline = index * lineHeight - metrics.ascent
                                r.text(lineText, 0f, baseline, argb, polyFont)
                                if (textUnderline) {
                                    val lineW = polyFont.measureTextWidth(lineText)
                                    r.line(0f, baseline + metrics.underlinePosition, lineW, baseline + metrics.underlinePosition, argb, metrics.underlineThickness)
                                }
                            }
                        }
                    }
                } else {
                    val mcText = buildString {
                        if (textBold) append("§l")
                        if (textItalic) append("§o")
                        if (textUnderline) append("§n")
                        append(previewText)
                    }
                    val scale = textScale
                    val textW = mcText.lines().maxOf { McFontQueue.measureTextWidth(it, scale) }
                    val textH = McFontQueue.measureBlockHeight(mcText.lines().size, scale)
                    val argb = textColor.toArgb()
                    val shadowColor = hud.shadowColor
                    Canvas(modifier = Modifier.size((textW / density).dp, (textH / density).dp)) {
                        drawPoly { ctx ->
                            if (showShadow && fullShadow) ctx.renderer.mcTextOutline(mcText, 0f, 0f, argb, shadowColor, scale)
                            else ctx.renderer.mcText(mcText, 0f, 0f, argb, showShadow, scale)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HudSettingTarget(hud, "font") {
                    Dropdown(
                        "Font",
                        font,
                        { Snapshot.withMutableSnapshot { font = it; hud.font = it } }
                    )
                }
                HudSettingTarget(hud, "textScale") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Font size", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        NumberSpinner(
                            textScale * 14f, { Snapshot.withMutableSnapshot { val s = it / 14f; textScale = s; hud.textScale = s } },
                            6f, 64f, 1f, width = 112.dp, unit = "px", icon = "align"
                        )
                    }
                }
                HudSettingTarget(hud, "textBold") {
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
                        }
                    }
                }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HudSettingTarget(hud, "textWeight") {
                    Dropdown(
                        "Weight",
                        textWeight,
                        { Snapshot.withMutableSnapshot { textWeight = it; hud.textWeight = it } }
                    )
                }
                HudSettingTarget(hud, "textAlign") {
                    Radio(
                        "Align",
                        textAlign,
                    ) { a -> Snapshot.withMutableSnapshot { textAlign = a; hud.textAlign = a.ordinal } }
                }
                HudSettingTarget(hud, "caseType") {
                    Radio(
                        "Case Type",
                        caseType,
                    ) { c -> Snapshot.withMutableSnapshot { caseType = c; hud.caseType = c.ordinal } }
                }
            }
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
                    HudSettingTarget(hud, "fullShadow") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SwitchControl(fullShadow) {
                                Snapshot.withMutableSnapshot { fullShadow = it; hud.fullShadow = it }
                            }
                            Text("Full Shadow", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        }
                    }
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

        Section(
            "Background", query, "Show Background", "Background Color", "Radius", "Merge With Neighbours",
            "Merge Diagonally", "Keep Background When Hidden",
        ) {
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
                                bgRadius, { Snapshot.withMutableSnapshot { bgRadius = it; hud.bgRadius = it } },
                                0f, 32f, 1f, width = 116.dp, unit = "px", label = "Radius"
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
                            HudSettingTarget(hud, "keepBgWhenHidden") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    SwitchControl(keepBgWhenHidden) {
                                        Snapshot.withMutableSnapshot { keepBgWhenHidden = it; hud.keepBgWhenHidden = it }
                                    }
                                    Text("Keep Background When Hidden", color = LocalTheme.current.textColor, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
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

    // the chroma swatch cycles with the same PolyColor.argb math the HUD uses at render time so the
    // preview matches in-game and the stored base colour is never mutated
    var displayColor by remember { mutableStateOf(color) }
    val baseArgb = color.toArgb()
    LaunchedEffect(pickerModel.chromaEnabled, pickerModel.chromaSpeed, baseArgb) {
        if (!pickerModel.chromaEnabled) {
            displayColor = Color(baseArgb)
            return@LaunchedEffect
        }
        while (true) {
            withInfiniteAnimationFrameNanos {
                displayColor = Color(PolyColor(baseArgb, true, pickerModel.chromaSpeed).argb)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = theme.textColor, fontSize = 14.sp)
        Box {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(theme.sideBarNavigationEntryShape)
                    .background(displayColor)
                    .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape)
                    .onClick(rememberInteractionSource()) { expanded = !expanded }
                    .pointerHoverIcon(PointerIcon.Hand)
            )
            if (expanded) {
                ShellPopup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, 40),
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    ColorPickerPopup(
                        model = pickerModel,
                        onColorChanged = { c ->
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
