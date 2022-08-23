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

import cc.polyfrost.oneconfig.gui.animations.*;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.renderer.Icon;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

public final class Notification {
    private String title;
    private String message;
    private Icon icon;
    private final Animation animation;
    private final Callable<Boolean> progressBar;
    private final Runnable action;
    private final InputHandler inputHandler = new InputHandler();
    private final ColorAnimation bgColor = new ColorAnimation(new ColorPalette(Colors.GRAY_800, Colors.GRAY_700, Colors.GRAY_900));
    private final ColorAnimation titleColor = new ColorAnimation(new ColorPalette(Colors.WHITE_80, Colors.WHITE, Colors.WHITE));
    private final ColorAnimation messageColor = new ColorAnimation(new ColorPalette(Colors.WHITE_60, Colors.WHITE_90, Colors.WHITE_90));
    private float height = 110;

    Notification(String title, String message, @Nullable Icon icon, float duration, @Nullable Callable<Boolean> progressBar, @Nullable Runnable action) {
        this.title = title;
        this.message = message;
        this.icon = icon;
        this.animation = new ChainedAnimation(
                new EaseInOutQuad(300, 0, 314, false),
                new DummyAnimation(314, duration),
                new EaseInOutQuad(300, 314, 0, false)
        );
        this.progressBar = progressBar;
        this.action = action;
    }

    void draw(final long vg) {
        float x = UResolution.getWindowWidth() - 32 - animation.get();
        float y = UResolution.getWindowHeight() - 32 - height;
        boolean hovered = inputHandler.isAreaHovered(x, y, 134, height);
        boolean clicked = hovered && inputHandler.isClicked();
        int bgColor = this.bgColor.getColor(hovered, clicked);
        int titleColor = this.titleColor.getColor(hovered, clicked);
        int messageColor = this.messageColor.getColor(hovered, clicked);
    }

    public boolean isFinished() {
        return animation.isFinished();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public Runnable getAction() {
        return action;
    }
}
