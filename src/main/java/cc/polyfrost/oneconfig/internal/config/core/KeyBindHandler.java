/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyBindHandler {
    public static final KeyBindHandler INSTANCE = new KeyBindHandler();
    private final ConcurrentHashMap<Map.Entry<Field, Object>, OneKeyBind> keyBinds = new ConcurrentHashMap<>();

    @Subscribe
    private void onKeyPressed(KeyInputEvent event) {
        for (OneKeyBind keyBind : keyBinds.values()) {
            if (keyBind.isActive()) {
                keyBind.run();
            }
        }
    }

    public void addKeyBind(Field field, Object instance, OneKeyBind keyBind) {
        keyBinds.put(new Map.Entry<Field, Object>() {

            @Override
            public Field getKey() {
                return field;
            }

            @Override
            public Object getValue() {
                return instance;
            }

            @Override
            public Object setValue(Object value) {
                return null;
            }
        }, keyBind);
    }

    public void reInitKeyBinds() {
        for (Map.Entry<Field, Object> field : keyBinds.keySet()) {
            if (field.getValue() == null) continue;
            try {
                keyBinds.put(field, (OneKeyBind) field.getKey().get(field.getValue()));
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    public void clearKeyBinds() {
        keyBinds.clear();
    }
}
