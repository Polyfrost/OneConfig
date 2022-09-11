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

import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuad;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.lang.reflect.Field;

public class ConfigSwitch extends BasicOption {
    private ColorAnimation color;
    private Animation animation;

    public ConfigSwitch(Field field, Object parent, String name, String description, String category, String subcategory, int size) {
        super(field, parent, name, description, category, subcategory, size);
    }

    public static ConfigSwitch create(Field field, Object parent) {
        Switch options = field.getAnnotation(Switch.class);
        return new ConfigSwitch(field, parent, options.name(), options.description(), options.category(), options.subcategory(), options.size());
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        boolean toggled = false;
        try {
            toggled = (boolean) get();
            if (animation == null) {
                animation = new DummyAnimation(toggled ? 1 : 0);
                color = new ColorAnimation(toggled ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
            }
        } catch (IllegalAccessException ignored) {
        }
        float percentOn = animation.get();
        int x2 = x + 3 + (int) (percentOn * 18);
        boolean hovered = inputHandler.isAreaHovered(x, y, 42, 32);
        if (!isEnabled()) RenderManager.setAlpha(vg, 0.5f);
        RenderManager.drawRoundedRect(vg, x, y + 4, 42, 24, color.getColor(hovered, hovered && Platform.getMousePlatform().isButtonDown(0)), 12f);
        RenderManager.drawRoundedRect(vg, x2, y + 7, 18, 18, Colors.WHITE, 9f);
        RenderManager.drawText(vg, name, x + 50, y + 17, nameColor, 14f, Fonts.MEDIUM);

        if (inputHandler.isAreaClicked(x, y, 42, 32) && isEnabled()) {
            toggled = !toggled;
            animation = new EaseInOutQuad(200, 0, 1, !toggled);
            color.setPalette(toggled ? ColorPalette.PRIMARY : ColorPalette.SECONDARY);
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        RenderManager.setAlpha(vg, 1f);
    }

    @Override
    protected int getNameX(int x) {
        return x + 50;
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
