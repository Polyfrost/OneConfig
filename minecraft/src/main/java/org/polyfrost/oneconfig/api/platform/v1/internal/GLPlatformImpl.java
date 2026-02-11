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

import dev.deftu.omnicore.api.client.render.state.OmniRenderStates;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.polyfrost.oneconfig.api.platform.v1.GLPlatform;
import org.polyfrost.oneconfig.utils.v1.MHUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

public class GLPlatformImpl implements GLPlatform {
    //@formatter:off
    //#if MC <= 1.12.2
    private static final java.util.function.Function<String, Long> getProcAddress =
            MHUtils.getFunctionHandle(org.lwjgl.opengl.GLContext.class, "getFunctionAddress", long.class, String.class)
                    .logIfErr().getOrElse(v -> 0L);
    //#endif

    private final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(
            //#if MC >= 1.13
            //$$ 4
            //#else
            16
            //#endif
    );

    @Override
    public long getFunctionAddress(String addr) {
        //#if MC <= 1.12.2
        return getProcAddress.apply(addr);
        //#else
        //$$ return org.lwjgl.glfw.GLFW.glfwGetProcAddress(addr);
        //#endif
    }
    //@formatter:on


    @Override
    public long memAddress(ByteBuffer buf) {
        //#if MC <= 1.12.2
        return org.lwjgl.MemoryUtil.getAddress(buf);
        //#else
        //$$ return org.lwjgl.system.MemoryUtil.memAddress(buf);
        //#endif
    }

    /**
     * This method is called to update the game's internally tracked OpenGL state
     * to match what NanoVG leaves dropped into the OpenGL context.
     */
    @Override
    public void syncOpenGLContext() {
        OmniRenderStates.syncBlend();
        OmniRenderStates.syncDepth();
        OmniRenderStates.syncCull();
        OmniRenderStates.syncColorMask();

        //#if MC >= 1.17.1 && MC < 1.21.5
        //$$ com.mojang.blaze3d.vertex.BufferUploader.reset();
        //#endif
    }

    @Override
    public int @NotNull [] glViewport(int @Nullable [] in) {
        ((Buffer) VIEWPORT).clear();
        //#if MC >= 1.13
        //$$ glGetIntegerv(GL_VIEWPORT, VIEWPORT);
        //#else
        glGetInteger(GL_VIEWPORT, VIEWPORT);
        //#endif
        int[] out = in != null ? in : new int[4];
        VIEWPORT.get(out, 0, 4);
        return out;
    }
}
