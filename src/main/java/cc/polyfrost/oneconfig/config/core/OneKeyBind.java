package cc.polyfrost.oneconfig.config.core;

import cc.polyfrost.oneconfig.libs.universal.UKeyboard;

import java.util.ArrayList;

public class OneKeyBind {
    protected final ArrayList<Integer> keyBinds = new ArrayList<>();
    protected transient Runnable runnable;
    protected transient boolean hasRun;

    /**
     * @param keys  The bound keys
     */
    public OneKeyBind(int... keys) {
        for (int key : keys) {
            keyBinds.add(key);
        }
    }

    /**
     * @return If the keys are pressed
     */
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

    /**
     * Run the set Runnable
     */
    public void run() {
        if (runnable == null || hasRun) return;
        runnable.run();
        hasRun = true;
    }

    /**
     * @return The set keys as the name of the keys
     */
    public String getDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int keyBind : keyBinds) {
            if (sb.length() != 0) sb.append(" + ");
            sb.append(UKeyboard.getKeyName(keyBind, -1));
        }
        return sb.toString().trim();
    }

    /**
     * @param key   Add a Key to keys
     */
    public void addKey(int key) {
        if (!keyBinds.contains(key)) keyBinds.add(key);
    }

    /**
     * Clear the keys List
     */
    public void clearKeys() {
        keyBinds.clear();
    }

    /**
     * @return The amount of key in the keys List
     */
    public int getSize() {
        return keyBinds.size();
    }

    /**
     * Set the Runnable that gets ran when OneKeyBind#run() is called
     * @param runnable The Runnable to run
     */
    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    /**
     * @return The key in the keys List
     */
    public ArrayList<Integer> getKeyBinds() {
        return keyBinds;
    }
}
