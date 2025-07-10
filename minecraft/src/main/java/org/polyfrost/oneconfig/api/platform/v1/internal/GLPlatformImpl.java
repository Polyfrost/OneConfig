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

import dev.deftu.omnicore.client.render.OmniRenderState;
import dev.deftu.omnicore.client.render.OmniTextureManager;
import dev.deftu.omnicore.client.render.state.OmniManagedBlendState;
import dev.deftu.omnicore.client.render.state.OmniManagedColorMask;
import dev.deftu.omnicore.client.render.state.OmniManagedDepthState;
import dev.deftu.omnicore.client.render.state.OmniManagedScissorState;
import org.lwjgl.opengl.GL11;
import org.polyfrost.oneconfig.api.platform.v1.GLPlatform;
import org.polyfrost.oneconfig.utils.v1.MHUtils;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

//#if MC <= 1.12.2
import net.minecraft.client.renderer.GlStateManager;
//#endif

public class GLPlatformImpl implements GLPlatform {

    //@formatter:off
    //#if MC <= 1.12.2
    private static final java.util.function.Function<String, Long> getProcAddress =
            MHUtils.getFunctionHandle(org.lwjgl.opengl.GLContext.class, "getFunctionAddress", long.class, String.class)
                    .logIfErr().getOrElse(v -> 0L);
    //#endif

    @Override
    public long getFunctionAddress(String addr) {
        //#if MC <= 1.12.2
        return getProcAddress.apply(addr);
        //#else
        //$$ return org.lwjgl.glfw.GLFW.glfwGetProcAddress(addr);
        //#endif
    }
    //@formatter:on

    /**
     * This method is called to update the game's internally tracked OpenGL state
     * to match what NanoVG leaves dropped into the OpenGL context.
     */
    @Override
    public void updateGameRenderStateAlongsideNanoVG() {
        // Blending
        OmniManagedBlendState.enableBlend();
        OmniManagedBlendState.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Depth
        OmniManagedDepthState.disableDepth();

        // Culling
        //noinspection deprecation
        OmniRenderState.enableCull();
        //#if MC <= 1.8.9
        GlStateManager.cullFace(GL11.GL_BACK);
        //#endif

        // Scissor
        OmniManagedScissorState.disable();

        // Color mask
        new OmniManagedColorMask(true, true, true, true).activate();

        //#if MC >= 1.17.1 && MC < 1.21.5
        //$$ net.minecraft.client.render.BufferRenderer.unbindAll();
        //#endif
    }

}
