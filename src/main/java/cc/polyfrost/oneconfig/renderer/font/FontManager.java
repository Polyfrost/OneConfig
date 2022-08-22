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

package cc.polyfrost.oneconfig.renderer.font;

import cc.polyfrost.oneconfig.utils.IOUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import static org.lwjgl3.nanovg.NanoVG.nvgCreateFontMem;

public class FontManager {
    public static FontManager INSTANCE = new FontManager();

    /**
     * Load all fonts in the Fonts class
     *
     * @param vg NanoVG context
     */

    public void initialize(long vg) {
        for (Field field : Fonts.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object font = field.get(null);
                if (!(font instanceof Font)) continue;
                loadFont(vg, (Font) font);
            } catch (Exception e) {
                throw new RuntimeException("Could not initialize fonts");
            }
        }
    }

    /**
     * Load a font into NanoVG
     *
     * @param vg   NanoVG context
     * @param font The font to be loaded
     */
    public void loadFont(long vg, Font font) {
        if (font.isLoaded()) return;
        int loaded = -1;
        try {
            ByteBuffer buffer = IOUtils.resourceToByteBuffer(font.getFileName());
            loaded = nvgCreateFontMem(vg, font.getName(), buffer, 0);
            font.setBuffer(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (loaded == -1) {
            throw new RuntimeException("Failed to initialize font " + font.getName());
        } else {
            font.setLoaded(true);
        }
    }
}
