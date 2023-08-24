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

package org.polyfrost.oneconfig.renderer.scissor;

import org.polyfrost.oneconfig.renderer.LwjglManager;

/**
 * Provides an easy way to manage and group scissor rectangles.
 */
public interface ScissorHelper {
    @SuppressWarnings("deprecation")
    ScissorHelper INSTANCE = LwjglManager.INSTANCE.getScissorHelper();

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
    Scissor scissor(long vg, float x, float y, float width, float height);

    /**
     * Resets the scissor rectangle provided.
     *
     * @param vg      The NanoVG context.
     * @param scissor The scissor rectangle to reset.
     */
    void resetScissor(long vg, Scissor scissor);

    /**
     * Clear all scissor rectangles.
     *
     * @param vg The NanoVG context.
     */
    void clearScissors(long vg);

    void save();

    void restore(long vg);
}
