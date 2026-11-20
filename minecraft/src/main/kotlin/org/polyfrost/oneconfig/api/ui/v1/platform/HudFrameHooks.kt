@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.minecraft.hud.HudLayer
import org.polyfrost.oneconfig.api.hud.v1.HudClickInput
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.HudMouseButton
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

//? if = 1.8.9 {
/*import org.lwjgl.input.Mouse
*///?}

internal object HudFrameHooks {
    private val LOGGER = LogManager.getLogger("OneConfig/HudFrame")
    private var pointerFailed = false

    @JvmStatic
    fun beginFrame(partialTick: Float) {
        HudManager.partialTick = partialTick
        try {
            feedPointer()
        } catch (e: Throwable) {
            if (e is VirtualMachineError) throw e
            if (!pointerFailed) {
                pointerFailed = true
                LOGGER.error("Failed to read the pointer for HUD click regions", e)
            }
        }
    }

    @JvmStatic
    fun consumeButton(button: Int, pressed: Boolean): Boolean =
        HudClickInput.consumeButton(
            when (button) {
                InputConstants.MOUSE_BUTTON_LEFT -> HudMouseButton.Left.ordinal
                InputConstants.MOUSE_BUTTON_RIGHT -> HudMouseButton.Right.ordinal
                InputConstants.MOUSE_BUTTON_MIDDLE -> HudMouseButton.Middle.ordinal
                else -> -1
            },
            pressed,
        )

    private fun feedPointer() {
        val screen = Platform.screen()
        val current = if (Minecraft.getInstance().isWindowActive) runCatching { screen.current<Any?>() }.getOrNull() else null
        val allowed = current is ChatScreen || current is HudLayer.InputScreen
        //? if > 1.8.9 {
        val mouse = Minecraft.getInstance().mouseHandler
        val windowX = mouse.xpos().toFloat()
        val windowY = mouse.ypos().toFloat()
        //?} else {
        /*val pixelRatio = screen.pixelRatio()
        val windowX = Mouse.getX() / pixelRatio
        val windowY = (screen.viewportHeight() - Mouse.getY() - 1) / pixelRatio
        *///?}
        val guiX = windowX * screen.guiWidth() / screen.windowWidth().coerceAtLeast(1)
        val guiY = windowY * screen.guiHeight() / screen.windowHeight().coerceAtLeast(1)
        HudClickInput.updatePointer(guiX, guiY, allowed, current != null)
    }
}
