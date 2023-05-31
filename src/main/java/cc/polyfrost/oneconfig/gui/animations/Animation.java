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

import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

public abstract class Animation {
    protected final boolean reverse;
    protected final float duration;
    protected final float start;
    protected final float change;
    protected float timePassed = 0;

    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param end      The end of the animation
     * @param reverse  Reverse the animation
     */
    public Animation(float duration, float start, float end, boolean reverse) {
        this.duration = duration;
        if (reverse) {
            float temp = start;
            start = end;
            end = temp;
        }
        this.start = start;
        this.change = end - start;
        this.reverse = reverse;
    }

    /**
     * @param deltaTime The time since the last frame
     * @return The new value
     */
    public float get(float deltaTime) {
        timePassed += deltaTime;
        if (timePassed >= duration) return start + change;
        return animate(timePassed / duration) * change + start;
    }

    /**
     * @return The new value
     */
    public float get() {
        return get(GuiUtils.getDeltaTime());
    }

    /**
     * @return If the animation is finished or not
     */
    public boolean isFinished() {
        return timePassed >= duration;
    }

    /**
     * @return If the animation is reversed
     */
    public boolean isReversed() {
        return reverse;
    }

    /**
     * @return The start position of the animation
     */
    public float getStart() {
        return start;
    }

    /**
     * @return The end position of the animation
     */
    public float getEnd() {
        return start + change;
    }

    protected abstract float animate(float x);
}
