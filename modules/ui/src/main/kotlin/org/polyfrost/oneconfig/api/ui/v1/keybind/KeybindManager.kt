package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.event.v1.eventHandler
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.events.WindowFocusEvent
import org.polyfrost.oneconfig.api.platform.v1.Platform
import java.util.ServiceLoader
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Suppress("UnstableApiUsage")
object KeybindManager {
    private val LOGGER = LogManager.getLogger("OneConfig/Keybinds")

    private val binds = java.util.concurrent.CopyOnWriteArrayList<OneConfigKeybind>()
    private val activeBinds = HashSet<OneConfigKeybind>()

    /**
     * Bridge into Minecraft's native Controls menu if present
     */
    private val bridge: MinecraftKeybindBridge? by lazy {
        try {
            ServiceLoader.load(MinecraftKeybindBridge::class.java, MinecraftKeybindBridge::class.java.classLoader)
                .iterator().let { if (it.hasNext()) it.next() else null }
        } catch (t: Throwable) {
            LOGGER.warn("Failed to load MinecraftKeybindBridge; OneConfig keybinds will not appear in the Controls menu", t)
            null
        }
    }

    /** Listeners notified when a keybind is rebound from Minecraft's Controls menu with the old and new instances */
    private val rebindListeners = ArrayList<(OneConfigKeybind, OneConfigKeybind) -> Unit>()

    /** Listeners notified when a keybind is edited in-place from Minecraft's Controls menu (same instance) */
    private val menuEditListeners = ArrayList<(OneConfigKeybind) -> Unit>()

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
            // key == 0 marks a character event not a coded key press and it never reports a release
            // so tracking it in downKeys would leave 0 stuck and match any keybind bound to code 0
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
            if (state == 1) {
                downMouse += btn
            } else {
                downMouse -= btn
            }
        }

        eventHandler { _: TickEvent.End ->
            checkBinds()
        }

        eventHandler { (screen): ScreenOpenEvent ->
            if (screen == null) {
                TextInputFocus.clear()
                clearState()
            }
        }

        eventHandler { _: WindowFocusEvent.Lost ->
            clearState()
        }
    }

    @JvmStatic
    fun register(bind: OneConfigKeybind): OneConfigKeybind {
        if (bind !in binds) binds.add(bind)
        bridge?.register(bind)
        return bind
    }

    @JvmStatic
    fun unregister(bind: OneConfigKeybind) {
        binds.remove(bind)
        activeBinds.remove(bind)
        bridge?.unregister(bind)
    }

    @JvmStatic
    fun isRegistered(bind: OneConfigKeybind): Boolean = bind in binds

    /**
     * Re-push an already-registered keybind to the Minecraft Controls menu
     *
     * Use after mutating a keybind's [OneConfigKeybind.name]/[OneConfigKeybind.category] so the vanilla
     * mapping picks up the new metadata
     */
    @JvmStatic
    fun refreshMinecraftBinding(bind: OneConfigKeybind) {
        if (bind in binds) bridge?.register(bind)
    }

    /**
     * Swap a registered keybind for a new one while preserving its registration
     *
     * Setting the config property may either mutate [old] in place (so [old] and [new] are the same instance and
     * already registered) or hand back a fresh instance in which case the stale one is swapped out here
     *
     * Nothing happens if [old] was never registered
     */
    @JvmStatic
    fun replace(old: OneConfigKeybind?, new: OneConfigKeybind): OneConfigKeybind {
        if (old != null && new !== old) {
            if (new.name == null) new.name = old.name
            if (new.category == null) new.category = old.category
            if (new.defaultKeybind == null) new.defaultKeybind = old.defaultKeybind
        }
        if (old === new) {
            bridge?.sync(new)
            return new
        }
        if (old == null || old !in binds) return new
        unregister(old)
        register(new)
        return new
    }

    @JvmStatic
    fun addRebindListener(listener: (OneConfigKeybind, OneConfigKeybind) -> Unit) {
        rebindListeners.add(listener)
    }

    @JvmStatic
    fun addMenuEditListener(listener: (OneConfigKeybind) -> Unit) {
        menuEditListeners.add(listener)
    }

    /**
     * Notify that [bind] was mutated in place by Minecraft's Controls menu (rebind/unbind/reset)
     *
     * Re-syncs the vanilla mapping and notifies [menuEditListeners]
     */
    @JvmStatic
    fun notifyMenuEdit(bind: OneConfigKeybind) {
        for (listener in menuEditListeners) {
            try {
                listener(bind)
            } catch (t: Throwable) {
                LOGGER.error("Keybind menu-edit listener threw an exception", t)
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun rebindFromMinecraft(
        old: OneConfigKeybind,
        keyCodes: IntArray?,
        mouseBtns: IntArray? = null,
        mods: Byte = KeyModifiers.NONE,
    ): OneConfigKeybind {
        val new = old.copyWith(keyCodes, mouseBtns, mods)
        replace(old, new)
        for (listener in rebindListeners) {
            try {
                listener(old, new)
            } catch (t: Throwable) {
                LOGGER.error("Keybind rebind listener threw an exception", t)
            }
        }
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
        val typing = TextInputFocus.isTyping
        for (bind in binds) {
            @Suppress("SENSELESS_COMPARISON")
            if (bind.action == null) continue
            val triggered = (!typing || bind.firesWhileTyping) && bind.test(downKeys, downMouse, mods)
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
