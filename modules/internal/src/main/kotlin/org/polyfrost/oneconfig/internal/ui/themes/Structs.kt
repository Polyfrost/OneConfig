package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily

data class UITheme(
    val pageBackground: Color,
    val sidebarBackground: Color,
    val chipBackground: Color,
    val modCardBackground: Color,
    val componentBackground: Color,

    val borderColor: Color,
    val textColor: Color,
    val textColorSecondary: Color,

    val backgroundShape: Shape,
    val sideBarNavigationEntryShape: Shape,
    val modCardShape: Shape,
    val checkBoxShape: Shape,

    val branding: UIBranding,
    val typography: UITypography,
)

data class UIBranding(
    val logoPath: String
)

data class UITypography(
    val family: FontFamily
)