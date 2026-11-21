package org.polyfrost.oneconfig.internal.ui.compose

import org.polyfrost.compose.host.ComposeHost
import org.polyfrost.compose.host.GameInputLayer
import org.polyfrost.compose.host.OverlayRenderer
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsRenderer
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.platform.HudFrameHooks
import org.polyfrost.oneconfig.internal.OneConfigConfig

//? if = 1.8.9 {
/*import org.polyfrost.compose.host.LegacyInputObserver
import org.polyfrost.oneconfig.internal.legacy.LegacyInputEvents
*///?}

object OneConfigComposeHooks {
    fun install() {
        ComposeHost.addGameInput(ToastInput)
        ComposeHost.addGameInput(HudClickInput)
        ComposeHost.addOverlay(Toasts)
        ComposeHost.onFontsChanged { HudManager.invalidate() }
        //? if = 1.8.9
        //ComposeHost.addLegacyInput(LegacyEvents)
    }

    val modifierTapKeys: Set<Int>?
        get() {
            val bind = OneConfigConfig.oneConfigKeybind
            if (bind.mouseBtns?.isNotEmpty() == true || bind.mods != KeyModifiers.NONE) return null
            return bind.keyCodes?.toSet()
        }

    @JvmField
    val drawHuds: (RenderContext, RenderContext) -> Unit = { stable, volatile ->
        HudManager.render(stable, HudManager.guiScreenWidth, HudManager.guiScreenHeight, volatile)
    }

    private object ToastInput : GameInputLayer {
        override val order: Int get() = GameInputLayer.ABOVE_HUD_LAYERS
        override fun consumeButton(button: Int, pressed: Boolean): Boolean = NotificationsRenderer.consumeMouseButton(button, pressed)
        override fun consumeScroll(x: Double, y: Double): Boolean = NotificationsRenderer.isPointerOverToast()
    }

    private object HudClickInput : GameInputLayer {
        override val order: Int get() = GameInputLayer.BELOW_HUD_LAYERS
        override fun consumeButton(button: Int, pressed: Boolean): Boolean = HudFrameHooks.consumeButton(button, pressed)
    }

    private object Toasts : OverlayRenderer {
        override val visible: Boolean get() = NotificationsManager.count > 0
        override fun prepare(guiWidth: Float, guiHeight: Float): Boolean = NotificationsRenderer.prepare(guiWidth, guiHeight)
        override fun draw(ctx: RenderContext) = NotificationsRenderer.draw(ctx)
    }

    //? if = 1.8.9 {
    /*private object LegacyEvents : LegacyInputObserver {
        override fun onDrainedMouse(): Boolean = LegacyInputEvents.postMouse()
        override fun onDrainedKeyboard() = LegacyInputEvents.postKeyboard()
    }
    *///?}
}
