package org.polyfrost.oneconfig.api.ui.v1.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
object OneConfigTheme {
    @JvmStatic
    val POLY_GLASS_DARK: OneConfigUITheme by lazy { componentsBridge().polyGlassDark() }

    @JvmStatic
    val POLY_GLASS_LIGHT: OneConfigUITheme by lazy { componentsBridge().polyGlassLight() }

    @JvmStatic
    val MINECRAFT_DARK: OneConfigUITheme by lazy { componentsBridge().minecraftDark() }

    @JvmStatic
    val MINECRAFT_LIGHT: OneConfigUITheme by lazy { componentsBridge().minecraftLight() }

    val current: OneConfigUITheme
        @Composable get() = componentsBridge().currentTheme()

    val accent: Color
        @Composable get() = componentsBridge().accent()

    @JvmStatic
    val active: OneConfigUITheme?
        get() = componentsBridge().activeTheme()

    @JvmStatic
    fun builtIn(): List<OneConfigUITheme> = listOf(POLY_GLASS_DARK, POLY_GLASS_LIGHT, MINECRAFT_DARK, MINECRAFT_LIGHT)

    @JvmStatic
    fun activate(theme: OneConfigUITheme) {
        componentsBridge().activateTheme(theme)
    }

    @JvmStatic
    fun ensureLoaded() {
        componentsBridge().ensureThemeLoaded()
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun ProvideOneConfigTheme(content: @Composable () -> Unit) {
    componentsBridge().ProvideTheme(content)
}
