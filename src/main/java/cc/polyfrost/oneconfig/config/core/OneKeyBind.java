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
    // basically since a lot of mods still use `keyBinds` instead of `keys` we have to keep it
    // so keyBinds is used for internal stuff and keys is used for storing the actual keys in the config
    protected transient final ArrayList<Integer> keyBinds = new ArrayList<>();
    private final ArrayList<Key> keys = new ArrayList<>();
    protected transient Runnable runnable;
    protected transient boolean hasRun;

    /**
     * @param keys  The bound keys
     * @deprecated Use {@link #OneKeyBind(Key...)} instead
     */
    @Deprecated
    public OneKeyBind(int... keys) {
        this();
        for (int key : keys) {
            this.keys.add(new Key(key, Key.Type.KEYBOARD));
        }
        for (Key key : this.keys) {
            keyBinds.add(key.getKey());
        }
    }

    /**
     * @param keys  The bound keys
     */
    public OneKeyBind(Key... keys) {
        this();
        Collections.addAll(this.keys, keys);
        for (Key key : this.keys) {
            keyBinds.add(key.getKey());
        }
    }

    public OneKeyBind() {

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
            sb.append(Platform.getI18nPlatform().getKeyName(keyBind, -1));
        }
        return sb.toString().trim();
    }

    /**
     * @param key   Add a Key to keys
     * @deprecated Use {@link #addKey(Key)} instead
     */
    public void addKey(int key) {
        if (keyBinds.contains(key)) return;
        Key keyClass = new Key(key, Key.Type.KEYBOARD);
        keys.add(keyClass);
        keyBinds.add(keyClass.getKey());
    }

    /**
     * @param key   Add a Key to keys
     */
    public void addKey(Key key) {
        if (keys.contains(key)) return;
        keys.add(key);
        keyBinds.add(key.getKey());
    }

    /**
     * Clear the keys List
     */
    public void clearKeys() {
        keyBinds.clear();
        keys.clear();
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
     * @return The keys in the key List
     * @deprecated Use {@link #getKeys()} instead
     */
    @Deprecated
    public ArrayList<Integer> getKeyBinds() {
        return keyBinds;
    }

    /**
     * @return The keys in the key List
     */
    public Key[] getKeys() {
        return keys.toArray(new Key[0]);
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
