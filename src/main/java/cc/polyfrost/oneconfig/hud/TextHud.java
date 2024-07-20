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
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.TextRenderer;

import java.util.ArrayList;
import java.util.List;

public abstract class TextHud extends BasicHud {
    protected transient List<String> lines = new ArrayList<>();

    @Color(
            name = "Text Color"
    )
    protected OneColor color = new OneColor(255, 255, 255);

    @Dropdown(
            name = "Text Type",
            options = {"No Shadow", "Shadow", "Full Shadow"}
    )
    protected int textType = 0;

    public TextHud(boolean enabled, float x, float y, float scale, boolean background, boolean rounded, float cornerRadius, float paddingX, float paddingY, OneColor bgColor, boolean border, float borderSize, OneColor borderColor) {
        super(enabled, x, y, scale, background, rounded, cornerRadius, paddingX, paddingY, bgColor, border, borderSize, borderColor);
        EventManager.INSTANCE.register(new TickHandler());
    }

    public TextHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
        EventManager.INSTANCE.register(new TickHandler());
    }

    public TextHud(boolean enabled) {
        this(enabled, 0, 0);
    }

    /**
     * This function is called every tick
     *
     * @param lines Empty ArrayList to add your hud text too
     */
    protected abstract void getLines(List<String> lines, boolean example);

    /**
     * This function is called every frame
     *
     * @param lines The current lines of the hud
     */
    protected void getLinesFrequent(List<String> lines, boolean example) {
    }

    @Override
    public void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        if (lines == null || lines.size() == 0) return;

        float textY = y;
        for (String line : lines) {
            drawLine(line, x, textY, scale);
            textY += 12 * scale;
        }
    }

    /**
     * Function that can be overwritten to implement different behavior easily
     *
     * @param line  The line
     * @param x     The X coordinate
     * @param y     The Y coordinate
     * @param scale The scale
     */
    protected void drawLine(String line, float x, float y, float scale) {
        TextRenderer.drawScaledString(line, x, y, color.getRGB(), TextRenderer.TextType.toType(textType), scale);
    }

    /**
     * Function that can be overwritten to implement different behavior easily
     *
     * @param line The line
     * @return The width of the line (scaled accordingly)
     */
    protected float getLineWidth(String line, float scale) {
        return Platform.getGLPlatform().getStringWidth(line) * scale;
    }

    @Override
    protected void preRender(boolean example) {
        getLinesFrequent(lines, example);
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        if (lines == null) return 0;
        float width = 0;
        for (String line : lines) {
            width = Math.max(width, getLineWidth(line, scale));
        }
        return width;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        return lines == null ? 0 : (lines.size() * 12 - 4) * scale;
    }

    @Override
    public boolean shouldDrawBackground() {
        return super.shouldDrawBackground() && lines != null && lines.size() > 0;
    }

    private class TickHandler {
        @Subscribe
        private void onTick(TickEvent event) {
            if (event.stage != Stage.END || !isEnabled()) return;
            lines.clear();
            getLines(lines, HudCore.editing);
        }
    }
}
