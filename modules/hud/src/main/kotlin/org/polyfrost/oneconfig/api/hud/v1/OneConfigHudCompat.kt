package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.Composable
import org.polyfrost.compose.layout.PolyAlign

private class OneConfigHudCompat(val hud: OneConfigHudWrapper) : Hud(hud.id, hud.name, Category.COMPAT), LegacyHudMarker {
    @Composable
    override fun Content() {}
    override fun update(): Boolean = false

    init {
        super.alignment = PolyAlign.Center
    }

    override var x: Float by hud::x
    override var y: Float by hud::y
    override var relativeX: Float by this::y
    override var relativeY: Float by this::y
    override var scaledWidth: Float by hud::scaledWith
    override var scaledHeight: Float by hud::scaledHeight

    override var renderedW: Float by hud::scaledWith
    override var renderedH: Float by hud::scaledHeight

    override fun updateRelativeX(absX: Float) {
        x = absX
    }

    override fun updateRelativeY(absY: Float) {
        y = absY
    }
}

interface OneConfigHudWrapper {
    var id: String
    var name: String

    var x: Float
    var y: Float
    var scaledHeight: Float
    var scaledWith: Float

    fun register() {
        var e = OneConfigHudCompat(this)
        HudManager.register()
        HudManager.activeInstances.add(e)
    }
}