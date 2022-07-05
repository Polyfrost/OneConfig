package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.libs.universal.UGraphics;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.platform.GLPlatform;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;

@SuppressWarnings("unused")
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

        float f = (float)(color >> 24 & 0xFF) / 255.0F;
        float g = (float)(color >> 16 & 0xFF) / 255.0F;
        float h = (float)(color >> 8 & 0xFF) / 255.0F;
        float j = (float)(color & 0xFF) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        UGraphics.enableBlend();
        UGraphics.disableTexture2D();
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0);
        UGraphics.color4f(g, h, j, f);
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x, y2, 0.0).endVertex();
        worldRenderer.pos(x2, y2, 0.0).endVertex();
        worldRenderer.pos(x2, y, 0.0).endVertex();
        worldRenderer.pos(x, y, 0.0).endVertex();
        tessellator.draw();
        UGraphics.enableTexture2D();
        UGraphics.disableBlend();
    }

    @Override
    public void enableStencil() {
        Framebuffer framebuffer = UMinecraft.getMinecraft().getFramebuffer();
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
