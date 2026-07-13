package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.Composable

private class OneConfigHudCompat(val wrapper: OneConfigHudWrapper) :
    Hud(wrapper.id, wrapper.name, Category.COMPAT), LegacyHudMarker {

    @Composable
    override fun Content() {
    }

    override fun update(): Boolean = false
    override fun multipleInstancesAllowed(): Boolean = false

    override var x: Float by wrapper::x
    override var y: Float by wrapper::y
    override var relativeX: Float by wrapper::x
    override var relativeY: Float by wrapper::y

    override val scaledWidth: Float get() = wrapper.scaledWidth
    override val scaledHeight: Float get() = wrapper.scaledHeight

    override var renderedW: Float
        get() = wrapper.scaledWidth
        set(_) {}
    override var renderedH: Float
        get() = wrapper.scaledHeight
        set(_) {}

    override fun updateRelativeX(absX: Float) { x = absX }
    override fun updateRelativeY(absY: Float) { y = absY }
}

interface OneConfigHudWrapper {
    var id: String
    var name: String

    var x: Float
    var y: Float
    var scaledWidth: Float
    var scaledHeight: Float

    fun register() {
        val hud = OneConfigHudCompat(this)
        HudManager.register(hud)
        HudManager.activeInstances.add(hud)
        hud.setup()
        hud.captureStaticSizeDefaults()
        hud.capturePositionDefaults()
    }
}
