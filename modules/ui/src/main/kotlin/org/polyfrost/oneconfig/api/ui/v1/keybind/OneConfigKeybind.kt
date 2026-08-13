package org.polyfrost.oneconfig.api.ui.v1.keybind

import org.polyfrost.oneconfig.utils.v1.OverwriteMergeable
import kotlin.experimental.and

open class OneConfigKeybind(
    var keyCodes: IntArray?,
    var mouseBtns: IntArray?,
    var mods: Byte,
    var durationNanos: Long,
    @Transient
    val action: (Boolean) -> Boolean,
) : OverwriteMergeable {
    /**
     * Human-readable name shown for this keybind in Minecraft's Controls menu or `null` if it should not be
     * surfaced there
     *
     * Set automatically for config-backed keybinds (from the `@Keybind` title) and may be set
     * via [KeybindHelper.name]
     */
    @Transient
    var name: String? = null

    /**
     * Category heading this keybind is grouped under in Minecraft's Controls menu
     *
     * Falls back to `"OneConfig"` when unset
     *
     * Set automatically for config-backed keybinds (from the `@Keybind` category) and may be set
     * via [KeybindHelper.category]
     */
    @Transient
    var category: String? = null

    /**
     * The code-default keybind this one was created from so Minecraft's Controls menu resets back to the true
     * OneConfig default
     *
     * Set by the config layer from `"default"` metadata
     *
     * Only its keys and modifiers are read
     */
    @Transient
    var defaultKeybind: OneConfigKeybind? = null

    val isBound get() = keyCodes?.isNotEmpty() == true || mouseBtns?.isNotEmpty() == true

    /**
     * The single input code Minecraft's Controls menu binds to
     *
     * It is the first key code else the first mouse button else `-1` (GLFW_KEY_UNKNOWN)
     *
     * Combos and modifiers cannot be represented by a single vanilla mapping so they are shown
     * via the keybind's display label instead
     */
    val boundCode get() = keyCodes?.firstOrNull() ?: mouseBtns?.firstOrNull() ?: -1

    val isMousePrimary get() = (keyCodes?.isNotEmpty() != true) && (mouseBtns?.isNotEmpty() == true)

    /**
     * Create a copy of this keybind bound to the given keys/modifiers
     *
     * Preserves the action and duration and subtype and Controls-menu metadata
     */
    open fun copyWith(keyCodes: IntArray?, mouseBtns: IntArray?, mods: Byte): OneConfigKeybind =
        OneConfigKeybind(keyCodes, mouseBtns, mods, durationNanos, action).also {
            it.name = name
            it.category = category
            it.defaultKeybind = defaultKeybind
        }

    /**
     * Absorb the keys/modifiers of a freshly-loaded keybind into this instance
     *
     * Keeps this instance's transient action (lost on deserialization) and Controls-menu metadata
     */
    override fun mergeOverwrite(incoming: Any): Any {
        if (incoming is OneConfigKeybind && incoming !== this) {
            keyCodes = incoming.keyCodes
            mouseBtns = incoming.mouseBtns
            mods = incoming.mods
            durationNanos = incoming.durationNanos
        }
        return this
    }

    open fun test(downKeys: Set<Int>, downMouse: Set<Int>, currentMods: Byte): Boolean {
        if (!isBound) return false
        if (mods != KeyModifiers.NONE && (currentMods and mods) != mods) return false
        val keysHeld = keyCodes?.all { it in downKeys } ?: true
        val mouseHeld = mouseBtns?.all { it in downMouse } ?: true
        return keysHeld && mouseHeld
    }

    fun conflictsWith(other: OneConfigKeybind): Boolean {
        if (this === other || !isBound || !other.isBound) return false
        return mods == other.mods &&
            sameCodes(keyCodes, other.keyCodes) &&
            sameCodes(mouseBtns, other.mouseBtns)
    }

    private fun sameCodes(a: IntArray?, b: IntArray?): Boolean {
        val sa = a?.toHashSet() ?: emptySet()
        val sb = b?.toHashSet() ?: emptySet()
        return sa == sb
    }

    /**
     * Human-readable label for this keybind such as `"Shift + G"` or `"Right Shift"`
     *
     * Suitable for display in UI or messages
     *
     * Returns `"None"` when unbound
     */
    fun displayName(): String {
        if (!isBound) return "None"
        val parts = LinkedHashSet<String>()
        parts += modifierNames(mods)
        keyCodes?.forEach { parts += keyName(it) }
        mouseBtns?.forEach { parts += "Mouse ${it + 1}" }
        return parts.joinToString(" + ").ifEmpty { "None" }
    }

    companion object {
        /** Human-readable name for a GLFW key code */
        @JvmStatic
        fun keyName(glfwCode: Int): String = when (glfwCode) {
            -1 -> "None"
            32 -> "Space"
            39 -> "'"
            44 -> ","
            45 -> "-"
            46 -> "."
            47 -> "/"
            59 -> ";"
            61 -> "="
            91 -> "["
            92 -> "\\"
            93 -> "]"
            96 -> "`"
            161 -> "Non-US #1"
            162 -> "Non-US #2"
            256 -> "Escape"
            257 -> "Enter"
            258 -> "Tab"
            259 -> "Backspace"
            260 -> "Insert"
            261 -> "Delete"
            262 -> "Right"
            263 -> "Left"
            264 -> "Down"
            265 -> "Up"
            266 -> "Page Up"
            267 -> "Page Down"
            268 -> "Home"
            269 -> "End"
            280 -> "Caps Lock"
            281 -> "Scroll Lock"
            282 -> "Num Lock"
            283 -> "Print Screen"
            284 -> "Pause"
            330 -> "Numpad ."
            331 -> "Numpad /"
            332 -> "Numpad *"
            333 -> "Numpad -"
            334 -> "Numpad +"
            335 -> "Numpad Enter"
            336 -> "Numpad ="
            340 -> "Left Shift"
            344 -> "Right Shift"
            341 -> "Left Ctrl"
            345 -> "Right Ctrl"
            342 -> "Left Alt"
            346 -> "Right Alt"
            343 -> "Left Super"
            347 -> "Right Super"
            348 -> "Menu"
            in 48..57 -> ('0' + (glfwCode - 48)).toString()
            in 65..90 -> ('A' + (glfwCode - 65)).toString()
            in 290..313 -> "F${glfwCode - 289}"          // F1 to F24
            in 320..329 -> "Numpad ${glfwCode - 320}"    // KP_0 to KP_9
            else -> "Key $glfwCode"
        }

        private fun modifierNames(mods: Byte): List<String> = buildList {
            if (KeyModifiers.has(mods, KeyModifiers.CTRL)) add("Ctrl")
            if (KeyModifiers.has(mods, KeyModifiers.SHIFT)) add("Shift")
            if (KeyModifiers.has(mods, KeyModifiers.ALT)) add("Alt")
            if (KeyModifiers.has(mods, KeyModifiers.META)) add("Meta")
        }
    }
}
