package org.polyfrost.oneconfig.api.ui.v1.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
public object OneConfigTheme {
    @JvmStatic
    public val POLYFROSTED_DARK: OneConfigUITheme by lazy { componentsBridge().polyfrostedDark() }

    @JvmStatic
    public val POLYFROSTED_LIGHT: OneConfigUITheme by lazy { componentsBridge().polyfrostedLight() }

    @JvmStatic
    public val GLASS_DARK: OneConfigUITheme by lazy { componentsBridge().glassDark() }

    @JvmStatic
    public val GLASS_LIGHT: OneConfigUITheme by lazy { componentsBridge().glassLight() }

    @JvmStatic
    @Deprecated("Renamed to Polyfrosted", ReplaceWith("OneConfigTheme.POLYFROSTED_DARK"))
    public val POLY_GLASS_DARK: OneConfigUITheme get() = POLYFROSTED_DARK

    @JvmStatic
    @Deprecated("Renamed to Polyfrosted", ReplaceWith("OneConfigTheme.POLYFROSTED_LIGHT"))
    public val POLY_GLASS_LIGHT: OneConfigUITheme get() = POLYFROSTED_LIGHT

    @JvmStatic
    public val MINECRAFT_DARK: OneConfigUITheme by lazy { componentsBridge().minecraftDark() }

    @JvmStatic
    public val MINECRAFT_LIGHT: OneConfigUITheme by lazy { componentsBridge().minecraftLight() }

    public val current: OneConfigUITheme
        @Composable get() = componentsBridge().currentTheme()

    public val accent: Color
        @Composable get() = componentsBridge().accent()

    @JvmStatic
    public val active: OneConfigUITheme?
        get() = componentsBridge().activeTheme()

    @JvmStatic
    public fun builtIn(): List<OneConfigUITheme> = listOf(
        POLYFROSTED_DARK, POLYFROSTED_LIGHT,
        GLASS_DARK, GLASS_LIGHT,
        MINECRAFT_DARK, MINECRAFT_LIGHT,
    )

    @JvmStatic
    public fun activate(theme: OneConfigUITheme) {
        componentsBridge().activateTheme(theme)
    }

    @JvmStatic
    public fun ensureLoaded() {
        componentsBridge().ensureThemeLoaded()
    }

    @JvmStatic
    public var logoOverride: String? by mutableStateOf(null)
        private set

    @JvmStatic
    public fun overrideLogo(path: String?) {
        logoOverride = path?.takeIf { it.isNotBlank() }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ProvideOneConfigTheme(content: @Composable () -> Unit) {
    componentsBridge().ProvideTheme(content)
}
