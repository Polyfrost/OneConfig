package org.polyfrost.oneconfig.api.ui.v1.keybind

import dev.deftu.omnicore.api.client.input.OmniKey
import kotlin.experimental.or

class KeybindHelper {
    private val keyCodes = mutableListOf<Int>()
    private val mouseBtns = mutableListOf<Int>()
    private var mods: Byte = KeyModifiers.NONE
    private var inScreens = false
    private var durationNanos: Long = 0L
    private var action: ((Boolean) -> Boolean)? = null

    fun key(vararg codes: Int) = apply { keyCodes.addAll(codes.asList()) }
    fun key(vararg keys: OmniKey) = apply { keyCodes.addAll(keys.map { it.code }) }
    fun mouse(vararg btns: Int) = apply { mouseBtns.addAll(btns.asList()) }
    fun shift() = apply { mods = (mods or KeyModifiers.SHIFT).toByte() }
    fun ctrl() = apply { mods = (mods or KeyModifiers.CTRL).toByte() }
    fun alt() = apply { mods = (mods or KeyModifiers.ALT).toByte() }
    fun meta() = apply { mods = (mods or KeyModifiers.META).toByte() }
    fun inScreens() = apply { inScreens = true }
    fun duration(nanos: Long) = apply { durationNanos = nanos }
    fun action(fn: (Boolean) -> Boolean) = apply { action = fn }
    fun action(fn: () -> Unit) = apply { action = { fn(); true } }
    fun action(fn: Runnable) = apply { action = { fn.run(); true } }
    fun action(fn: java.util.function.Consumer<Boolean>) = apply { action = { b -> fn.accept(b); true } }

    fun build(): OneConfigKeybind {
        val fn = requireNotNull(action) { "KeybindHelper: action must be set before build()" }
        val keys = keyCodes.toIntArray().takeIf { it.isNotEmpty() }
        val mouse = mouseBtns.toIntArray().takeIf { it.isNotEmpty() }
        return if (inScreens) OneConfigKeybind(keys, mouse, mods, durationNanos, fn)
        else BindNotInScreen(keys, mouse, mods, durationNanos, fn)
    }

    fun register(): OneConfigKeybind = KeybindManager.register(build())

    companion object {
        @JvmStatic
        fun builder() = KeybindHelper()
    }
}
