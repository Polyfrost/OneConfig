package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry

val Accent: Color get() = Color(ThemeConfig.accentColor.argb)
val LocalTheme = compositionLocalOf<UITheme> { error("A UI theme is required but was not provided") }

@Composable
fun Theme(content: @Composable () -> Unit) =CompositionLocalProvider(
    LocalTheme provides (ThemeRegistry.activeTheme ?: error("No active theme provided")),
    content = content
)