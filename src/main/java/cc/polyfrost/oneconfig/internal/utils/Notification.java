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

package cc.polyfrost.oneconfig.internal.utils;

import cc.polyfrost.oneconfig.gui.animations.*;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.renderer.Icon;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorManager;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.MathUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

public final class Notification {
    private final String title;
    private final String message;
    private final Icon icon;
    private final Animation animation;
    private final Callable<Float> progressBar;
    private final Runnable action;
    private final InputHandler inputHandler = new InputHandler();
    private final ColorAnimation bgColor = new ColorAnimation(new ColorPalette(Colors.GRAY_800, Colors.GRAY_700, Colors.GRAY_900));
    private final ColorAnimation titleColor = new ColorAnimation(new ColorPalette(Colors.WHITE_80, Colors.WHITE, Colors.WHITE));
    private final ColorAnimation messageColor = new ColorAnimation(new ColorPalette(Colors.WHITE_60, Colors.WHITE_90, Colors.WHITE_90));

    public Notification(String title, String message, @Nullable Icon icon, float duration, @Nullable Callable<Float> progressBar, @Nullable Runnable action) {
        this.title = title;
        this.message = message;
        this.icon = icon;
        this.animation = new ChainedAnimation(
                new EaseInOutQuad(250, 0, 330, false),
                progressBar == null ? new DummyAnimation(330, duration) : new DummyAnimation(330, () -> progressBar.call() >= 1f),
                new EaseInOutQuad(250, 330, 0, false)
        );
        this.progressBar = progressBar;
        this.action = action;
    }

    public float draw(final long vg, float y) {
        float x = UResolution.getWindowWidth() - animation.get();
        float textX = icon == null ? x + 16 : x + 64;
        float textMaxLength = icon == null ? 268 : 220;
        float messageHeight = RenderManager.getWrappedStringHeight(vg, message, textMaxLength, 12f, 1.75f, Fonts.REGULAR);
        float height = getHeight(messageHeight);
        y -= height;
        boolean hovered = inputHandler.isAreaHovered(x, y, 314, height);
        if (hovered && inputHandler.isClicked() && action != null) action.run();
        int bgColor = this.bgColor.getColor(hovered, hovered && inputHandler.isMouseDown());
        int titleColor = this.titleColor.getColor(hovered, hovered && inputHandler.isMouseDown());
        int messageColor = this.messageColor.getColor(hovered, hovered && inputHandler.isMouseDown());
        RenderManager.drawRoundedRect(vg, x, y, 314f, height, bgColor, 8f);
        if (icon != null)
            icon.draw(vg, x + 16f, y + (height - (progressBar == null ? 0f : 5f)) / 2f - 16f, 32f, 32f, titleColor);
        RenderManager.drawText(vg, title, textX, y + 30, titleColor, 16f, Fonts.SEMIBOLD);
        RenderManager.drawWrappedString(vg, message, textX, y + 46, textMaxLength, messageColor, 12f, 1.75f, Fonts.REGULAR);
        if (progressBar != null) {
            try {
                float progress = MathUtils.clamp(progressBar.call());
                Scissor scissor1 = ScissorManager.scissor(vg, x + 314f * progress, y + height - 5f, 314f * (1 - progress), 5f);
                RenderManager.drawRoundedRect(vg, x, y, 314f, height, Colors.PRIMARY_800, 8f);
                ScissorManager.resetScissor(vg, scissor1);
                Scissor scissor2 = ScissorManager.scissor(vg, x, y + height - 5f, 314f * progress - (314f * progress < 2.5f || 311.5f * progress > 2.5f ? 0f : 2.5f), 5f);
                RenderManager.drawRoundedRect(vg, x, y, 314f, height, Colors.PRIMARY_500, 8f);
                ScissorManager.resetScissor(vg, scissor2);
                if (314f * progress >= 2.5f && 311.5f * progress <= 2.5f)
                    RenderManager.drawRoundedRect(vg, x + 2.5f, y + height - 5f, Math.max(0, 314f * progress - 5f), 5f, Colors.PRIMARY_500, 2.5f);
            } catch (Exception ignored) {
            }
        }
        return height;
    }

    private float getHeight(float messageHeight) {
        float height = 68 + messageHeight;
        if (progressBar != null) height += 5f;
        return height;
    }

    public boolean isFinished() {
        return animation.isFinished();
    }
}
