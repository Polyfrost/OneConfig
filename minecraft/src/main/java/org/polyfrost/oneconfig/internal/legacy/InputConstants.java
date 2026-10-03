package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiFunction;

import net.minecraft.client.resources.language.I18n;
import org.lwjgl.sdl.SDLKeyboard;
import net.minecraft.network.chat.Component;

public class InputConstants {
    public static final int KEY_0 = 39;
    public static final int KEY_1 = 30;
    public static final int KEY_2 = 31;
    public static final int KEY_3 = 32;
    public static final int KEY_4 = 33;
    public static final int KEY_5 = 34;
    public static final int KEY_6 = 35;
    public static final int KEY_7 = 36;
    public static final int KEY_8 = 37;
    public static final int KEY_9 = 38;
    public static final int KEY_A = 4;
    public static final int KEY_B = 5;
    public static final int KEY_C = 6;
    public static final int KEY_D = 7;
    public static final int KEY_E = 8;
    public static final int KEY_F = 9;
    public static final int KEY_G = 10;
    public static final int KEY_H = 11;
    public static final int KEY_I = 12;
    public static final int KEY_J = 13;
    public static final int KEY_K = 14;
    public static final int KEY_L = 15;
    public static final int KEY_M = 16;
    public static final int KEY_N = 17;
    public static final int KEY_O = 18;
    public static final int KEY_P = 19;
    public static final int KEY_Q = 20;
    public static final int KEY_R = 21;
    public static final int KEY_S = 22;
    public static final int KEY_T = 23;
    public static final int KEY_U = 24;
    public static final int KEY_V = 25;
    public static final int KEY_W = 26;
    public static final int KEY_X = 27;
    public static final int KEY_Y = 28;
    public static final int KEY_Z = 29;
    public static final int KEY_F1 = 58;
    public static final int KEY_F2 = 59;
    public static final int KEY_F3 = 60;
    public static final int KEY_F4 = 61;
    public static final int KEY_F5 = 62;
    public static final int KEY_F6 = 63;
    public static final int KEY_F7 = 64;
    public static final int KEY_F8 = 65;
    public static final int KEY_F9 = 66;
    public static final int KEY_F10 = 67;
    public static final int KEY_F11 = 68;
    public static final int KEY_F12 = 69;
    public static final int KEY_F13 = 104;
    public static final int KEY_F14 = 105;
    public static final int KEY_F15 = 106;
    public static final int KEY_F16 = 107;
    public static final int KEY_F17 = 108;
    public static final int KEY_F18 = 109;
    public static final int KEY_F19 = 110;
    public static final int KEY_F20 = 111;
    public static final int KEY_F21 = 112;
    public static final int KEY_F22 = 113;
    public static final int KEY_F23 = 114;
    public static final int KEY_F24 = 115;
    public static final int KEY_NUMLOCK = 83;
    public static final int KEY_NUMPAD0 = 98;
    public static final int KEY_NUMPAD1 = 89;
    public static final int KEY_NUMPAD2 = 90;
    public static final int KEY_NUMPAD3 = 91;
    public static final int KEY_NUMPAD4 = 92;
    public static final int KEY_NUMPAD5 = 93;
    public static final int KEY_NUMPAD6 = 94;
    public static final int KEY_NUMPAD7 = 95;
    public static final int KEY_NUMPAD8 = 96;
    public static final int KEY_NUMPAD9 = 97;
    public static final int KEY_NUMPADCOMMA = 220;
    public static final int KEY_NUMPADENTER = 88;
    public static final int KEY_NUMPADEQUALS = 103;
    public static final int KEY_DOWN = 81;
    public static final int KEY_LEFT = 80;
    public static final int KEY_RIGHT = 79;
    public static final int KEY_UP = 82;
    public static final int KEY_ADD = 87;
    public static final int KEY_APOSTROPHE = 52;
    public static final int KEY_BACKSLASH = 49;
    public static final int KEY_COMMA = 54;
    public static final int KEY_EQUALS = 46;
    public static final int KEY_GRAVE = 53;
    public static final int KEY_LBRACKET = 47;
    public static final int KEY_MINUS = 45;
    public static final int KEY_MULTIPLY = 85;
    public static final int KEY_PERIOD = 55;
    public static final int KEY_RBRACKET = 48;
    public static final int KEY_SEMICOLON = 51;
    public static final int KEY_SLASH = 56;
    public static final int KEY_SPACE = 44;
    public static final int KEY_TAB = 43;
    public static final int KEY_LALT = 226;
    public static final int KEY_LCONTROL = 224;
    public static final int KEY_LSHIFT = 225;
    public static final int KEY_LGUI = 227;
    public static final int KEY_RALT = 230;
    public static final int KEY_RCONTROL = 228;
    public static final int KEY_RSHIFT = 229;
    public static final int KEY_RGUI = 231;
    public static final int KEY_RETURN = 40;
    public static final int KEY_ESCAPE = 41;
    public static final int KEY_BACKSPACE = 42;
    public static final int KEY_DELETE = 76;
    public static final int KEY_END = 77;
    public static final int KEY_HOME = 74;
    public static final int KEY_INSERT = 73;
    public static final int KEY_PAGEDOWN = 78;
    public static final int KEY_PAGEUP = 75;
    public static final int KEY_CAPSLOCK = 57;
    public static final int KEY_PAUSE = 72;
    public static final int KEY_SCROLLLOCK = 71;
    public static final int KEY_PRINTSCREEN = 70;
    public static final int PRESS = 1;
    public static final int RELEASE = 0;
    public static final int REPEAT = -1;
    public static final int MOUSE_BUTTON_LEFT = 1;
    public static final int MOUSE_BUTTON_MIDDLE = 2;
    public static final int MOUSE_BUTTON_RIGHT = 3;
    public static final int MOUSE_BUTTON_4 = 4;
    public static final int MOUSE_BUTTON_5 = 5;
    public static final int MOUSE_BUTTON_6 = 6;
    public static final int MOUSE_BUTTON_7 = 7;
    public static final int MOUSE_BUTTON_8 = 8;
    public static final int MOD_SHIFT = 3;
    public static final int MOD_CONTROL = 192;
    public static final int MOD_ALT = 768;
    public static final int MOD_SUPER = 3072;
    public static final int MOD_CAPS_LOCK = 8192;
    public static final int MOD_NUM_LOCK = 4096;
    public static final int KEYCODE_A = 97;
    public static final int KEYCODE_B = 98;
    public static final int KEYCODE_C = 99;
    public static final int KEYCODE_E = 101;
    public static final int KEYCODE_F = 102;
    public static final int KEYCODE_L = 108;
    public static final int KEYCODE_M = 109;
    public static final int KEYCODE_O = 111;
    public static final int KEYCODE_R = 114;
    public static final int KEYCODE_U = 117;
    public static final int KEYCODE_V = 118;
    public static final int KEYCODE_W = 119;
    public static final int KEYCODE_X = 120;
    public static final int KEYCODE_Y = 121;
    public static final int KEYCODE_Z = 122;
    public static final int KEYCODE_RETURN = 13;
    public static final int KEYCODE_NUMPADENTER = 1073741912;
    public static final int KEYCODE_PAGEUP = 1073741899;
    public static final int KEYCODE_PAGEDOWN = 1073741902;
    public static final int KEYCODE_BACKSPACE = 8;
    public static final int KEYCODE_UP = 1073741906;
    public static final int KEYCODE_DOWN = 1073741905;
    public static final int KEYCODE_FORWARD = 1073741906;
    public static final int KEYCODE_BACKWARD = 1073741905;
    public static final int KEYCODE_LEFT = 1073741904;
    public static final int KEYCODE_RIGHT = 1073741903;
    public static final int KEYCODE_NUMPAD9 = 1073741921;
    public static final int KEYCODE_NUMPAD3 = 1073741915;
    public static final int KEYCODE_DELETE = 127;
    public static final int KEYCODE_HOME = 1073741898;
    public static final int KEYCODE_END = 1073741901;
    public static final int KEYCODE_F5 = 1073741886;
    public static final int KEYCODE_TAB = 9;
    public static final int KEYCODE_LCONTROL = 1073742048;
    public static final int KEYCODE_RCONTROL = 1073742052;
    public static final int KEYCODE_SPACE = 32;
    public static final InputConstants.Key UNKNOWN = InputConstants.Type.KEYBOARD.getOrCreate(0);

