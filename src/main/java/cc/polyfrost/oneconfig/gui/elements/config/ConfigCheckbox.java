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

import cc.polyfrost.oneconfig.config.annotations.Checkbox;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.animations.quad.EaseInOutQuad;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;

import java.awt.*;
import java.lang.reflect.Field;

public class ConfigCheckbox extends BasicOption {
    private final ColorAnimation color = new ColorAnimation(ColorPalette.SECONDARY);
    private Animation animation;

    public ConfigCheckbox(Field field, Object parent, String name, String description, String category, String subcategory, int size) {
        super(field, parent, name, description, category, subcategory, size);
    }

    public static ConfigCheckbox create(Field field, Object parent) {
        Checkbox checkbox = field.getAnnotation(Checkbox.class);
        return new ConfigCheckbox(field, parent, checkbox.name(), checkbox.description(), checkbox.category(), checkbox.subcategory(), checkbox.size());
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        if (!isEnabled()) nanoVGHelper.setAlpha(vg, 0.5f);
        boolean toggled = false;
        try {
            toggled = (boolean) get();
            if (animation == null) animation = new DummyAnimation(toggled ? 1 : 0);
        } catch (IllegalAccessException ignored) {
        }
        boolean hover = inputHandler.isAreaHovered(x, y + 4, 24, 24);

        boolean clicked = inputHandler.isClicked() && hover;
        if (clicked && isEnabled()) {
            toggled = !toggled;
            animation = new EaseInOutQuad(100, 0, 1, !toggled);
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        float percentOn = animation.get();

        nanoVGHelper.drawText(vg, name, x + 32, y + 17, nameColor, 14f, Fonts.MEDIUM);

        nanoVGHelper.drawRoundedRect(vg, x, y + 4, 24, 24, color.getColor(hover, hover && Platform.getMousePlatform().isButtonDown(0)), 6f);
        nanoVGHelper.drawHollowRoundRect(vg, x, y + 4, 23.5f, 23.5f, Colors.GRAY_300, 6f, 1f);        // the 0.5f is to make it look better ok

        nanoVGHelper.drawRoundedRect(vg, x, y + 4, 24, 24, ColorUtils.setAlpha(Colors.PRIMARY_500, (int) (percentOn * 255)), 6f);
        nanoVGHelper.drawSvg(vg, SVGs.CHECKBOX_TICK, x, y + 4, 24, 24, new Color(1f, 1f, 1f, percentOn).getRGB());

        if (toggled && hover)
            nanoVGHelper.drawHollowRoundRect(vg, x - 1, y + 3, 24, 24, Colors.PRIMARY_600, 6f, 2f);
        nanoVGHelper.setAlpha(vg, 1f);
    }

    @Override
    protected float getNameX(int x) {
        return x + 32;
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
