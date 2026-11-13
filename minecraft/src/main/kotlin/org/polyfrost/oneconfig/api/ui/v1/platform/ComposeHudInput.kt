@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import androidx.compose.ui.input.pointer.PointerButton
import com.mojang.blaze3d.platform.InputConstants
import java.util.Collections
import java.util.WeakHashMap
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.EscapeStack
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen

internal object ComposeHudInput {
    private class Press(val receivers: List<ComposeHudLayer>, val consumed: Boolean)

    private val layers: MutableSet<ComposeHudLayer> = Collections.newSetFromMap(WeakHashMap())
    private val presses = HashMap<Int, Press>()
    private var escapeConsumed = false

    fun register(layer: ComposeHudLayer) {
        synchronized(layers) { layers += layer }
    }

    fun unregister(layer: ComposeHudLayer) {
        synchronized(layers) { layers -= layer }
    }

    @JvmStatic
    fun consumeButton(button: Int, pressed: Boolean): Boolean {
        val pointerButton = when (button) {
            InputConstants.MOUSE_BUTTON_LEFT -> PointerButton.Primary
            InputConstants.MOUSE_BUTTON_RIGHT -> PointerButton.Secondary
            InputConstants.MOUSE_BUTTON_MIDDLE -> PointerButton.Tertiary
            else -> return false
        }
        if (!pressed) {
            val press = presses.remove(button) ?: return false
            for (layer in press.receivers) layer.release(pointerButton)
            return press.consumed
        }
        presses.remove(button)
        val receivers = ArrayList<ComposeHudLayer>()
        var consumed = false
        for (layer in topmostFirst()) {
            if (!layer.takingInput) continue
            receivers += layer
            if (layer.press(pointerButton)) {
                consumed = true
                break
            }
        }
        if (receivers.isNotEmpty()) presses[button] = Press(receivers, consumed)
        return consumed
    }

    @JvmStatic
    fun consumeScroll(x: Double, y: Double): Boolean {
        if (x == 0.0 && y == 0.0) return false
        for (layer in topmostFirst()) {
            if (layer.takingInput && layer.scroll(x, y)) return true
        }
        return false
    }

    @JvmStatic
    fun consumeKey(key: Int, state: Int): Boolean {
        if (key != InputConstants.KEY_ESCAPE) return false
        return when (state) {
            KeyInputEvent.PRESSED -> {
                escapeConsumed = escapeClosesPopup()
                escapeConsumed
            }
            KeyInputEvent.REPEAT -> escapeConsumed
            KeyInputEvent.RELEASED -> escapeConsumed.also { escapeConsumed = false }
            else -> false
        }
    }

    private fun escapeClosesPopup(): Boolean {
        if (runCatching { Platform.screen().current<Any?>() }.getOrNull() is ComposeScreen) return false
        if (topmostFirst().none { it.takingInput }) return false
        return EscapeStack.dispatch()
    }

    private fun topmostFirst(): List<ComposeHudLayer> {
        val live = synchronized(layers) { if (layers.isEmpty()) return emptyList() else layers.toList() }
        return live.sortedByDescending { it.drawnAt }
    }
}
