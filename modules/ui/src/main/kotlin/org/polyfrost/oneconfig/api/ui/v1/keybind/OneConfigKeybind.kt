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
     * Human-readable name shown for this keybind in Minecraft's Controls menu, or `null` if it should not be
     * surfaced there. Set automatically for config-backed keybinds (from the `@Keybind` title) and may be set
     * via [KeybindHelper.name].
     */
    @Transient
    var name: String? = null

    /**
     * Category heading this keybind is grouped under in Minecraft's Controls menu. Falls back to `"OneConfig"`
     * when unset. Set automatically for config-backed keybinds (from the `@Keybind` category) and may be set
     * via [KeybindHelper.category].
     */
    @Transient
    var category: String? = null

    /**
     * The original (code-default) keybind this one was created from, if any — used so Minecraft's Controls menu can
     * reset back to the true OneConfig default (keys + modifiers), matching the OneConfig GUI's reset. Set by the
     * config layer from `"default"` metadata; its action is irrelevant (only keys/mods are read).
     */
    @Transient
    var defaultKeybind: OneConfigKeybind? = null

    val isBound get() = keyCodes?.isNotEmpty() == true || mouseBtns?.isNotEmpty() == true

    /**
     * The single input code Minecraft's Controls menu binds to: the first key code, else the first mouse button, else
     * `-1` (GLFW_KEY_UNKNOWN). Combos and modifiers can't be represented by a single vanilla mapping; they are shown
     * via the keybind's display label instead.
     */
    val boundCode get() = keyCodes?.firstOrNull() ?: mouseBtns?.firstOrNull() ?: -1

    val isMousePrimary get() = (keyCodes?.isNotEmpty() != true) && (mouseBtns?.isNotEmpty() == true)

    /**
     * Create a copy of this keybind bound to the given keys/modifiers, preserving the action, duration, subtype, and
     * Controls-menu metadata.
     */
    open fun copyWith(keyCodes: IntArray?, mouseBtns: IntArray?, mods: Byte): OneConfigKeybind =
        OneConfigKeybind(keyCodes, mouseBtns, mods, durationNanos, action).also {
            it.name = name
            it.category = category
            it.defaultKeybind = defaultKeybind
        }

    /**
     * Absorb the keys/modifiers of a freshly-loaded keybind into this instance, keeping this instance's transient
     * action (lost on deserialization) and Controls-menu metadata.
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
}
