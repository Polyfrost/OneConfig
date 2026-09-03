package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import org.lwjgl.input.Keyboard;

public final class KeyCodes {
    private static final int[] MODERN_TO_LWJGL = new int[InputConstants.KEY_MENU + 1];
    private static final int[] LWJGL_TO_MODERN = new int[Keyboard.KEYBOARD_SIZE];

    public static int toLegacy(int keyCode) {
        return MODERN_TO_LWJGL[keyCode];
    }

    public static int toLegacy(InputConstants.Key key) {
        if (key == null || key.equals(InputConstants.UNKNOWN)) return 0;
        if (key.getType() == InputConstants.Type.MOUSE) return key.getValue() - 100;
        return MODERN_TO_LWJGL[key.getValue()];
    }

    public static InputConstants.Key fromLegacy(int keyCode) {
        if (keyCode == 0) return InputConstants.UNKNOWN;
        if (keyCode < 0) return InputConstants.Type.MOUSE.getOrCreate(keyCode + 100);
        int modern = LWJGL_TO_MODERN[keyCode];
        return modern == 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(modern);
    }

    static {
      MODERN_TO_LWJGL[0x00] = Keyboard.KEY_NONE;
      MODERN_TO_LWJGL[InputConstants.KEY_SPACE] = Keyboard.KEY_SPACE;
      MODERN_TO_LWJGL[InputConstants.KEY_APOSTROPHE] = Keyboard.KEY_APOSTROPHE;
      MODERN_TO_LWJGL[InputConstants.KEY_COMMA] = Keyboard.KEY_COMMA;
      MODERN_TO_LWJGL[InputConstants.KEY_MINUS] = Keyboard.KEY_MINUS;
      MODERN_TO_LWJGL[InputConstants.KEY_PERIOD] = Keyboard.KEY_PERIOD;
      MODERN_TO_LWJGL[InputConstants.KEY_SLASH] = Keyboard.KEY_SLASH;
      MODERN_TO_LWJGL[InputConstants.KEY_0] = Keyboard.KEY_0;
      MODERN_TO_LWJGL[InputConstants.KEY_1] = Keyboard.KEY_1;
      MODERN_TO_LWJGL[InputConstants.KEY_2] = Keyboard.KEY_2;
      MODERN_TO_LWJGL[InputConstants.KEY_3] = Keyboard.KEY_3;
      MODERN_TO_LWJGL[InputConstants.KEY_4] = Keyboard.KEY_4;
      MODERN_TO_LWJGL[InputConstants.KEY_5] = Keyboard.KEY_5;
      MODERN_TO_LWJGL[InputConstants.KEY_6] = Keyboard.KEY_6;
      MODERN_TO_LWJGL[InputConstants.KEY_7] = Keyboard.KEY_7;
      MODERN_TO_LWJGL[InputConstants.KEY_8] = Keyboard.KEY_8;
      MODERN_TO_LWJGL[InputConstants.KEY_9] = Keyboard.KEY_9;
      MODERN_TO_LWJGL[InputConstants.KEY_SEMICOLON] = Keyboard.KEY_SEMICOLON;
      MODERN_TO_LWJGL[InputConstants.KEY_EQUALS] = Keyboard.KEY_EQUALS;
      MODERN_TO_LWJGL[InputConstants.KEY_A] = Keyboard.KEY_A;
      MODERN_TO_LWJGL[InputConstants.KEY_B] = Keyboard.KEY_B;
      MODERN_TO_LWJGL[InputConstants.KEY_C] = Keyboard.KEY_C;
      MODERN_TO_LWJGL[InputConstants.KEY_D] = Keyboard.KEY_D;
      MODERN_TO_LWJGL[InputConstants.KEY_E] = Keyboard.KEY_E;
      MODERN_TO_LWJGL[InputConstants.KEY_F] = Keyboard.KEY_F;
      MODERN_TO_LWJGL[InputConstants.KEY_G] = Keyboard.KEY_G;
      MODERN_TO_LWJGL[InputConstants.KEY_H] = Keyboard.KEY_H;
      MODERN_TO_LWJGL[InputConstants.KEY_I] = Keyboard.KEY_I;
      MODERN_TO_LWJGL[InputConstants.KEY_J] = Keyboard.KEY_J;
      MODERN_TO_LWJGL[InputConstants.KEY_K] = Keyboard.KEY_K;
      MODERN_TO_LWJGL[InputConstants.KEY_L] = Keyboard.KEY_L;
      MODERN_TO_LWJGL[InputConstants.KEY_M] = Keyboard.KEY_M;
      MODERN_TO_LWJGL[InputConstants.KEY_N] = Keyboard.KEY_N;
      MODERN_TO_LWJGL[InputConstants.KEY_O] = Keyboard.KEY_O;
      MODERN_TO_LWJGL[InputConstants.KEY_P] = Keyboard.KEY_P;
      MODERN_TO_LWJGL[InputConstants.KEY_Q] = Keyboard.KEY_Q;
      MODERN_TO_LWJGL[InputConstants.KEY_R] = Keyboard.KEY_R;
      MODERN_TO_LWJGL[InputConstants.KEY_S] = Keyboard.KEY_S;
      MODERN_TO_LWJGL[InputConstants.KEY_T] = Keyboard.KEY_T;
      MODERN_TO_LWJGL[InputConstants.KEY_U] = Keyboard.KEY_U;
      MODERN_TO_LWJGL[InputConstants.KEY_V] = Keyboard.KEY_V;
      MODERN_TO_LWJGL[InputConstants.KEY_W] = Keyboard.KEY_W;
      MODERN_TO_LWJGL[InputConstants.KEY_X] = Keyboard.KEY_X;
      MODERN_TO_LWJGL[InputConstants.KEY_Y] = Keyboard.KEY_Y;
      MODERN_TO_LWJGL[InputConstants.KEY_Z] = Keyboard.KEY_Z;
      MODERN_TO_LWJGL[InputConstants.KEY_LBRACKET] = Keyboard.KEY_LBRACKET;
      MODERN_TO_LWJGL[InputConstants.KEY_BACKSLASH] = Keyboard.KEY_BACKSLASH;
      MODERN_TO_LWJGL[InputConstants.KEY_RBRACKET] = Keyboard.KEY_RBRACKET;
      MODERN_TO_LWJGL[InputConstants.KEY_GRAVE] = Keyboard.KEY_GRAVE;
      MODERN_TO_LWJGL[InputConstants.KEY_WORLD_1] = Keyboard.KEY_WORLD_1;
      MODERN_TO_LWJGL[InputConstants.KEY_WORLD_2] = Keyboard.KEY_WORLD_2;
      MODERN_TO_LWJGL[InputConstants.KEY_ESCAPE] = Keyboard.KEY_ESCAPE;
      MODERN_TO_LWJGL[InputConstants.KEY_RETURN] = Keyboard.KEY_RETURN;
      MODERN_TO_LWJGL[InputConstants.KEY_TAB] = Keyboard.KEY_TAB;
      MODERN_TO_LWJGL[InputConstants.KEY_BACKSPACE] = Keyboard.KEY_BACK;
      MODERN_TO_LWJGL[InputConstants.KEY_INSERT] = Keyboard.KEY_INSERT;
      MODERN_TO_LWJGL[InputConstants.KEY_DELETE] = Keyboard.KEY_DELETE;
      MODERN_TO_LWJGL[InputConstants.KEY_RIGHT] = Keyboard.KEY_RIGHT;
      MODERN_TO_LWJGL[InputConstants.KEY_LEFT] = Keyboard.KEY_LEFT;
      MODERN_TO_LWJGL[InputConstants.KEY_DOWN] = Keyboard.KEY_DOWN;
      MODERN_TO_LWJGL[InputConstants.KEY_UP] = Keyboard.KEY_UP;
      MODERN_TO_LWJGL[InputConstants.KEY_PAGEUP] = Keyboard.KEY_PRIOR;
      MODERN_TO_LWJGL[InputConstants.KEY_PAGEDOWN] = Keyboard.KEY_NEXT;
      MODERN_TO_LWJGL[InputConstants.KEY_HOME] = Keyboard.KEY_HOME;
      MODERN_TO_LWJGL[InputConstants.KEY_END] = Keyboard.KEY_END;
      MODERN_TO_LWJGL[InputConstants.KEY_CAPSLOCK] = Keyboard.KEY_CAPITAL;
      MODERN_TO_LWJGL[InputConstants.KEY_SCROLLLOCK] = Keyboard.KEY_SCROLL;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMLOCK] = Keyboard.KEY_NUMLOCK;
      MODERN_TO_LWJGL[InputConstants.KEY_PRINTSCREEN] = Keyboard.KEY_PRINT_SCREEN;
      MODERN_TO_LWJGL[InputConstants.KEY_PAUSE] = Keyboard.KEY_PAUSE;
      MODERN_TO_LWJGL[InputConstants.KEY_F1] = Keyboard.KEY_F1;
      MODERN_TO_LWJGL[InputConstants.KEY_F2] = Keyboard.KEY_F2;
      MODERN_TO_LWJGL[InputConstants.KEY_F3] = Keyboard.KEY_F3;
      MODERN_TO_LWJGL[InputConstants.KEY_F4] = Keyboard.KEY_F4;
      MODERN_TO_LWJGL[InputConstants.KEY_F5] = Keyboard.KEY_F5;
      MODERN_TO_LWJGL[InputConstants.KEY_F6] = Keyboard.KEY_F6;
      MODERN_TO_LWJGL[InputConstants.KEY_F7] = Keyboard.KEY_F7;
      MODERN_TO_LWJGL[InputConstants.KEY_F8] = Keyboard.KEY_F8;
      MODERN_TO_LWJGL[InputConstants.KEY_F9] = Keyboard.KEY_F9;
      MODERN_TO_LWJGL[InputConstants.KEY_F10] = Keyboard.KEY_F10;
      MODERN_TO_LWJGL[InputConstants.KEY_F11] = Keyboard.KEY_F11;
      MODERN_TO_LWJGL[InputConstants.KEY_F12] = Keyboard.KEY_F12;
      MODERN_TO_LWJGL[InputConstants.KEY_F13] = Keyboard.KEY_F13;
      MODERN_TO_LWJGL[InputConstants.KEY_F14] = Keyboard.KEY_F14;
      MODERN_TO_LWJGL[InputConstants.KEY_F15] = Keyboard.KEY_F15;
      MODERN_TO_LWJGL[InputConstants.KEY_F16] = Keyboard.KEY_F16;
      MODERN_TO_LWJGL[InputConstants.KEY_F17] = Keyboard.KEY_F17;
      MODERN_TO_LWJGL[InputConstants.KEY_F18] = Keyboard.KEY_F18;
      MODERN_TO_LWJGL[InputConstants.KEY_F19] = Keyboard.KEY_F19;
      MODERN_TO_LWJGL[InputConstants.KEY_F20] = Keyboard.KEY_F20;
      MODERN_TO_LWJGL[InputConstants.KEY_F21] = Keyboard.KEY_F21;
      MODERN_TO_LWJGL[InputConstants.KEY_F22] = Keyboard.KEY_F22;
      MODERN_TO_LWJGL[InputConstants.KEY_F23] = Keyboard.KEY_F23;
      MODERN_TO_LWJGL[InputConstants.KEY_F24] = Keyboard.KEY_F24;
      MODERN_TO_LWJGL[InputConstants.KEY_F25] = Keyboard.KEY_F25;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD0] = Keyboard.KEY_NUMPAD0;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD1] = Keyboard.KEY_NUMPAD1;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD2] = Keyboard.KEY_NUMPAD2;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD3] = Keyboard.KEY_NUMPAD3;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD4] = Keyboard.KEY_NUMPAD4;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD5] = Keyboard.KEY_NUMPAD5;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD6] = Keyboard.KEY_NUMPAD6;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD7] = Keyboard.KEY_NUMPAD7;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD8] = Keyboard.KEY_NUMPAD8;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPAD9] = Keyboard.KEY_NUMPAD9;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPADCOMMA] = Keyboard.KEY_DECIMAL;
      MODERN_TO_LWJGL[InputConstants.KEY_DIVIDE] = Keyboard.KEY_DIVIDE;
      MODERN_TO_LWJGL[InputConstants.KEY_MULTIPLY] = Keyboard.KEY_MULTIPLY;
      MODERN_TO_LWJGL[InputConstants.KEY_SUBTRACT] = Keyboard.KEY_SUBTRACT;
      MODERN_TO_LWJGL[InputConstants.KEY_ADD] = Keyboard.KEY_ADD;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPADENTER] = Keyboard.KEY_NUMPADENTER;
      MODERN_TO_LWJGL[InputConstants.KEY_NUMPADEQUALS] = Keyboard.KEY_NUMPADEQUALS;
      MODERN_TO_LWJGL[InputConstants.KEY_LSHIFT] = Keyboard.KEY_LSHIFT;
      MODERN_TO_LWJGL[InputConstants.KEY_LCONTROL] = Keyboard.KEY_LCONTROL;
      MODERN_TO_LWJGL[InputConstants.KEY_LALT] = Keyboard.KEY_LMENU;
      MODERN_TO_LWJGL[InputConstants.KEY_LSUPER] = Keyboard.KEY_LMETA;
      MODERN_TO_LWJGL[InputConstants.KEY_RSHIFT] = Keyboard.KEY_RSHIFT;
      MODERN_TO_LWJGL[InputConstants.KEY_RCONTROL] = Keyboard.KEY_RCONTROL;
      MODERN_TO_LWJGL[InputConstants.KEY_RALT] = Keyboard.KEY_RMENU;
      MODERN_TO_LWJGL[InputConstants.KEY_RSUPER] = Keyboard.KEY_RMETA;
      MODERN_TO_LWJGL[InputConstants.KEY_MENU] = Keyboard.KEY_APPS;
    }

    static {
        for (int modern = 1; modern < MODERN_TO_LWJGL.length; modern++) {
            int legacy = MODERN_TO_LWJGL[modern];
            // Some modern keys alias the same LWJGL2 code, so only keep the first
            if (legacy > 0 && LWJGL_TO_MODERN[legacy] == 0) {
                LWJGL_TO_MODERN[legacy] = modern;
            }
        }
    }
}
*///?}
