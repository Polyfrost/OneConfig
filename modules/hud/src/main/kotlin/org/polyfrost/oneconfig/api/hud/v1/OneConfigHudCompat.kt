package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.config.v1.Property

private class OneConfigHudCompat(val wrapper: OneConfigHudWrapper) :
    Hud(wrapper.id, wrapper.name, Category.COMPAT), LegacyHudMarker {

    @Composable
    override fun Content() {
    }

    override fun update(): Boolean = false
    override fun multipleInstancesAllowed(): Boolean = false

    override val supportsScale: Boolean get() = wrapper.supportsScale

    override var x: Float by wrapper::x
    override var y: Float by wrapper::y
    override var relativeX: Float by wrapper::x
    override var relativeY: Float by wrapper::y

    override var customScale: Float by wrapper::scale

    override val scaledWidth: Float get() = wrapper.scaledWidth
    override val scaledHeight: Float get() = wrapper.scaledHeight

    override var renderedW: Float
        get() = wrapper.scaledWidth
        set(_) {}
    override var renderedH: Float
        get() = wrapper.scaledHeight
        set(_) {}

    override var staticW: Float
        get() = wrapper.scaledWidth
        set(_) {}
    override var staticH: Float
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

    var scale: Float

    var scaledWidth: Float
    var scaledHeight: Float

    val supportsScale: Boolean get() = true

    fun linkedProperties(): List<Property<*>> = emptyList()

    fun register() {
        val hud = OneConfigHudCompat(this)
        HudManager.register(hud)
        hud.make()
        HudManager.activeInstances.add(hud)
        hud.setup()
        val tree = hud.tree
        if (tree != null) {
            for (prop in linkedProperties()) tree.put(prop)
        }
        hud.captureStaticSizeDefaults()
        hud.capturePositionDefaults()
    }
}
