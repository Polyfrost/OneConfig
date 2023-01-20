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

package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.internal.gui.GuiNotification;
import cc.polyfrost.oneconfig.renderer.asset.SVG;
import cc.polyfrost.oneconfig.utils.InputHandler;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuiNotifications {
    public static final GuiNotifications INSTANCE = new GuiNotifications();
    private final CopyOnWriteArrayList<GuiNotification> notifications = new CopyOnWriteArrayList<>();
    private final ArrayList<GuiNotification> toRemove = new ArrayList<>();
    private final float DEFAULT_DURATION = 4000;

    private GuiNotifications() {
    }

    public void sendNotification(String message) {
        sendNotification(message, DEFAULT_DURATION);
    }

    public void sendNotification(String message, float duration) {
        sendNotification(message, duration, null);
    }

    public void sendNotification(String message, SVG icon) {
        sendNotification(message, DEFAULT_DURATION, icon);
    }

    public void sendNotification(String message, float duration, SVG icon) {
        notifications.add(new GuiNotification(message, duration, icon));
    }

    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        for (GuiNotification remove : toRemove) {
            notifications.remove(remove);
        }
        for (GuiNotification notification : notifications) {
            int draw = notification.draw(vg, x, y, inputHandler);
            if (draw == -1) {
                toRemove.add(notification);
            }
            y -= (draw + 10);
        }
    }
}
