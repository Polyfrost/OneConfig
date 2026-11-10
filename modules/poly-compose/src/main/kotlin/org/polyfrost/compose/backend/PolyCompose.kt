@file:OptIn(InternalComposeUiApi::class)

package org.polyfrost.compose.backend

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.platform.PlatformGraphicsRegistry
import androidx.compose.ui.text.platform.PlatformTextRegistry

public object PolyCompose {
    private var installed = false

    @JvmStatic
    @Synchronized
    public fun install() {
        if (installed) return
        installed = true
        PlatformGraphicsRegistry.register(PolyGraphics)
        PlatformTextRegistry.register(PolyText)
    }
}
