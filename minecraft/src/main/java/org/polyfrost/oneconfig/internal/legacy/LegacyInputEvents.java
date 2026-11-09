package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsRenderer;
import pl.tomgirl.pylon.window.DisplaySdl;

public final class LegacyInputEvents {
    public static boolean deferred;

    private LegacyInputEvents() {}

    public static void postKeyboard() {
        int keyCode = Keyboard.getEventKey();
        if (keyCode > 0) {
            int state = Keyboard.isRepeatEvent() ? KeyInputEvent.REPEAT : Keyboard.getEventKeyState() ? KeyInputEvent.PRESSED : KeyInputEvent.RELEASED;
            EventManager.INSTANCE.post(new KeyInputEvent(KeyCodes.fromLegacy(keyCode).getValue(), (char) 0, state));
        }

        char character = Keyboard.getEventCharacter();
        if (character != 0) {
            EventManager.INSTANCE.post(new KeyInputEvent(0, character, KeyInputEvent.PRESSED));
        }
    }

    public static boolean postMouse() {
        int button = Mouse.getEventButton();
        if (button >= 0) {
            int key = KeyCodes.mouseFromLegacy(button);
            boolean pressed = Mouse.getEventButtonState();
            if (NotificationsRenderer.consumeMouseButton(key, pressed)) return true;
            EventManager.INSTANCE.post(new MouseInputEvent(key, pressed ? 1 : 0));
            return false;
        }

        if (Mouse.getEventDWheel() != 0 && Minecraft.getInstance().screen != null && NotificationsRenderer.isPointerOverToast()) return true;

        if (Mouse.getEventDWheel() != 0 || Minecraft.getInstance().screen == null || Mouse.getEventDX() == 0 && Mouse.getEventDY() == 0) return false;

        DisplaySdl display = DisplaySdl.instance();
        float scaleX = (float) display.getWidth() / display.getWindowWidth();
        float scaleY = (float) display.getHeight() / display.getWindowHeight();
        float x = Mouse.getEventX() / scaleX;
        float y = (display.getHeight() - 1 - Mouse.getEventY()) / scaleY;
        MouseInputEvent.Moved.post(x, y);
        return false;
    }
}
*///?}
