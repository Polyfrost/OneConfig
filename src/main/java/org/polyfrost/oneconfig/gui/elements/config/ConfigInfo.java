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

package org.polyfrost.oneconfig.gui.elements.config;

import org.polyfrost.oneconfig.config.annotations.Info;
import org.polyfrost.oneconfig.config.data.InfoType;
import org.polyfrost.oneconfig.config.elements.BasicOption;
import org.polyfrost.oneconfig.renderer.NanoVGHelper;
import org.polyfrost.oneconfig.renderer.font.Fonts;
import org.polyfrost.oneconfig.renderer.scissor.Scissor;
import org.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import org.polyfrost.oneconfig.utils.InputHandler;

import java.lang.reflect.Field;

public class ConfigInfo extends BasicOption {
    private final InfoType type;

    public ConfigInfo(Field field, Object parent, String name, String category, String subcategory, int size, InfoType type) {
        super(field, parent, name, "", category, subcategory, size);
        this.type = type;
    }

    public static ConfigInfo create(Field field, Object parent) {
        Info info = field.getAnnotation(Info.class);
        return new ConfigInfo(field, parent, info.text(), info.category(), info.subcategory(), info.size(), info.type());
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        ScissorHelper scissorHelper = ScissorHelper.INSTANCE;

        Scissor scissor = scissorHelper.scissor(vg, x, y, size == 1 ? 448 : 960, 32);
        nanoVGHelper.drawInfo(vg, type, x, y + 4, 24);
        nanoVGHelper.drawText(vg, name, x + 32, y + 18, nameColor, 14, Fonts.MEDIUM);
        scissorHelper.resetScissor(vg, scissor);
    }

    @Override
    public int getHeight() {
        return 32;
    }
}
