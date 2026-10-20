package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.api.settings.BuiltinVisualizers
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundTheme
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

object ThemeRegistry {
    internal val registry = mutableStateListOf<UITheme>()
    internal var activeTheme by mutableStateOf<UITheme?>(null)

    private const val DEFAULT_THEME_NAME = "PolyGlass Dark"

    init {
        register(PolyGlassDark)
        register(PolyGlassLight)
        register(MinecraftDark)
        register(MinecraftLight)

        activeTheme = PolyGlassDark
        syncNotificationTheme(PolyGlassDark)

        BuiltinVisualizers.register()
    }

    @Deprecated("Use OneConfigTheme.ensureLoaded. Removed in OneConfig 1.6.0")
    fun init() {}

    fun register(theme: UITheme) {
        registry += theme
    }

    @Deprecated("Use OneConfigTheme.activate. Removed in OneConfig 1.6.0")
    fun activate(theme: UITheme) {
        val previous = activeTheme
        activeTheme = theme
        ThemeConfig.activeTheme = theme.name
        syncNotificationTheme(theme)
        saveThemeToConfig()
        if (previous != null) {
            if (UiSoundTheme.of(previous) != UiSoundTheme.of(theme)) {
                UiSounds.onSoundThemeChanged()
            } else {
                UiSounds.play(UiSoundEvent.CLICK)
            }
        }
    }

    @Deprecated("Use OneConfigTheme.ensureLoaded. Removed in OneConfig 1.6.0")
    fun loadFromConfig() {
        try {
            val theme = registry.find { it.name == ThemeConfig.activeTheme }
                ?: registry.find { it.name == DEFAULT_THEME_NAME }
            if (theme != null) {
                activeTheme = theme
                ThemeConfig.activeTheme = theme.name
                syncNotificationTheme(theme)
            }
        } catch (_: Exception) {}
    }

    private fun saveThemeToConfig() {
        try {
            ConfigManager.active().save("themes.json")
        } catch (_: Exception) {}
    }
}
