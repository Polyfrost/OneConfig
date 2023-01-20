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

package cc.polyfrost.oneconfig.internal.renderer;

import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorHelper;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides an easy way to manage and group scissor rectangles.
 */
public class ScissorHelperImpl implements ScissorHelper {
    private final List<List<Scissor>> previousScissors = new ArrayList<>();
    private List<Scissor> scissors = new ArrayList<>();

    /**
     * Adds and applies a scissor rectangle to the list of scissor rectangles.
     *
     * @param vg     The NanoVG context.
     * @param x      The x coordinate of the scissor rectangle.
     * @param y      The y coordinate of the scissor rectangle.
     * @param width  The width of the scissor rectangle.
     * @param height The height of the scissor rectangle.
     * @return The scissor rectangle.
     */
    public Scissor scissor(long vg, float x, float y, float width, float height) {
        Scissor scissor = new Scissor(x, y, width, height);
        if (scissors.contains(scissor)) return scissor;
        scissors.add(scissor);
        applyScissors(vg);
        return scissor;
    }

    /**
     * Resets the scissor rectangle provided.
     *
     * @param vg      The NanoVG context.
     * @param scissor The scissor rectangle to reset.
     */
    public void resetScissor(long vg, Scissor scissor) {
        if (scissors.contains(scissor)) {
            scissors.remove(scissor);
            applyScissors(vg);
        }
    }

    /**
     * Clear all scissor rectangles.
     *
     * @param vg The NanoVG context.
     */
    public void clearScissors(long vg) {
        scissors.clear();
        NanoVG.nvgResetScissor(vg);
    }

    /**
     * Save the current scissors
     */
    @Override
    public void save() {
        previousScissors.add(new ArrayList<>(scissors));
    }

    /**
     * Restore the scissors from the last save
     *
     * @param vg The NanoVG context.
     */
    @Override
    public void restore(long vg) {
        scissors = previousScissors.remove(0);
        applyScissors(vg);
    }

    private void applyScissors(long vg) {
        NanoVG.nvgResetScissor(vg);
        if (scissors.size() == 0) return;
        Scissor finalScissor = getFinalScissor(scissors);
        NanoVG.nvgScissor(vg, finalScissor.x, finalScissor.y, finalScissor.width, finalScissor.height);
    }

    private Scissor getFinalScissor(List<Scissor> scissors) {
        Scissor finalScissor = new Scissor(scissors.get(0));
        for (int i = 1; i < scissors.size(); i++) {
            Scissor scissor = scissors.get(i);
            float rightX = Math.min(scissor.x + scissor.width, finalScissor.x + finalScissor.width);
            float rightY = Math.min(scissor.y + scissor.height, finalScissor.y + finalScissor.height);
            finalScissor.x = Math.max(finalScissor.x, scissor.x);
            finalScissor.y = Math.max(finalScissor.y, scissor.y);
            finalScissor.width = rightX - finalScissor.x;
            finalScissor.height = rightY - finalScissor.y;
        }
        return finalScissor;
    }
}
