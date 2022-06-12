package cc.polyfrost.oneconfig.internal.config.core;

import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import java.util.ArrayList;

public class KeyBindHandler {
    private static final ArrayList<OneKeyBind> keyBinds = new ArrayList<>();

    @SubscribeEvent
    public void onKeyPressed(InputEvent.KeyInputEvent event) {
        for (OneKeyBind keyBind : keyBinds) {
            if (keyBind.isActive()) keyBind.run();
        }
    }

    public static void addKeyBind(OneKeyBind keyBind) {
        if (keyBind == null) return;
        keyBinds.add(keyBind);
    }

    public static void clearKeyBinds() {
        keyBinds.clear();
    }
}
