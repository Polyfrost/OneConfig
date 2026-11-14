package org.polyfrost.oneconfig.api.ui.v1.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.ContextMenuRoot
import org.polyfrost.oneconfig.api.ui.v1.components.FeedbackHosts
import org.polyfrost.oneconfig.api.ui.v1.components.ProvideOneConfigTextContextMenu
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

    public val typography: OneConfigTypography
        @Composable get() = current.typography

    public val shapes: OneConfigShapes
        @Composable get() = current.shapes

    public val spacing: OneConfigSpacing
        @Composable get() = current.spacing

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

    @Composable
    public fun shape(radius: Dp): Shape = componentsBridge().themeShape(radius)

    @Composable
    public fun icon(name: String): String = componentsBridge().themeIcon(name)

    @JvmStatic
    public fun custom(base: OneConfigUITheme, id: String, name: String, block: ThemeBuilder.() -> Unit): OneConfigUITheme {
        require(id.isNotBlank()) { "A custom theme needs a non-blank id" }
        return componentsBridge().customTheme(base, id, name, ThemeBuilder().apply(block))
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
public fun ProvideOneConfigTheme(
    theme: OneConfigUITheme? = null,
    scaleToUi: Boolean = false,
    content: @Composable () -> Unit,
) {
    val nested = LocalThemeProvided.current
    if (nested && theme == null && !scaleToUi) {
        content()
        return
    }
    componentsBridge().ProvideTheme(theme, scaleToUi) {
        if (nested) {
            content()
        } else {
            CompositionLocalProvider(LocalThemeProvided provides true) {
                ProvideOneConfigTextContextMenu { ContextMenuRoot { FeedbackHosts(content) } }
            }
        }
    }
}

private val LocalThemeProvided = staticCompositionLocalOf { false }

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ProvideOneConfigTheme(content: @Composable () -> Unit) {
    ProvideOneConfigTheme(null, false, content)
}
