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

package org.polyfrost.oneconfig.api.platform.v1.internal;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.event.v1.EventDelay;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform;
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;

public class ScreenPlatformImpl implements ScreenPlatform {
    private final int[] fbWidth = new int[1];
    private final int[] winWidth = new int[1];
    private volatile float cachedPixelRatio = -1.0f;

    public ScreenPlatformImpl() {
        org.polyfrost.oneconfig.api.event.v1.EventManager.register(
                org.polyfrost.oneconfig.api.event.v1.events.ResizeEvent.class,
                e -> cachedPixelRatio = -1.0f
        );
    }

    @Override
    public int viewportWidth() {
        return Minecraft.getInstance().getWindow().getWidth();
    }

    @Override
    public int viewportHeight() {
        return Minecraft.getInstance().getWindow().getHeight();
    }

    @Override
    public int windowWidth() {
        return Minecraft.getInstance().getWindow().getScreenWidth();
    }

    @Override
    public int windowHeight() {
        return Minecraft.getInstance().getWindow().getScreenHeight();
    }

    @Override
    public int guiWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    @Override
    public int guiHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    // On macOS, glfwGetWindowContentScale == framebufferSize / windowSize (e.g. 2.0 on Retina).
    // On Windows, they differ: framebuffer == window (ratio 1.0), but contentScale reflects DPI (e.g. 1.5).
    // Using contentScale as pixelRatio on Windows caused the UI to be rendered at the wrong size (#478).
    // Fix: compute the actual framebuffer-to-window ratio directly from GLFW, which is correct on all platforms.
    // See also: https://github.com/glfw/glfw/pull/2457
    @Override
    public float pixelRatio() {
        float cached = cachedPixelRatio;
        if (cached > 0.0f) return cached;
        long handle = Platform.compatibility().windowHandle();
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(handle, fbWidth, null);
        org.lwjgl.glfw.GLFW.glfwGetWindowSize(handle, winWidth, null);
        if (winWidth[0] > 0) {
            float ratio = (float) fbWidth[0] / winWidth[0];
            cachedPixelRatio = ratio;
            return ratio;
        }
        return 1.0f;
    }

    @Override
    public void display(@Nullable Object screen, int ticks) {
        if (screen instanceof ComposeScreen && !SkiaCtx.INSTANCE.isReady()) {
            warnUiUnavailable();
            return;
        }
        //? if >= 26.2 {
        /*// 26.2 removed Minecraft#setScreen. Use Gui#setScreen, not setScreenAndShow (which force-calls
        // renderFrame and re-enters our renderFrame mixin -> nested frame -> 1-frame black flash).
        if (ticks < 1) Minecraft.getInstance().gui.setScreen((Screen) screen);
        else EventDelay.tick(ticks, () -> Minecraft.getInstance().gui.setScreen((Screen) screen));
        *///?} else {
        if (ticks < 1) Minecraft.getInstance().setScreen((Screen) screen);
        else EventDelay.tick(ticks, () -> Minecraft.getInstance().setScreen((Screen) screen));
        //?}
    }

    private void warnUiUnavailable() {
        String reason = SkiaCtx.INSTANCE.unavailableReason();
        if (reason == null) return;
        //~ if >= 26.2 'gui.getChat' -> 'gui.hud.getChat'
        Minecraft.getInstance().gui.getChat()
                //~ if >= 26.1 'addMessage' -> 'addClientSystemMessage'
                .addClientSystemMessage(Component.literal(reason).withStyle(ChatFormatting.RED));
    }

    @Override
    @SuppressWarnings("unchecked" /*, reason = "reduces friction between versions" */)
    public <T> @Nullable T current() {
        //~ if >= 26.2 '.screen' -> '.gui.screen()'
        return (T) Minecraft.getInstance().screen;
    }

}
