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

import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseOutQuad;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.asset.SVG;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.util.LinkedHashMap;
import java.util.Map;

public class Dropdown extends BasicButton {
    // why a linked hash map, I hear you ask? well, so it can be iterated in the order it was added
    private final LinkedHashMap<String, ColorAnimation> opts;
    private int selected;
    private /* lateinit */ float optsWidth = -1;
    private float rotation;
    private final float xPadding, fontSize;
    private EaseOutQuad anim = null;

    private InputHandler inputHandler;

    public Dropdown(int width, int size, int xPadding, String[] opts, int selected, ColorPalette palette) {
        super(width, size, (size - 8) / 2, 8, xPadding, opts[selected], null, SVGs.CHEVRON_DOWN, ALIGNMENT_LEFT, palette);
        this.xPadding = xPadding;
        this.selected = selected;
        this.opts = new LinkedHashMap<>(opts.length);
        for (String opt : opts) {
            this.opts.put(opt, new ColorAnimation(palette == ColorPalette.TERTIARY ? ColorPalette.SECONDARY : palette));
        }
        this.fontSize = size == SIZE_48 ? 20 : (float) (size / 2 - 4);
    }


    @Override
    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        this.inputHandler = inputHandler;
        if (optsWidth == -1) {              // lateinit
            for (String s : opts.keySet()) {
                optsWidth = Math.max(optsWidth, nanoVGHelper.getTextWidth(vg, s, fontSize, Fonts.REGULAR));
            }
            // just in case the options are hella short
            optsWidth = Math.max(optsWidth, width);
            width = (int) optsWidth;
        }
        if (toggled) {
            inputHandler.stopBlockingInput();
        }
        if (opts.size() != 1) {
            if (anim != null) {
                final float val = anim.get();
                rotation = val / ((opts.size() - 1) * fontSize * 2) * 180;
                nanoVGHelper.drawRoundedRect(vg, x, y + height, optsWidth, val, colorPalette == ColorPalette.TERTIARY ? ColorPalette.SECONDARY.getNormalColor() : currentColor, 8);
                if (anim.isFinished() && !anim.isReversed() && toggled) {
                    float i = y + height;
                    int index = 0;
                    for (Map.Entry<String, ColorAnimation> entry : opts.entrySet()) {
                        final String s = entry.getKey();
                        final ColorAnimation anim = entry.getValue();
                        if (index == selected) {
                            index++;
                            continue;
                        }

                        final boolean hovered = inputHandler.isAreaHovered(x, i, optsWidth, 24);
                        final boolean clicked = hovered && inputHandler.isClicked(true);
                        nanoVGHelper.drawRoundedRect(vg, x, i, optsWidth, fontSize * 2, anim.getColor(hovered, clicked), 8);
                        nanoVGHelper.drawText(vg, s, x + xPadding, i + fontSize, Colors.WHITE_90, fontSize, Fonts.REGULAR);
                        if (clicked) {
                            // yes I could just add a tracker variable but those 32 bytes are worth it
                            selected = index;
                            onChange(selected);
                            super.text = s;
                            toggled = false;
                        }
                        index ++;
                        i += fontSize * 2;
                    }
                }
            }
        }
        super.draw(vg, x, y, inputHandler);
        if (toggled) {
            inputHandler.blockAllInput();
        }
    }

    @Override
    public void setToggled(boolean toggled) {
        super.setToggled(toggled);
        if (!toggled) {
            inputHandler.blockAllInput();
        }
    }

    @Override
    public void drawIcon(long vg, SVG icon, float x, float y, float width, float height, int color) {
        if(rotation == 0) {
            super.drawIcon(vg, icon, x, y, width, height, color);
            return;
        }
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        // this rotates the object around the center of the icon
        final float xx = x + width / 2f;
        final float yy = y + height / 2f;
        nanoVGHelper.translate(vg, xx, yy);
        nanoVGHelper.rotate(vg, rotation);
        nanoVGHelper.drawSvg(vg, icon, -(width / 2), -(height / 2), width, height, color);
        nanoVGHelper.rotate(vg, -rotation);
        nanoVGHelper.translate(vg, -xx, -yy);
    }

    @Override
    public void update(float x, float y, InputHandler inputHandler) {
        super.update(x, y, inputHandler);
        if(inputHandler.isClicked() && !inputHandler.isAreaHovered(x, y, optsWidth, height) && toggled) {
            toggled = false;
            inputHandler.stopBlockingInput();
        }
        if (toggled && anim == null) {
            anim = new EaseOutQuad(200, 0, (opts.size() - 1) * fontSize * 2, false);
        }
        if (anim != null && anim.isFinished() && anim.isReversed()) {
            anim = null;
        }
        if (!toggled && anim != null && !anim.isReversed()) {
            anim = new EaseOutQuad(200, 0, (opts.size() - 1) * fontSize * 2, true);
        }
    }


    /**
     * return the selected item as an index of the options array
     */
    public int getSelected() {
        return selected;
    }

    /**
     * select an option by index
     */
    public void select(int index) {
        this.selected = index;
        super.text = (String) opts.keySet().toArray()[index];
        onChange(index);
    }

    /** Method called when the selected option changes */
    public void onChange(int changedTo) {}
}