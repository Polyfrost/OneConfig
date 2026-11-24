package org.polyfrost.oneconfig.api.platform.v1.internal;

import com.mojang.blaze3d.platform.InputConstants;
import org.polyfrost.oneconfig.api.platform.v1.Keys;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindCodec;
//? if = 1.8.9
//import org.polyfrost.oneconfig.internal.legacy.KeyCodes;

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
    public int getKeyF() {
        return InputConstants.KEY_F;
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
    public int getKeyY() {
        return InputConstants.KEY_Y;
    }

    @Override
    public int getKeyZ() {
        return InputConstants.KEY_Z;
    }

    @Override
    public int getKeyDelete() {
        return InputConstants.KEY_DELETE;
    }

    @Override
    public int getKeyF1() {
        return InputConstants.KEY_F1;
    }

    @Override
    public int getKeySlash() {
        return InputConstants.KEY_SLASH;
    }

    @Override
    public int getKeyBackslash() {
        return InputConstants.KEY_BACKSLASH;
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
        //? if sdl {
        return InputConstants.KEY_LGUI;
        //?} elif >= 1.21.10 {
        /*return InputConstants.KEY_LSUPER;
        *///?} else
        //return InputConstants.KEY_LWIN;
    }

    @Override
    public int getKeyRightSuper() {
        //? if sdl {
        return InputConstants.KEY_RGUI;
        //?} elif >= 1.21.10 {
        /*return InputConstants.KEY_RSUPER;
        *///?} else
        //return InputConstants.KEY_RWIN;
    }

    @Override
    public int getKeyB() {
        return InputConstants.KEY_B;
    }

    @Override
    public int getKeyG() {
        return InputConstants.KEY_G;
    }

    @Override
    public int getKeyI() {
        return InputConstants.KEY_I;
    }

    @Override
    public int getKeyJ() {
        return InputConstants.KEY_J;
    }

    @Override
    public int getKeyK() {
        return InputConstants.KEY_K;
    }

    @Override
    public int getKeyM() {
        return InputConstants.KEY_M;
    }

    @Override
    public int getKeyN() {
        return InputConstants.KEY_N;
    }

    @Override
    public int getKeyO() {
        return InputConstants.KEY_O;
    }

    @Override
    public int getKeyP() {
        return InputConstants.KEY_P;
    }

    @Override
    public int getKeyQ() {
        return InputConstants.KEY_Q;
    }

    @Override
    public int getKeyS() {
        return InputConstants.KEY_S;
    }

    @Override
    public int getKeyT() {
        return InputConstants.KEY_T;
    }

    @Override
    public int getKeyU() {
        return InputConstants.KEY_U;
    }

    @Override
    public int getKeyW() {
        return InputConstants.KEY_W;
    }

    @Override
    public int getKey0() {
        return InputConstants.KEY_0;
    }

    @Override
    public int getKey1() {
        return InputConstants.KEY_1;
    }

    @Override
    public int getKey2() {
        return InputConstants.KEY_2;
    }

    @Override
    public int getKey3() {
        return InputConstants.KEY_3;
    }

    @Override
    public int getKey4() {
        return InputConstants.KEY_4;
    }

    @Override
    public int getKey5() {
        return InputConstants.KEY_5;
    }

    @Override
    public int getKey6() {
        return InputConstants.KEY_6;
    }

    @Override
    public int getKey7() {
        return InputConstants.KEY_7;
    }

    @Override
    public int getKey8() {
        return InputConstants.KEY_8;
    }

    @Override
    public int getKey9() {
        return InputConstants.KEY_9;
    }

    @Override
    public int getKeyF2() {
        return InputConstants.KEY_F2;
    }

    @Override
    public int getKeyF3() {
        return InputConstants.KEY_F3;
    }

    @Override
    public int getKeyF4() {
        return InputConstants.KEY_F4;
    }

    @Override
    public int getKeyF5() {
        return InputConstants.KEY_F5;
    }

    @Override
    public int getKeyF6() {
        return InputConstants.KEY_F6;
    }

    @Override
    public int getKeyF7() {
        return InputConstants.KEY_F7;
    }

    @Override
    public int getKeyF8() {
        return InputConstants.KEY_F8;
    }

    @Override
    public int getKeyF9() {
        return InputConstants.KEY_F9;
    }

    @Override
    public int getKeyF10() {
        return InputConstants.KEY_F10;
    }

    @Override
    public int getKeyF11() {
        return InputConstants.KEY_F11;
    }

    @Override
    public int getKeyF12() {
        return InputConstants.KEY_F12;
    }

    @Override
    public int getKeyEscape() {
        return InputConstants.KEY_ESCAPE;
    }

    @Override
    public int getKeyEnter() {
        return InputConstants.KEY_RETURN;
    }

    @Override
    public int getKeyTab() {
        return InputConstants.KEY_TAB;
    }

    @Override
    public int getKeySpace() {
        return InputConstants.KEY_SPACE;
    }

    @Override
    public int getKeyBackspace() {
        return InputConstants.KEY_BACKSPACE;
    }

    @Override
    public int getKeyInsert() {
        return InputConstants.KEY_INSERT;
    }

    @Override
    public int getKeyHome() {
        return InputConstants.KEY_HOME;
    }

    @Override
    public int getKeyEnd() {
        return InputConstants.KEY_END;
    }

    @Override
    public int getKeyPageUp() {
        return InputConstants.KEY_PAGEUP;
    }

    @Override
    public int getKeyPageDown() {
        return InputConstants.KEY_PAGEDOWN;
    }

    @Override
    public int getKeyUp() {
        return InputConstants.KEY_UP;
    }

    @Override
    public int getKeyDown() {
        return InputConstants.KEY_DOWN;
    }

    @Override
    public int getKeyLeft() {
        return InputConstants.KEY_LEFT;
    }

    @Override
    public int getKeyRight() {
        return InputConstants.KEY_RIGHT;
    }

    @Override
    public int getMouseButtonRight() {
        return InputConstants.MOUSE_BUTTON_RIGHT;
    }

    @Override
    public int getMouseButtonMiddle() {
        return InputConstants.MOUSE_BUTTON_MIDDLE;
    }

    @Override
    public int keyFromNative(int key) {
        //? if = 1.8.9 {
        /*InputConstants.Key input = KeyCodes.fromLegacy(key);
        return input.getType() == InputConstants.Type.KEYBOARD ? input.getValue() : InputConstants.UNKNOWN.getValue();
        *///?} else
        return key;
    }

    @Override
    public int keyToNative(int key) {
        //? if = 1.8.9 {
        /*return KeyCodes.toLegacy(key);
        *///?} else
        return key;
    }

    @Override
    public int mouseFromNative(int button) {
        //? if = 1.8.9 {
        /*return KeyCodes.mouseFromLegacy(button);
        *///?} else
        return button;
    }

    @Override
    public int mouseToNative(int button) {
        //? if = 1.8.9 {
        /*return KeyCodes.mouseToLegacy(button);
        *///?} else
        return button;
    }
}
