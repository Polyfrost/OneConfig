package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import java.util.Map;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OS;
import org.lwjgl.sdl.SDLKeyboard;

public class InputQuirks {
    private static final OS PLATFORM = Util.getPlatform();
    private static final boolean ON_WINDOWS = PLATFORM == OS.WINDOWS;
    private static final boolean ON_OSX = PLATFORM == OS.OSX;
    public static final boolean REPLACE_CTRL_KEY_WITH_CMD_KEY = ON_OSX;
    public static final int EDIT_SHORTCUT_KEY_MODIFIER = REPLACE_CTRL_KEY_WITH_CMD_KEY ? 3072 : 192;
    public static final boolean SHIFT_INVERTS_SCROLL_AXIS = ON_OSX;
    public static final boolean EMULATE_RIGHT_CLICK_WITH_CTRL_KEY = ON_OSX;
    public static final boolean RESTORE_KEY_STATE_AFTER_MOUSE_GRAB = !ON_OSX;
    private static final Map<String, String> KEYBOARD_DISPLAY_OVERRIDES;

    public static boolean isQuitShortcutDown() {
        int modifiers = SDLKeyboard.SDL_GetModState();
        return ON_OSX ? (modifiers & 3072) != 0 && InputConstants.isKeyDown(20) : (modifiers & 768) != 0 && InputConstants.isKeyDown(61);
    }

    public static String keyboardTranslationKey(final String name) {
        return (String)KEYBOARD_DISPLAY_OVERRIDES.getOrDefault(name, name);
    }

    static {
        KEYBOARD_DISPLAY_OVERRIDES = switch (PLATFORM) {
            case OSX -> Map.of(
                    "key.keyboard.left.alt",
                    "key.keyboard.left.option",
                    "key.keyboard.right.alt",
                    "key.keyboard.right.option",
                    "key.keyboard.left.win",
                    "key.keyboard.left.command",
                    "key.keyboard.right.win",
                    "key.keyboard.right.command"
            );
            case WINDOWS -> Map.of("key.keyboard.left.win", "key.keyboard.left.windows", "key.keyboard.right.win", "key.keyboard.right.windows");
            case LINUX -> Map.of("key.keyboard.left.win", "key.keyboard.left.meta", "key.keyboard.right.win", "key.keyboard.right.meta");
            default -> Map.of();
        };
    }
}
*///?}
