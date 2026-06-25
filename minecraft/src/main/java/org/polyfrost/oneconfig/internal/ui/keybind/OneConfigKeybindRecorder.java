package org.polyfrost.oneconfig.internal.ui.keybind;

/**
 * Implemented by the keybinds-screen mixin so an external recorder (e.g. the Controlling-mod key handler, which
 * doesn't route key presses through the vanilla screen) can push keys into the same in-progress combo recording.
 * Mouse + release-polling + commit already run through the vanilla screen's super calls, so only keys need this.
 */
public interface OneConfigKeybindRecorder {
    boolean oneconfig$isOurs();

    void oneconfig$recordKey(int keyCode);

    void oneconfig$recordEscape();
}
