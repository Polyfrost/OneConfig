package org.polyfrost.oneconfig.internal.ui.themes

import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeBuilder

internal fun modTheme(base: OneConfigUITheme, id: String, name: String, builder: ThemeBuilder): UITheme {
    require(base is UITheme) { "Custom themes must be derived from a theme OneConfig provides" }
    val themeId = "mod-$id"
    val theme = base.copy(
        previewImage = themeId,
        name = name,
        pageBackground = builder.pageBackground ?: base.pageBackground,
        sidebarBackground = builder.sidebarBackground ?: base.sidebarBackground,
        chipBackground = builder.chipBackground ?: base.chipBackground,
        modCardBackground = builder.modCardBackground ?: base.modCardBackground,
        componentBackground = builder.componentBackground ?: base.componentBackground,
        popupBackground = builder.popupBackground ?: base.popupBackground,
        borderColor = builder.borderColor ?: base.borderColor,
        textColor = builder.textColor ?: base.textColor,
        textColorSecondary = builder.textColorSecondary ?: base.textColorSecondary,
        accentTextColor = builder.accentTextColor ?: base.accentTextColor,
        shadowColor = builder.shadowColor ?: base.shadowColor,
        controlThumbColor = builder.controlThumbColor ?: base.controlThumbColor,
        favoriteColor = builder.favoriteColor ?: base.favoriteColor,
        shadowEnabled = builder.shadowEnabled ?: base.shadowEnabled,
    )
    UITheme.copyExtras(base.previewImage, themeId)
    base.palette?.let { theme.withPalette(it) } ?: clearPalette(themeId)
    return theme.withControlTrackColor(builder.controlTrackColor ?: base.controlTrackColor)
}
