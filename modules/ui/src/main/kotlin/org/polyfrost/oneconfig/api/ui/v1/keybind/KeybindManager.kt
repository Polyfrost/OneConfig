package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.events.WindowFocusEvent
import org.polyfrost.oneconfig.api.platform.v1.Platform
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
        Platform.compatibility().keys().keyLeftShift to KeyModifiers.SHIFT,
        Platform.compatibility().keys().keyRightShift to KeyModifiers.SHIFT,
        Platform.compatibility().keys().keyLeftControl to KeyModifiers.CTRL,
        Platform.compatibility().keys().keyRightControl to KeyModifiers.CTRL,
        Platform.compatibility().keys().keyLeftAlt to KeyModifiers.ALT,
        Platform.compatibility().keys().keyRightAlt to KeyModifiers.ALT,
        Platform.compatibility().keys().keyLeftSuper to KeyModifiers.META,
        Platform.compatibility().keys().keyRightSuper to KeyModifiers.META,
    )

    init {
        eventHandler { (key, _, state): KeyInputEvent ->
            // key == 0 marks a character (text input) event, not a coded key press. These only ever fire
            // with state == 1 (never released), so adding them to downKeys would leave 0 stuck there and
            // match any keybind bound to code 0. Ignore them entirely; keybinds only care about key codes.
            if (key == 0) return@eventHandler
            if (state == 2) return@eventHandler
            val down = state == 1
            val mod = MODIFIER_MAP[key]
            if (mod != null) {
                mods = if (down) (mods or mod) else (mods and mod.inv())
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
    fun isRegistered(bind: OneConfigKeybind): Boolean = bind in binds

    /**
     * Swap a registered keybind for a new one, preserving its registration.
     *
     * Used by the settings UI when the user rebinds a keybind: setting the config property may either mutate the
     * existing keybind in place (in which case [old] and [new] are the same instance and it is already registered
     * with the updated keys) or replace it with a fresh instance (in which case the old, stale instance must be
     * swapped out of the manager). Handling both here means a mod's keybind keeps working after a rebind without
     * the mod having to register its own change callback to unregister/re-register.
     *
     * If [old] was never registered, nothing happens — keybinds the caller chose not to manage are left alone.
     */
    @JvmStatic
    fun replace(old: OneConfigKeybind?, new: OneConfigKeybind): OneConfigKeybind {
        if (old === new) return new
        if (old == null || old !in binds) return new
        unregister(old)
        register(new)
        return new
    }

    @JvmStatic
    fun findConflicts(bind: OneConfigKeybind): List<OneConfigKeybind> {
        if (!bind.isBound) return emptyList()
        return binds.filter { it.conflictsWith(bind) }
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
