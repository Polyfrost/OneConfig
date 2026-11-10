package org.polyfrost.oneconfig.api.ui.v1.keybind

import java.util.function.Consumer
import kotlin.experimental.or

public class KeybindHelper {
    private val keyCodes = mutableListOf<Int>()
    private val mouseBtns = mutableListOf<Int>()
    private var mods: Byte = KeyModifiers.NONE
    private var inScreens = false
    private var firesWhileTyping = false
    private var durationNanos: Long = 0L
    private var action: ((Boolean) -> Boolean)? = null
    private var name: String? = null
    private var category: String? = null

    public fun key(vararg codes: Int): KeybindHelper = apply { keyCodes.addAll(codes.asList()) }
    public fun mouse(vararg btns: Int): KeybindHelper = apply { mouseBtns.addAll(btns.asList()) }
    /**
     * Name shown for this keybind in Minecraft's Controls menu
     */
    public fun name(name: String): KeybindHelper = apply { this.name = name }
    /**
     * Category heading for this keybind in Minecraft's Controls menu
     *
     * Defaults to "OneConfig"
     */
    public fun category(category: String): KeybindHelper = apply { this.category = category }
    public fun shift(): KeybindHelper = apply { mods = mods or KeyModifiers.SHIFT }
    public fun ctrl(): KeybindHelper = apply { mods = mods or KeyModifiers.CTRL }
    public fun alt(): KeybindHelper = apply { mods = mods or KeyModifiers.ALT }
    public fun meta(): KeybindHelper = apply { mods = mods or KeyModifiers.META }
    public fun inScreens(): KeybindHelper = apply { inScreens = true }
    public fun firesWhileTyping(): KeybindHelper = apply { firesWhileTyping = true }
    public fun duration(nanos: Long): KeybindHelper = apply { durationNanos = nanos }
    public fun action(fn: (Boolean) -> Boolean): KeybindHelper = apply { action = fn }
    public fun action(fn: () -> Unit): KeybindHelper = apply { action = { b -> if (b) fn(); true } }
    public fun action(fn: Runnable): KeybindHelper = apply { action = { b -> if (b) fn.run(); true } }
    public fun action(fn: Consumer<Boolean>): KeybindHelper = apply { action = { b -> fn.accept(b); true } }

    /**
     * Builds the keybind
     *
     * An action is optional
     *
     * Config-backed (`@Keybind`) binds exist only to be bound by the user and are polled by the owning code
     * so they build with a no-op action
     */
    public fun build(): OneConfigKeybind {
        val fn = action ?: NO_OP
        val keys = keyCodes.toIntArray().takeIf { it.isNotEmpty() }
        val mouse = mouseBtns.toIntArray().takeIf { it.isNotEmpty() }
        val bind = if (inScreens) OneConfigKeybind(keys, mouse, mods, durationNanos, fn)
        else BindNotInScreen(keys, mouse, mods, durationNanos, fn)
        bind.name = name
        bind.category = category
        bind.firesWhileTyping = firesWhileTyping
        return bind
    }

    public fun register(): OneConfigKeybind = KeybindManager.register(build())

    public companion object {
        private val NO_OP: (Boolean) -> Boolean = { true }

        @JvmStatic
        public fun builder(): KeybindHelper = KeybindHelper()
    }
}
