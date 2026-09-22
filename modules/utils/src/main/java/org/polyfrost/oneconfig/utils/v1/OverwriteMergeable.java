package org.polyfrost.oneconfig.utils.v1;

/**
 * Implemented by config values that need to preserve transient non-serialized state when a loaded value overwrites
 * them
 *
 * <p>See {@code OneConfigKeybind} where the key action is {@code transient} and therefore lost on
 * deserialization while the keys and modifiers are restored from disk
 *
 * <p>Merging the loaded keys into the existing keybind keeps the action intact
 */
public interface OverwriteMergeable {
    /**
     * Absorb the relevant state of [incoming] into this instance and return
     * the value that should be stored in the property
     *
     * <p>[incoming] is the freshly loaded deserialized value
     *
     * @param incoming the value being written over this one <p> never null
     * @return the value to store <p> usually {@code this}
     */
    Object mergeOverwrite(Object incoming);
}
