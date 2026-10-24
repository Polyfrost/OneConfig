package org.polyfrost.compose.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf
import org.polyfrost.compose.ui.unit.Density

val LocalDensity = staticCompositionLocalOf { Density(1f) }
