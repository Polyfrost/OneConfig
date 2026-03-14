package org.polyfrost.oneconfig.api.ui.v1.keybind

import dev.deftu.omnicore.api.client.input.OmniKeys
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.events.WindowFocusEvent
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Suppress("UnstableApiUsage")
object KeybindManager {
    private val LOGGER = LogManager.getLogger("OneConfig/Keybinds")

    private val binds = ArrayList<OneConfigKeybind>()
    private val activeBinds = HashSet<OneConfigKeybind>()

    private val downKeys = HashSet<Int>()
    private val downMouse = HashSet<Int>()
    private var mods: Byte = KeyModifiers.NONE

    private val MODIFIER_MAP = mapOf(
        OmniKeys.KEY_LEFT_SHIFT.code to KeyModifiers.SHIFT,
        OmniKeys.KEY_RIGHT_SHIFT.code to KeyModifiers.SHIFT,
        OmniKeys.KEY_LEFT_CONTROL.code to KeyModifiers.CTRL,
        OmniKeys.KEY_RIGHT_CONTROL.code to KeyModifiers.CTRL,
        OmniKeys.KEY_LEFT_ALT.code to KeyModifiers.ALT,
        OmniKeys.KEY_RIGHT_ALT.code to KeyModifiers.ALT,
        OmniKeys.KEY_LEFT_SUPER.code to KeyModifiers.META,
        OmniKeys.KEY_RIGHT_SUPER.code to KeyModifiers.META,
    )

    init {
        eventHandler { (key, _, state): KeyInputEvent ->
            if (state == 2) return@eventHandler
            val down = state == 1
            val mod = MODIFIER_MAP[key]
            if (mod != null) {
                mods = if (down) (mods or mod).toByte() else (mods and mod.inv()).toByte()
            }
            if (down) downKeys.add(key) else downKeys.remove(key)
        }

        eventHandler { (btn, state): MouseInputEvent ->
            if (state == 1) downMouse.add(btn) else downMouse.remove(btn)
        }

        eventHandler { _: TickEvent.End ->
            checkBinds()
        }

        eventHandler { (screen): ScreenOpenEvent ->
            if (screen == null) clearState()
        }

        eventHandler { _: WindowFocusEvent.Lost ->
            clearState()
        }
    }

    @JvmStatic
    fun register(bind: OneConfigKeybind): OneConfigKeybind {
        binds.add(bind)
        return bind
    }

    @JvmStatic
    fun unregister(bind: OneConfigKeybind) {
        binds.remove(bind)
        activeBinds.remove(bind)
    }

    @JvmStatic
    fun builder() = KeybindHelper()

    private fun checkBinds() {
        for (bind in binds) {
            val triggered = bind.test(downKeys, downMouse, mods)
            val wasActive = bind in activeBinds
            try {
                if (triggered && !wasActive) {
                    activeBinds.add(bind)
                    bind.action(true)
                } else if (!triggered && wasActive) {
                    activeBinds.remove(bind)
                    bind.action(false)
                }
            } catch (t: Throwable) {
                LOGGER.error("Keybind action threw an exception", t)
            }
        }
    }

    private fun clearState() {
        for (bind in activeBinds) {
            try { bind.action(false) } catch (_: Throwable) {}
        }
        activeBinds.clear()
        downKeys.clear()
        downMouse.clear()
        mods = KeyModifiers.NONE
    }
}
