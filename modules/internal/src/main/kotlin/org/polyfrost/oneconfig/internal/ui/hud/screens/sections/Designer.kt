package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.render.FontManager
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.internal.ui.components.SelectableIconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
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
    Italic("italic"),
    Underline("underline");
}
enum class Weight {
    Thin,
    Regular,
    Medium,
    Bold,
    Black;
}

@Composable
fun Designer(hud: Hud? = null) {
    if (hud == null) return

    var staticWidth by remember(hud) { mutableStateOf(hud.staticWidth) }
    var staticW by remember(hud) { mutableStateOf(hud.staticW) }
    var staticH by remember(hud) { mutableStateOf(hud.staticH) }
    var alignment by remember(hud) { mutableStateOf(hud.alignment) }
    var padLeft by remember(hud) { mutableStateOf(hud.padLeft) }
    var padRight by remember(hud) { mutableStateOf(hud.padRight) }
    var padTop by remember(hud) { mutableStateOf(hud.padTop) }
    var padBottom by remember(hud) { mutableStateOf(hud.padBottom) }
    var font by remember(hud) { mutableStateOf(hud.font) }
    var textScale by remember(hud) { mutableStateOf(hud.textScale) }
    var textBold by remember(hud) { mutableStateOf(hud.textBold) }
    var textItalic by remember(hud) { mutableStateOf(hud.textItalic) }
    var caseType by remember(hud) {
        mutableStateOf(
            CaseType.entries[hud.caseType.coerceIn(
                0,
                CaseType.entries.lastIndex
            )]
        )
    }
    var textAlign by remember(hud) {
        mutableStateOf(
            AlignType.entries[hud.textAlign.coerceIn(
                0,
                AlignType.entries.lastIndex
            )]
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Section("Size & Alignment") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SwitchControl(staticWidth) {
                        Snapshot.withMutableSnapshot { staticWidth = it; hud.staticWidth = it }
                    }
                    Text("Static Size", color = LocalTheme.current.textColor, fontSize = 14.sp)
                }

                if (staticWidth) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberSpinner(
                            "Width", "px",
                            staticW, { Snapshot.withMutableSnapshot { staticW = it; hud.staticW = it } },
                            20f, 2000f, 1f, width = 128.dp
                        )
                        NumberSpinner(
                            "Height", "px",
                            staticH, { Snapshot.withMutableSnapshot { staticH = it; hud.staticH = it } },
                            8f, 2000f, 1f, width = 128.dp
                        )
                    }
                }

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

        Section("Padding") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Edge padding", color = LocalTheme.current.textColor, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberSpinnerWithIcon(
                        "align", "px",
                        padLeft, { Snapshot.withMutableSnapshot { padLeft = it; hud.padLeft = it } }, 0f, 100f, 1f
                    )
                    NumberSpinnerWithIcon(
                        "align", "px",
                        padRight, { Snapshot.withMutableSnapshot { padRight = it; hud.padRight = it } }, 0f, 100f, 1f
                    )
                    NumberSpinnerWithIcon(
                        "align", "px",
                        padTop, { Snapshot.withMutableSnapshot { padTop = it; hud.padTop = it } }, 0f, 100f, 1f
                    )
                    NumberSpinnerWithIcon(
                        "align", "px",
                        padBottom, { Snapshot.withMutableSnapshot { padBottom = it; hud.padBottom = it } }, 0f, 100f, 1f
                    )
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
                    val fontName = when {
                        textBold && textItalic -> "poppins-bold-italic"
                        textBold -> "poppins-bold"
                        textItalic -> "poppins-italic"
                        else -> "poppins"
                    }
                    val skiaFont = FontManager.getFont(14f * textScale, fontName)
                    val textW = skiaFont.measureTextWidth(previewText)
                    val textH = skiaFont.metrics.let { it.descent - it.ascent }
                    Canvas(modifier = Modifier.size((textW / density).dp, (textH / density).dp)) {
                        drawIntoCanvas { canvas ->
                            val paint = org.jetbrains.skia.Paint().apply { color = 0xFFFFFFFF.toInt() }
                            canvas.nativeCanvas.drawString(previewText, 0f, -skiaFont.metrics.ascent, skiaFont, paint)
                        }
                    }
                } else {
                    val mcText = buildString {
                        if (textBold) append("§l")
                        if (textItalic) append("§o")
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
                                0xFFFFFFFF.toInt(),
                                false,
                                scale
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Dropdown(
                    "Font",
                    font,
                    { Snapshot.withMutableSnapshot { font = it; hud.font = it } }
                )
                NumberSpinner(
                    "Font size", "align", "px",
                    textScale, { Snapshot.withMutableSnapshot { textScale = it; hud.textScale = it } },
                    0f, 4f, 0.15f, width = 112.dp
                )
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
                            SelectableIconButton(
                                Modifiers.Underline.icon,
                                selected = false,
                                modifier = Modifier.size(18.dp),
                                onClick = {}
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Dropdown("Weight", Weight.Regular, {})
                Radio(
                    "Align",
                    textAlign,
                ) { a -> Snapshot.withMutableSnapshot { textAlign = a; hud.textAlign = a.ordinal } }
                Radio(
                    "Case Type",
                    caseType,
                ) { c -> Snapshot.withMutableSnapshot { caseType = c; hud.caseType = c.ordinal } }
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
