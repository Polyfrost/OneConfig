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

import org.polyfrost.oneconfig.utils.v1.MHUtils;
import org.polyfrost.universal.UGraphics;
import org.polyfrost.universal.UMatrixStack;
import org.polyfrost.universal.UMinecraft;
import org.polyfrost.oneconfig.api.platform.v1.GLPlatform;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;

public class GLPlatformImpl implements GLPlatform {

    @Override
    @SuppressWarnings("deprecation")
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
        UGraphics.disableTexture2D();
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0);
        UGraphics.color4f(g, h, j, f);
        //#if MC<11700
        worldRenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION);
        //#elseif FORGE
        //$$ worldRenderer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION);
        //#else
        //$$ worldRenderer.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION);
        //#endif
        //#if FABRIC || MC<11700
        worldRenderer.pos(x, y2, 0.0).endVertex();
        worldRenderer.pos(x2, y2, 0.0).endVertex();
        worldRenderer.pos(x2, y, 0.0).endVertex();
        worldRenderer.pos(x, y, 0.0).endVertex();
        //#else
        //$$ worldRenderer.vertex(x, y2, 0.0).endVertex();
        //$$ worldRenderer.vertex(x2, y2, 0.0).endVertex();
        //$$ worldRenderer.vertex(x2, y, 0.0).endVertex();
        //$$ worldRenderer.vertex(x, y, 0.0).endVertex();
        //#endif
        tessellator.draw();
        UGraphics.enableTexture2D();
        UGraphics.disableBlend();
    }

    @Override
    public float drawText(UMatrixStack matrixStack, String text, float x, float y, int color, boolean shadow) {
        //#if MC>12000
            //#if FABRIC
            //$$ return UMinecraft.getFontRenderer().draw(text, x, y, color, shadow,
            //$$    matrixStack.toMC().peek().getPositionMatrix(), UMinecraft.getMinecraft().getBufferBuilders().getEntityVertexConsumers(),
            //$$    net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 15728880);
            //#else
            //$$ return UMinecraft.getFontRenderer().drawInBatch(text, x, y, color, shadow,
            //$$         matrixStack.toMC().last().pose(), UMinecraft.getMinecraft().renderBuffers().bufferSource(),
            //$$         net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);
            //#endif
        //#elseif MC<=11202
        return UMinecraft.getFontRenderer().drawString(text, x, y, color, shadow);
        //#else
        //$$ net.minecraft.client.gui.FontRenderer fr = UMinecraft.getFontRenderer();
        //$$ if(shadow) {
        //$$    return fr.drawStringWithShadow(matrixStack.toMC(), text, x, y, color);
        //$$ } else return fr.drawString(matrixStack.toMC(), text, x, y, color);
        //#endif
    }

    @Override
    public int getStringWidth(String text) {
        return UMinecraft.getFontRenderer().getStringWidth(text);
    }

    //#if MC<=11202
    private static final java.util.function.Function<String, Long> getProcAddress =
            MHUtils.getFunctionHandle(org.lwjgl.opengl.GLContext.class, "getFunctionAddress", long.class, String.class)
                    .logIfErr().getOrElse(v -> 0L);
    //#endif

    @Override
    public long getFunctionAddress(String addr) {
        //#if MC<=11202
        return getProcAddress.apply(addr);
        //#else
        //$$ return org.lwjgl.glfw.GLFW.glfwGetProcAddress(addr);
        //#endif
    }
}
