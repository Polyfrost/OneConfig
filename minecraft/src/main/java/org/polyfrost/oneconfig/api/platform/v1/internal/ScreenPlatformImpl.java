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
import org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform;
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen;
import org.polyfrost.oneconfig.internal.ui.compose.ComposeSupport;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;

public class ScreenPlatformImpl implements ScreenPlatform {
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
    // Fix: compute the actual framebuffer-to-window ratio directly, which is correct on all platforms.
    // See also: https://github.com/glfw/glfw/pull/2457
    @Override
    public float pixelRatio() {
        int win = windowWidth();
        if (win > 0) return (float) viewportWidth() / win;
        return 1.0f;
    }

    @Override
    public void display(@Nullable Object screen, int ticks) {
        if (screen instanceof ComposeScreen) {
            String unavailable = ComposeSupport.INSTANCE.unavailableReason();
            if (unavailable != null) {
                showMessage(unavailable);
                return;
            }
            if (!SkiaCtx.INSTANCE.isReady()) {
                showMessage(SkiaCtx.INSTANCE.unavailableReason());
                return;
            }
        }
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().schedule(() -> display(screen, ticks));
            return;
        }
        //? if >= 26.2 {
        // 26.2 removed Minecraft#setScreen. Use Gui#setScreen, not setScreenAndShow (which force-calls
        // renderFrame and re-enters our renderFrame mixin -> nested frame -> 1-frame black flash).
        if (ticks < 1) Minecraft.getInstance().gui.setScreen((Screen) screen);
        else EventDelay.tick(ticks, () -> Minecraft.getInstance().gui.setScreen((Screen) screen));
        //?} else {
        /*if (ticks < 1) Minecraft.getInstance().setScreen((Screen) screen);
        else EventDelay.tick(ticks, () -> Minecraft.getInstance().setScreen((Screen) screen));
        *///?}
    }

    @Override
    public void showMessage(@Nullable String message) {
        if (message == null || message.isEmpty()) return;
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().schedule(() -> showMessage(message));
            return;
        }
        if (Minecraft.getInstance().gui == null) return;
        //~ if >= 26.2 'gui.getChat' -> 'gui.hud.getChat'
        var chat = Minecraft.getInstance().gui.hud.getChat();
        if (chat == null) return;
        //~ if >= 26.1 'addMessage' -> 'addClientSystemMessage'
        chat.addClientSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    @Override
    @SuppressWarnings("unchecked" /*, reason = "reduces friction between versions" */)
    public <T> @Nullable T current() {
        //~ if >= 26.2 '.screen' -> '.gui.screen()'
        return (T) Minecraft.getInstance().gui.screen();
    }

}
