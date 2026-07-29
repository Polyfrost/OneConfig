package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import org.polyfrost.oneconfig.internal.OneConfigConfig

/** Sets a panel background color's alpha from an opacity percentage (0-100; 0 = transparent, 100 = opaque). */
fun Color.withOpacityPercent(percent: Float): Color =
    copy(alpha = (percent / 100f).coerceIn(0f, 1f))

data class UITheme(
    val previewImage: String,
    val name: String,

    val pageBackground: Color,
    val sidebarBackground: Color,
    val chipBackground: Color,
    /** Unfilled track of a slider or switch; needs ~3:1 against the panel it sits on. */
    val controlTrackColor: Color,
    val modCardBackground: Color,
    val componentBackground: Color,
    val popupBackground: Color,

    val borderColor: Color,
    val textColor: Color,
    val textColorSecondary: Color,
    val accentTextColor: Color,

    val shadowColor: Color,
    val controlThumbColor: Color,

    val shadowEnabled: Boolean,

    val backgroundShape: Shape,
    val sideBarNavigationEntryShape: Shape,
    val modCardShape: Shape,
    val checkBoxShape: Shape,
    val buttonShape: Shape,
    val popupShape: Shape,
    val circleShape: Shape,

    val branding: UIBranding,
    val typography: UITypography,
    val iconOverrides: Map<String, String> = emptyMap(),
)

data class UIBranding(
    val logoPath: String
)

data class UITypography(
    val family: FontFamily
)
