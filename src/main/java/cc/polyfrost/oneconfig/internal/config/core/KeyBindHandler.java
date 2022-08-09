/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
