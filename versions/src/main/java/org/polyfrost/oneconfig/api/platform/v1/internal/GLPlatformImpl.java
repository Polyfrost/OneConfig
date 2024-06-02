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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.polyfrost.oneconfig.api.platform.v1.GLPlatform;
import org.polyfrost.oneconfig.utils.v1.MHUtils;
import org.polyfrost.universal.UGraphics;
import org.polyfrost.universal.UMatrixStack;

public class GLPlatformImpl implements GLPlatform {

    @Override
    public void drawRect(UMatrixStack stack, double x, double y, double x2, double y2, int color) {
        if (x < x2) {
            double i = x;
            x = x2;
            x2 = i;
        }

        if (y < y2) {
            double i = y;
            y = y2;
            y2 = i;
        }

        float r = (float) (color >> 16 & 0xFF) / 255.0F;
        float g = (float) (color >> 8 & 0xFF) / 255.0F;
        float b = (float) (color & 0xFF) / 255.0F;
        float a = (float) (color >> 24 & 0xFF) / 255.0F;
        UGraphics graphics = new UGraphics(Tessellator.getInstance().getWorldRenderer());
        graphics.beginWithDefaultShader(UGraphics.DrawMode.QUADS, DefaultVertexFormats.POSITION);
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0);
        UGraphics.enableBlend();
        UGraphics.disableAlpha();
        graphics.color(r, g, b, a);
        graphics.pos(stack, x, y2, 0.0).endVertex();
        graphics.pos(stack, x2, y2, 0.0).endVertex();
        graphics.pos(stack, x2, y, 0.0).endVertex();
        graphics.pos(stack, x, y, 0.0).endVertex();
        graphics.drawDirect();
        UGraphics.disableBlend();
        UGraphics.enableAlpha();
    }

    @Override
    public float drawText(UMatrixStack stack, String text, float x, float y, int color, boolean shadow) {
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        //#if MC>12000
        //#if FABRIC
        //$$ return fr.draw(text, x, y, color, shadow,
        //$$    stack.toMC().peek().getPositionMatrix(), MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
        //$$    net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 15728880);
        //#else
        //$$ return fr.drawInBatch(text, x, y, color, shadow,
        //$$         stack.toMC().last().pose(), Minecraft.getInstance().renderBuffers().bufferSource(),
        //$$         net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);
        //#endif
        //#elseif MC<=11202
        return fr.drawString(text, x, y, color, shadow);
        //#else
        //$$ if(shadow) {
        //$$    return fr.drawStringWithShadow(stack.toMC(), text, x, y, color);
        //$$ } else return fr.drawString(stack.toMC(), text, x, y, color);
        //#endif
    }

    @Override
    public int getStringWidth(String text) {
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
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
