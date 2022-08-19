/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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
