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

package org.polyfrost.oneconfig.internal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MainMenuFpsEvent;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen;

public final class MainMenuFpsSampler {
    private static final String POLYPLUS_PACKAGE = "org.polyfrost.polyplus.";

    private static volatile boolean sampling = false;

    private boolean started = false;
    private long startNanos = 0L;
    private long fpsSum = 0L;
    private int sampleCount = 0;

    private MainMenuFpsSampler() {
    }

    public static boolean isSampling() {
        return sampling;
    }

    public static void init() {
        MainMenuFpsSampler sampler = new MainMenuFpsSampler();
        EventHandler.ofRemoving(TickEvent.End.class, e -> sampler.tick()).register();
    }

    private boolean tick() {
        Object screen = Platform.screen().current();

        if (!started) {
            if (!(screen instanceof TitleScreen)) return false;
            started = true;
            sampling = true; // lift the menu FPS cap for the duration of the window
            startNanos = System.nanoTime();
        } else if (Minecraft.getInstance().level != null) {
            finish();
            return true;
        }

        boolean representative = !(screen instanceof LevelLoadingScreen)
                && (!(screen instanceof ComposeScreen) || screen.getClass().getName().startsWith(POLYPLUS_PACKAGE));
        if (representative) {
            fpsSum += Math.max(0, Platform.compatibility().fps());
            sampleCount++;
        }

        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        if (elapsedMillis >= MainMenuFpsEvent.WINDOW_MILLIS) {
            finish();
            return true;
        }
        return false;
    }

    private void finish() {
        sampling = false; // restore the normal menu FPS cap
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        float average = sampleCount > 0 ? (float) fpsSum / sampleCount : 0f;
        EventManager.INSTANCE.post(new MainMenuFpsEvent(average, sampleCount, elapsedMillis));
    }
}
