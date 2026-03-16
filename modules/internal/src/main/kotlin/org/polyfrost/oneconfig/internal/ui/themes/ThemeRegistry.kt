package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.polyfrost.oneconfig.internal.ui.MinecraftDark
import org.polyfrost.oneconfig.internal.ui.MinecraftLight
import org.polyfrost.oneconfig.internal.ui.PolyGlassDark
import org.polyfrost.oneconfig.internal.ui.PolyGlassLight

object ThemeRegistry {
    internal val registry = mutableStateListOf<UITheme>()
    internal var activeTheme by mutableStateOf<UITheme?>(null)

    init {
        register(PolyGlassDark)
        register(PolyGlassLight)
        register(MinecraftDark)
        register(MinecraftLight)

        activate(PolyGlassDark)
    }

    fun init() {}

    fun register(theme: UITheme) {
        registry += theme
    }

    fun activate(theme: UITheme) {
        activeTheme = theme
    }
}