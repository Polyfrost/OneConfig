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

public class ChainedAnimation extends Animation {
    protected final Animation[] animations;
    private int currentAnimation = 0;
    private float value;
    private float totalDuration = 0;

    public ChainedAnimation(Animation... animations) {
        super(1, 0, 0, false, false);
        this.animations = animations;
    }

    public ChainedAnimation(boolean x, Animation... animations) {
        super(1, 0, 0, false, x);
        this.animations = animations;
        for (Animation animation : animations) {
            totalDuration += animation.duration;
        }
    }

    @Override
    public float get(float deltaTime) {
        timePassed += deltaTime;
        if (currentAnimation >= animations.length) return value;
        value = animations[currentAnimation].get(deltaTime);
        if (animations[currentAnimation].isFinished()) currentAnimation++;
        return value;
    }

    @Override
    public boolean isFinished() {
        return timePassed >= totalDuration;
    }

    @Override
    protected float animate(float x) {
        return 0;
    }

    public float getTimePassed() { return timePassed; }

    public float getDuration() { return totalDuration; }
}
