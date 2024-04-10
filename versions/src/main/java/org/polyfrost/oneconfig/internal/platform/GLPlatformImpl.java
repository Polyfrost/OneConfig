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

package org.polyfrost.oneconfig.internal.platform;

import org.polyfrost.oneconfig.libs.universal.UGraphics;
import org.polyfrost.oneconfig.libs.universal.UMatrixStack;
import org.polyfrost.oneconfig.libs.universal.UMinecraft;
import org.polyfrost.oneconfig.platform.GLPlatform;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

//#if FORGE==1
import net.minecraft.client.shader.Framebuffer;
//#else
//$$ import org.polyfrost.oneconfig.internal.hook.FramebufferHook;
//#endif

public class GLPlatformImpl implements GLPlatform {

    @Override
    public void drawRect(float x, float y, float x2, float y2, int color) {
        if (x < x2) {
            float i = x;
            x = x2;
            x2 = i;
        }

        if (y < y2) {
            float i = y;
            y = y2;
            y2 = i;
        }

        float f = (float) (color >> 24 & 0xFF) / 255.0F;
        float g = (float) (color >> 16 & 0xFF) / 255.0F;
        float h = (float) (color >> 8 & 0xFF) / 255.0F;
        float j = (float) (color & 0xFF) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        UGraphics.enableBlend();
        //noinspection deprecation
        UGraphics.disableTexture2D();
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0);
        UGraphics.color4f(g, h, j, f);
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x, y2, 0.0).endVertex();
        worldRenderer.pos(x2, y2, 0.0).endVertex();
        worldRenderer.pos(x2, y, 0.0).endVertex();
        worldRenderer.pos(x, y, 0.0).endVertex();
        tessellator.draw();
        //noinspection deprectation
        UGraphics.enableTexture2D();
        UGraphics.disableBlend();
    }

    @Override
    public void enableStencil() {
        //#if FORGE==1
        Framebuffer framebuffer = UMinecraft.getMinecraft().getFramebuffer();
        //#else
        //$$ FramebufferHook framebuffer = ((FramebufferHook) UMinecraft.getMinecraft().getFramebuffer());
        //#endif
        if (!framebuffer.isStencilEnabled()) {
            framebuffer.enableStencil();
        }
    }

    @Override
    public float drawText(UMatrixStack matrixStack, String text, float x, float y, int color, boolean shadow) {
        //#if MC<=11202
        return UMinecraft.getFontRenderer().drawString(text, x, y, color, shadow);
        //#else
        //$$ if(shadow) {
        //$$    return UMinecraft.getFontRenderer().drawStringWithShadow(matrixStack.toMC(), text, x, y, color);
        //$$ } else return UMinecraft.getFontRenderer().drawString(matrixStack.toMC(), text, x, y, color);
        //#endif
    }

    @Override
    public int getStringWidth(String text) {
        return UMinecraft.getFontRenderer().getStringWidth(text);
    }
}
