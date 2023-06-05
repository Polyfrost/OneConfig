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

package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.utils.Notification;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.Notifications;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class NotificationsPage extends Page {
    private static final SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
    private int size = 728;
    private boolean first = true;
    private int prevSize;

    public NotificationsPage() {
        super("Notifications");
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        int originalY = y;
        y += 16;
        if (Notifications.INSTANCE.getAllNotifications().isEmpty()) {
            NanoVGHelper.INSTANCE.drawText(vg, "No notifications have been sent!", x + 16, y + 2, Colors.WHITE, 16, Fonts.REGULAR);
        } else {
            for (Map.Entry<Notification, Long> entry : Notifications.INSTANCE.getAllNotifications().entrySet()) {
                // convert to readable time
                NanoVGHelper.INSTANCE.drawText(vg, "Sent: " + sdf.format(new Date(entry.getValue())), x + 16, y + 2, Colors.WHITE, 16, Fonts.BOLD);
                y += 16;
                y += entry.getKey().draw(vg, x + 16, y, 1, 0, false) + 24;
            }
            y += 16;
            NanoVGHelper.INSTANCE.drawText(vg, "This is a really bad GUI and should only be used for debug purposes.", x + 16, y + 2, Colors.WHITE, 16, Fonts.BOLD);
            y += 16;
        }
        size = Math.max(y - originalY, 728);
        if (first || (prevSize != size && scroll == -(prevSize - 728))) {
            scroll = scrollTarget = -(size - 728);
            scrollAnimation = null;
            if (scroll == -(size - 728)) {
                first = false;
            }
        }
        prevSize = size;
    }

    @Override
    public int getMaxScrollHeight() {
        return size;
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
