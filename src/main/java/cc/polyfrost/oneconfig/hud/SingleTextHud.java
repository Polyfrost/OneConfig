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

package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.renderer.TextRenderer;

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
     * @param paddingX     X-Padding of the HUD
     * @param paddingY     Y-Padding of the HUD
     * @param bgColor      Background color
     * @param border       If the hud has a border or not
     * @param borderSize   Thickness of the border
     * @param borderColor  The color of the border
     */
    public SingleTextHud(String title, boolean enabled, float x, float y, float scale, boolean background, boolean rounded, float cornerRadius, float paddingX, float paddingY, OneColor bgColor, boolean border, float borderSize, OneColor borderColor) {
        super(enabled, x, y, scale, background, rounded, cornerRadius, paddingX, paddingY, bgColor, border, borderSize, borderColor);
        this.title = title;
    }

    public SingleTextHud(String title, boolean enabled, int x, int y) {
        this(title, enabled, x, y, 1f, true, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
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

    @Override
    public void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        float textX = x;
        if (brackets) {
            drawLine("[", textX, y, bracketsColor, scale);
            textX += getLineWidth("[", scale);
        }
        drawLine(lines.get(0), textX, y, scale);
        if (brackets) {
            textX += getLineWidth(lines.get(0), scale);
            drawLine("]", textX, y, bracketsColor, scale);
        }
    }

    protected void drawLine(String line, float x, float y, OneColor color, float scale) {
        TextRenderer.drawScaledString(line, x, y, color.getRGB(), TextRenderer.TextType.toType(textType), scale);
    }

    protected final String getCompleteText(String text) {
        boolean showTitle = !title.trim().isEmpty();
        StringBuilder builder = new StringBuilder();

        if (showTitle && titleLocation == 0) {
            builder.append(title).append(": ");
        }

        builder.append(text);

        if (showTitle && titleLocation == 1) {
            builder.append(" ").append(title);
        }

        return builder.toString();
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (lines == null) return 0f;
        if (!brackets) return getLineWidth(lines.get(0), scale);
        return getLineWidth("[" + lines.get(0) + "]", scale);
    }

    @Switch(
        name = "Brackets"
    )
    protected boolean brackets = false;

    @Color(
        name = "Brackets Color"
    )
    protected OneColor bracketsColor = new OneColor(0xFFFFFFFF);

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