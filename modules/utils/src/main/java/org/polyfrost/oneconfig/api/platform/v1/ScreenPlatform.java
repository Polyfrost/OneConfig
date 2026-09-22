/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.platform.v1;

import org.jetbrains.annotations.Nullable;

public interface ScreenPlatform {

    /** Runs an action on the thread which owns the game's UI. */
    default void runOnUiThread(Runnable action) {
        action.run();
    }

    int viewportWidth();

    int viewportHeight();

    int windowWidth();

    int windowHeight();

    int guiWidth();

    int guiHeight();

    default float pixelRatio() {
        return (float) viewportWidth() / windowWidth();
    }

    default float surfaceRatio() {
        int win = windowWidth();
        return win > 0 ? (float) viewportWidth() / win : 1f;
    }

    /**
     * Return a scaling factor from the Minecraft scaled coordinate system to standard <b>window coordinates</b>
     * <br>
     * These window/screen coordinates are used by mouse and input as well as resizing
     * <br>
     * See the inverse {@link #screenToMcScale()}
     */
    default float mcToScreenScale() {
        return Platform.compatibility().options().getGuiScale() / surfaceRatio();
    }

    /**
     * Return a scaling factor from the standard window coordinates to Minecraft scaled coordinate space
     * <br>
     * These coordinates can be used in Minecraft rendering methods so they render at the correct place
     * <br>
     * See the inverse {@link #mcToScreenScale()}
     * @implNote same as {@code 1f / }{@link  #mcToScreenScale()}
     */
    default float screenToMcScale() {
        return surfaceRatio() / Platform.compatibility().options().getGuiScale();
    }

    default void showMessage(String message) {
    }

    void display(@Nullable Object screen, int frames);

    default void display(Object screen) {
        display(screen, 1);
    }

    default void close() {
        display(null, 0);
    }

    @Nullable
    <T> T current();
}
