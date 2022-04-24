package io.polyfrost.oneconfig.lwjgl;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.lwjgl.font.Font;
import io.polyfrost.oneconfig.lwjgl.font.FontManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.lwjgl.image.Image;
import io.polyfrost.oneconfig.lwjgl.image.ImageLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.nanovg.*;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.function.LongConsumer;

import static org.lwjgl.nanovg.NanoSVG.NSVG_FLAGS_VISIBLE;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL2.NVG_ANTIALIAS;
import static org.lwjgl.nanovg.NanoVGGL2.nvgCreate;

public final class RenderManager {
    private RenderManager() {

    }

    //nanovg

    private static long vg = -1;

    public static void setupAndDraw(LongConsumer consumer) {
        setupAndDraw(false, consumer);
    }

    public static void setupAndDraw(boolean mcScaling, LongConsumer consumer) {
        if (vg == -1) {
            vg = nvgCreate(NVG_ANTIALIAS);
            if (vg == -1) {
                throw new RuntimeException("Failed to create nvg context");
            }
            FontManager.INSTANCE.initialize(vg);
        }

        Framebuffer fb = Minecraft.getMinecraft().getFramebuffer();
        if (!fb.isStencilEnabled()) {
            fb.enableStencil();
        }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        if (mcScaling) {
            ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
            nvgBeginFrame(vg, (float) resolution.getScaledWidth_double(), (float) resolution.getScaledHeight_double(), resolution.getScaleFactor());
        } else {
            // If we get blurry problems with high DPI monitors, 1 might need to be  replaced with Display.getPixelScaleFactor()
            nvgBeginFrame(vg, Display.getWidth(), Display.getHeight(), 1);
        }

        consumer.accept(vg);

        nvgEndFrame(vg);

        GlStateManager.popAttrib();
    }

    public static void drawRectangle(long vg, float x, float y, float width, float height, int color) {
        if (OneConfigConfig.ROUNDED_CORNERS) {
            drawRoundedRect(vg, x, y, width, height, color, OneConfigConfig.CORNER_RADIUS);
        } else {
            drawRect(vg, x, y, width, height, color);
        }
    }

    public static void drawGradientRectangle(long vg, float x, float y, float width, float height, int color1, int color2) {
        if (OneConfigConfig.ROUNDED_CORNERS) {
            drawGradientRoundedRect(vg, x, y, width, height, color1, color2, OneConfigConfig.CORNER_RADIUS);
        } else {
            drawGradientRect(vg, x, y, width, height, color1, color2);
        }
    }

    public static void drawGradientRoundedRect(long vg, float x, float y, float width, float height, int color, int color2, float radius) {
        NVGPaint bg = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        NVGColor nvgColor = color(vg, color);
        NVGColor nvgColor2 = color(vg, color2);
        nvgFillPaint(vg, nvgLinearGradient(vg, x, y + height, x + width, y, nvgColor, nvgColor2, bg));      // like the gradient is blocky
        nvgFillPaint(vg, bg);
        nvgFill(vg);
        nvgColor.free();
        nvgColor2.free();
    }

