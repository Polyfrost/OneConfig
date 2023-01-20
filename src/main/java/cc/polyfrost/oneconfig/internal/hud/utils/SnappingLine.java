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

package cc.polyfrost.oneconfig.internal.hud.utils;

import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;

import java.awt.*;

public class SnappingLine {
    private static final int COLOR = new Color(138, 43, 226).getRGB();
    private final float line;
    private final float distance;
    private final float position;

    public SnappingLine(float line, float left, float size, boolean multipleSides) {
        this.line = line;
        float center = left + size / 2f;
        float right = left + size;
        float leftDistance = Math.abs(line - left);
        float centerDistance = Math.abs(line - center);
        float rightDistance = Math.abs(line - right);
        if (!multipleSides || leftDistance <= centerDistance && leftDistance <= rightDistance) {
            distance = leftDistance;
            position = line;
        } else if (centerDistance <= rightDistance) {
            distance = centerDistance;
            position = line - size / 2f;
        } else {
            distance = rightDistance;
            position = line - size;
        }
    }

    public void drawLine(long vg, float lineWidth, boolean isX) {
        float pos = (float) (line * UResolution.getScaleFactor() - lineWidth / 2f);
        if (isX) {
            NanoVGHelper.INSTANCE.drawLine(vg, pos, 0, pos, UResolution.getWindowHeight(), lineWidth, COLOR);
        } else {
            NanoVGHelper.INSTANCE.drawLine(vg, 0, pos, UResolution.getWindowWidth(), pos, lineWidth, COLOR);
        }
    }

    public float getPosition() {
        return position;
    }

    public float getDistance() {
        return distance;
    }
}
