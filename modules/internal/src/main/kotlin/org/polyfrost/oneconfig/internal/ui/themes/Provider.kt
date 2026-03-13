package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF2B4BFF)
val LocalTheme = compositionLocalOf<UITheme> { error("A UI theme is required but was not provided") }