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
import org.polyfrost.oneconfig.api.event.v1.events.FramebufferRenderEvent;
import org.polyfrost.oneconfig.api.event.v1.events.MainMenuFpsEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen;
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class MainMenuFpsSampler {
    private static final String POLYPLUS_PACKAGE = "org.polyfrost.polyplus.";

    private static final long MAX_TICK_GAP_NANOS = 250L * 1_000_000L;

    private static final long FPS_SETTLE_NANOS = 2_000L * 1_000_000L;

    private static volatile boolean sampling = false;

    private boolean started = false;
    private long lastTickNanos = 0L;
    private long elapsedNanos = 0L;
    private long settledNanos = 0L;
    private long fpsSum = 0L;
    private int sampleCount = 0;

    private MainMenuFpsSampler() {
    }

    public static boolean isSampling() {
        return sampling;
    }

    public static boolean isAnySampling() {
        if (sampling) return true;
        MethodHandle polyPlus = PolyPlusSampler.IS_SAMPLING;
        if (polyPlus == null) return false;
        try {
            return (boolean) polyPlus.invokeExact();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isMainMenu(Object screen) {
        return !hasOverlay() && (screen instanceof TitleScreen || screen instanceof ComposeScreen && screen.getClass().getName().startsWith(POLYPLUS_PACKAGE));
    }

    private static boolean hasOverlay() {
        //? if >= 26.2 {
        return Minecraft.getInstance().gui.overlay() != null;
        //?} else if > 1.12.2 {
        /*return Minecraft.getInstance().getOverlay() != null;
        *///?} else
        //return false;
    }

    public static void init() {
        MainMenuFpsSampler sampler = new MainMenuFpsSampler();
        EventHandler.ofRemoving(FramebufferRenderEvent.End.class, e -> sampler.tick()).register();
    }

    private boolean tick() {
        Object screen = Platform.screen().current();
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        long gap = Math.min(now - lastTickNanos, MAX_TICK_GAP_NANOS);
        lastTickNanos = now;

        if (started && mc.level != null) return finish();

        if (!mc.isWindowActive()) {
            sampling = false; // don't fight power saving while nobody is looking
            elapsedNanos = 0L;
            fpsSum = 0L;
            sampleCount = 0;
            settledNanos = 0L;
            return false;
        }

        if (!started) {
            if (!(screen instanceof TitleScreen) || hasOverlay()) return false;
            started = true;
            elapsedNanos = 0L;
            gap = 0L;
        }
        boolean representative = !OneConfigUIScreen.warmBusy() && !hasOverlay() && !(screen instanceof LevelLoadingScreen)
                && (!(screen instanceof ComposeScreen) || screen.getClass().getName().startsWith(POLYPLUS_PACKAGE));
        sampling = representative;
        if (!representative) {
            settledNanos = 0L;
            return false;
        }
        if (settledNanos < FPS_SETTLE_NANOS) {
            settledNanos += gap;
            return false;
        }
        elapsedNanos += gap;
        fpsSum += Math.max(0, Platform.compatibility().fps());
        sampleCount++;

        if (elapsedNanos / 1_000_000L >= MainMenuFpsEvent.WINDOW_MILLIS) return finish();
        return false;
    }

    private static final class PolyPlusSampler {
        static final MethodHandle IS_SAMPLING = find();

        private static MethodHandle find() {
            try {
                Class<?> owner = Class.forName(POLYPLUS_PACKAGE + "client.features.AdaptiveBlurDefaults", false, MainMenuFpsSampler.class.getClassLoader());
                return MethodHandles.publicLookup().findStatic(owner, "isSampling", MethodType.methodType(boolean.class));
            } catch (Throwable t) {
                return null;
            }
        }
    }

    private boolean finish() {
        sampling = false;
        if (sampleCount == 0) {
            started = false;
            return false;
        }
        EventManager.INSTANCE.post(new MainMenuFpsEvent((float) fpsSum / sampleCount, sampleCount, elapsedNanos / 1_000_000L));
        return true;
    }
}
