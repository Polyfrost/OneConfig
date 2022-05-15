package cc.polyfrost.oneconfig.lwjgl;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.lwjgl.font.FontManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.ImageLoader;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import gg.essential.universal.UGraphics;
import gg.essential.universal.UMinecraft;
import gg.essential.universal.UResolution;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.nanovg.*;
import org.lwjgl.opengl.GL11;

import java.util.function.LongConsumer;

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

        Framebuffer fb = UMinecraft.getMinecraft().getFramebuffer();
        if (!fb.isStencilEnabled()) {
            fb.enableStencil();
        }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        if (mcScaling) {
            nvgBeginFrame(vg, (float) UResolution.getScaledWidth(), (float) UResolution.getScaledHeight(), (float) UResolution.getScaleFactor());
        } else {
            // If we get blurry problems with high DPI monitors, 1 might need to be  replaced with Display.getPixelScaleFactor()
            nvgBeginFrame(vg, UResolution.getWindowWidth(), UResolution.getWindowHeight(), 1);
        }

        consumer.accept(vg);

        nvgEndFrame(vg);

        GL11.glPopAttrib();
    }

    public static void drawRectangle(long vg, float x, float y, float width, float height, int color) {     // TODO make everything use this one day
        if (OneConfigConfig.ROUNDED_CORNERS) {
            drawRoundedRect(vg, x, y, width, height, color, OneConfigConfig.CORNER_RADIUS);
        } else {
            drawRect(vg, x, y, width, height, color);
        }
    }

    public static void drawGradientRoundedRect(long vg, float x, float y, float width, float height, int color, int color2, float radius) {
        NVGPaint bg = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        NVGColor nvgColor = color(vg, color);
        NVGColor nvgColor2 = color(vg, color2);
        nvgFillPaint(vg, nvgLinearGradient(vg, x, y, x + width, y, nvgColor, nvgColor2, bg));
        nvgFill(vg);
        nvgColor.free();
        nvgColor2.free();
    }

    public static void drawHSBBox(long vg, float x, float y, float width, float height, int colorTarget) {
        drawRoundedRect(vg, x, y, width, height, colorTarget, 8f);

        NVGPaint bg = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, 8f);
        NVGColor nvgColor = color(vg, OneConfigConfig.WHITE); // Do not use OneConfigConfig colors for this, use rgba code - MoonTidez
        NVGColor nvgColor2 = color(vg, OneConfigConfig.TRANSPARENT_25);
        nvgFillPaint(vg, nvgLinearGradient(vg, x, y, x + width, y, nvgColor, nvgColor2, bg));
        nvgFill(vg);
        nvgColor.free();
        nvgColor2.free();

        NVGPaint bg2 = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, 8f);
        NVGColor nvgColor3 = color(vg, OneConfigConfig.TRANSPARENT_25);
        NVGColor nvgColor4 = color(vg, OneConfigConfig.BLACK); // Do not use OneConfigConfig colors for this, use rgba code - MoonTidez
        nvgFillPaint(vg, nvgLinearGradient(vg, x, y, x, y + height, nvgColor3, nvgColor4, bg2));
        nvgFill(vg);
        nvgColor3.free();
        nvgColor4.free();
    }

    public static void drawGradientRect(long vg, float x, float y, float width, float height, int color, int color2) {
        NVGPaint bg = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, height);
        NVGColor nvgColor = color(vg, color);
        NVGColor nvgColor2 = color(vg, color2);
        nvgFillPaint(vg, nvgLinearGradient(vg, x, y, x, y + width, nvgColor, nvgColor2, bg));
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

    public static void drawString(long vg, String text, float x, float y, int color, float size, int lineHeight, Fonts font) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, font.font.getName());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        nvgTextMetrics(vg, new float[]{10f}, new float[]{10f}, new float[]{lineHeight});
        NVGColor nvgColor = color(vg, color);
        nvgText(vg, x, y, text);
        nvgFill(vg);
        nvgColor.free();
    }

    public static void drawWrappedString(long vg, String text, float x, float y, float width, int color, float size, Fonts font) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, font.font.getName());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        NVGColor nvgColor = color(vg, color);
        nvgTextBox(vg, x, y, width, text);
        nvgFill(vg);
        nvgColor.free();
    }

    public static void drawImage(long vg, String filePath, float x, float y, float width, float height) {
        if (ImageLoader.INSTANCE.loadImage(vg, filePath)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = ImageLoader.INSTANCE.getImage(filePath);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    public static void drawImage(long vg, String filePath, float x, float y, float width, float height, int color) {
        if (ImageLoader.INSTANCE.loadImage(vg, filePath)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = ImageLoader.INSTANCE.getImage(filePath);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRGBA((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF), imagePaint.innerColor());
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    public static void drawRoundImage(long vg, String filePath, float x, float y, float width, float height, float radius) {
        if (ImageLoader.INSTANCE.loadImage(vg, filePath)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = ImageLoader.INSTANCE.getImage(filePath);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRoundedRect(vg, x, y, width, height, radius);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    public static void drawRoundImage(long vg, Images filePath, float x, float y, float width, float height, float radius) {
        drawRoundImage(vg, filePath.filePath, x, y, width, height, radius);
    }

    public static void drawImage(long vg, Images filePath, float x, float y, float width, float height) {
        drawImage(vg, filePath.filePath, x, y, width, height);
    }

    public static void drawImage(long vg, Images filePath, float x, float y, float width, float height, int color) {
        drawImage(vg, filePath.filePath, x, y, width, height, color);
    }


    public static float getTextWidth(long vg, String text, float fontSize, Fonts font) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, font.font.getName());
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

    public static void drawSvg(long vg, String filePath, float x, float y, float width, float height) {
        float w = width;
        float h = height;
        if (OneConfigGui.INSTANCE != null) {
            w *= OneConfigGui.INSTANCE.getScaleFactor();
            h *= OneConfigGui.INSTANCE.getScaleFactor();
        }
        if (ImageLoader.INSTANCE.loadSVG(vg, filePath, w, h)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = ImageLoader.INSTANCE.getSVG(filePath, w, h);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    public static void drawSvg(long vg, String filePath, float x, float y, float width, float height, int color) {
        float w = width;
        float h = height;
        if (OneConfigGui.INSTANCE != null) {
            w *= OneConfigGui.INSTANCE.getScaleFactor();
            h *= OneConfigGui.INSTANCE.getScaleFactor();
        }
        if (ImageLoader.INSTANCE.loadSVG(vg, filePath, w, h)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = ImageLoader.INSTANCE.getSVG(filePath, w, h);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRGBA((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF), imagePaint.innerColor());
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    public static void drawSvg(long vg, SVGs svg, float x, float y, float width, float height) {
        drawSvg(vg, svg.filePath, x, y, width, height);
    }

    public static void drawSvg(long vg, SVGs svg, float x, float y, float width, float height, int color) {
        drawSvg(vg, svg.filePath, x, y, width, height, color);
    }

    /*public static void drawSvg(long vg, String filename) {
        if (ImageLoader.INSTANCE.loadSVGImage(filename)) {
            try {
                NSVGImage image = ImageLoader.INSTANCE.getSVG(filename);
                NSVGShape shape = image.shapes();
                NSVGPath path = shape.paths();
                while (shape.address() != 0) {
                    while (path.address() != 0) {
                        nvgBeginPath(vg);
                        nvgFillColor(vg, color(vg, new Color(255, 255, 255).getRGB()));
                        nvgStrokeColor(vg, color(vg, new Color(255, 255, 255).getRGB()));
                        nvgStrokeWidth(vg, shape.strokeWidth());
                        FloatBuffer points = path.pts();
                        nvgMoveTo(vg, points.get(), points.get());
                        while (points.remaining() >= 6){
                            nvgBezierTo(vg, points.get(), points.get(), points.get(), points.get(), points.get(), points.get());
                        }
                        if (path.closed() == 1) {
                            nvgLineTo(vg, points.get(0), points.get(1));
                        }
                        nvgStroke(vg);
                        nvgClosePath(vg);
                        path = path.next();
                    }
                    shape = shape.next();
                }
                path.free();
                shape.free();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }*/

    // gl

    public static void drawScaledString(String text, float x, float y, int color, boolean shadow, float scale) { //todo replace eventually with either nanovg or UMatrixStack
        UGraphics.GL.pushMatrix();
        UGraphics.GL.scale(scale, scale, 1);
        UMinecraft.getFontRenderer().drawString(text, x * (1 / scale), y * (1 / scale), color, shadow);
        UGraphics.GL.popMatrix();
    }

    public static void drawGlRect(int x, int y, int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
    }
}
