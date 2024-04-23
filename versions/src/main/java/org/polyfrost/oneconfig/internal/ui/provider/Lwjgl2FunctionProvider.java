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

package org.polyfrost.oneconfig.internal.ui.provider;
//#if MC<=11202

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.FunctionProvider;
import org.polyfrost.oneconfig.internal.OneConfig;
import org.polyfrost.oneconfig.utils.v1.MHUtils;

import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;

/**
 * Taken from LWJGLTwoPointFive under The Unlicense
 * <a href="https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/">https://github.com/DJtheRedstoner/LWJGLTwoPointFive/blob/master/LICENSE/</a>
 */
public class Lwjgl2FunctionProvider implements FunctionProvider {
    private static final Class<?> GL_CONTEXT;
    private final MethodHandle getFunctionAddress;

    static {
        String libraryPath = System.getProperty("oneconfig.lwjgl2.librarypath", "");
        if (!libraryPath.isEmpty()) {
            System.setProperty("org.lwjgl.librarypath", libraryPath);
        }

        try {
            GL_CONTEXT = Class.forName("org.lwjgl.opengl.GLContext", true, OneConfig.class.getClassLoader());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Error initializing Lwjgl2FunctionProvider", e);
        }
    }

    public Lwjgl2FunctionProvider() {
        getFunctionAddress = MHUtils.getStaticMethodHandle(GL_CONTEXT, "getFunctionAddress", long.class, String.class).getOrThrow();
    }

    @Override
    public long getFunctionAddress(@NotNull CharSequence functionName) {
        try {
            return (long) getFunctionAddress.invokeExact(functionName.toString());
        } catch (Throwable exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public long getFunctionAddress(@NotNull ByteBuffer byteBuffer) {
        throw new UnsupportedOperationException(
                "LWJGL 2 does not support this method"
        );
    }
}
//#endif