    public static InputConstants.Key getKey(final String name) {
        if (InputConstants.Key.NAME_MAP.containsKey(name)) {
            return (InputConstants.Key) InputConstants.Key.NAME_MAP.get(name);
        }

        for (InputConstants.Type type : InputConstants.Type.values()) {
            if (name.startsWith(type.defaultPrefix)) {
                String humanReadableValue = name.substring(type.defaultPrefix.length() + 1);
                int intValue = Integer.parseInt(humanReadableValue);
                return type.getOrCreate(intValue);
            }
        }

        throw new IllegalArgumentException("Unknown key name: " + name);
    }

    public static boolean isKeyDown(final int key) {
        ByteBuffer keyboardState = SDLKeyboard.SDL_GetKeyboardState();
        return keyboardState != null && keyboardState.get(key) != 0;
    }

    public static final class Key {
        private final String name;
        private final InputConstants.Type type;
        private final int value;
        private static final Map<String, InputConstants.Key> NAME_MAP = Maps.newHashMap();

        private Key(final String name, final InputConstants.Type type, final int value) {
            this.name = name;
            this.type = type;
            this.value = value;
            NAME_MAP.put(name, this);
        }

        public InputConstants.Type getType() {
            return this.type;
        }

        public int getValue() {
            return this.value;
        }

