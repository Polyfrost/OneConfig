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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.event.v1.EventDelay;
import org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform;

public class ScreenPlatformImpl implements ScreenPlatform {
    //#if MC > 1.13
    //$$ private final int[] fbWidth = new int[1];
    //$$ private final int[] winWidth = new int[1];
    //#endif

    @Override
    public int viewportWidth() {
        //#if MC>=11502
        //$$ return Minecraft.getInstance().getWindow().getWidth();
        //#else
        return Minecraft.getMinecraft().displayWidth;
        //#endif
    }

    @Override
    public int viewportHeight() {
        //#if MC>=11502
        //$$ return Minecraft.getInstance().getWindow().getHeight();
        //#else
        return Minecraft.getMinecraft().displayHeight;
        //#endif
    }

    @Override
    public int windowWidth() {
        //#if MC>=11502
        //$$ return Minecraft.getInstance().getWindow().getScreenWidth();
        //#else
        return (int) (Minecraft.getMinecraft().displayWidth / org.lwjgl.opengl.Display.getPixelScaleFactor());
        //#endif
    }

    @Override
    public int windowHeight() {
        //#if MC>=11502
        //$$ return Minecraft.getInstance().getWindow().getScreenHeight();
        //#else
        return (int) (Minecraft.getMinecraft().displayHeight / org.lwjgl.opengl.Display.getPixelScaleFactor());
        //#endif
    }

    // On macOS, glfwGetWindowContentScale == framebufferSize / windowSize (e.g. 2.0 on Retina).
    // On Windows, they differ: framebuffer == window (ratio 1.0), but contentScale reflects DPI (e.g. 1.5).
    // Using contentScale as pixelRatio on Windows caused the UI to be rendered at the wrong size (#478).
    // Fix: compute the actual framebuffer-to-window ratio directly from GLFW, which is correct on all platforms.
    // See also: https://github.com/glfw/glfw/pull/2457
    @Override
    public float pixelRatio() {
        //#if MC > 1.13
        //$$ long handle = Minecraft.getInstance().getWindow().getWindow();
        //$$ org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(handle, fbWidth, null);
        //$$ org.lwjgl.glfw.GLFW.glfwGetWindowSize(handle, winWidth, null);
        //$$ if (winWidth[0] > 0) {
        //$$     return (float) fbWidth[0] / winWidth[0];
        //$$ }
        //$$ return 1.0f;
        //#else
        return org.lwjgl.opengl.Display.getPixelScaleFactor();
        //#endif
    }

    @Override
    public void display(@Nullable Object screen, int ticks) {
        if (ticks < 1) Minecraft.getMinecraft().displayGuiScreen((GuiScreen) screen);
        else EventDelay.tick(ticks, () -> Minecraft.getMinecraft().displayGuiScreen((GuiScreen) screen));
    }

    @Override
    @SuppressWarnings("unchecked" /*, reason = "reduces friction between versions" */)
    public <T> @Nullable T current() {
        return (T) Minecraft.getMinecraft().currentScreen;
    }

}
