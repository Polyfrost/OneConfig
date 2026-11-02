package org.polyfrost.oneconfig.api.ui.v1.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
object OneConfigTheme {
    @JvmStatic
    val POLYFROSTED_DARK: OneConfigUITheme by lazy { componentsBridge().polyfrostedDark() }

    @JvmStatic
    val POLYFROSTED_LIGHT: OneConfigUITheme by lazy { componentsBridge().polyfrostedLight() }

    @JvmStatic
    val FROSTED_POLYGLASS_DARK: OneConfigUITheme by lazy { componentsBridge().frostedPolyglassDark() }

    @JvmStatic
    val FROSTED_POLYGLASS_LIGHT: OneConfigUITheme by lazy { componentsBridge().frostedPolyglassLight() }

    @JvmStatic
    @Deprecated("Renamed to Polyfrosted", ReplaceWith("OneConfigTheme.POLYFROSTED_DARK"))
    val POLY_GLASS_DARK: OneConfigUITheme get() = POLYFROSTED_DARK

    @JvmStatic
    @Deprecated("Renamed to Polyfrosted", ReplaceWith("OneConfigTheme.POLYFROSTED_LIGHT"))
    val POLY_GLASS_LIGHT: OneConfigUITheme get() = POLYFROSTED_LIGHT

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
    fun builtIn(): List<OneConfigUITheme> = listOf(
        POLYFROSTED_DARK, POLYFROSTED_LIGHT,
        FROSTED_POLYGLASS_DARK, FROSTED_POLYGLASS_LIGHT,
        MINECRAFT_DARK, MINECRAFT_LIGHT,
    )

    @JvmStatic
    fun activate(theme: OneConfigUITheme) {
        componentsBridge().activateTheme(theme)
    }

    @JvmStatic
    fun ensureLoaded() {
        componentsBridge().ensureThemeLoaded()
    }

    @JvmStatic
    var logoOverride: String? by mutableStateOf(null)
        private set

    @JvmStatic
    fun overrideLogo(path: String?) {
        logoOverride = path?.takeIf { it.isNotBlank() }
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun ProvideOneConfigTheme(content: @Composable () -> Unit) {
    componentsBridge().ProvideTheme(content)
}
