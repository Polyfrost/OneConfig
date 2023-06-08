/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 * Co-author: ForBai <https://github.com/ForBai> (non-copyrightable contribution)
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.config.core;

import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.platform.Platform;

import java.util.ArrayList;
import java.util.Collections;

public class OneKeyBind {
    protected final ArrayList<Key> keyBinds = new ArrayList<>();
    protected transient Runnable runnable;
    protected transient boolean hasRun;

    /**
     * @param keys  The bound keys
     * @deprecated Use {@link #OneKeyBind(Key...)} instead
     */
    @Deprecated
    public OneKeyBind(int... keys) {
        for (int key : keys) {
            keyBinds.add(new Key(key, Key.Type.KEYBOARD));
        }
    }

    /**
     * @param keys  The bound keys
     */
    public OneKeyBind(Key... keys) {
        Collections.addAll(keyBinds, keys);
    }

    public OneKeyBind() {

    }

    /**
     * @return If the keys are pressed
     */
    public boolean isActive() {
        if (keyBinds.size() == 0) return false;
        for (Key keyBind : keyBinds) {
            if (!UKeyboard.isKeyDown(keyBind.getKey())) {
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
        for (Key keyBind : keyBinds) {
            if (sb.length() != 0) sb.append(" + ");
            sb.append(Platform.getI18nPlatform().getKeyName(keyBind.getKey(), -1));
        }
        return sb.toString().trim();
    }

    /**
     * @param key   Add a Key to keys
     * @deprecated Use {@link #addKey(Key)} instead
     */
    public void addKey(int key) {
        for (Key keyBind : keyBinds) {
            if (keyBind.getRawKey() == key) return;
        }
        keyBinds.add(new Key(key, Key.Type.KEYBOARD));
    }

    /**
     * @param key   Add a Key to keys
     */
    public void addKey(Key key) {
        if (keyBinds.contains(key)) return;
        keyBinds.add(key);
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
     * @deprecated Use {@link #getKeys()} instead
     */
    public ArrayList<Integer> getKeyBinds() {
        ArrayList<Integer> keyBinds = new ArrayList<>();
        for (Key keyBind : this.keyBinds) {
            keyBinds.add(keyBind.getRawKey());
        }
        return keyBinds;
    }

    /**
     * @return The key in the keys List
     */
    public ArrayList<Key> getKeys() {
        return keyBinds;
    }

    public static class Key {
        private final int key;
        private final Type type;

        public Key(int key, Type type) {
            this.key = key;
            this.type = type;
        }

        public int getRawKey() {
            return key;
        }

        public int getKey() {
            if (Platform.getInstance().getMinecraftVersion() < 11300 && type == Type.MOUSE) return key - 100;
            return key;
        }

        public enum Type {
            KEYBOARD,
            MOUSE
        }
    }
}
