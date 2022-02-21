package io.polyfrost.oneconfig.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.*;

import java.awt.*;

public class Renderer extends Gui {
    public static final Logger renderLog = LogManager.getLogger("OneConfig Renderer");
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final FontRenderer fr = mc.fontRendererObj;
    private static Tessellator tessellator = Tessellator.getInstance();
    private static WorldRenderer worldRenderer = tessellator.getWorldRenderer();


    public static void drawRectangle(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    public static void drawTextScale(String text, float x, float y, int color, boolean shadow, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1);
        mc.fontRendererObj.drawString(text, x * (1 / scale), y * (1 / scale), color, shadow);
        GlStateManager.popMatrix();
    }

    public static void drawScaledImage(ResourceLocation location, int x, int y, int targetX, int targetY) {
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(location);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, targetX, targetY, targetX, targetY, targetX, targetY);
    }

    public static void drawRegularPolygon(double x, double y, int radius, int sides, int color, double lowerAngle, double upperAngle) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        setGlColor(color);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        worldRenderer.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x, y, 0).endVertex();
        //GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        //GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        //GL11.glCullFace(GL11.GL_FRONT);
        //GL11.glCullFace(GL11.GL_FRONT_AND_BACK);
        //GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_FILL);
        //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        //GL11.glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);


        for (int i = 0; i <= sides; i++) {
            double angle = ((Math.PI * 2) * i / sides) + Math.toRadians(180);
            if (angle > lowerAngle && angle < upperAngle) {            // >0 <4.75; >4.7 <6.3; >6.25 <7.9; >7.8 <10 80 side mode
                worldRenderer.pos(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius, 0).endVertex();
            }
        }
        tessellator.draw();
        GlStateManager.disableBlend();
        //GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void drawRegularPolygon(double x, double y, int radius, int sides, int color) {
        drawRegularPolygon(x, y, radius, sides, color, 0d, 10000d);
    }

    /**
     * Draw a round rectangle at the given coordinates.
     * @param radius radius of the corners
     * @param color color as a rgba integer
     */
    public static void drawRoundRect(double x, double y, double width, double height, int radius, int color) {
        GL11.glEnable(GL11.GL_BLEND);
        //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        Gui.drawRect((int) x + radius, (int) y, (int) (x + width - radius), (int) (y + radius), color);                          // top
        Gui.drawRect((int) x + radius, (int) (y + height - radius), (int) (x + width - radius), (int) (y + height), color);      // bottom
        Gui.drawRect((int) x, (int) y + radius, (int) (x + width), (int) (y + height - radius), color);                          // main
        drawRegularPolygon(x + radius, y + radius, radius, 80, color, 0d, 4.75d);                    // top left
        drawRegularPolygon(x + width - radius, y + radius, radius, 80, color, 7.8d, 10d);            // top right
        drawRegularPolygon(x + radius, y + height - radius, radius, 80, color, 4.7d, 6.3d);          // bottom left
        drawRegularPolygon(x + width - radius, y + height - radius, radius, 80, color, 6.25d, 7.9d); // bottom right
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1f,1f,1f,1f);
    }

    public static float clamp(float number) {
        return number < (float) 0.0 ? (float) 0.0 : Math.min(number, (float) 1.0);
    }

    public static float easeOut(float current, float goal) {
        if (Math.floor(Math.abs(goal - current) / (float) 0.01) > 0) {
            return current + (goal - current) / (float) 20.0;
        } else {
            return goal;
        }
    }

    /**
     * Return a java.awt.Color object from the given Integer.
     *
     * @param color rgba color, parsed into an integer
     */
    public static Color getColorFromInt(int color) {
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        float f3 = (float) (color >> 24 & 255) / 255.0F;
        return new Color(f, f1, f2, f3);
    }

    /**
     * Set GL color from the given Color variable.
     */
    public static void setGlColor(Color color) {
        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    /**
     * Set GL color from the given color as an Integer.
     */
    public static void setGlColor(int color) {
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        float f3 = (float) (color >> 24 & 255) / 255.0F;
        GlStateManager.color(f,f1,f2,f3);
    }

}
