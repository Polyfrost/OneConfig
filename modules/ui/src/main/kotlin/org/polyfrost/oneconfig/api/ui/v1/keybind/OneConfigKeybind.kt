package org.polyfrost.oneconfig.api.ui.v1.keybind

import kotlin.experimental.and
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.utils.v1.OverwriteMergeable

public open class OneConfigKeybind(
    public var keyCodes: IntArray?,
    public var mouseBtns: IntArray?,
    public var mods: Byte,
    public var durationNanos: Long,
    @Transient
    public val action: (Boolean) -> Boolean,
) : OverwriteMergeable {
    /**
     * Human-readable name shown for this keybind in Minecraft's Controls menu or `null` if it should not be
     * surfaced there
     *
     * Set automatically for config-backed keybinds (from the `@Keybind` title) and may be set
     * via [KeybindHelper.name]
     */
    @Transient
    public var name: String? = null

    /**
     * Category heading this keybind is grouped under in Minecraft's Controls menu
     *
     * Falls back to `"OneConfig"` when unset
     *
     * Set automatically for config-backed keybinds (from the `@Keybind` category) and may be set
     * via [KeybindHelper.category]
     */
    @Transient
    public var category: String? = null

    /**
     * The code-default keybind this one was created from so Minecraft's Controls menu resets back to the true
     * OneConfig default
     *
     * Set by the config layer from `"default"` metadata
     *
     * Only its keys and modifiers are read
     */
    @Transient
    public var defaultKeybind: OneConfigKeybind? = null

    @Transient
    public var firesWhileTyping: Boolean = false

    @ExperimentalOneConfigUiApi
    @Transient
    public var mirrorToVanilla: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            KeybindManager.refreshMinecraftBinding(this)
        }

    /**
     * Serialized key inputs that could not be resolved on this Minecraft version.
     *
     * They stay inactive but are written back on save so they don't get erased by a version switch.
     */
    @Transient
    public var unresolvedKeyInputs: List<Any>? = null

    /*
     * Serialized mouse inputs that could not be resolved on this Minecraft version.
     *
     * @see unresolvedKeyInputs
     */
    @Transient
    public var unresolvedMouseInputs: List<Any>? = null

    public val hasUnresolvedInputs: Boolean get() = !unresolvedKeyInputs.isNullOrEmpty() || !unresolvedMouseInputs.isNullOrEmpty()

    public val isBound: Boolean get() = keyCodes?.isNotEmpty() == true || mouseBtns?.isNotEmpty() == true

    /**
     * The single input code Minecraft's Controls menu binds to
     *
     * It is the first key code else the first mouse button else `-1` (GLFW_KEY_UNKNOWN)
     *
     * Combos and modifiers cannot be represented by a single vanilla mapping so they are shown
     * via the keybind's display label instead
     */
    public val boundCode: Int get() = keyCodes?.firstOrNull() ?: mouseBtns?.firstOrNull() ?: -1

    public val isMousePrimary: Boolean get() = (keyCodes?.isNotEmpty() != true) && (mouseBtns?.isNotEmpty() == true)

    /**
     * Create a copy of this keybind bound to the given keys/modifiers
     *
     * Preserves the action and duration and subtype and Controls-menu metadata
     */
    @OptIn(ExperimentalOneConfigUiApi::class)
    public open fun copyWith(keyCodes: IntArray?, mouseBtns: IntArray?, mods: Byte): OneConfigKeybind =
        OneConfigKeybind(keyCodes, mouseBtns, mods, durationNanos, action).also {
            it.name = name
            it.category = category
            it.defaultKeybind = defaultKeybind
            it.firesWhileTyping = firesWhileTyping
            it.mirrorToVanilla = mirrorToVanilla
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
            unresolvedKeyInputs = incoming.unresolvedKeyInputs
            unresolvedMouseInputs = incoming.unresolvedMouseInputs
        }
        return this
    }

    public open fun test(downKeys: Set<Int>, downMouse: Set<Int>, currentMods: Byte): Boolean {
        if (!isBound) return false
        if (mods != KeyModifiers.NONE && (currentMods and mods) != mods) return false
        val keysHeld = keyCodes?.all { it in downKeys } ?: true
        val mouseHeld = mouseBtns?.all { it in downMouse } ?: true
        return keysHeld && mouseHeld
    }

    public fun conflictsWith(other: OneConfigKeybind): Boolean {
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
    public fun displayName(): String {
        if (!isBound) return "None"
        val parts = LinkedHashSet<String>()
        parts += modifierNames(mods)
        keyCodes?.forEach { parts += keyName(it) }
        mouseBtns?.forEach { parts += Platform.compatibility().keys().mouseName(it) }
        return parts.joinToString(" + ").ifEmpty { "None" }
    }

    public companion object {
        /** Human-readable name for a key code */
        @JvmStatic
        public fun keyName(key: Int): String = Platform.compatibility().keys().keyName(key)

        private fun modifierNames(mods: Byte): List<String> = buildList {
            if (KeyModifiers.has(mods, KeyModifiers.CTRL)) add("Ctrl")
            if (KeyModifiers.has(mods, KeyModifiers.SHIFT)) add("Shift")
            if (KeyModifiers.has(mods, KeyModifiers.ALT)) add("Alt")
            if (KeyModifiers.has(mods, KeyModifiers.META)) add("Meta")
        }
    }
}
