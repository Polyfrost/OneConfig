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

import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ConfigButton extends BasicOption {
    private final BasicButton button;

    public ConfigButton(Runnable runnable, Object parent, String name, String description, String category, String subcategory, int size, String text) {
        super(null, parent, name, description, category, subcategory, size);
        this.button = new BasicButton(size == 1 ? 128 : 256, 32, text, BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY);
        this.button.setClickAction(runnable);
    }

    public ConfigButton(Field field, Object parent, String name, String description, String category, String subcategory, int size, String text) {
        super(field, parent, name, description, category, subcategory, size);
        this.button = new BasicButton(size == 1 ? 128 : 256, 32, text, BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY);
        this.button.setClickAction(getRunnableFromField(field, parent));
    }

    public ConfigButton(Method method, Object parent, String name, String description, String category, String subcategory, int size, String text) {
        super(null, parent, name, description, category, subcategory, size);
        this.button = new BasicButton(size == 1 ? 128 : 256, 32, text, BasicButton.ALIGNMENT_CENTER, ColorPalette.PRIMARY);
        this.button.setClickAction(() -> {
            try {
                method.invoke(parent);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("wrong number of arguments")) {
                    throw new IllegalArgumentException("Button method " + method.getDeclaringClass().getName() + "." + method.getName() + "(" + Arrays.toString(method.getGenericParameterTypes()) + ") must take no arguments!");
                } else e.printStackTrace();
            }
        });
    }

    public static ConfigButton create(Field field, Object parent) {
        Button button = field.getAnnotation(Button.class);
        return new ConfigButton(field, parent, button.name(), button.description(), button.category(), button.subcategory(), button.size(), button.text());
    }

    public static ConfigButton create(Method method, Object parent) {
        method.setAccessible(true);
        Button button = method.getAnnotation(Button.class);
        return new ConfigButton(method, parent, button.name(), button.description(), button.category(), button.subcategory(), button.size(), button.text());
    }

    private static Runnable getRunnableFromField(Field field, Object parent) {
        Runnable runnable = () -> {
        };
        try {
            runnable = (Runnable) field.get(parent);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return runnable;
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        button.disable(!isEnabled());
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        if (!isEnabled()) nanoVGHelper.setAlpha(vg, 0.5f);
        nanoVGHelper.drawText(vg, name, x, y + 17, nameColor, 14f, Fonts.MEDIUM);
        button.draw(vg, x + (size == 1 ? 352 : 736), y, inputHandler);
        nanoVGHelper.setAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
