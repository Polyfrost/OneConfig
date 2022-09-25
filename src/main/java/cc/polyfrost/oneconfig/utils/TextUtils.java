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

package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.renderer.LwjglManager;

import cc.polyfrost.oneconfig.renderer.font.Font;

import java.util.ArrayList;

/**
 * Simple text utility class for NanoVG text rendering.
 */
public final class TextUtils {

    /**
     * Wraps a string into an array of lines.
     *
     * @param vg       The NanoVG context.
     * @param text     The text to wrap.
     * @param maxWidth The maximum width of each line.
     * @param fontSize The font size.
     * @param font     The font to use.
     * @return The array of lines.
     */
    public static ArrayList<String> wrapText(long vg, String text, float maxWidth, float fontSize, Font font) {
        ArrayList<String> wrappedText = new ArrayList<>();
        text += " ";
        int prevIndex = 0;
        for (int i = text.indexOf(" "); i >= 0; i = text.indexOf(" ", i + 1)) {
            String textPart = text.substring(0, i);
            float textWidth = LwjglManager.INSTANCE.getNanoVGHelper().getTextWidth(vg, textPart, fontSize, font);
            if (textWidth < maxWidth) {
                prevIndex = i;
                continue;
            }
            wrappedText.add(text.substring(0, prevIndex) + " ");
            wrappedText.addAll(wrapText(vg, text.substring(prevIndex + 1), maxWidth, fontSize, font));
            break;
        }
        if (wrappedText.size() == 0) wrappedText.add(text);
        String temp = wrappedText.get(wrappedText.size() - 1);
        if (temp.length() != 0) {
            wrappedText.remove(wrappedText.size() - 1);
            wrappedText.add(temp.substring(0, temp.length() - 1));
        }
        return wrappedText;
    }
}
