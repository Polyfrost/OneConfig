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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.Weight
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingTarget
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.components.SelectableIconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerModel
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerPopup
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.components.AlignmentPicker
import org.polyfrost.oneconfig.internal.ui.hud.components.Dropdown
import org.polyfrost.oneconfig.internal.ui.hud.components.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.hud.components.NumberSpinnerWithIcon
import org.polyfrost.oneconfig.internal.ui.hud.components.Radio
import org.polyfrost.oneconfig.internal.ui.hud.components.RadioValue
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
fun Designer(hud: Hud? = null) {
    if (hud == null) return

    LaunchedEffect(hud) { repairHudStaticSize(hud) }

    HudSettingsContent(hud) {
    var staticWidth by remember { mutableStateOf(hud.staticWidth) }
    var staticW by remember { mutableStateOf(hud.staticW) }
    var staticH by remember { mutableStateOf(hud.staticH) }
    var alignment by remember { mutableStateOf(hud.alignment) }
    var padLeft by remember { mutableStateOf(hud.padLeft) }
    var padRight by remember { mutableStateOf(hud.padRight) }
    var padTop by remember { mutableStateOf(hud.padTop) }
    var padBottom by remember { mutableStateOf(hud.padBottom) }
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

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Section("Size & Alignment") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                "Width", "px",
                                staticW, { Snapshot.withMutableSnapshot { staticW = it; hud.staticW = it } },
                                20f, 2000f, 1f, width = 128.dp
                            )
                        }
                        HudSettingTarget(hud, "staticH") {
                            NumberSpinner(
                                "Height", "px",
                                staticH, { Snapshot.withMutableSnapshot { staticH = it; hud.staticH = it } },
                                8f, 2000f, 1f, width = 128.dp
                            )
                        }
                    }
                }

                HudSettingTarget(hud, "alignment") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.alpha(if (staticWidth) 1f else 0.35f)) {
                        AlignmentPicker(alignment) {
                            if (staticWidth) Snapshot.withMutableSnapshot { alignment = it; hud.alignment = it }
                        }
                    }
                    if (!staticWidth) {
                        Text(
                            "Enable Static Size\nto use alignment",
                            color = LocalTheme.current.textColorSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                }
            }
        }

        Section("Padding") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Edge padding", color = LocalTheme.current.textColor, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HudSettingTarget(hud, "padLeft") {
                        NumberSpinnerWithIcon(
                            "align", "px",
                            padLeft, { Snapshot.withMutableSnapshot { padLeft = it; hud.padLeft = it } }, 0f, 100f, 1f
                        )
                    }
                    HudSettingTarget(hud, "padRight") {
                        NumberSpinnerWithIcon(
                            "align", "px",
                            padRight, { Snapshot.withMutableSnapshot { padRight = it; hud.padRight = it } }, 0f, 100f, 1f
                        )
                    }
                    HudSettingTarget(hud, "padTop") {
                        NumberSpinnerWithIcon(
                            "align", "px",
                            padTop, { Snapshot.withMutableSnapshot { padTop = it; hud.padTop = it } }, 0f, 100f, 1f
                        )
                    }
                    HudSettingTarget(hud, "padBottom") {
                        NumberSpinnerWithIcon(
                            "align", "px",
                            padBottom, { Snapshot.withMutableSnapshot { padBottom = it; hud.padBottom = it } }, 0f, 100f, 1f
                        )
                    }
                }
            }
        }

        Section("Text Options") {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(58.dp)
                    .background(Color(0x192126).copy(.7f), RoundedCornerShape(8.dp))
                    .border(1.dp, LocalTheme.current.borderColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val previewText = run {
                    val raw = "Hello, OneConfig!"
                    when (caseType.ordinal) {
                        1 -> raw.uppercase()
                        2 -> raw.lowercase()
                        else -> raw
                    }
                }
                val density = LocalDensity.current.density
                if (font == Font.Poppins) {
                    val fontName = hud.getPoppinsFontName()
                    val skiaFont = FontManager.getFont(14f * textScale, fontName)
                    val textW = skiaFont.measureTextWidth(previewText)
                    val textH = skiaFont.metrics.let { it.descent - it.ascent }
                    Canvas(modifier = Modifier.size((textW / density).dp, (textH / density).dp)) {
                        drawIntoCanvas { canvas ->
                            val paint = org.jetbrains.skia.Paint().apply { color = textColor.toArgb() }
                            val baseline = -skiaFont.metrics.ascent
                            canvas.nativeCanvas.drawString(previewText, 0f, baseline, skiaFont, paint)
                            if (textUnderline) {
                                val underlinePos = skiaFont.metrics.underlinePosition ?: (14f * textScale * 0.08f)
                                val underlineThick = skiaFont.metrics.underlineThickness ?: (14f * textScale * 0.06f)
                                val linePaint = org.jetbrains.skia.Paint().apply {
                                    color = textColor.toArgb()
                                    strokeWidth = underlineThick
                                }
                                canvas.nativeCanvas.drawLine(0f, baseline + underlinePos, textW, baseline + underlinePos, linePaint)
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
                    val textW = McFontQueue.measureTextWidth(mcText, scale)
                    val textH = McFontQueue.measureTextHeight(scale)
                    Canvas(modifier = Modifier.size((textW / density).dp, (textH / density).dp)) {
                        drawIntoCanvas { canvas ->
                            McFontQueue.renderer?.invoke(
                                canvas.nativeCanvas,
                                mcText,
                                0f,
                                0f,
                                textColor.toArgb(),
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
                        font,
                        { Snapshot.withMutableSnapshot { font = it; hud.font = it } }
                    )
                }
                HudSettingTarget(hud, "textScale") {
                    NumberSpinner(
                        "Font size", "align", "px",
                        textScale * 14f, { Snapshot.withMutableSnapshot { val s = it / 14f; textScale = s; hud.textScale = s } },
                        6f, 64f, 1f, width = 112.dp
                    )
                }
                HudSettingTarget(hud, "textBold") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Modifiers", color = LocalTheme.current.textColor, fontSize = 14.sp)
                    Box(
                        modifier = Modifier
                            .background(Color(0x192126).copy(.7f), RoundedCornerShape(6.dp))
                            .border(1.dp, LocalTheme.current.borderColor, RoundedCornerShape(6.dp))
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
        }
    }
    }
}

@Composable
fun Section(title: String, content: @Composable () -> Unit) = Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Text(title.uppercase(), color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
    content()
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

    // Live swatch preview. When chroma is enabled, cycle the swatch using the exact same math the
    // HUD uses at render time (PolyColor.argb) so the preview matches the in-game result. The stored
    // base colour is never mutated by this animation - it only drives the preview.
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
                Popup(
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
