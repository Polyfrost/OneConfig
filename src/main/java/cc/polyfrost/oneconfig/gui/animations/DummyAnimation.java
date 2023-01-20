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

import java.util.concurrent.Callable;

public class DummyAnimation extends Animation {
    protected final float value;
    protected Callable<Boolean> done = null;

    /**
     * @param value The value that is returned
     * @param done  A callable that returns if the animation is finished
     */
    public DummyAnimation(float value, Callable<Boolean> done) {
        super(0, value, value, false);
        this.value = value;
        this.done = done;
    }

    /**
     * @param value    The value that is returned
     * @param duration The duration of the animation
     */
    public DummyAnimation(float value, float duration) {
        super(duration, value, value, false);
        this.value = value;
    }

    /**
     * @param value The value that is returned
     */
    public DummyAnimation(float value) {
        this(value, 0);
    }

    @Override
    public float get(float deltaTime) {
        timePassed += deltaTime;
        return value;
    }

    @Override
    public boolean isFinished() {
        if (done != null) {
            try {
                return done.call();
            } catch (Exception ignored) {
            }
        }
        return super.isFinished();
    }

    @Override
    protected float animate(float x) {
        return x;
    }
}
