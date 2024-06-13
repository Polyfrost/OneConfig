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

package cc.polyfrost.oneconfig.internal.hud;

import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.events.event.HudRenderEvent;
import cc.polyfrost.oneconfig.events.event.InitializationEvent;
import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UGraphics;
import cc.polyfrost.oneconfig.platform.Platform;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HudCore {
    public static final ConcurrentHashMap<Map.Entry<Field, Object>, Hud> huds = new ConcurrentHashMap<>();
    public static final ArrayList<BasicOption> hudOptions = new ArrayList<>();
    private static boolean isPatcher = false;
    public static boolean editing = false;

    @Subscribe
    public void onRender(HudRenderEvent event) {
        if (editing) return;
        for (Hud hud : huds.values()) {
            if (!hud.isEnabled()) continue;
            //#if FORGE==1 && MC<=11202
            if (hud.isCachingIgnored()) continue;
            //#endif
            hud.deltaTicks = event.deltaTicks;
            UGraphics.enableAlpha();
            UGraphics.enableBlend();
            hud.drawAll(event.matrices, false);
        }
    }

    @Subscribe
    public void onInit(InitializationEvent event) {
        isPatcher = Platform.getLoaderPlatform().isModLoaded("patcher");
    }

    public static void reInitHuds() {
        for (Map.Entry<Field, Object> field : huds.keySet()) {
            if (field == null || field.getKey() == null || field.getValue() == null) continue;
            try {
                field.getKey().setAccessible(true);
                Hud oldHud = huds.get(field);
                Hud newHud = (Hud) field.getKey().get(field.getValue());
                newHud.setConfig(oldHud.getConfig());
                for (BasicOption option : hudOptions) {
                    if (option.getParent().equals(oldHud)) {
                        option.setParent(newHud);
                    }
                }
                huds.put(field, newHud);
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    public static boolean isPatcher() {
        return isPatcher;
    }
}
