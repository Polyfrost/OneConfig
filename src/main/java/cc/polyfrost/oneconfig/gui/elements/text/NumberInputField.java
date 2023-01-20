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

package cc.polyfrost.oneconfig.gui.elements.text;

import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

public class NumberInputField extends TextInputField {
    private final BasicElement upArrow = new BasicElement(12, 14, false);
    private final BasicElement downArrow = new BasicElement(12, 14, false);
    private final ColorAnimation colorTop = new ColorAnimation(ColorPalette.SECONDARY);
    private final ColorAnimation colorBottom = new ColorAnimation(ColorPalette.SECONDARY);
    private float min;
    private float max;
    private float step;
    private float current;

    public NumberInputField(int width, int height, float defaultValue, float min, float max, float step) {
        super(width - 16, height, true, "");
        super.onlyNums = true;
        this.min = min;
        this.max = max;
        this.step = step;
        this.input = String.format("%.01f", defaultValue);
    }

    @Override
    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        super.errored = false;
        if (disabled) nanoVGHelper.setAlpha(vg, 0.5f);
        nanoVGHelper.drawRoundedRect(vg, x + width + 4, y, 12, 28, Colors.GRAY_500, 6f);
        upArrow.disable(disabled);
        downArrow.disable(disabled);
        upArrow.update(x + width + 4, y, inputHandler);
        downArrow.update(x + width + 4, y + 14, inputHandler);
        try {
            current = Float.parseFloat(input);
        } catch (NumberFormatException e) {
            super.errored = true;
        }

        if (current < min || current > max) {
            super.errored = true;
        } else {
            upArrow.disable(false);
            downArrow.disable(false);
        }

        if (upArrow.isClicked()) {
            current += step;
            if (current > max) current = max;
            setCurrentValue(current);
        }
        if (downArrow.isClicked()) {
            current -= step;
            if (current < min) current = min;
            setCurrentValue(current);
        }
        if (current >= max && !disabled) {
            nanoVGHelper.setAlpha(vg, 0.3f);
            upArrow.disable(true);
        }
        nanoVGHelper.drawRoundedRectVaried(vg, x + width + 4, y, 12, 14, colorTop.getColor(upArrow.isHovered(), upArrow.isPressed()), 6f, 6f, 0f, 0f);
        nanoVGHelper.drawSvg(vg, SVGs.CHEVRON_UP, x + width + 5, y + 2, 10, 10);
        if (current >= max && !disabled) nanoVGHelper.setAlpha(vg, 1f);

        if (current <= min && !disabled) {
            nanoVGHelper.setAlpha(vg, 0.3f);
            downArrow.disable(true);
        }
        nanoVGHelper.drawRoundedRectVaried(vg, x + width + 4, y + 14, 12, 14, colorBottom.getColor(downArrow.isHovered(), downArrow.isPressed()), 0f, 0f, 6f, 6f);
        nanoVGHelper.drawSvg(vg, SVGs.CHEVRON_DOWN, x + width + 5, y + 15, 10, 10);
        if (!disabled) nanoVGHelper.setAlpha(vg, 1f);

        try {
            super.draw(vg, x, y - 2, inputHandler);
        } catch (Exception e) {
            setCurrentValue(current);
            super.caretPos = 0;
            super.prevCaret = 0;
        }
        if (disabled) nanoVGHelper.setAlpha(vg, 1f);
    }


    public float getCurrentValue() {
        return current;
    }

    public void setCurrentValue(float value) {
        input = String.format("%.01f", value);
    }

    @Override
    public void onClose() {
        try {
            if (current < min) current = min;
            if (current > max) current = max;
            setCurrentValue(current);
        } catch (Exception ignored) {

        }
    }

    public void setStep(float step) {
        this.step = step;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public boolean arrowsClicked() {
        return upArrow.isClicked() || downArrow.isClicked();
    }
}
