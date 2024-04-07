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

package org.polyfrost.oneconfig.internal.platform;

import org.polyfrost.oneconfig.libs.universal.UMouse;
import org.polyfrost.oneconfig.platform.MousePlatform;
//#if MC>=11600
//$$ import org.polyfrost.oneconfig.libs.universal.UMinecraft;
//$$ import org.lwjgl.glfw.GLFW;
//#else
import org.lwjgl.input.Mouse;
//#endif

public class MousePlatformImpl implements MousePlatform {

    @Override
    public double getMouseX() {
        return UMouse.Raw.getX();
    }

    @Override
    public double getMouseY() {
        return UMouse.Raw.getY();
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
