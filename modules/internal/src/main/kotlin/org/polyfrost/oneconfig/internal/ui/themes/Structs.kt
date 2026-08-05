package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.round

/** Sets a panel background color's alpha from an opacity percentage (0-100; 0 = transparent, 100 = opaque). */
fun Color.withOpacityPercent(percent: Float): Color =
    copy(alpha = (percent / 100f).coerceIn(0f, 1f))

data class UITheme(
    val previewImage: String,
    val name: String,

    val pageBackground: Color,
    val sidebarBackground: Color,
    val chipBackground: Color,
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
) {
    var controlTrackColor: Color = DefaultControlTrackColor
        private set

    fun withControlTrackColor(color: Color): UITheme =
        copy().also { it.controlTrackColor = color }

    companion object {
        val DefaultControlTrackColor = Color(0xFF74777F)
    }
}

data class UIBranding(
    val logoPath: String
)

data class UITypography(
    val family: FontFamily,
    val scale: Float = 1f,
) {
    val fontScale: Float = (round(scale / SCALE_STEP) * SCALE_STEP).coerceIn(MIN_SCALE, MAX_SCALE)

    companion object {
        const val SCALE_STEP = 0.05f
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 2f
    }
}
