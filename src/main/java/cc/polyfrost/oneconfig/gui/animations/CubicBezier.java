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

public class CubicBezier extends Animation {
    private static final float CubicErrorBound = 0.001f;
    protected final float P0;
    protected final float P1;
    protected final float P2;
    protected final float P3;

    /**
     * @param P0       First control point X
     * @param P1       First control point Y
     * @param P2       Second control point X
     * @param P3       Second control point Y
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public CubicBezier(float P0, float P1, float P2, float P3, float duration, float start, float end, boolean reverse) {
        super(duration, start, end, reverse);
        this.P0 = P0;
        this.P1 = P1;
        this.P2 = P2;
        this.P3 = P3;
    }

    /**
     * This constructor should be used with JDT's getCubicBezier method
     *
     * @param points   The control points
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public CubicBezier(float[] points, float duration, float start, float end, boolean reverse) {
        this(points[0], points[1], points[2], points[3], duration, start, end, reverse);
    }

    /**
     * Adapted from <a href="https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/animation/animation-core/src/commonMain/kotlin/androidx/compose/animation/core/Easing.kt">Android</a>
     * Under Apache 2.0 license
     *
     * @param x The current point
     * @return The value
     */
    @Override
    protected float animate(float x) {
        float start = 0.0f;
        float end = 1.0f;
        while (true) {
            float midpoint = (start + end) / 2;
            float estimate = evaluateCubic(P0, P2, midpoint);
            if (Math.abs(x - estimate) < CubicErrorBound)
                return evaluateCubic(P1, P3, midpoint);
            if (estimate < x) start = midpoint;
            else end = midpoint;
        }
    }

    /**
     * Adapted from <a href="https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/animation/animation-core/src/commonMain/kotlin/androidx/compose/animation/core/Easing.kt">Android</a>
     * Under Apache 2.0 license
     */
    private float evaluateCubic(float a, float b, float m) {
        return 3 * a * (1 - m) * (1 - m) * m +
                3 * b * (1 - m) * /*    */ m * m +
                /*                      */ m * m * m;
    }
}
