/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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
import org.polyfrost.oneconfig.libs.universal.UScreen;
import org.polyfrost.polyui.input.Translator;
import org.polyfrost.polyui.property.Settings;

/**
 * A class containing utility methods for working with GuiScreens.
 */
public final class GuiUtils {
    public static final Translator translator = new Translator(new Settings(), "");

    /**
     * Displays a screen after a tick, preventing mouse sync issues.
     *
     * @param screen the screen to display.
     * @deprecated Not actually deprecated, but should not be used as is not type-checked.
     */
    @ApiStatus.Internal
    public static void displayScreen(Object screen) {
        displayScreen(((GuiScreen) screen));
    }

    /**
     * Displays a screen after a tick, preventing mouse sync issues.
     *
     * @param screen the screen to display.
     */
    public static void displayScreen(GuiScreen screen) {
        //noinspection ConstantConditions
        displayScreen(screen, 1);
    }

    /**
     * Displays a screen after the specified amount of ticks.
     *
     * @param screen the screen to display.
     * @param ticks  the amount of ticks to wait for before displaying the screen.
     */
    public static void displayScreen(GuiScreen screen, int ticks) {
        new TickDelay(ticks, () -> UScreen.displayScreen(screen));
    }


    /**
     * Close the current open GUI screen.
     */
    public static void closeScreen() {
        UScreen.displayScreen(null);
    }
}
