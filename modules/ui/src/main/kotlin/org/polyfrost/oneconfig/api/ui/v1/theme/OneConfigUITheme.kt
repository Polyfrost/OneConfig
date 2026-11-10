package org.polyfrost.oneconfig.api.ui.v1.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public abstract class OneConfigUITheme {
    public abstract val name: String

    public abstract val pageBackground: Color
    public abstract val sidebarBackground: Color
    public abstract val chipBackground: Color
    public abstract val modCardBackground: Color
    public abstract val componentBackground: Color
    public abstract val popupBackground: Color

    public abstract val borderColor: Color
    public abstract val textColor: Color
    public abstract val textColorSecondary: Color
    public abstract val accentTextColor: Color

    public abstract val shadowColor: Color
    public abstract val controlThumbColor: Color
    public abstract val favoriteColor: Color

    public abstract val shadowEnabled: Boolean

    public abstract val backgroundShape: Shape
    public abstract val sideBarNavigationEntryShape: Shape
    public abstract val modCardShape: Shape
    public abstract val checkBoxShape: Shape
    public abstract val buttonShape: Shape
    public abstract val popupShape: Shape
    public abstract val circleShape: Shape

    public abstract val fontFamily: FontFamily
    public abstract val logoPath: String
}
