package org.polyfrost.oneconfig.api.ui.v1.keybind

private const val NOT_PRESSED = Long.MIN_VALUE

/**
 * How a key drives a temporary on/off state
 */
public enum class KeyPressMode {
    /** Active only while the key is held */
    Held,

    /** Each press flips the state */
    Toggle,

    /**
     * A press flips the state straight away
     *
     * Releasing it after holding for longer than the hold threshold puts the state back to how it was
     * before the press so a tap acts as [Toggle] and a hold acts as [Held]
     */
    QuickPress;
}

/**
 * Edge-driven state machine turning key presses and releases into an active flag according to a [KeyPressMode]
 *
 * Feed it every change of the key's held state through [input], from a keybind action or by polling a key
 */
public class KeyPressState {
    /** Whether the state is currently on */
    public var active: Boolean = false
        private set

    /** Whether the key was down at the last [input] */
    public var isDown: Boolean = false
        private set

    private var pressedAt = NOT_PRESSED
    private var activeBeforePress = false

    /**
     * Records the key as [down] or released
     *
     * Repeated calls with the same [down] are ignored so this is safe to call every tick or frame
     *
     * @param holdThresholdMs how long a [KeyPressMode.QuickPress] press must be held to count as a hold
     * @return whether [active] changed
     */
    @JvmOverloads
    public fun input(down: Boolean, mode: KeyPressMode, holdThresholdMs: Long, nowNanos: Long = System.nanoTime()): Boolean {
        if (down == isDown) return false
        isDown = down
        val before = active
        when (mode) {
            KeyPressMode.Held -> active = down
            KeyPressMode.Toggle -> if (down) active = !active
            KeyPressMode.QuickPress -> if (down) {
                activeBeforePress = active
                pressedAt = nowNanos
                active = !active
            } else {
                // a press which began under another mode has no start time and so cannot be a hold
                if (pressedAt != NOT_PRESSED && nowNanos - pressedAt > holdThresholdMs * 1_000_000L) active = activeBeforePress
                pressedAt = NOT_PRESSED
            }
        }
        if (mode != KeyPressMode.QuickPress) pressedAt = NOT_PRESSED
        return active != before
    }

    /**
     * Turns the state off and forgets any press in progress
     *
     * @return whether [active] changed
     */
    public fun reset(): Boolean {
        val was = active
        active = false
        isDown = false
        pressedAt = NOT_PRESSED
        return was
    }
}
