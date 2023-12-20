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

import java.util.regex.Pattern;

public class TextRenderer {
    private static final Pattern regex = Pattern.compile("(?i)\u00A7[0-9a-f]");
    private static boolean drawingBorder = false;

    public static int drawBorderedText(String text, float x, float y, int color, int opacity) {
        String noColors = regex.matcher(text).replaceAll("\u00A7r");
        drawingBorder = true;
        int yes = 0;
        if (opacity / 4 > 3) {
            for (int xOff = -2; xOff <= 2; xOff++) {
                for (int yOff = -2; yOff <= 2; yOff++) {
                    if (xOff * xOff != yOff * yOff) {
                        yes += Platform.getGLPlatform().drawText(
                                noColors, (xOff / 2f) + x, (yOff / 2f) + y, (opacity / 4) << 24, false
                        );
                    }
                }
            }
        }
        yes += (int) Platform.getGLPlatform().drawText(text, x, y, color, false);
        drawingBorder = false;
        return yes;
    }

    public static float getStringWidth(String text) {
        return Platform.getGLPlatform().getStringWidth(text);
    }

    public static void drawScaledString(String text, float x, float y, int color, TextType type, float scale) {
        UGraphics.GL.pushMatrix();
        UGraphics.GL.scale(scale, scale, 1);
        switch (type) {
            case NONE:
                Platform.getGLPlatform().drawText(text, x * (1 / scale), y * (1 / scale), color, false);
                break;
            case SHADOW:
                Platform.getGLPlatform().drawText(text, x * (1 / scale), y * (1 / scale), color, true);
                break;
            case FULL:
                drawBorderedText(text, x * (1 / scale), y * (1 / scale), color, 255);
                break;
        }
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
