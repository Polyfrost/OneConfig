@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.minecraft.hud.HudInput
import org.polyfrost.oneconfig.api.hud.v1.HudClickInput
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.HudMouseButton
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

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
        val anyScreen = Minecraft.getInstance().isWindowActive && currentScreen() != null
        HudClickInput.updatePointer(HudInput.pointerX, HudInput.pointerY, HudInput.pointerAllowed, anyScreen)
    }

    private fun currentScreen(): Any? =
        try {
            Platform.screen().current<Any?>()
        } catch (_: Throwable) {
            null
        }
}