        public String getName() {
            return this.name;
        }

        public Component getDisplayName() {
            return (Component) this.type.displayTextSupplier.apply(this.value, this.name);
        }

        public OptionalInt getNumericKeyValue() {
            return switch (this.value) {
                case 30, 89 -> OptionalInt.of(1);
                case 31, 90 -> OptionalInt.of(2);
                case 32, 91 -> OptionalInt.of(3);
                case 33, 92 -> OptionalInt.of(4);
                case 34, 93 -> OptionalInt.of(5);
                case 35, 94 -> OptionalInt.of(6);
                case 36, 95 -> OptionalInt.of(7);
                case 37, 96 -> OptionalInt.of(8);
                case 38, 97 -> OptionalInt.of(9);
                case 39, 98 -> OptionalInt.of(0);
                default -> OptionalInt.empty();
            };
        }

        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                InputConstants.Key key = (InputConstants.Key) o;
                return this.value == key.value && this.type == key.type;
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.type, this.value});
        }

        public String toString() {
            return this.name;
        }
    }

    public enum Type {
        KEYBOARD(
                "key.keyboard",
                (value, name) -> {
                    if ("key.keyboard.unknown".equals(name)) {
                        return Component.translatable(name);
                    }

                    int keycode = SDLKeyboard.SDL_GetKeyFromScancode(value, (short) 0, false);
                    String systemName = SDLKeyboard.SDL_GetKeyName(keycode);
                    if (systemName != null && systemName.codePointCount(0, systemName.length()) == 1) {
                        return Component.literal(systemName.toUpperCase(Locale.ROOT));
                    }

                    String translationKey = InputQuirks.keyboardTranslationKey(name);
                    String fallback = SDLKeyboard.SDL_GetScancodeName(value);
                    return fallback != null
                            ? Component.translatableWithFallback(translationKey, fallback)
                            : Component.translatable(translationKey);
                }
        ),
        MOUSE("key.mouse", (value, name) -> L10n.has(name) ? Component.translatable(name) : Component.translatable("key.mouse", value));

        private static final String KEY_KEYBOARD_UNKNOWN = "key.keyboard.unknown";
        private final Int2ObjectMap<InputConstants.Key> map = new Int2ObjectOpenHashMap();
        private final String defaultPrefix;
        private final BiFunction<Integer, String, Component> displayTextSupplier;

        private static void addKey(final InputConstants.Type type, final String name, final int value) {
            InputConstants.Key key = new InputConstants.Key(name, type, value);
            type.map.put(value, key);
        }

        Type(final String defaultPrefix, final BiFunction<Integer, String, Component> displayTextSupplier) {
            this.defaultPrefix = defaultPrefix;
            this.displayTextSupplier = displayTextSupplier;
        }

        public InputConstants.Key getOrCreate(final int value) {
            return (InputConstants.Key) this.map.computeIfAbsent(value, intValue -> {
                String name = this.defaultPrefix + "." + intValue;
                return new InputConstants.Key(name, this, intValue);
            });
        }

        static {
            addKey(KEYBOARD, "key.keyboard.unknown", 0);
            addKey(MOUSE, "key.mouse.left", 1);
            addKey(MOUSE, "key.mouse.right", 3);
            addKey(MOUSE, "key.mouse.middle", 2);
            addKey(MOUSE, "key.mouse.4", 4);
            addKey(MOUSE, "key.mouse.5", 5);
            addKey(MOUSE, "key.mouse.6", 6);
            addKey(MOUSE, "key.mouse.7", 7);
            addKey(MOUSE, "key.mouse.8", 8);
            addKey(KEYBOARD, "key.keyboard.a", 4);
            addKey(KEYBOARD, "key.keyboard.b", 5);
            addKey(KEYBOARD, "key.keyboard.c", 6);
            addKey(KEYBOARD, "key.keyboard.d", 7);
            addKey(KEYBOARD, "key.keyboard.e", 8);
            addKey(KEYBOARD, "key.keyboard.f", 9);
            addKey(KEYBOARD, "key.keyboard.g", 10);
            addKey(KEYBOARD, "key.keyboard.h", 11);
            addKey(KEYBOARD, "key.keyboard.i", 12);
            addKey(KEYBOARD, "key.keyboard.j", 13);
            addKey(KEYBOARD, "key.keyboard.k", 14);
            addKey(KEYBOARD, "key.keyboard.l", 15);
            addKey(KEYBOARD, "key.keyboard.m", 16);
            addKey(KEYBOARD, "key.keyboard.n", 17);
            addKey(KEYBOARD, "key.keyboard.o", 18);
            addKey(KEYBOARD, "key.keyboard.p", 19);
            addKey(KEYBOARD, "key.keyboard.q", 20);
            addKey(KEYBOARD, "key.keyboard.r", 21);
            addKey(KEYBOARD, "key.keyboard.s", 22);
            addKey(KEYBOARD, "key.keyboard.t", 23);
            addKey(KEYBOARD, "key.keyboard.u", 24);
            addKey(KEYBOARD, "key.keyboard.v", 25);
            addKey(KEYBOARD, "key.keyboard.w", 26);
            addKey(KEYBOARD, "key.keyboard.x", 27);
            addKey(KEYBOARD, "key.keyboard.y", 28);
            addKey(KEYBOARD, "key.keyboard.z", 29);
            addKey(KEYBOARD, "key.keyboard.1", 30);
            addKey(KEYBOARD, "key.keyboard.2", 31);
            addKey(KEYBOARD, "key.keyboard.3", 32);
            addKey(KEYBOARD, "key.keyboard.4", 33);
            addKey(KEYBOARD, "key.keyboard.5", 34);
            addKey(KEYBOARD, "key.keyboard.6", 35);
            addKey(KEYBOARD, "key.keyboard.7", 36);
            addKey(KEYBOARD, "key.keyboard.8", 37);
            addKey(KEYBOARD, "key.keyboard.9", 38);
            addKey(KEYBOARD, "key.keyboard.0", 39);
            addKey(KEYBOARD, "key.keyboard.enter", 40);
            addKey(KEYBOARD, "key.keyboard.escape", 41);
            addKey(KEYBOARD, "key.keyboard.backspace", 42);
            addKey(KEYBOARD, "key.keyboard.tab", 43);
            addKey(KEYBOARD, "key.keyboard.space", 44);
            addKey(KEYBOARD, "key.keyboard.minus", 45);
            addKey(KEYBOARD, "key.keyboard.equal", 46);
            addKey(KEYBOARD, "key.keyboard.left.bracket", 47);
            addKey(KEYBOARD, "key.keyboard.right.bracket", 48);
            addKey(KEYBOARD, "key.keyboard.backslash", 49);
            addKey(KEYBOARD, "key.keyboard.world.2", 50);
            addKey(KEYBOARD, "key.keyboard.semicolon", 51);
            addKey(KEYBOARD, "key.keyboard.apostrophe", 52);
            addKey(KEYBOARD, "key.keyboard.grave.accent", 53);
            addKey(KEYBOARD, "key.keyboard.comma", 54);
            addKey(KEYBOARD, "key.keyboard.period", 55);
            addKey(KEYBOARD, "key.keyboard.slash", 56);
            addKey(KEYBOARD, "key.keyboard.caps.lock", 57);
            addKey(KEYBOARD, "key.keyboard.f1", 58);
            addKey(KEYBOARD, "key.keyboard.f2", 59);
            addKey(KEYBOARD, "key.keyboard.f3", 60);
            addKey(KEYBOARD, "key.keyboard.f4", 61);
            addKey(KEYBOARD, "key.keyboard.f5", 62);
            addKey(KEYBOARD, "key.keyboard.f6", 63);
            addKey(KEYBOARD, "key.keyboard.f7", 64);
            addKey(KEYBOARD, "key.keyboard.f8", 65);
            addKey(KEYBOARD, "key.keyboard.f9", 66);
            addKey(KEYBOARD, "key.keyboard.f10", 67);
            addKey(KEYBOARD, "key.keyboard.f11", 68);
            addKey(KEYBOARD, "key.keyboard.f12", 69);
            addKey(KEYBOARD, "key.keyboard.print.screen", 70);
            addKey(KEYBOARD, "key.keyboard.scroll.lock", 71);
            addKey(KEYBOARD, "key.keyboard.pause", 72);
            addKey(KEYBOARD, "key.keyboard.insert", 73);
            addKey(KEYBOARD, "key.keyboard.home", 74);
            addKey(KEYBOARD, "key.keyboard.page.up", 75);
            addKey(KEYBOARD, "key.keyboard.delete", 76);
            addKey(KEYBOARD, "key.keyboard.end", 77);
            addKey(KEYBOARD, "key.keyboard.page.down", 78);
            addKey(KEYBOARD, "key.keyboard.right", 79);
            addKey(KEYBOARD, "key.keyboard.left", 80);
            addKey(KEYBOARD, "key.keyboard.down", 81);
            addKey(KEYBOARD, "key.keyboard.up", 82);
            addKey(KEYBOARD, "key.keyboard.num.lock", 83);
            addKey(KEYBOARD, "key.keyboard.keypad.divide", 84);
            addKey(KEYBOARD, "key.keyboard.keypad.multiply", 85);
            addKey(KEYBOARD, "key.keyboard.keypad.subtract", 86);
            addKey(KEYBOARD, "key.keyboard.keypad.add", 87);
            addKey(KEYBOARD, "key.keyboard.keypad.enter", 88);
            addKey(KEYBOARD, "key.keyboard.keypad.1", 89);
            addKey(KEYBOARD, "key.keyboard.keypad.2", 90);
            addKey(KEYBOARD, "key.keyboard.keypad.3", 91);
            addKey(KEYBOARD, "key.keyboard.keypad.4", 92);
            addKey(KEYBOARD, "key.keyboard.keypad.5", 93);
            addKey(KEYBOARD, "key.keyboard.keypad.6", 94);
            addKey(KEYBOARD, "key.keyboard.keypad.7", 95);
            addKey(KEYBOARD, "key.keyboard.keypad.8", 96);
            addKey(KEYBOARD, "key.keyboard.keypad.9", 97);
            addKey(KEYBOARD, "key.keyboard.keypad.0", 98);
            addKey(KEYBOARD, "key.keyboard.keypad.period", 99);
            addKey(KEYBOARD, "key.keyboard.world.1", 100);
            addKey(KEYBOARD, "key.keyboard.application", 101);
            addKey(KEYBOARD, "key.keyboard.power", 102);
            addKey(KEYBOARD, "key.keyboard.keypad.equal", 103);
            addKey(KEYBOARD, "key.keyboard.f13", 104);
            addKey(KEYBOARD, "key.keyboard.f14", 105);
            addKey(KEYBOARD, "key.keyboard.f15", 106);
            addKey(KEYBOARD, "key.keyboard.f16", 107);
            addKey(KEYBOARD, "key.keyboard.f17", 108);
            addKey(KEYBOARD, "key.keyboard.f18", 109);
            addKey(KEYBOARD, "key.keyboard.f19", 110);
            addKey(KEYBOARD, "key.keyboard.f20", 111);
            addKey(KEYBOARD, "key.keyboard.f21", 112);
            addKey(KEYBOARD, "key.keyboard.f22", 113);
            addKey(KEYBOARD, "key.keyboard.f23", 114);
            addKey(KEYBOARD, "key.keyboard.f24", 115);
            addKey(KEYBOARD, "key.keyboard.execute", 116);
            addKey(KEYBOARD, "key.keyboard.help", 117);
            addKey(KEYBOARD, "key.keyboard.menu", 118);
            addKey(KEYBOARD, "key.keyboard.select", 119);
            addKey(KEYBOARD, "key.keyboard.stop", 120);
            addKey(KEYBOARD, "key.keyboard.again", 121);
            addKey(KEYBOARD, "key.keyboard.undo", 122);
            addKey(KEYBOARD, "key.keyboard.cut", 123);
            addKey(KEYBOARD, "key.keyboard.copy", 124);
            addKey(KEYBOARD, "key.keyboard.paste", 125);
            addKey(KEYBOARD, "key.keyboard.find", 126);
            addKey(KEYBOARD, "key.keyboard.mute", 127);
            addKey(KEYBOARD, "key.keyboard.volume.up", 128);
            addKey(KEYBOARD, "key.keyboard.volume.down", 129);
            addKey(KEYBOARD, "key.keyboard.keypad.comma", 133);
            addKey(KEYBOARD, "key.keyboard.keypad.equals.as400", 134);
            addKey(KEYBOARD, "key.keyboard.international1", 135);
            addKey(KEYBOARD, "key.keyboard.international2", 136);
            addKey(KEYBOARD, "key.keyboard.international3", 137);
            addKey(KEYBOARD, "key.keyboard.international4", 138);
            addKey(KEYBOARD, "key.keyboard.international5", 139);
            addKey(KEYBOARD, "key.keyboard.international6", 140);
            addKey(KEYBOARD, "key.keyboard.international7", 141);
            addKey(KEYBOARD, "key.keyboard.international8", 142);
            addKey(KEYBOARD, "key.keyboard.international9", 143);
            addKey(KEYBOARD, "key.keyboard.lang1", 144);
            addKey(KEYBOARD, "key.keyboard.lang2", 145);
            addKey(KEYBOARD, "key.keyboard.lang3", 146);
            addKey(KEYBOARD, "key.keyboard.lang4", 147);
            addKey(KEYBOARD, "key.keyboard.lang5", 148);
            addKey(KEYBOARD, "key.keyboard.lang6", 149);
            addKey(KEYBOARD, "key.keyboard.lang7", 150);
            addKey(KEYBOARD, "key.keyboard.lang8", 151);
            addKey(KEYBOARD, "key.keyboard.lang9", 152);
            addKey(KEYBOARD, "key.keyboard.alternate.erase", 153);
            addKey(KEYBOARD, "key.keyboard.sys.req", 154);
            addKey(KEYBOARD, "key.keyboard.cancel", 155);
            addKey(KEYBOARD, "key.keyboard.clear", 156);
            addKey(KEYBOARD, "key.keyboard.prior", 157);
            addKey(KEYBOARD, "key.keyboard.enter2", 158);
            addKey(KEYBOARD, "key.keyboard.separator", 159);
            addKey(KEYBOARD, "key.keyboard.out", 160);
            addKey(KEYBOARD, "key.keyboard.oper", 161);
            addKey(KEYBOARD, "key.keyboard.clear.again", 162);
            addKey(KEYBOARD, "key.keyboard.crsel", 163);
            addKey(KEYBOARD, "key.keyboard.exsel", 164);
            addKey(KEYBOARD, "key.keyboard.keypad.00", 176);
            addKey(KEYBOARD, "key.keyboard.keypad.000", 177);
            addKey(KEYBOARD, "key.keyboard.thousands.separator", 178);
            addKey(KEYBOARD, "key.keyboard.decimal.separator", 179);
            addKey(KEYBOARD, "key.keyboard.currency.unit", 180);
            addKey(KEYBOARD, "key.keyboard.currency.subunit", 181);
            addKey(KEYBOARD, "key.keyboard.keypad.left.parenthesis", 182);
            addKey(KEYBOARD, "key.keyboard.keypad.right.parenthesis", 183);
            addKey(KEYBOARD, "key.keyboard.keypad.left.brace", 184);
            addKey(KEYBOARD, "key.keyboard.keypad.right.brace", 185);
            addKey(KEYBOARD, "key.keyboard.keypad.tab", 186);
            addKey(KEYBOARD, "key.keyboard.keypad.backspace", 187);
            addKey(KEYBOARD, "key.keyboard.keypad.a", 188);
            addKey(KEYBOARD, "key.keyboard.keypad.b", 189);
            addKey(KEYBOARD, "key.keyboard.keypad.c", 190);
            addKey(KEYBOARD, "key.keyboard.keypad.d", 191);
            addKey(KEYBOARD, "key.keyboard.keypad.e", 192);
            addKey(KEYBOARD, "key.keyboard.keypad.f", 193);
            addKey(KEYBOARD, "key.keyboard.keypad.xor", 194);
            addKey(KEYBOARD, "key.keyboard.keypad.power", 195);
            addKey(KEYBOARD, "key.keyboard.keypad.percent", 196);
            addKey(KEYBOARD, "key.keyboard.keypad.less", 197);
            addKey(KEYBOARD, "key.keyboard.keypad.greater", 198);
            addKey(KEYBOARD, "key.keyboard.keypad.ampersand", 199);
            addKey(KEYBOARD, "key.keyboard.keypad.double.ampersand", 200);
            addKey(KEYBOARD, "key.keyboard.keypad.vertical.bar", 201);
            addKey(KEYBOARD, "key.keyboard.keypad.double.vertical.bar", 202);
            addKey(KEYBOARD, "key.keyboard.keypad.colon", 203);
            addKey(KEYBOARD, "key.keyboard.keypad.hash", 204);
            addKey(KEYBOARD, "key.keyboard.keypad.space", 205);
            addKey(KEYBOARD, "key.keyboard.keypad.at", 206);
            addKey(KEYBOARD, "key.keyboard.keypad.exclamation", 207);
            addKey(KEYBOARD, "key.keyboard.keypad.memory.store", 208);
            addKey(KEYBOARD, "key.keyboard.keypad.memory.recall", 209);
            addKey(KEYBOARD, "key.keyboard.keypad.memory.clear", 210);
            addKey(KEYBOARD, "key.keyboard.keypad.memory.add", 211);
            addKey(KEYBOARD, "key.keyboard.keypad.memory.subtract", 212);
            addKey(KEYBOARD, "key.keyboard.keypad.memory.multiply", 213);
            addKey(KEYBOARD, "key.keyboard.keypad.memory.divide", 214);
            addKey(KEYBOARD, "key.keyboard.keypad.plus.minus", 215);
            addKey(KEYBOARD, "key.keyboard.keypad.clear", 216);
            addKey(KEYBOARD, "key.keyboard.keypad.clear.entry", 217);
            addKey(KEYBOARD, "key.keyboard.keypad.binary", 218);
            addKey(KEYBOARD, "key.keyboard.keypad.octal", 219);
            addKey(KEYBOARD, "key.keyboard.keypad.decimal", 220);
            addKey(KEYBOARD, "key.keyboard.keypad.hexadecimal", 221);
            addKey(KEYBOARD, "key.keyboard.left.control", 224);
            addKey(KEYBOARD, "key.keyboard.left.shift", 225);
            addKey(KEYBOARD, "key.keyboard.left.alt", 226);
            addKey(KEYBOARD, "key.keyboard.left.win", 227);
            addKey(KEYBOARD, "key.keyboard.right.control", 228);
            addKey(KEYBOARD, "key.keyboard.right.shift", 229);
            addKey(KEYBOARD, "key.keyboard.right.alt", 230);
            addKey(KEYBOARD, "key.keyboard.right.win", 231);
            addKey(KEYBOARD, "key.keyboard.mode", 257);
            addKey(KEYBOARD, "key.keyboard.sleep", 258);
            addKey(KEYBOARD, "key.keyboard.wake", 259);
            addKey(KEYBOARD, "key.keyboard.channel.up", 260);
            addKey(KEYBOARD, "key.keyboard.channel.down", 261);
            addKey(KEYBOARD, "key.keyboard.media.play", 262);
            addKey(KEYBOARD, "key.keyboard.media.pause", 263);
            addKey(KEYBOARD, "key.keyboard.media.record", 264);
            addKey(KEYBOARD, "key.keyboard.media.fast.forward", 265);
            addKey(KEYBOARD, "key.keyboard.media.rewind", 266);
            addKey(KEYBOARD, "key.keyboard.media.next.track", 267);
            addKey(KEYBOARD, "key.keyboard.media.previous.track", 268);
            addKey(KEYBOARD, "key.keyboard.media.stop", 269);
            addKey(KEYBOARD, "key.keyboard.media.eject", 270);
            addKey(KEYBOARD, "key.keyboard.media.play.pause", 271);
            addKey(KEYBOARD, "key.keyboard.media.select", 272);
            addKey(KEYBOARD, "key.keyboard.ac.new", 273);
            addKey(KEYBOARD, "key.keyboard.ac.open", 274);
            addKey(KEYBOARD, "key.keyboard.ac.close", 275);
            addKey(KEYBOARD, "key.keyboard.ac.exit", 276);
            addKey(KEYBOARD, "key.keyboard.ac.save", 277);
            addKey(KEYBOARD, "key.keyboard.ac.print", 278);
            addKey(KEYBOARD, "key.keyboard.ac.properties", 279);
            addKey(KEYBOARD, "key.keyboard.ac.search", 280);
            addKey(KEYBOARD, "key.keyboard.ac.home", 281);
            addKey(KEYBOARD, "key.keyboard.ac.back", 282);
            addKey(KEYBOARD, "key.keyboard.ac.forward", 283);
            addKey(KEYBOARD, "key.keyboard.ac.stop", 284);
            addKey(KEYBOARD, "key.keyboard.ac.refresh", 285);
            addKey(KEYBOARD, "key.keyboard.ac.bookmarks", 286);
            addKey(KEYBOARD, "key.keyboard.soft.left", 287);
            addKey(KEYBOARD, "key.keyboard.soft.right", 288);
            addKey(KEYBOARD, "key.keyboard.call", 289);
            addKey(KEYBOARD, "key.keyboard.end.call", 290);
        }
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE_USE)
    public @interface Value {
    }
}
*///?}
