package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.api.settings.BuiltinVisualizers
import org.polyfrost.oneconfig.internal.ui.screens.THEMES_ID
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundTheme
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

object ThemeRegistry {
    internal val registry = mutableStateListOf<UITheme>()
    internal var activeTheme by mutableStateOf<UITheme?>(null)

    private const val DEFAULT_THEME_NAME = "Polyfrosted Dark"

    private const val CUSTOM_KEY_PREFIX = "custom:"

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
        register(CatppuccinLatte)
        register(CatppuccinFrappe)
        register(CatppuccinMacchiato)
        register(CatppuccinMocha)
        register(NordDark)
        register(NordLight)

        CustomThemes.loadSpecs()
        CustomThemes.specs.forEach { register(it.toUITheme()) }

        activeTheme = PolyfrostedDark
        syncNotificationTheme(PolyfrostedDark)

        BuiltinVisualizers.register()

        ConfigManager.addProfileChangeListener {
            Platform.screen().runOnUiThread {
                loadFromConfig()
                UiSounds.syncAmbienceTheme()
            }
        }
    }

    fun init() {}

    fun register(theme: UITheme) {
        registry += theme
    }

    fun activate(theme: UITheme) {
        val previous = activeTheme
        activeTheme = theme
        ThemeConfig.activeTheme = theme.configKey
        syncNotificationTheme(theme)
        saveThemeToConfig()
        if (previous != null) {
            if (UiSoundTheme.resolve(previous) != UiSoundTheme.resolve(theme)) {
                UiSounds.onSoundThemeChanged()
            } else {
                UiSounds.play(UiSoundEvent.CLICK)
            }
        }
    }

    fun loadFromConfig() {
        updateAccent()
        CustomThemes.adoptProfileThemes()
        try {
            val saved = if (ThemeConfig.polyfrostedRenameApplied) ThemeConfig.activeTheme else renamedThemes[ThemeConfig.activeTheme] ?: ThemeConfig.activeTheme
            val resolved = resolve(saved)
            val theme = resolved ?: registry.find { !it.isCustom && it.name == DEFAULT_THEME_NAME }
            if (theme != null) {
                activeTheme = theme
                if (resolved != null || !saved.startsWith(CUSTOM_KEY_PREFIX)) ThemeConfig.activeTheme = theme.configKey
                syncNotificationTheme(theme)
            }
            if (!ThemeConfig.polyfrostedRenameApplied && ConfigManager.active().exists(THEMES_ID)) {
                ThemeConfig.polyfrostedRenameApplied = true
                saveThemeToConfig()
            }
        } catch (_: Exception) {}
    }

    internal fun upsertCustom(theme: UITheme) {
        val index = registry.indexOfFirst { it.previewImage == theme.previewImage }
        if (index >= 0) registry[index] = theme else registry += theme
        if (activeTheme?.previewImage == theme.previewImage) {
            activeTheme = theme
            syncNotificationTheme(theme)
            UiSounds.syncAmbienceTheme()
        }
    }

    internal fun removeCustom(themeId: String) {
        registry.removeAll { it.previewImage == themeId }
        if (activeTheme?.previewImage == themeId) {
            registry.find { !it.isCustom && it.name == DEFAULT_THEME_NAME }?.let(::activate)
        }
        UITheme.clearExtras(themeId)
        clearPalette(themeId)
    }

    private fun resolve(saved: String): UITheme? =
        if (saved.startsWith(CUSTOM_KEY_PREFIX)) {
            val themeId = "custom-" + saved.removePrefix(CUSTOM_KEY_PREFIX)
            registry.find { it.previewImage == themeId }
        } else {
            registry.find { !it.isCustom && it.name == saved }
        }

    private val UITheme.configKey: String
        get() = if (isCustom) CUSTOM_KEY_PREFIX + previewImage.removePrefix("custom-") else name

    private fun saveThemeToConfig() {
        try {
            ConfigManager.active().save(THEMES_ID)
        } catch (_: Exception) {}
    }
}
