package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.polyfrost.oneconfig.internal.ThemeConfig

private var _accent by mutableStateOf(Color(ThemeConfig.accentColor.argb))

val Accent: Color get() = _accent

fun updateAccent() { _accent = Color(ThemeConfig.accentColor.argb) }

val LocalTheme = compositionLocalOf<UITheme> { error("A UI theme is required but was not provided") }

@Composable
fun Theme(content: @Composable () -> Unit) {
    _accent = Color(ThemeConfig.accentColor.argb)
    val target = ThemeRegistry.activeTheme ?: error("No active theme provided")
    CompositionLocalProvider(
        LocalTheme provides animateTheme(target),
        content = content
    )
}
