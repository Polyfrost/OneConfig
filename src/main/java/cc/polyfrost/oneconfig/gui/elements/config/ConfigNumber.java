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

import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.text.NumberInputField;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;

import java.lang.reflect.Field;

public class ConfigNumber extends BasicOption {
    private final NumberInputField inputField;
    private boolean isFloat = true;

    public ConfigNumber(Field field, Object parent, String name, String description, String category, String subcategory, float min, float max, int step) {
        super(field, parent, name, description, category, subcategory, 2);
        this.inputField = new NumberInputField(84, 32, 0, min, max, step);
    }

    public static ConfigNumber create(Field field, Object parent) {
        Number slider = field.getAnnotation(Number.class);
        return new ConfigNumber(field, parent, slider.name(), slider.description(), slider.category(), slider.subcategory(), slider.min(), slider.max(), slider.step());
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        float value = 0;
        inputField.disable(!isEnabled());
        if (!isEnabled()) nanoVGHelper.setAlpha(vg, 0.5f);
        if (inputField.isToggled() || inputField.arrowsClicked()) {
            value = inputField.getCurrentValue();
        }
        if (inputField.isToggled() || inputField.arrowsClicked()) {
            setValue(value);
        }

        if (!inputField.isToggled()) {
            try {
                Object object = get();
                if (object instanceof Integer)
                    isFloat = false;
                if (isFloat) value = (float) object;
                else value = (int) object;
            } catch (IllegalAccessException ignored) {
            }
        }
        if (!inputField.isToggled()) inputField.setCurrentValue(value);

        nanoVGHelper.drawText(vg, name, x, y + 17, nameColor, 14f, Fonts.MEDIUM);
        inputField.draw(vg, x + 892, y, inputHandler);
        nanoVGHelper.setAlpha(vg, 1f);
    }

    private void setValue(float value) {
        try {
            if (isFloat) set(value);
            else set(Math.round(value));
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        inputField.keyTyped(key, keyCode);
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    protected boolean shouldDrawDescription() {
        return super.shouldDrawDescription() && !inputField.isToggled();
    }
}
