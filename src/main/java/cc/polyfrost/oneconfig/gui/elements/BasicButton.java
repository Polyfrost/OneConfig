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

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.pages.Page;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.asset.SVG;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import org.jetbrains.annotations.NotNull;

public class BasicButton extends BasicElement {

    protected String text;
    protected SVG icon1, icon2;
    private final int alignment;
    private final float fontSize, cornerRadius;
    private final float xSpacing, xPadding;
    private final int iconSize;
    public float x, y;
    public static final int ALIGNMENT_LEFT = 0;
    public static final int ALIGNMENT_CENTER = 2;
    public static final int ALIGNMENT_JUSTIFIED = 3;

    public static final int SIZE_32 = 32;
    public static final int SIZE_36 = 36;
    public static final int SIZE_40 = 40;
    public static final int SIZE_48 = 48;
    private boolean toggleable = false;
    private Page page;
    private Runnable runnable;

    public BasicButton(int width, int size, int iconSize, int xSpacing, int xPadding, String text, SVG icon1, SVG icon2, int align, @NotNull ColorPalette colorPalette) {
        super(width, 32, colorPalette, true);
        if (text != null) this.text = text;
        if (icon1 != null) this.icon1 = icon1;
        if (icon2 != null) this.icon2 = icon2;
        this.colorPalette = colorPalette;
        this.alignment = align;
        this.cornerRadius = size == SIZE_48 ? 14f : 10f; // radius was originally 16f and 12f respectively, decreased both by two.
        // SIZE_48 doesn't seem to be used anywhere, so I'm not sure if this is correct.
        this.xSpacing = xSpacing;
        this.xPadding = xPadding;
        this.height = size;
        this.iconSize = iconSize;
        this.fontSize = size == SIZE_48 ? 20 : (float) (size / 2 - 4);
    }

    public BasicButton(int width, int size, String text, SVG icon1, SVG icon2, int align, @NotNull ColorPalette colorPalette) {
        this(width, size, size / 2, size == SIZE_48 ? 12 : 8, (size == SIZE_36 || size == SIZE_40) ? 16 : size == SIZE_48 ? 20 : 12, text, icon1, icon2, align, colorPalette);
    }

    public BasicButton(int width, int size, SVG icon, int align, @NotNull ColorPalette colorPalette) {
        this(width, size, null, icon, null, align, colorPalette);
    }

    public BasicButton(int width, int size, String text, int align, @NotNull ColorPalette colorPalette) {
        this(width, size, text, null, null, align, colorPalette);
    }

    @Override
    public void draw(long vg, float x, float y, InputHandler inputHandler) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        this.x = x;
        this.y = y;
        this.update(x, y, inputHandler);
        if (disabled) nanoVGHelper.setAlpha(vg, 0.5f);
        float contentWidth = 0f;
        int color;
        if (colorPalette == ColorPalette.TERTIARY || colorPalette == ColorPalette.TERTIARY_DESTRUCTIVE) {
            color = currentColor;
        } else {
            nanoVGHelper.drawRoundedRect(vg, x, y, this.width, this.height, currentColor, this.cornerRadius);
            color = ColorUtils.setAlpha(Colors.WHITE, (int) (colorAnimation.getAlpha() * 255));
        }
        final float middle = x + width / 2f;
        final float middleYIcon = y + height / 2f - iconSize / 2f;
        final float middleYText = y + height / 2f + fontSize / 8f;
        if (this.text != null) {
            contentWidth += nanoVGHelper.getTextWidth(vg, text, fontSize, Fonts.MEDIUM);
        }
        if (alignment == ALIGNMENT_CENTER) {
            if (icon1 != null && icon2 == null && text == null) {
                drawIcon(vg, icon1, middle - iconSize / 2f, middleYIcon, iconSize, iconSize, color);
            } else {
                if (icon1 != null)
                    contentWidth += iconSize + xSpacing;
                if (icon2 != null)
                    contentWidth += iconSize + xSpacing;
                if (text != null)
                    nanoVGHelper.drawText(vg, text, middle - contentWidth / 2 + (icon1 == null ? 0 : iconSize + xSpacing), middleYText, color, fontSize, Fonts.MEDIUM);
                if (icon1 != null)
                    drawIcon(vg, icon1, middle - contentWidth / 2, middleYIcon, iconSize, iconSize, color);
                if (icon2 != null)
                    drawIcon(vg, icon2, middle + contentWidth / 2 - iconSize, middleYIcon, iconSize, iconSize, color);
            }
        } else if (alignment == ALIGNMENT_JUSTIFIED) {
            if (text != null)
                nanoVGHelper.drawText(vg, text, middle - contentWidth / 2, middleYText, color, fontSize, Fonts.MEDIUM);
            if (icon1 != null)
                drawIcon(vg, icon1, x + xPadding, middleYIcon, iconSize, iconSize, color);
            if (icon2 != null)
                drawIcon(vg, icon2, x + width - xPadding - iconSize, middleYIcon, iconSize, iconSize, color);
        } else if (alignment == ALIGNMENT_LEFT) {
            contentWidth = xPadding;
            if (icon1 != null) {
                drawIcon(vg, icon1, x + contentWidth, middleYIcon, iconSize, iconSize, color);
                contentWidth += iconSize + xSpacing;
            }
            if (text != null) {
                nanoVGHelper.drawText(vg, text, x + contentWidth, middleYText, color, fontSize, Fonts.MEDIUM);
            }
            if (icon2 != null)
                drawIcon(vg, icon2, x + width - xPadding - iconSize, middleYIcon, iconSize, iconSize, color);
        }
        if (disabled) nanoVGHelper.setAlpha(vg, 1f);
    }

    /**
     * Override this method to perform transformations on the icon before it is drawn. <br>
     * Make sure to reset your transformations afterwards.
     */
    protected void drawIcon(long vg, SVG icon, float x, float y, float width, float height, int color) {
        NanoVGHelper.INSTANCE.drawSvg(vg, icon, x, y, width, height, color);
    }

    @Override
    public void onClick() {
        if (disabled) return;
        if (this.page != null && OneConfigGui.INSTANCE != null) {
            OneConfigGui.INSTANCE.openPage(page);
        } else if (this.runnable != null) {
            runnable.run();
        }
        if (toggleable && toggled) setColorPalette(ColorPalette.PRIMARY);
        else if (toggleable) setColorPalette(ColorPalette.SECONDARY);
    }

    @Override
    public void setToggled(boolean toggled) {
        this.toggled = toggled;
        if (toggled && toggleable) setColorPalette(ColorPalette.PRIMARY);
        else if (toggleable) setColorPalette(ColorPalette.SECONDARY);
    }

    public void setToggleable(boolean state) {
        this.toggleable = state;
    }

    public void setClickAction(Page page) {
        this.page = page;
    }

    public void setClickAction(Runnable runnable) {
        this.runnable = runnable;
    }

    public Page getPage() {
        return page;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setLeftIcon(SVG icon) {
        icon1 = icon;
    }

    public void setRightIcon(SVG icon) {
        icon2 = icon;
    }

    public boolean hasClickAction() {
        return page != null || runnable != null;
    }
}
