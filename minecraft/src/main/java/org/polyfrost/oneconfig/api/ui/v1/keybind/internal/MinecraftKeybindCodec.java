package org.polyfrost.oneconfig.api.ui.v1.keybind.internal;

import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.Nullable;

//? if !sdl_keycodes {
import java.util.HashMap;
import java.util.Map;
//?}

/**
 * Translates between platform input codes and the stable {@code key.keyboard.*}/{@code key.mouse.*} names
 * used for serialization.
 * <p>
 * On GLFW versions this deliberately never calls {@link InputConstants.Type#getOrCreate(int)} or
 * {@link InputConstants#getKey(String)} with unvalidated inputs, because it could shadow real keys
 * like {@code key.keyboard.1}.
 */
public final class MinecraftKeybindCodec implements KeybindCodec {
    //? if sdl_keycodes {
    /*private static final int MAX_KEY = Integer.MAX_VALUE; // SDL keycodes are sparse and unicode/scancode-masked
    *///?} else
    private static final int MAX_KEY = 348; // GLFW_KEY_LAST

    private static final int MIN_MOUSE = InputConstants.MOUSE_BUTTON_LEFT;
    private static final int MAX_MOUSE = InputConstants.MOUSE_BUTTON_LEFT + 7;

    //? if !sdl_keycodes {
    private static final Map<String, Integer> KEY_CODES_BY_NAME = new HashMap<>();
    private static final Map<String, Integer> MOUSE_BUTTONS_BY_NAME = new HashMap<>();

    static {
        for (int code = 0; code <= MAX_KEY; code++) {
            String name = glfwKeyName(code);
            if (name != null) KEY_CODES_BY_NAME.put(name, code);
        }
        // GLFW aliases of the SDL names returned by glfwKeyName
        KEY_CODES_BY_NAME.put("key.keyboard.keypad.decimal", 330);
        KEY_CODES_BY_NAME.put("key.keyboard.menu", 348);
        for (int button = MIN_MOUSE; button <= MAX_MOUSE; button++) {
            MOUSE_BUTTONS_BY_NAME.put(legacyMouseNameOf(button), button);
        }
    }
    //?}

    /**
     * A KEYSYM key for the code, or {@link InputConstants#UNKNOWN}.
     */
    public static InputConstants.Key keysym(int code) {
        //? if sdl_keycodes {
        /*if (code >= 1 && code <= 3) return InputConstants.UNKNOWN;
        return InputConstants.Type.KEYBOARD.getOrCreate(code);
        *///?} else {
        if (glfwKeyName(code) == null) return InputConstants.UNKNOWN;
        // every named GLFW code is pre-registered by vanilla, so this never creates a key
        return InputConstants.Type.KEYSYM.getOrCreate(code);
        //?}
    }

    /**
     * A MOUSE key for the button, or {@link InputConstants#UNKNOWN}.
     */
    public static InputConstants.Key mouse(int button) {
        if (button < MIN_MOUSE || button > MAX_MOUSE) return InputConstants.UNKNOWN;
        return InputConstants.Type.MOUSE.getOrCreate(button);
    }

    @Override
    public @Nullable String keyName(int code) {
        //? if sdl_keycodes {
        /*if (code < 0 || code > MAX_KEY) return null;

        InputConstants.Key key = keysym(code);
        if (key == InputConstants.UNKNOWN) return null;

        // Ignore fallback names
        if (("key.keyboard." + code).equals(key.getName())) return null;

        return key.getName();
        *///?} else {
        return glfwKeyName(code);
        //?}
    }

    @Override
    public @Nullable Integer keyCode(String name) {
        //? if sdl_keycodes {
        /*InputConstants.Key key = lookup(name);
        if (key == null) return null;

        if (("key.keyboard." + key.getValue()).equals(name)) return null;

        return key.getType() == InputConstants.Type.KEYBOARD && key != InputConstants.UNKNOWN ? key.getValue() : null;
        *///?} else {
        return KEY_CODES_BY_NAME.get(name);
        //?}
    }

    @Override
    public @Nullable String mouseName(int button) {
        //? if sdl_keycodes {
        /*InputConstants.Key key = mouse(button);
        return key == InputConstants.UNKNOWN ? null : key.getName();
        *///?} else {
        return legacyMouseNameOf(button);
        //?}
    }

    @Override
    public @Nullable Integer mouseButton(String name) {
        //? if sdl_keycodes {
        /*InputConstants.Key key = lookup(name);
        if (key == null || key.getType() != InputConstants.Type.MOUSE) return null;

        int value = key.getValue();
        return value >= MIN_MOUSE && value <= MAX_MOUSE ? value : null;
        *///?} else {
        return MOUSE_BUTTONS_BY_NAME.get(name);
        //?}
    }

    //? if sdl_keycodes {
    /*private static @Nullable InputConstants.Key lookup(String name) {
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
    *///?}

    @Override
    public @Nullable String legacyKeyName(int code) {
        return glfwKeyName(code);
    }

    @Override
    public @Nullable String legacyMouseName(int button) {
        return legacyMouseNameOf(button);
    }

    /**
     * Vanilla's name for a GLFW key code, using the SDL spellings for the two keys whose names changed
     * in 26.3 ({@code keypad.period} and {@code application}) so serialized values stay portable
     */
    private static @Nullable String glfwKeyName(int code) {
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

    private static @Nullable String legacyMouseNameOf(int button) {
        return switch (button) {
            case 0 -> "key.mouse.left";
            case 1 -> "key.mouse.right";
            case 2 -> "key.mouse.middle";
            case 3, 4, 5, 6, 7 -> "key.mouse." + (button + 1);
            default -> null;
        };
    }
}