    public static void drawGradientRect(long vg, float x, float y, float width, float height, int color, int color2) {
        NVGPaint bg = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, height);
        NVGColor nvgColor = color(vg, color);
        NVGColor nvgColor2 = color(vg, color2);
        nvgFillPaint(vg, nvgLinearGradient(vg, x, y + height, x + width, y, nvgColor, nvgColor2, bg));      // like the gradient is blocky
        nvgFillPaint(vg, bg);
        nvgFill(vg);
        nvgColor.free();
        nvgColor2.free();
    }

    public static void drawRect(long vg, float x, float y, float width, float height, int color) {
        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, height);
        NVGColor nvgColor = color(vg, color);
        nvgFill(vg);
        nvgColor.free();
    }

    public static void drawRoundedRect(long vg, float x, float y, float width, float height, int color, float radius) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        color(vg, color);
        NVGColor nvgColor = color(vg, color);
        nvgFill(vg);
        nvgColor.free();
    }

    public static void drawRoundedRectVaried(long vg, float x, float y, float width, float height, int color, float radiusTL, float radiusTR, float radiusBR, float radiusBL) {
        nvgBeginPath(vg);
        nvgRoundedRectVarying(vg, x, y, width, height, radiusTL, radiusTR, radiusBR, radiusBL);
        color(vg, color);
        NVGColor nvgColor = color(vg, color);
        nvgFill(vg);
        nvgColor.free();
    }

    public static void drawHollowRoundRect(long vg, float x, float y, float width, float height, int color, float radius, float thickness) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + thickness, y + thickness, width - thickness, height - thickness, radius);
        nvgStrokeWidth(vg, thickness);
        nvgPathWinding(vg, NVG_HOLE);
        color(vg, color);
        NVGColor nvgColor = color(vg, color);
        nvgStrokeColor(vg, nvgColor);
        nvgStroke(vg);
        nvgColor.free();
    }

    public static void drawCircle(long vg, float x, float y, float radius, int color) {
        nvgBeginPath(vg);
        nvgCircle(vg, x, y, radius);
        NVGColor nvgColor = color(vg, color);
        nvgFill(vg);
        nvgColor.free();
    }


    public static void drawString(long vg, String text, float x, float y, int color, float size, Fonts font) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, font.font.getName());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        NVGColor nvgColor = color(vg, color);
        nvgText(vg, x, y, text);
        nvgFill(vg);
        nvgColor.free();
    }

    public static void drawWrappedString(long vg, String text, float x, float y, float width, int color, float size, Font font) {
        drawWrappedString(vg, text, x, y, width, color, size, font.getName());
    }

    public static void drawWrappedString(long vg, String text, float x, float y, float width, int color, float size, String fontName) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, fontName);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        NVGColor nvgColor = color(vg, color);
        nvgTextBox(vg, x, y, width, text);
        nvgFill(vg);
        nvgColor.free();
    }

    public static void drawImage(long vg, String fileName, float x, float y, float width, float height) {
        if (ImageLoader.INSTANCE.loadImage(vg, fileName)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            Image image = ImageLoader.INSTANCE.getImage(fileName);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image.getReference(), 1, imagePaint);
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    public static void drawSVGImage(long vg, String fileName, float x, float y, float width, float height) {
        if (ImageLoader.INSTANCE.loadSVGImage(fileName)) {
            try {
                NSVGImage image = ImageLoader.INSTANCE.getSVG(fileName);
                NSVGShape shape;
                NSVGPath path;
                int i;
                for (shape = image.shapes(); shape != null; shape.next()) {
                    if ((shape.flags() == NSVG_FLAGS_VISIBLE)) {
                        continue;
                    }

                    nvgFillColor(vg, color(vg, shape.fill().color()));
                    nvgStrokeColor(vg, color(vg, shape.stroke().color()));
                    nvgStrokeWidth(vg, shape.strokeWidth());

                    for (path = shape.paths(); path != null; path.next()) {
                        nvgBeginPath(vg);
                        FloatBuffer points = path.pts();
                        nvgMoveTo(vg, points.get(0), points.get(1));
                        for (i = 1; i < path.npts() - 1; i += 3) {
                            int b = i * 2;
                            nvgBezierTo(vg, points.get(b), points.get(b + 1), points.get(b + 2), points.get(b + 3), points.get(b + 4), points.get(b + 5));
                        }
                        if (path.closed() == 1) {
                            nvgLineTo(vg, points.get(0), points.get(1));
                        }
                        nvgStroke(vg);
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    public static float getTextWidth(long vg, String text, float fontSize) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        return nvgTextBounds(vg, 0, 0, text, bounds);
    }

    public static void drawLine(long vg, float x, float y, float endX, float endY, float width, int color) {
        nvgBeginPath(vg);
        nvgMoveTo(vg, x, y);
        nvgLineTo(vg, endX, endY);
        NVGColor nvgColor = color(vg, color);
        nvgStrokeColor(vg, nvgColor);
        nvgStrokeWidth(vg, width);
        nvgStroke(vg);
        nvgColor.free();
    }

    public static void drawDropShadow(long vg, float x, float y, float w, float h, float cornerRadius, float spread, int color) {       // TODO broken
        NVGColor color1 = NVGColor.calloc();
        NVGColor color2 = NVGColor.calloc();
        NVGPaint shadowPaint = NVGPaint.calloc();
        nvgRGBA((byte) 0, (byte) 0, (byte) 0, (byte) 128, color1);
        nvgRGBA((byte) 0, (byte) 0, (byte) 0, (byte) 0, color2);
        nvgBoxGradient(vg, x, y + 2, w, h, cornerRadius * 2, 10f, color2, color1, shadowPaint);
        nvgBeginPath(vg);
        nvgRect(vg, x - 10, y - 10, w + 20, h + 30);
        nvgRoundedRect(vg, x, y, w, h, cornerRadius);
        nvgPathWinding(vg, NVG_HOLE);
        nvgFillPaint(vg, shadowPaint);
        nvgFill(vg);
        shadowPaint.free();
        color1.free();
        color2.free();

    }


    public static NVGColor color(long vg, int color) {
        NVGColor nvgColor = NVGColor.calloc();
        nvgRGBA((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF), nvgColor);
        nvgFillColor(vg, nvgColor);
        return nvgColor;
    }

    //gl

    public static void drawScaledString(String text, float x, float y, int color, boolean shadow, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1);
        Minecraft.getMinecraft().fontRendererObj.drawString(text, x * (1 / scale), y * (1 / scale), color, shadow);
        GlStateManager.popMatrix();
    }

    public static void color(Color color) {
        color(color.getRGB());
    }

    public static void color(int color) {
        float f = (float) (color >> 24 & 255) / 255.0F;
        float f1 = (float) (color >> 16 & 255) / 255.0F;
        float f2 = (float) (color >> 8 & 255) / 255.0F;
        float f3 = (float) (color & 255) / 255.0F;
        GlStateManager.color(f1, f2, f3, f);
    }

    public static void drawDottedLine(float sx, float sy, float ex, float ey, int width, int factor, int color) {
        GlStateManager.pushMatrix();
        GL11.glLineStipple(factor, (short) 0xAAAA);
        GL11.glEnable(GL11.GL_LINE_STIPPLE);
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        color(color);
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(sx, sy);
        GL11.glVertex2d(ex, ey);
        GL11.glEnd();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        GL11.glDisable(GL11.GL_LINE_STIPPLE);
        GlStateManager.popMatrix();
    }
}
