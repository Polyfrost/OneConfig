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

package cc.polyfrost.oneconfig.renderer;

import cc.polyfrost.oneconfig.libs.universal.UGraphics;
import cc.polyfrost.oneconfig.platform.Platform;

public class TextRenderer {
    private static boolean drawingBorder = false;

    public static int drawBorderedText(String text, float x, float y, int color, int opacity) {
        drawingBorder = true;
        int yes = (int) Platform.getGLPlatform().drawText(text, x, y, (color & 0xFFFFFF) | (opacity << 24), TextType.FULL);
        drawingBorder = false;
        return yes;
    }

    public static float getStringWidth(String text) {
        return Platform.getGLPlatform().getStringWidth(text);
    }

    public static void drawScaledString(String text, float x, float y, int color, TextType type, float scale) {
        UGraphics.GL.pushMatrix();
        UGraphics.GL.scale(scale, scale, 1);
        Platform.getGLPlatform().drawText(text, x * (1 / scale), y * (1 / scale), color, type);
        UGraphics.GL.popMatrix();
    }

    public static boolean isDrawingTextBorder() {
        return drawingBorder;
    }

    public enum TextType {
        NONE, SHADOW, FULL;

        public static TextType toType(int type) {
            return values()[type];
        }
    }
}
