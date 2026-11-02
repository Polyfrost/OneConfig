package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.api.settings.BuiltinVisualizers
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundTheme
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

object ThemeRegistry {
    internal val registry = mutableStateListOf<UITheme>()
    internal var activeTheme by mutableStateOf<UITheme?>(null)

    private const val DEFAULT_THEME_NAME = "Polyfrosted Dark"

    private val renamedThemes = mapOf(
        "PolyGlass Dark" to "Polyfrosted Dark",
        "PolyGlass Light" to "Polyfrosted Light",
    )

    init {
        register(PolyfrostedDark)
        register(PolyfrostedLight)
        register(PolyGlassDark)
        register(PolyGlassLight)
        register(MinecraftDark)
        register(MinecraftLight)

        activeTheme = PolyfrostedDark
        syncNotificationTheme(PolyfrostedDark)

        BuiltinVisualizers.register()

        ConfigManager.addProfileChangeListener {
            Platform.screen().runOnUiThread { loadFromConfig() }
        }
    }

    fun init() {}

    fun register(theme: UITheme) {
        registry += theme
    }

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

    fun loadFromConfig() {
        updateAccent()
        try {
            val saved = if (ThemeConfig.polyfrostedRenameApplied) ThemeConfig.activeTheme else renamedThemes[ThemeConfig.activeTheme] ?: ThemeConfig.activeTheme
            val theme = registry.find { it.name == saved }
                ?: registry.find { it.name == DEFAULT_THEME_NAME }
            if (theme != null) {
                activeTheme = theme
                ThemeConfig.activeTheme = theme.name
                syncNotificationTheme(theme)
            }
            if (!ThemeConfig.polyfrostedRenameApplied) {
                ThemeConfig.polyfrostedRenameApplied = true
                saveThemeToConfig()
            }
        } catch (_: Exception) {}
    }

    private fun saveThemeToConfig() {
        try {
            ConfigManager.active().save("themes.json")
        } catch (_: Exception) {}
    }
}
