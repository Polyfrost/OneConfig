package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.toArgb
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.notifications.v1.NotificationTheme
import org.polyfrost.oneconfig.internal.ThemeConfig

private var _accent by mutableStateOf(Color(ThemeConfig.accentColor.argb))

val Accent: Color get() = _accent

fun updateAccent() { _accent = Color(ThemeConfig.accentColor.argb) }

val LocalTheme = compositionLocalOf<UITheme> { error("A UI theme is required but was not provided") }

@Composable
fun Theme(content: @Composable () -> Unit) {
    _accent = Color(ThemeConfig.accentColor.argb)

    LaunchedEffect(Unit) {
        while (true) {
            if (ThemeConfig.accentColor.chroma) {
                withFrameNanos { }
                updateAccent()
            } else {
                delay(200)
            }
        }
    }

    val target = ThemeRegistry.activeTheme ?: error("No active theme provided")
    val animated = animateTheme(target)

    SideEffect { syncNotificationTheme(animated) }

    val density = LocalDensity.current
    val scaled = animated.typography.fontScale
    val themedDensity = remember(density, scaled) {
        if (scaled == 1f) density else Density(density.density, density.fontScale * scaled)
    }

    CompositionLocalProvider(
        LocalTheme provides animated,
        LocalDensity provides themedDensity,
        content = content
    )
}

internal fun syncNotificationTheme(theme: UITheme) {
    val minecraft = theme.previewImage.startsWith("minecraft")
    NotificationTheme.set(
        background = PolyColor(theme.popupBackground.toArgb()),
        border = PolyColor(theme.borderColor.toArgb()),
        textPrimary = PolyColor(theme.textColor.toArgb()),
        textSecondary = PolyColor(theme.textColorSecondary.toArgb()),
        accent = PolyColor(Accent.toArgb()),
        subtleButton = PolyColor(theme.componentBackground.toArgb()),
        actionPrimaryText = PolyColor(theme.accentTextColor.toArgb()),
        actionSubtleText = PolyColor(theme.textColor.toArgb()),
        fontTitle = if (minecraft) "minecraft-bold" else "poppins-medium",
        fontBody = if (minecraft) "minecraft" else null,
        radiusCard = if (minecraft) 0f else 12f,
        radiusButton = if (minecraft) 0f else 4f,
        radiusProgress = if (minecraft) 0f else 3f,
    )
}
