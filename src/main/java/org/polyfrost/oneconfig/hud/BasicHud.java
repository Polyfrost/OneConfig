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

package org.polyfrost.oneconfig.hud;

import org.polyfrost.oneconfig.config.core.OneColor;
import org.polyfrost.oneconfig.libs.universal.UMatrixStack;
import org.polyfrost.oneconfig.renderer.NanoVGHelper;


public abstract class BasicHud extends Hud {
    protected boolean background;
    protected boolean rounded;
    protected boolean border;
    protected OneColor bgColor;
    protected OneColor borderColor;
    protected float cornerRadius;
    protected float borderSize;
    protected float paddingX;
    protected float paddingY;

    /**
     * @param enabled      If the hud is enabled
     * @param x            X-coordinate of hud on a 1080p display
     * @param y            Y-coordinate of hud on a 1080p display
     * @param scale        Scale of the hud
     * @param background   If the HUD should have a background
     * @param rounded      If the corner is rounded or not
     * @param cornerRadius Radius of the corner
     * @param paddingX     Horizontal background padding
     * @param paddingY     Vertical background padding
     * @param bgColor      Background color
     * @param border       If the hud has a border or not
     * @param borderSize   Thickness of the border
     * @param borderColor  The color of the border
     */
    public BasicHud(boolean enabled, float x, float y, float scale, boolean background, boolean rounded, float cornerRadius, float paddingX, float paddingY, OneColor bgColor, boolean border, float borderSize, OneColor borderColor) {
        super(enabled, x, y, scale);
        this.background = background;
        this.rounded = rounded;
        this.cornerRadius = cornerRadius;
        this.paddingX = paddingX;
        this.paddingY = paddingY;
        this.bgColor = bgColor;
        this.border = border;
        this.borderSize = borderSize;
        this.borderColor = borderColor;
        position.setSize(getWidth(scale, true) + paddingX * scale * 2f, getHeight(scale, true) + paddingY * scale * 2f);
    }

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     * @param scale   Scale of the hud
     */
    public BasicHud(boolean enabled, float x, float y, float scale) {
        this(enabled, x, y, scale, true, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     */
    public BasicHud(boolean enabled, float x, float y) {
        this(enabled, x, y, 1, true, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    /**
     * @param enabled If the hud is enabled
     */
    public BasicHud(boolean enabled) {
        this(enabled, 0, 0, 1, true, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    public BasicHud() {
        this(false, 0, 0, 1, true, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    @Override
    public void drawAll(UMatrixStack matrices, boolean example) {
        if (!example && !shouldShow()) return;
        preRender(example);
        position.setSize(getWidth(scale, example) + paddingX * scale * 2f, getHeight(scale, example) + paddingY * scale * 2f);
        if (shouldDrawBackground() && background)
            drawBackground(position.getX(), position.getY(), position.getWidth(), position.getHeight(), scale);
        draw(matrices, position.getX() + paddingX * scale, position.getY() + paddingY * scale, scale, example);
    }

    /**
     * Set a new scale value
     *
     * @param scale   The new scale
     * @param example If the HUD is being rendered in example form
     */
    @Override
    public void setScale(float scale, boolean example) {
        this.scale = scale;
        position.updateSizePosition(getWidth(scale, example) + paddingX * scale * 2f, getHeight(scale, example) + paddingY * scale * 2f);
    }

    /**
     * @return If the background should be drawn
     */
    protected boolean shouldDrawBackground() {
        return true;
    }

    protected void drawBackground(float x, float y, float width, float height, float scale) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        nanoVGHelper.setupAndDraw(true, (vg) -> {
            if (rounded) {
                nanoVGHelper.drawRoundedRect(vg, x, y, width, height, bgColor.getRGB(), cornerRadius * scale);
                if (border)
                    nanoVGHelper.drawHollowRoundRect(vg, x - borderSize * scale, y - borderSize * scale, width + borderSize * scale, height + borderSize * scale, borderColor.getRGB(), cornerRadius * scale, borderSize * scale);
            } else {
                nanoVGHelper.drawRect(vg, x, y, width, height, bgColor.getRGB());
                if (border)
                    nanoVGHelper.drawHollowRoundRect(vg, x - borderSize * scale, y - borderSize * scale, width + borderSize * scale, height + borderSize * scale, borderColor.getRGB(), 0, borderSize * scale);
            }
        });
    }
}
