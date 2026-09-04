package org.polyfrost.oneconfig.api.ui.v1.keybind.internal;

import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;

public final class MinecraftKeybindCodec implements KeybindCodec {
    //? if >= 26.3 || = 1.8.9
    /*private static final int MAX_KEY = Integer.MAX_VALUE; // SDL keycodes are sparse and unicode/scancode-masked*/
    //? if < 26.3 && > 1.8.9
    private static final int MAX_KEY = 348; // GLFW_KEY_LAST

    private static final int MIN_MOUSE = InputConstants.MOUSE_BUTTON_LEFT;
    private static final int MAX_MOUSE = InputConstants.MOUSE_BUTTON_LEFT + 7; // vanilla names eight mouse buttons

    public static InputConstants.Key keysym(int code) {
        //~ if >= 26.3 || = 1.8.9 'code >= 0 && code <= 9' -> 'code >= 1 && code <= 3'
        if (code >= 0 && code <= 9) return InputConstants.UNKNOWN;
        //~ if < 26.3 && > 1.8.9 'Type.KEYBOARD' -> 'Type.KEYSYM'
        return InputConstants.Type.KEYSYM.getOrCreate(code);
    }

    public static InputConstants.Key mouse(int button) {
        if (button < MIN_MOUSE || button > MAX_MOUSE) return InputConstants.UNKNOWN;
        return InputConstants.Type.MOUSE.getOrCreate(button);
    }

    @Override
    public @Nullable String keyName(int code) {
        if (code < 0 || code > MAX_KEY) return null;

        InputConstants.Key key = keysym(code);
        if (key == InputConstants.UNKNOWN) return null;

        // Ignore fallback names
        if (("key.keyboard." + code).equals(key.getName())) return null;

        //? if < 26.3 && > 1.8.9 {
        // GLFW to SDL aliases
        if ("key.keyboard.keypad.decimal".equals(key.getName())) return "key.keyboard.keypad.period";
        if ("key.keyboard.menu".equals(key.getName())) return "key.keyboard.application";
        //?}

        return key.getName();
    }

    @Override
    public @Nullable Integer keyCode(String name) {
        //? if < 26.3 && > 1.8.9 {
        // SDL to GLFW aliases
        if ("key.keyboard.keypad.period".equals(name)) name = "key.keyboard.keypad.decimal";
        if ("key.keyboard.application".equals(name)) name = "key.keyboard.menu";
        //?}

        InputConstants.Key key = lookup(name);
        if (key == null) return null;

        if (("key.keyboard." + key.getValue()).equals(name)) return null;

        //~ if < 26.3 && > 1.8.9 'Type.KEYBOARD' -> 'Type.KEYSYM'
        return key.getType() == InputConstants.Type.KEYSYM && key != InputConstants.UNKNOWN ? key.getValue() : null;
    }

    @Override
    public @Nullable String mouseName(int button) {
        InputConstants.Key key = mouse(button);
        return key == InputConstants.UNKNOWN ? null : key.getName();
    }

    @Override
    public @Nullable Integer mouseButton(String name) {
        InputConstants.Key key = lookup(name);
        if (key == null || key.getType() != InputConstants.Type.MOUSE) return null;

        int value = key.getValue();
        return value >= MIN_MOUSE && value <= MAX_MOUSE ? value : null;
    }

    private static @Nullable InputConstants.Key lookup(String name) {
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            try {
                int suffix = Integer.parseInt(name.substring(dot + 1));
                if (name.startsWith("key.mouse.")) {
                    if (suffix - 1 < MIN_MOUSE || suffix - 1 > MAX_MOUSE) return null;
                } else if (suffix < 0 || suffix > MAX_KEY) {
                    return null;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            return InputConstants.getKey(name);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Override
    public @Nullable String legacyKeyName(int code) {
        if (code >= 48 && code <= 57) return "key.keyboard." + (char) code;
        if (code >= 65 && code <= 90) return "key.keyboard." + (char) (code + 32);
        if (code >= 290 && code <= 314) return "key.keyboard.f" + (code - 289);
        if (code >= 320 && code <= 329) return "key.keyboard.keypad." + (code - 320);
        return switch (code) {
            case 32 -> "key.keyboard.space";
            case 39 -> "key.keyboard.apostrophe";
            case 44 -> "key.keyboard.comma";
            case 45 -> "key.keyboard.minus";
            case 46 -> "key.keyboard.period";
            case 47 -> "key.keyboard.slash";
            case 59 -> "key.keyboard.semicolon";
            case 61 -> "key.keyboard.equal";
            case 91 -> "key.keyboard.left.bracket";
            case 92 -> "key.keyboard.backslash";
            case 93 -> "key.keyboard.right.bracket";
            case 96 -> "key.keyboard.grave.accent";
            case 161 -> "key.keyboard.world.1";
            case 162 -> "key.keyboard.world.2";
            case 256 -> "key.keyboard.escape";
            case 257 -> "key.keyboard.enter";
            case 258 -> "key.keyboard.tab";
            case 259 -> "key.keyboard.backspace";
            case 260 -> "key.keyboard.insert";
            case 261 -> "key.keyboard.delete";
            case 262 -> "key.keyboard.right";
            case 263 -> "key.keyboard.left";
            case 264 -> "key.keyboard.down";
            case 265 -> "key.keyboard.up";
            case 266 -> "key.keyboard.page.up";
            case 267 -> "key.keyboard.page.down";
            case 268 -> "key.keyboard.home";
            case 269 -> "key.keyboard.end";
            case 280 -> "key.keyboard.caps.lock";
            case 281 -> "key.keyboard.scroll.lock";
            case 282 -> "key.keyboard.num.lock";
            case 283 -> "key.keyboard.print.screen";
            case 284 -> "key.keyboard.pause";
            case 330 -> "key.keyboard.keypad.period";
            case 331 -> "key.keyboard.keypad.divide";
            case 332 -> "key.keyboard.keypad.multiply";
            case 333 -> "key.keyboard.keypad.subtract";
            case 334 -> "key.keyboard.keypad.add";
            case 335 -> "key.keyboard.keypad.enter";
            case 336 -> "key.keyboard.keypad.equal";
            case 340 -> "key.keyboard.left.shift";
            case 341 -> "key.keyboard.left.control";
            case 342 -> "key.keyboard.left.alt";
            case 343 -> "key.keyboard.left.win";
            case 344 -> "key.keyboard.right.shift";
            case 345 -> "key.keyboard.right.control";
            case 346 -> "key.keyboard.right.alt";
            case 347 -> "key.keyboard.right.win";
            case 348 -> "key.keyboard.application";
            default -> null;
        };
    }

    @Override
    public @Nullable String legacyMouseName(int button) {
        return switch (button) {
            case 0 -> "key.mouse.left";
            case 1 -> "key.mouse.right";
            case 2 -> "key.mouse.middle";
            case 3, 4, 5, 6, 7 -> "key.mouse." + (button + 1);
            default -> null;
        };
    }
}
