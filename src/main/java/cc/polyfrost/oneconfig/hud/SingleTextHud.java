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

package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;

import java.util.List;

public abstract class SingleTextHud extends TextHud {
    /**
     * @param enabled      If the hud is enabled
     * @param x            X-coordinate of hud on a 1080p display
     * @param y            Y-coordinate of hud on a 1080p display
     * @param scale        Scale of the hud
     * @param background   If the HUD should have a background
     * @param rounded      If the corner is rounded or not
     * @param cornerRadius Radius of the corner
     * @param width        The width
     * @param height       The height
     * @param bgColor      Background color
     * @param border       If the hud has a border or not
     * @param borderSize   Thickness of the border
     * @param borderColor  The color of the border
     */
    public SingleTextHud(String title, boolean enabled, float x, float y, float scale, boolean background, boolean rounded, float cornerRadius, float width, float height, OneColor bgColor, boolean border, float borderSize, OneColor borderColor) {
        super(enabled, x, y, scale, background, rounded, cornerRadius, width, height, bgColor, border, borderSize, borderColor);
        this.title = title;
    }

    public SingleTextHud(String title, boolean enabled, int x, int y) {
        this(title, enabled, x, y, 1f, true, false, 2, 56, 18, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    public SingleTextHud(String title, boolean enabled) {
        this(title, enabled, 0, 0);
    }

    /**
     * This function is called every tick
     *
     * @return The new text
     */
    protected abstract String getText(boolean example);

    /**
     * This function is called every frame
     *
     * @return The new text, null to use the cached value
     */
    protected String getTextFrequent(boolean example) {
        return null;
    }

    @Override
    public void drawAll(UMatrixStack matrices, boolean example) {
        if (!example && !shouldShow()) return;
        preRender(example);
        float contentWidth = getWidth(scale, example);
        float contentHeight = getHeight(scale, example);
        position.setSize(Math.max(contentWidth, paddingX * scale), Math.max(contentHeight, paddingY * scale));
        if (shouldDrawBackground() && background)
            drawBackground(position.getX(), position.getY(), position.getWidth(), position.getHeight(), scale);
        draw(
                matrices,
                position.getX() + position.getWidth() / 2f - contentWidth / 2f,
                position.getY() + position.getHeight() / 2f - contentHeight / 2f,
                scale,
                example
        );
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        lines.add(getCompleteText(getText(example)));
    }

    @Override
    protected void getLinesFrequent(List<String> lines, boolean example) {
        String text = getTextFrequent(example);
        if (text == null) return;
        lines.clear();
        lines.add(getCompleteText(text));
    }

    protected final String getCompleteText(String text) {
        boolean showTitle = !title.trim().isEmpty();
        StringBuilder builder = new StringBuilder();
        if (brackets) {
            builder.append("[");
        }

        if (showTitle && titleLocation == 0) {
            builder.append(title).append(": ");
        }

        builder.append(text);

        if (showTitle && titleLocation == 1) {
            builder.append(" ").append(title);
        }

        if (brackets) {
            builder.append("]");
        }
        return builder.toString();
    }


    @Switch(
            name = "Brackets"
    )
    protected boolean brackets = false;

    @Text(
            name = "Title"
    )
    protected String title;

    @Dropdown(
            name = "Title Location",
            options = {"Left", "Right"}
    )
    protected int titleLocation = 0;
}
