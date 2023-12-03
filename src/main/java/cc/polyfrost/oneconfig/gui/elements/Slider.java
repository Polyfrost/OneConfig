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

package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.utils.InputHandler;

public class Slider extends BasicElement {
    public static final int VERTICAL = 1, HORIZONTAL = 2;
    private final float min, max;
    protected float value;
    protected float currentDragPoint;
    protected float dragPointerSize = 8f;
    private boolean dragging = false;
    private final int mode;
    private boolean mouseWasDown = false;

    public Slider(int length, float min, float max, float startValue, int mode) {
        super(mode == HORIZONTAL ? length : 8, mode == HORIZONTAL ? 8 : length, false);
        this.min = min;
        this.max = max;
        this.mode = mode;
        setValue(startValue);
    }

    @Override
    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        if(!disabled) update(x, y, inputHandler);
        else nanoVGHelper.setAlpha(vg, 0.5f);
        if (dragging) {
            inputHandler.stopBlockingInput();
        }

        if(mode == HORIZONTAL) {
            nanoVGHelper.drawRoundedRect(vg, x, y + 2, width, height - 4, Colors.GRAY_300, 3f);
            nanoVGHelper.drawRoundedRect(vg, x, y + 2, width * value, height - 4, Colors.PRIMARY_500, 3f);
            nanoVGHelper.drawRoundedRect(vg, currentDragPoint - dragPointerSize / 2, y - 8 + 6, 12, 12, Colors.WHITE, 12f);
        } else {
            nanoVGHelper.drawRoundedRect(vg, x + 2, y, width - 4, height, Colors.GRAY_300, 3f);
            nanoVGHelper.drawRoundedRect(vg, x + 2, y + height - height * value, width - 4, height * value, Colors.PRIMARY_500, 3f);
            nanoVGHelper.drawRoundedRect(vg, x - 8 + 6, currentDragPoint - dragPointerSize / 2, 12, 12, Colors.WHITE, 12f);
        }
        nanoVGHelper.setAlpha(vg, 1f);
        if (dragging) {
            inputHandler.blockAllInput();
        }
    }

    public void update(float x, float y, InputHandler inputHandler) {
        super.update(x, y, inputHandler);
        boolean isMouseDown = Platform.getMousePlatform().isButtonDown(0);
        final boolean hovered;
        if (mode == HORIZONTAL) hovered = inputHandler.isAreaHovered(x - 6, y - 3, width + 12, height + 6);
        else hovered = inputHandler.isAreaHovered(x - 3, y - 6, width + 6, height + 12);

        if (hovered && isMouseDown && !mouseWasDown) dragging = true;
        mouseWasDown = isMouseDown;
        if (dragging) {
            if(mode == HORIZONTAL) value = (inputHandler.mouseX() - x) / width;
            else value = (inputHandler.mouseY() - y) / height;
        }
        if (dragging && inputHandler.isClicked(true)) {
            dragging = false;
            inputHandler.stopBlockingInput();
            if(mode == HORIZONTAL) value = (inputHandler.mouseX() - x) / width;
            else value = (inputHandler.mouseY() - y) / height;
        }

        if (value < 0) value = 0;
        if (value > 1) value = 1;

        if (mode == HORIZONTAL) currentDragPoint = x + (width - dragPointerSize) * value;
        else currentDragPoint = y + (height - dragPointerSize) * value;

    }

    public static float invert(float max, float value) {
        return max - value;
    }

    public float getValueInverted() {
        return invert(1, value) * (max - min) + min;
    }

    public float getValue() {
        return value * (max - min) + min;
    }

    public void setValue(float value) {
        this.value = (value - min) / (max - min);
    }

    public void setValueInverted(float value) {
        setValue(invert(max, value));
    }

    public boolean isDragging() {
        return dragging;
    }
}
