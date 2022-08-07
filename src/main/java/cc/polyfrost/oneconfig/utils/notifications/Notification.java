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

/**
 * @deprecated Reserved for future use, not implemented yet.
 */
@Deprecated
public final class Notification {
    private String title;
    private String message;
    private final float duration;
    private float x;
    private float y;

    private final Runnable action;
    private final Runnable onClose;

    Notification(String title, String message, float duration, float x, float y, Runnable action, Runnable onClose) {
        this.title = title;
        this.message = message;
        this.duration = duration;
        this.x = x;
        this.y = y;
        this.action = action;
        this.onClose = onClose;
    }

    void draw(final long vg) {

    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public float getDuration() {
        return duration;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public Runnable getAction() {
        return action;
    }

    public Runnable getOnClose() {
        return onClose;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    void setX(float x) {
        this.x = x;
    }

    void setY(float y) {
        this.y = y;
    }
}
