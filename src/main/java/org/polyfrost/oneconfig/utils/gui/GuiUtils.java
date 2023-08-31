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

package org.polyfrost.oneconfig.utils.gui;

import org.polyfrost.oneconfig.events.EventManager;
import org.polyfrost.oneconfig.events.event.RenderEvent;
import org.polyfrost.oneconfig.events.event.Stage;
import org.polyfrost.oneconfig.events.event.TimerUpdateEvent;
import org.polyfrost.oneconfig.libs.eventbus.Subscribe;
import org.polyfrost.oneconfig.libs.universal.UMinecraft;
import org.polyfrost.oneconfig.platform.Platform;
import org.polyfrost.oneconfig.utils.TickDelay;

/**
 * A class containing utility methods for working with GuiScreens.
 */
public final class GuiUtils {
    private static long time = -1L;
    private static long deltaTime = 17L;
    private static boolean wasMouseDown = false;

    static {
        EventManager.INSTANCE.register(new GuiUtils());
    }

    /**
     * Displays a screen after a tick, preventing mouse sync issues.
     *
     * @param screen the screen to display.
     */
    @Deprecated
    public static void displayScreen(Object screen) {
        displayScreen(screen, 1);
    }

    /**
     * Displays a screen after the specified amount of ticks.
     *
     * @param screen the screen to display.
     * @param ticks  the amount of ticks to wait for before displaying the screen.
     */
    public static void displayScreen(Object screen, int ticks) {
        new TickDelay(ticks, () -> Platform.getGuiPlatform().setCurrentScreen(screen));
    }

    /**
     * Close the current open GUI screen.
     */
    public static void closeScreen() {
        Platform.getGuiPlatform().setCurrentScreen(null);
    }

    /**
     * Gets the delta time (in milliseconds) between frames.
     * <p><b>
     * Not to be confused with Minecraft deltaTicks / renderPartialTicks, which can be gotten via
     * {@link TimerUpdateEvent}
     * </b></p>
     *
     * @return the delta time.
     */
    public static float getDeltaTime() {
        return deltaTime;
    }

    /**
     * @return If the mouse was down last frame
     */
    public static boolean wasMouseDown() {
        return wasMouseDown;
    }

    @Subscribe
    private void onRenderEvent(RenderEvent event) {
        if (event.stage == Stage.START) {
            if (time == -1) time = UMinecraft.getTime();
            else {
                long currentTime = UMinecraft.getTime();
                deltaTime = currentTime - time;
                time = currentTime;
            }
        } else if (event.stage == Stage.END) {
            wasMouseDown = Platform.getMousePlatform().isButtonDown(0);
        }
    }
}
