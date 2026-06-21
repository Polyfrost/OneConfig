package org.polyfrost.oneconfig.utils.v1;

/**
 * Implemented by config values that need to preserve transient (non-serialized) state when a loaded value overwrites
 * them.
 *
 * <p>See {@code OneConfigKeybind}: its key action is {@code transient} and therefore lost on
 * deserialization, but the keys/modifiers are restored from disk. Merging the loaded keys into the existing keybind
 * keeps the action intact.
 */
public interface OverwriteMergeable {
    /**
     * Absorb the relevant state of [incoming] (the freshly-loaded/deserialized value) into this instance and return
     * the value that should be stored in the property.
     *
     * @param incoming the value being written over this one; never null
     * @return the value to store (usually {@code this})
     */
    Object mergeOverwrite(Object incoming);
}
