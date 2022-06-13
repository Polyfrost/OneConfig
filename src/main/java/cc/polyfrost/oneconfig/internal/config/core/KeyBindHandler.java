package cc.polyfrost.oneconfig.internal.config.core;

import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.events.event.KeyInputEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;

import java.util.ArrayList;

public class KeyBindHandler {
    public static final KeyBindHandler INSTANCE = new KeyBindHandler();
    private final ArrayList<OneKeyBind> keyBinds = new ArrayList<>();

    @Subscribe
    private void onKeyPressed(KeyInputEvent event) {
        for (OneKeyBind keyBind : keyBinds) {
            if (keyBind.isActive()) keyBind.run();
        }
    }

    public void addKeyBind(OneKeyBind keyBind) {
        keyBinds.add(keyBind);
    }

    public void clearKeyBinds() {
        keyBinds.clear();
    }
}
