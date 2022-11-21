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

package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.internal.hook.FramebufferHook;
import com.mojang.blaze3d.platform.GLX;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Modified from MinecraftForge
 * <a href="https://github.com/MinecraftForge/MinecraftForge/blob/1.19.x/LICENSE.txt">...</a>
 */
@Mixin(Framebuffer.class)
public abstract class FramebufferMixin implements FramebufferHook {

    @Shadow public abstract void resize(int width, int height);

    @Shadow public int viewportWidth;
    @Shadow public int viewportHeight;
    private boolean oneconfig$stencilEnabled = false;

    @Redirect(method = "attachTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GLX;advancedRenderBufferStorage(IIII)V"))
    private void stencilSupport1(int i, int j, int k, int l) {
        if (oneconfig$stencilEnabled) {
            GLX.advancedRenderBufferStorage(GLX.renderbuffer, 35056, k, l);
        } else {
            GLX.advancedRenderBufferStorage(i, j, k, l);
        }
    }

    @Redirect(method = "attachTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GLX;advancedFramebufferRenderbuffer(IIII)V"))
    private void stencilSupport2(int i, int j, int k, int l) {
        if (oneconfig$stencilEnabled) {
            GLX.advancedFramebufferRenderbuffer(GLX.framebuffer, 36096, GLX.renderbuffer, l);
            GLX.advancedFramebufferRenderbuffer(GLX.framebuffer, 36128, GLX.renderbuffer, l);
        } else {
            GLX.advancedFramebufferRenderbuffer(i, j, k, l);
        }
    }

    @Override
    public boolean isStencilEnabled() {
        return oneconfig$stencilEnabled;
    }

    @Override
    public void enableStencil() {
        if (!this.oneconfig$stencilEnabled) {
            this.oneconfig$stencilEnabled = true;
            resize(this.viewportWidth, this.viewportHeight);
        }
    }
}
