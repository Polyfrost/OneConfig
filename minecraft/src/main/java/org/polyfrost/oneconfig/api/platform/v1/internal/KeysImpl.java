package org.polyfrost.oneconfig.api.platform.v1.internal;

import com.mojang.blaze3d.platform.InputConstants;
import org.polyfrost.oneconfig.api.platform.v1.Keys;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindCodec;

public class KeysImpl implements Keys {
    @Override
    public String keyName(int key) {
        InputConstants.Key input = MinecraftKeybindCodec.keysym(key);
        return input == InputConstants.UNKNOWN ? "None" : input.getDisplayName().getString();
    }

    @Override
    public String mouseName(int button) {
        InputConstants.Key input = MinecraftKeybindCodec.mouse(button);
        return input == InputConstants.UNKNOWN ? "None" : input.getDisplayName().getString();
    }

    @Override
    public int getKeyA() {
        return InputConstants.KEY_A;
    }

    @Override
    public int getKeyC() {
        return InputConstants.KEY_C;
    }

    @Override
    public int getKeyD() {
        return InputConstants.KEY_D;
    }

    @Override
    public int getKeyE() {
        return InputConstants.KEY_E;
    }

    @Override
    public int getKeyH() {
        return InputConstants.KEY_H;
    }

    @Override
    public int getKeyL() {
        return InputConstants.KEY_L;
    }

    @Override
    public int getKeyR() {
        return InputConstants.KEY_R;
    }

    @Override
    public int getKeyV() {
        return InputConstants.KEY_V;
    }

    @Override
    public int getKeyX() {
        return InputConstants.KEY_X;
    }

    @Override
    public int getKeyDelete() {
        return InputConstants.KEY_DELETE;
    }

    @Override
    public int getMouseButtonLeft() {
        return InputConstants.MOUSE_BUTTON_LEFT;
    }

    @Override
    public int getKeyLeftShift() {
        return InputConstants.KEY_LSHIFT;
    }

    @Override
    public int getKeyRightShift() {
        return InputConstants.KEY_RSHIFT;
    }

    @Override
    public int getKeyLeftControl() {
        return InputConstants.KEY_LCONTROL;
    }

    @Override
    public int getKeyRightControl() {
        return InputConstants.KEY_RCONTROL;
    }

    @Override
    public int getKeyLeftAlt() {
        return InputConstants.KEY_LALT;
    }

    @Override
    public int getKeyRightAlt() {
        return InputConstants.KEY_RALT;
    }

    @Override
    public int getKeyLeftSuper() {
        //? if >= 26.3 || = 1.8.9 {
        /*return InputConstants.KEY_LGUI;
        *///?} elif >= 1.21.10 {
        return InputConstants.KEY_LSUPER;
        //?} else
        //return InputConstants.KEY_LWIN;
    }

    @Override
    public int getKeyRightSuper() {
        //? if >= 26.3 || = 1.8.9 {
        /*return InputConstants.KEY_RGUI;
        *///?} elif >= 1.21.10 {
        return InputConstants.KEY_RSUPER;
        //?} else
        //return InputConstants.KEY_RWIN;
    }
}
