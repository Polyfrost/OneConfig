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

package cc.polyfrost.oneconfig.gui.animations;

public class EaseOutBump extends Animation {
    private static final double CONSTANT_1 = 1.7;
    private static final double CONSTANT_2 = 2.7;

    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public EaseOutBump(int duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
    }

    // Courtesy of https://easings.net/
    @Override
    protected float animate(float x) {
        // return x == 0 ? 0 : (float) (x == 1 ? 1 : Math.pow(2, -0.5 * x) * Math.sin((x * 100 - 2) * c4) + 1);
        return (float) (1 + CONSTANT_2 * Math.pow(x-1, 3) + CONSTANT_1 * 1.2 * Math.pow(x-1, 2));
    }
}
