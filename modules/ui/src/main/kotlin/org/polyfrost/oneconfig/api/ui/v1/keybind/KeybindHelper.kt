package org.polyfrost.oneconfig.api.ui.v1.keybind

import kotlin.experimental.or

class KeybindHelper {
    private val keyCodes = mutableListOf<Int>()
    private val mouseBtns = mutableListOf<Int>()
    private var mods: Byte = KeyModifiers.NONE
    private var inScreens = false
    private var durationNanos: Long = 0L
    private var action: ((Boolean) -> Boolean)? = null
    private var name: String? = null
    private var category: String? = null

    fun key(vararg codes: Int) = apply { keyCodes.addAll(codes.asList()) }
    fun mouse(vararg btns: Int) = apply { mouseBtns.addAll(btns.asList()) }
    /**
     * Name shown for this keybind in Minecraft's Controls menu.
     */
    fun name(name: String) = apply { this.name = name }
    /**
     * Category heading for this keybind in Minecraft's Controls menu. Defaults to "OneConfig".
     */
    fun category(category: String) = apply { this.category = category }
    fun shift() = apply { mods = (mods or KeyModifiers.SHIFT).toByte() }
    fun ctrl() = apply { mods = (mods or KeyModifiers.CTRL).toByte() }
    fun alt() = apply { mods = (mods or KeyModifiers.ALT).toByte() }
    fun meta() = apply { mods = (mods or KeyModifiers.META).toByte() }
    fun inScreens() = apply { inScreens = true }
    fun duration(nanos: Long) = apply { durationNanos = nanos }
    fun action(fn: (Boolean) -> Boolean) = apply { action = fn }
    fun action(fn: () -> Unit) = apply { action = { b -> if (b) fn(); true } }
    fun action(fn: Runnable) = apply { action = { b -> if (b) fn.run(); true } }
    fun action(fn: java.util.function.Consumer<Boolean>) = apply { action = { b -> fn.accept(b); true } }

    fun build(): OneConfigKeybind {
        val fn = requireNotNull(action) { "KeybindHelper: action must be set before build()" }
        val keys = keyCodes.toIntArray().takeIf { it.isNotEmpty() }
        val mouse = mouseBtns.toIntArray().takeIf { it.isNotEmpty() }
        val bind = if (inScreens) OneConfigKeybind(keys, mouse, mods, durationNanos, fn)
        else BindNotInScreen(keys, mouse, mods, durationNanos, fn)
        bind.name = name
        bind.category = category
        return bind
    }

    fun register(): OneConfigKeybind = KeybindManager.register(build())

    companion object {
        @JvmStatic
        fun builder() = KeybindHelper()
    }
}
