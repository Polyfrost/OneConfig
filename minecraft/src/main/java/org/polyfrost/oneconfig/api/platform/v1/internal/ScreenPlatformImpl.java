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

import dev.deftu.omnicore.client.render.OmniMatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.event.v1.EventDelay;
import org.polyfrost.oneconfig.api.platform.v1.ScreenPlatform;

public class ScreenPlatformImpl implements ScreenPlatform {
    //#if MC > 1.13
    //$$ private final float[] pixelScaleFactor = new float[1];
    //#endif

    private OmniMatrixStack smuggled = new OmniMatrixStack();

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

    @Override
    public float pixelRatio() {
        // asm: considerably more reliable than just doing viewport / window
        //#if MC > 1.13
        //$$ org.lwjgl.glfw.GLFW.glfwGetWindowContentScale(Minecraft.getInstance().getWindow().getWindow(), pixelScaleFactor, null);
        //$$ return pixelScaleFactor[0];
        //#else
        return org.lwjgl.opengl.Display.getPixelScaleFactor();
        //#endif
    }

    @Override
    public void setSmuggledMatrixStack(OmniMatrixStack stack) {
        if (stack == null) return;
        this.smuggled = stack;
    }

    @Override
    public OmniMatrixStack getSmuggledMatrixStack() {
        return smuggled;
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
