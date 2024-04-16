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

package org.polyfrost.oneconfig.utils;

import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.PlatformDeclaration;
import org.polyfrost.oneconfig.libs.universal.UScreen;

/**
 * A class containing utility methods for working with GuiScreens.
 */
@PlatformDeclaration
public final class GuiUtils {

    // correct signature for platform compatibility
    @ApiStatus.Internal
    public static void displayScreen(Object o) {
        displayScreen((GuiScreen) o);
    }

    /**
     * Displays a screen after a tick, preventing mouse sync issues.
     *
     * @param screen the screen to display.
     */
    public static void displayScreen(GuiScreen screen) {
        displayScreen(screen, 1);
    }

    /**
     * Displays a screen after the specified amount of ticks.
     *
     * @param screen the screen to display.
     * @param ticks  the amount of ticks to wait for before displaying the screen.
     */
    public static void displayScreen(GuiScreen screen, int ticks) {
        if (ticks < 1) UScreen.displayScreen(screen);
        else TickDelay.of(ticks, () -> UScreen.displayScreen(screen));
    }


    /**
     * Close the current open GUI screen.
     */
    public static void closeScreen() {
        displayScreen(null, 0);
    }
}
