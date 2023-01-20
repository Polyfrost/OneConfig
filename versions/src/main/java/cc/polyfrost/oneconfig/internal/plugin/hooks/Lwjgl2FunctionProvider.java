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

package cc.polyfrost.oneconfig.internal.plugin.hooks;
//#if MC<=11202
import cc.polyfrost.oneconfig.internal.OneConfig;
import org.lwjgl.system.FunctionProvider;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Taken from LWJGLTwoPointFive under The Unlicense
 * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
 */
public class Lwjgl2FunctionProvider implements FunctionProvider {
    private static final Class<?> GL_CONTEXT;
    private final Method getFunctionAddress;

    static {
        String libraryPath = System.getProperty("oneconfig.lwjgl2.librarypath", "");
        if (!libraryPath.isEmpty()) {
            System.setProperty("org.lwjgl.librarypath", libraryPath);
        }

        try {
            GL_CONTEXT = Class.forName("org.lwjgl.opengl.GLContext", true, OneConfig.class.getClassLoader());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public Lwjgl2FunctionProvider() {
        try {
            getFunctionAddress = GL_CONTEXT.getDeclaredMethod("getFunctionAddress", String.class);
            getFunctionAddress.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Error initializing LWJGL2FunctionProvider", exception);
        }
    }

    @Override
    public long getFunctionAddress(CharSequence functionName) {
        try {
            return (long) getFunctionAddress.invoke(
                    null,
                    functionName.toString()
            );
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public long getFunctionAddress(ByteBuffer byteBuffer) {
        throw new UnsupportedOperationException(
                "LWJGL 2 does not support this method"
        );
    }
}
//#endif