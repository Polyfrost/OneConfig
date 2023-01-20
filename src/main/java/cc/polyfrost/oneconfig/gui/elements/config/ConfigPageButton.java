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

package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.gui.pages.Page;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.lang.reflect.Field;

public class ConfigPageButton extends BasicOption {
    public final Page page;
    public final String description;
    private final ColorAnimation backgroundColor = new ColorAnimation(ColorPalette.SECONDARY);

    public ConfigPageButton(Field field, Object parent, String name, String description, String category, String subcategory, OptionPage page) {
        super(field, parent, name, "", category, subcategory, 2);
        this.description = description;
        this.page = new ModConfigPage(page);
    }

    public ConfigPageButton(Field field, Object parent, String name, String description, String category, String subcategory, Page page) {
        super(field, parent, name, "", category, subcategory, 2);
        this.description = description;
        this.page = page;
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        int height = description.equals("") ? 64 : 96;
        boolean hovered = inputHandler.isAreaHovered(x - 16, y, 1024, height) && isEnabled();
        boolean clicked = hovered && inputHandler.isClicked();
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        if (!isEnabled())
            nanoVGHelper.setAlpha(vg, 0.5f);

        nanoVGHelper.drawRoundedRect(vg, x - 16, y, 1024, height, backgroundColor.getColor(hovered, hovered && Platform.getMousePlatform().isButtonDown(0)), 20);
        nanoVGHelper.drawText(vg, name, x + 10, y + 32, Colors.WHITE_90, 24, Fonts.MEDIUM);
        if (!description.equals(""))
            nanoVGHelper.drawText(vg, description, x + 10, y + 70, Colors.WHITE_90, 14, Fonts.MEDIUM);
        nanoVGHelper.drawSvg(vg, SVGs.CARET_RIGHT, x + 981f, y + (description.equals("") ? 20f : 36f), 13, 22);

        if (clicked)
            OneConfigGui.INSTANCE.openPage(page);
        nanoVGHelper.setAlpha(vg, 1f);
    }

    @Override
    public int getHeight() {
        return description.equals("") ? 64 : 96;
    }
}
