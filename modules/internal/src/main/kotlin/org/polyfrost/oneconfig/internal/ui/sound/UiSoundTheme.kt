package org.polyfrost.oneconfig.internal.ui.sound

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme

enum class UiSoundTheme {
    MODERN,
    MINECRAFT;

    companion object {
        fun current(): UiSoundTheme = resolve(ThemeRegistry.activeTheme)

        fun resolve(theme: UITheme?): UiSoundTheme = when (ThemeConfig.soundTheme) {
            ThemeConfig.SOUND_THEME_ONECONFIG -> MODERN
            ThemeConfig.SOUND_THEME_MINECRAFT -> MINECRAFT
            else -> of(theme)
        }

        fun of(theme: UITheme?): UiSoundTheme {
            theme ?: return MODERN
            return if (theme.previewImage.startsWith("minecraft-")) MINECRAFT else MODERN
        }
    }
}

object SoundThemeSelection {
    var value by mutableIntStateOf(ThemeConfig.SOUND_THEME_MATCH)
}
