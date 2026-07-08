package org.polyfrost.oneconfig.internal.ui.hud

import org.jetbrains.skia.Canvas

object LegacyHudOverlayBridge {
    @Volatile
    @JvmField
    var painter: ((Canvas) -> Unit)? = null
}
