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

package cc.polyfrost.oneconfig.gui.animations;

import cc.polyfrost.oneconfig.utils.color.ColorPalette;

public class ColorAnimation {
    private ColorPalette palette;
    private final int duration;
    /**
     * 0 = nothing
     * 1 = hovered
     * 2 = pressed
     * 3 = color palette changed
     */
    private int prevState = 0;
    private Animation redAnimation;
    private Animation greenAnimation;
    private Animation blueAnimation;
    private Animation alphaAnimation;

    public ColorAnimation(ColorPalette palette, int duration) {
        this.palette = palette;
        this.duration = duration;
        redAnimation = new DummyAnimation(palette.getNormalColorf()[0]);
        greenAnimation = new DummyAnimation(palette.getNormalColorf()[1]);
        blueAnimation = new DummyAnimation(palette.getNormalColorf()[2]);
        alphaAnimation = new DummyAnimation(palette.getNormalColorf()[3]);
    }

    public ColorAnimation(ColorPalette palette) {
        this(palette, 100);
    }

    /**
     * Return the current color at the current time, according to a EaseInOut quadratic animation.
     *
     * @param hovered the hover state of the element
     * @param pressed the pressed state of the element
     * @return the current color
     */
    public int getColor(boolean hovered, boolean pressed) {
        int state = pressed ? 2 : hovered ? 1 : 0;
        if (state != prevState) {
            float[] newColors = pressed ? palette.getPressedColorf() : hovered ? palette.getHoveredColorf() : palette.getNormalColorf();
            redAnimation = new EaseInOutQuad(duration, redAnimation.get(), newColors[0], false);
            greenAnimation = new EaseInOutQuad(duration, greenAnimation.get(), newColors[1], false);
            blueAnimation = new EaseInOutQuad(duration, blueAnimation.get(), newColors[2], false);
            alphaAnimation = new EaseInOutQuad(duration, alphaAnimation.get(), newColors[3], false);
            prevState = state;
        }
        return ((int) (alphaAnimation.get() * 255) << 24) | ((int) (redAnimation.get() * 255) << 16) | ((int) (greenAnimation.get() * 255) << 8) | ((int) (blueAnimation.get() * 255));
    }

    /**
     * Return the current alpha of the color. This method is used to get the alpha of pressed buttons that have text/icons on them, so they also darken accordingly.
     */
    public float getAlpha() {
        return alphaAnimation.get(0);
    }

    public ColorPalette getPalette() {
        return palette;
    }

    public void setPalette(ColorPalette palette) {
        if (this.palette.equals(palette)) return;
        this.palette = palette;
        prevState = 3;
    }

    public void setColors(float[] colors) {
        redAnimation = new DummyAnimation(colors[0]);
        greenAnimation = new DummyAnimation(colors[1]);
        blueAnimation = new DummyAnimation(colors[2]);
        alphaAnimation = new DummyAnimation(colors[3]);
    }
}
