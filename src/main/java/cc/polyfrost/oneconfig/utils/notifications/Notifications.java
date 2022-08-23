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

package cc.polyfrost.oneconfig.utils.notifications;

import cc.polyfrost.oneconfig.events.event.HudRenderEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.renderer.Icon;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public final class Notifications {
    public static final Notifications INSTANCE = new Notifications();
    private final ArrayList<Notification> notifications = new ArrayList<>();
    private final float DEFAULT_DURATION = 5000;

    private Notifications() {
    }

    public Notification send(String title, String message, Icon icon, float duration, @Nullable Callable<Boolean> progressbar, @Nullable Runnable action) {
        Notification notification = new Notification(title, message, icon, duration, progressbar, action);
        notifications.add(notification);
        return notification;
    }

    public Notification send(String title, String message, float duration, @Nullable Callable<Boolean> progressbar, @Nullable Runnable action) {
        return send(title, message, null, duration, progressbar, action);
    }

    public Notification send(String title, String message, Icon icon, float duration, @Nullable Callable<Boolean> progressbar) {
        return send(title, message, icon, duration, progressbar, null);
    }

    public Notification send(String title, String message, Icon icon, float duration, @Nullable Runnable action) {
        return send(title, message, icon, duration, null, action);
    }

    public Notification send(String title, String message, float duration, @Nullable Callable<Boolean> progressbar) {
        return send(title, message, duration, progressbar, null);
    }

    public Notification send(String title, String message, float duration, @Nullable Runnable action) {
        return send(title, message, duration, null, action);
    }

    public Notification send(String title, String message, Icon icon, @Nullable Callable<Boolean> progressbar) {
        return send(title, message, icon, DEFAULT_DURATION, progressbar);
    }

    public Notification send(String title, String message, Icon icon, @Nullable Runnable action) {
        return send(title, message, icon, DEFAULT_DURATION, action);
    }

    public Notification send(String title, String message, @Nullable Callable<Boolean> progressbar) {
        return send(title, message, DEFAULT_DURATION, progressbar);
    }

    public Notification send(String title, String message, @Nullable Runnable action) {
        return send(title, message, DEFAULT_DURATION, action);
    }

    public Notification send(String title, String message, Icon icon, float duration) {
        return send(title, message, icon, duration, (Callable<Boolean>) null);
    }

    public Notification send(String title, String message, float duration) {
        return send(title, message, duration, (Callable<Boolean>) null);
    }

    public Notification send(String title, String message, Icon icon) {
        return send(title, message, icon, (Callable<Boolean>) null);
    }

    public Notification send(String title, String message) {
        return send(title, message, (Callable<Boolean>) null);
    }

    @Subscribe
    public void onHudRender(HudRenderEvent event) {
        RenderManager.setupAndDraw((vg) -> {
            for (Notification notification : notifications) {
                notification.draw(vg);
            }
            notifications.removeIf(Notification::isFinished);
        });
    }
}
