package org.polyfrost.compose.backend

import androidx.compose.ui.graphics.platform.PlatformGraphicsRegistry
import androidx.compose.ui.text.platform.PlatformTextRegistry

object PolyCompose {
    private var installed = false

    @JvmStatic
    @Synchronized
    fun install() {
        if (installed) return
        installed = true
        PlatformGraphicsRegistry.register(PolyGraphics)
        PlatformTextRegistry.register(PolyText)
    }
}
