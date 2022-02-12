package io.polyfrost.oneconfig.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

public class Renderer extends Gui {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final FontRenderer fr = mc.fontRendererObj;

    public static void drawRectangle(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    public static void drawString(String text, int x, int y, int color, boolean shadow) {
        fr.drawString(text, x, y, color, shadow);
    }

    public static void drawScaledImage(ResourceLocation location, int x, int y, int targetX, int targetY) {
        //GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(location);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, targetX, targetY, targetX, targetY, targetX, targetY);
    }

    public static void drawRoundRectangle(int left, int top, int right, int bottom, int cornerRadius, int color) {

    }


    public static float easeOut(float current, float goal) {
        if (Math.floor(Math.abs(goal - current) / (float) 0.01) > 0) {
            return current + (goal - current) / (float) 20.0;
        } else {
            return goal;
        }
    }

}
