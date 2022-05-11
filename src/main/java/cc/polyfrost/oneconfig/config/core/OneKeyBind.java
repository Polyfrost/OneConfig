package cc.polyfrost.oneconfig.config.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;

public class OneKeyBind {
    private final ArrayList<Integer> keyBinds = new ArrayList<>();

    public OneKeyBind(int... keys) {
        for (int key : keys) {
            keyBinds.add(key);
        }
    }

    public boolean isActive() {
        if (keyBinds.size() == 0) return false;
        for (int keyBind : keyBinds) {
            if (!Keyboard.isKeyDown(keyBind)) return false;
        }
        return true;
    }

    public String getDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int keyBind : keyBinds) {
            if (sb.length() != 0) sb.append(" + ");
            sb.append(Keyboard.getKeyName(keyBind));
        }
        return sb.toString().trim();
    }

    public void addKey(int key) {
        if (!keyBinds.contains(key)) keyBinds.add(key);
    }

    public void clearKeys() {
        keyBinds.clear();
    }

    public int getSize() {
        return keyBinds.size();
    }
}
