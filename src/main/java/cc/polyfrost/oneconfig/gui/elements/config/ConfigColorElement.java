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

package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.gui.elements.ColorSelector;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.Images;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;

import java.lang.reflect.Field;

public class ConfigColorElement extends BasicOption {
    private final TextInputField hexField = new TextInputField(104, 32, "", false, false);
    private final TextInputField alphaField = new TextInputField(72, 32, "", false, false);
    private final BasicElement element = new BasicElement(64, 32, false);
    private boolean open = false;
    private final boolean allowAlpha;

    public ConfigColorElement(Field field, Object parent, String name, String description, String category, String subcategory, int size, boolean allowAlpha) {
        super(field, parent, name, description, category, subcategory, size);
        hexField.setCentered(true);
        alphaField.setCentered(true);
        alphaField.onlyAcceptNumbers(true);
        this.allowAlpha = allowAlpha;
    }

    public static ConfigColorElement create(Field field, Object parent) {
        Color color = field.getAnnotation(Color.class);
        return new ConfigColorElement(field, parent, color.name(), color.description(), color.category(), color.subcategory(), color.size(), color.allowAlpha());
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        if(OneConfigGui.INSTANCE == null) return;
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        if (!isEnabled()) nanoVGHelper.setAlpha(vg, 0.5f);
        hexField.disable(!isEnabled());
        alphaField.disable(!isEnabled() || !allowAlpha);
        element.disable(!isEnabled());

        int x1 = size == 1 ? x : x + 512;
        OneColor color;
        try {
            color = (OneColor) get();
        } catch (IllegalAccessException e) {
            return;
        }
        nanoVGHelper.drawText(vg, name, x, y + 16, nameColor, 14f, Fonts.MEDIUM);
        if (!hexField.isToggled()) hexField.setInput("#" + color.getHex());
        hexField.setErrored(false);
        if (hexField.isToggled()) {
            try {
                color.setColorFromHex(hexField.getInput());
            } catch (NumberFormatException e) {
                hexField.setErrored(true);
            }
        }
        hexField.draw(vg, x1 + 224, y, inputHandler);

        if (!alphaField.isToggled()) alphaField.setInput(Math.round(color.getAlpha() / 2.55f) + "%");
        alphaField.setErrored(false);
        if (alphaField.isToggled()) {
            try {
                float input = Float.parseFloat(alphaField.getInput().replace("%", ""));
                if (input < 0f) {
                    alphaField.setErrored(true);
                    input = 100f;
                }
                if (input > 100f) {
                    alphaField.setErrored(true);
                    input = 100f;
                }
                color = new OneColor((float) color.getHue(), color.getSaturation(), color.getBrightness(), Math.round(input * 2.55f));
            } catch (NumberFormatException e) {
                alphaField.setErrored(true);
            }
        }
        alphaField.draw(vg, x1 + 336, y, inputHandler);

        element.update(x1 + 416, y, inputHandler);
        nanoVGHelper.drawHollowRoundRect(vg, x1 + 415, y - 1, 64, 32, Colors.GRAY_300, 12f, 2f);
        nanoVGHelper.drawRoundImage(vg, Images.ALPHA_GRID.filePath, x1 + 420, y + 4, 56, 24, 8f);
        nanoVGHelper.drawRoundedRect(vg, x1 + 420, y + 4, 56, 24, color.getRGB(), 8f);
        if (element.isClicked() && !open) {
            open = true;
            OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(color, inputHandler.mouseX(), inputHandler.mouseY(), allowAlpha, inputHandler));
        }
        if (OneConfigGui.INSTANCE.currentColorSelector == null) open = false;
        else if (open) color = (OneConfigGui.INSTANCE.getColor());
        setColor(color);
        nanoVGHelper.setAlpha(vg, 1f);
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        alphaField.keyTyped(key, keyCode);
        hexField.keyTyped(key, keyCode);
    }

    protected void setColor(OneColor color) {
        try {
            set(color);
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
