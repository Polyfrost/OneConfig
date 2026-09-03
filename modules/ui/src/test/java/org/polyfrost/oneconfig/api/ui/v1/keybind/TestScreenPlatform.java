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

package org.polyfrost.oneconfig.api.ui.v1.keybind;

import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform;

public class TestScreenPlatform implements ScreenPlatform {
    private static Object current;

    public static void setCurrent(@Nullable Object screen) {
        current = screen;
    }

    @Override
    public int viewportWidth() {
        return 1920;
    }

    @Override
    public int viewportHeight() {
        return 1080;
    }

    @Override
    public int windowWidth() {
        return 1920;
    }

    @Override
    public int windowHeight() {
        return 1080;
    }

    @Override
    public int guiWidth() {
        return 1920;
    }

    @Override
    public int guiHeight() {
        return 1080;
    }

    @Override
    public void display(@Nullable Object screen, int frames) {
        current = screen;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T current() {
        return (T) current;
    }
}
