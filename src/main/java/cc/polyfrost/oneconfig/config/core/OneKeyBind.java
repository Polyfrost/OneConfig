package cc.polyfrost.oneconfig.config.core;

import cc.polyfrost.oneconfig.libs.universal.UKeyboard;

import java.util.ArrayList;

public class OneKeyBind {
    protected final ArrayList<Integer> keyBinds = new ArrayList<>();
    protected transient final Runnable runnable;
    protected transient boolean hasRun;

    public OneKeyBind(Runnable runnable, int... keys) {
        this.runnable = runnable;
        for (int key : keys) {
            keyBinds.add(key);
        }
    }

    public OneKeyBind(int... keys) {
        this(null, keys);
    }

    public boolean isActive() {
        if (keyBinds.size() == 0) return false;
        for (int keyBind : keyBinds) {
            if (!UKeyboard.isKeyDown(keyBind)) {
                hasRun = false;
                return false;
            }
        }
        return true;
    }

    public void run() {
        if (runnable == null || hasRun) return;
        runnable.run();
        hasRun = true;
    }

    public String getDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int keyBind : keyBinds) {
            if (sb.length() != 0) sb.append(" + ");
            sb.append(UKeyboard.getKeyName(keyBind, -1));
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
