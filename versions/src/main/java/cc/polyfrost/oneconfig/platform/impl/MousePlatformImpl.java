/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.platform.MousePlatform;
//#if MC>=11600
//$$ import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
//$$ import cc.polyfrost.oneconfig.internal.mixin.MouseAccessor;
//$$ import org.lwjgl.glfw.GLFW;
//#else
import org.lwjgl.input.Mouse;
//#endif

public class MousePlatformImpl implements MousePlatform {

    //#if MC>=11600
    //$$ private double prevScroll = 0;
    //#endif

    @Override
    public double getMouseX() {
        //#if MC>=11600
        //$$ return UMinecraft.getMinecraft().mouseHelper.getMouseX();
        //#else
        return Mouse.getX();
        //#endif
    }

    @Override
    public double getMouseY() {
        //#if MC>=11600
        //$$ return UMinecraft.getMinecraft().mouseHelper.getMouseY();
        //#else
        return Mouse.getY();
        //#endif
    }

    @Override
    public double getDWheel() {
        //#if MC>=11600
        //$$ double scrollDelta = ((MouseAccessor) UMinecraft.getMinecraft().mouseHelper).getEventDeltaWheel();
        //$$ double amount = scrollDelta - prevScroll;
        //$$ prevScroll = scrollDelta;
        //$$ return amount;
        //#else
        return Mouse.getDWheel();
        //#endif
    }

    @Override
    public double getMouseDX() {
        //#if MC>=11600
            //#if FORGE==1
            //$$ return UMinecraft.getMinecraft().mouseHelper.getXVelocity();
            //#else
            //$$ return ((MouseAccessor) UMinecraft.getMinecraft().mouse).getCursorDeltaX();
            //#endif
        //#else
        return Mouse.getDX();
        //#endif
    }

    @Override
    public double getMouseDY() {
        //#if MC>=11600
            //#if FORGE==1
            //$$ return UMinecraft.getMinecraft().mouseHelper.getYVelocity();
            //#else
            //$$ return ((MouseAccessor) UMinecraft.getMinecraft().mouse).getCursorDeltaY();
            //#endif
        //#else
        return Mouse.getDY();
        //#endif
    }

    @Override
    public int getButtonState(int button) {
        //#if MC>=11600
        //$$ return GLFW.glfwGetMouseButton(UMinecraft.getMinecraft().getMainWindow().getHandle(), button);
        //#else
        return Mouse.isButtonDown(button) ? 1 : 0;
        //#endif
    }

    @Override
    public boolean isButtonDown(int button) {
        //#if MC>=11600
        //$$ return GLFW.glfwGetMouseButton(UMinecraft.getMinecraft().getMainWindow().getHandle(), button) == GLFW.GLFW_PRESS;
        //#else
        return Mouse.isButtonDown(button);
        //#endif
    }
}
