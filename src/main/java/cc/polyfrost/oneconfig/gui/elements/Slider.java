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
    private final float min, max;
    protected float value;
    protected float currentDragPoint;
    protected float dragPointerSize = 8f;
    private boolean dragging = false;
    private boolean mouseWasDown = false;

    public Slider(int length, float min, float max, float startValue) {
        super(length, 8, false);
        this.min = min;
        this.max = max;
        setValue(startValue);
    }

    @Override
    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        if(!disabled) update(x, y, inputHandler);
        else nanoVGHelper.setAlpha(vg, 0.5f);

        nanoVGHelper.drawRoundedRect(vg, x, y + 2, width, height - 4, Colors.GRAY_300, 3f);
        nanoVGHelper.drawRoundedRect(vg, x, y + 2, width * value, height - 4, Colors.PRIMARY_500, 3f);
        nanoVGHelper.drawRoundedRect(vg, currentDragPoint - dragPointerSize / 2, y - 8, 24, 24, Colors.WHITE, 12f);
        nanoVGHelper.setAlpha(vg, 1f);
    }

    public void update(float x, float y, InputHandler inputHandler) {
        super.update(x, y, inputHandler);
        boolean isMouseDown = Platform.getMousePlatform().isButtonDown(0);
        boolean hovered = inputHandler.isAreaHovered(x - 6, y - 3, width + 12, height + 6);
        if (hovered && isMouseDown && !mouseWasDown) dragging = true;
        mouseWasDown = isMouseDown;
        if (dragging) {
            value = (inputHandler.mouseX() - x) / width;
        }
        if (dragging && inputHandler.isClicked(true)) {
            dragging = false;
            value = (inputHandler.mouseX() - x) / width;
        }

        if (value < 0) value = 0;
        if (value > 1) value = 1;

        currentDragPoint = x + (width - dragPointerSize) * value;

    }

    public float getValue() {
        return value * (max - min) + min;
    }

    public void setValue(float value) {
        this.value = (value - min) / (max - min);
    }

    public boolean isDragging() {
        return dragging;
    }
}
