package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.oneconfig.internal.ui.hud.components.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.hud.components.AlignmentPicker
import org.polyfrost.oneconfig.internal.ui.hud.components.Dropdown
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
enum class Font {
    Minecraft,
    Poppins;
}
enum class Weight {
    Thin,
    Regular,
    Medium,
    Bold,
    Black;
}

@Composable
fun Designer() = Column(
    verticalArrangement = Arrangement.spacedBy(22.dp)
) {
    Section("Component Options") {
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text("Dimensions", color = LocalTheme.current.textColorSecondary, fontSize = 14.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NumberSpinner(
                    "Width:",
                    "px",
                    32f,
                    {},
                    0f,
                    100f,
                    2f
                )
                NumberSpinner(
                    "Height:",
                    "px",
                    32f,
                    {},
                    0f,
                    100f,
                    2f
                )
            }
        }
    }
    Section("Alignment & Padding") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlignmentPicker(PolyAlign.Center) {}
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    NumberSpinner(
                        "Pad between",
                        "align",
                        "px",
                        32f,
                        {},
                        0f,
                        100f,
                        2f
                    )
                    Dropdown("Padding Type", PaddingType.SpaceBetween, {})
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Edge padding", color = LocalTheme.current.textColor, fontSize = 14.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            NumberSpinnerWithIcon(
                                "align",
                                "px",
                                32f,
                                {},
                                0f,
                                100f,
                                2f
                            )
                            NumberSpinnerWithIcon(
                                "align",
                                "px",
                                32f,
                                {},
                                0f,
                                100f,
                                2f
                            )
                            NumberSpinnerWithIcon(
                                "align",
                                "px",
                                32f,
                                {},
                                0f,
                                100f,
                                2f
                            )
                            NumberSpinnerWithIcon(
                                "align",
                                "px",
                                32f,
                                {},
                                0f,
                                100f,
                                2f
                            )
                        }
                    }
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
            Text("The quick brown fox jumps over the lazy dog", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = LocalTheme.current.textColor)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Dropdown(
                "Font",
                Font.Minecraft,
                {}
            )
            NumberSpinner(
                "Font size",
                "align",
                "px",
                32f,
                {},
                0f,
                100f,
                2f,
                width = 112.dp
            )
            Radio(
                "Modifiers",
                Modifiers.Bold,
            ) {}
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Dropdown(
                "Weight",
                Weight.Regular,
                {}
            )
            Radio(
                "Align",
                AlignType.Center,
            ) {}
            Radio(
                "Case Type",
                CaseType.AA
            ) {}
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