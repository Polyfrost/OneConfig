package org.polyfrost.oneconfig.api.ui.v1.theme

import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.text.font.FontFamily
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
abstract class OneConfigUITheme {
    abstract val name: String

    abstract val pageBackground: Color
    abstract val sidebarBackground: Color
    abstract val chipBackground: Color
    abstract val modCardBackground: Color
    abstract val componentBackground: Color
    abstract val popupBackground: Color

    abstract val borderColor: Color
    abstract val textColor: Color
    abstract val textColorSecondary: Color
    abstract val accentTextColor: Color

    abstract val shadowColor: Color
    abstract val controlThumbColor: Color
    abstract val favoriteColor: Color

    abstract val shadowEnabled: Boolean

    abstract val backgroundShape: Shape
    abstract val sideBarNavigationEntryShape: Shape
    abstract val modCardShape: Shape
    abstract val checkBoxShape: Shape
    abstract val buttonShape: Shape
    abstract val popupShape: Shape
    abstract val circleShape: Shape

    abstract val fontFamily: FontFamily
    abstract val logoPath: String
}
