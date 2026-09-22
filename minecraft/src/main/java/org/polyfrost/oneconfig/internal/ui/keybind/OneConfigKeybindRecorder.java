package org.polyfrost.oneconfig.internal.ui.keybind;

/**
 * Implemented by the keybinds-screen mixin so an external recorder such as the Controlling key
 * handler can push keys into the same in-progress combo recording
 * <p>
 * Only keys need this since mouse and release polling and commit go through the vanilla screen
 */
public interface OneConfigKeybindRecorder {
    boolean oneconfig$isOurs();

    void oneconfig$recordKey(int keyCode);

    void oneconfig$recordEscape();
}
