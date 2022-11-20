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

package cc.polyfrost.oneconfig.gui.animations;

import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

import java.util.stream.Stream;

public enum PageAnimation {
    LEFT(300, 224, 1904, false),
    RIGHT(300, 224, -1904, false),
    UP(200, 72, 200, true),
    DOWN(200, 72, -200, true);

    /**
     * @param duration The duration of the animation
     * @param start    The start of the animation
     * @param offset      The offset of the animation
     */
    PageAnimation(int duration, float start, float offset, boolean isUpDown) {
        this.duration = duration;
        this.change = offset;
        this.isUpDown = isUpDown;
        if (this.isUpDown) {
            this.starts = new float[] {start, start - offset};
            this.ends = new float[] {start - offset, start};
            animQuart = new EaseInOutQuart(duration / 2, this.starts[1], this.ends[1], false);
            animLin = new Linear(duration / 2, this.starts[0], this.ends[0], false);
        } else {
            this.start = start;
            this.animExpo = new EaseOutExpo(300, start + offset, start + offset, false);
        }
    }

    private boolean isUpDown;
    private float change;
    private float start;
    private float duration;
    private float[] starts;
    private float[] ends;
    private Linear animLin;
    private EaseInOutQuart animQuart;
    private EaseOutExpo animExpo;
    protected float timePassed = 0;

    /**
     * @param deltaTime The time since the last frame
     * @return The new value
     */
    public float get(float deltaTime) {
        timePassed += deltaTime;
        if (this.isUpDown) {
            if (timePassed >= duration)  {
                return starts[1] + change;
            }
            if ((timePassed / duration) < 0.5) {
                return animLin.animate(timePassed / duration) * change + starts[0];
            }
            return animQuart.animate(timePassed / duration) * change + starts[1];
        } else {
            if (timePassed >= duration) return start + change;
            return animExpo.animate(timePassed / duration) * change + start;
        }
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
        if (timePassed >= duration) {
            this.reset();
            return true;
        }
        return false;
    }

    /**
     * @return The start position of the animation
     */
    public float[] getStart() {
        return starts;
    }

    /**
     * @return The end position of the animation
     */
    public float[] getEnd() {
        return ends;
    }

    public boolean isLeftRight() {
        return !this.isUpDown;
    }

    public float getTimePassed() {
        return this.timePassed;
    }
    public float getDuration() {
        return this.duration;
    }
    public void reset() {
        this.timePassed = 0;
    }
}
