package org.polyfrost.oneconfig.api.ui.v1.theme

import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public class ThemeBuilder internal constructor() {
    public var pageBackground: Color? = null
    public var sidebarBackground: Color? = null
    public var chipBackground: Color? = null
    public var modCardBackground: Color? = null
    public var componentBackground: Color? = null
    public var popupBackground: Color? = null
    public var borderColor: Color? = null
    public var textColor: Color? = null
    public var textColorSecondary: Color? = null
    public var accentTextColor: Color? = null
    public var shadowColor: Color? = null
    public var controlThumbColor: Color? = null
    public var favoriteColor: Color? = null
    public var controlTrackColor: Color? = null
    public var shadowEnabled: Boolean? = null
}
