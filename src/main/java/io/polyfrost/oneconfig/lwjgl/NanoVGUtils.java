package io.polyfrost.oneconfig.lwjgl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL2.*;

public final class NanoVGUtils {
    private NanoVGUtils() {

    }
    private static long vg = -1;
    private static int font = -1;

    public static void setupAndDraw(LongConsumer consumer) {
        if (vg == -1) {
            vg = nvgCreate(NVG_ANTIALIAS);
            if (vg == -1) {
                throw new RuntimeException("Failed to create nvg context");
            }
        }
        if (font == -1) {
            try {
                font = nvgCreateFontMem(vg, "custom-font", IOUtil.resourceToByteBuffer("/assets/oneconfig/font/Roboto-Regular.ttf", 150 * 1024), 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (font == -1) {
                throw new RuntimeException("Failed to create custom font");
            }
        }

        Framebuffer fb = Minecraft.getMinecraft().getFramebuffer();
        if (!fb.isStencilEnabled()) {
            fb.enableStencil();
        }
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        nvgBeginFrame(vg, Display.getWidth(), Display.getHeight(), 1);

        consumer.accept(vg);

        nvgEndFrame(vg);

        GlStateManager.popAttrib();
    }

    public static void drawRect(long vg, float x, float y, float width, float height, int color) {
        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, height);
        color(vg, color);
        nvgFill(vg);
    }

    public static void drawRoundedRect(long vg, float x, float y, float width, float height, int color, float radius) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        color(vg, color);
        nvgFill(vg);
    }

    public static void drawCircle(long vg, float x, float y, float radius, int color) {
        nvgBeginPath(vg);
        nvgCircle(vg, x, y, radius);
        color(vg, color);
        nvgFill(vg);
    }

    public static void drawString(long vg, String text, float x, float y, int color, float size) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, "custom-font");
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer textByte = stack.ASCII(text, false);
            nvgFontBlur(vg, 0);
            color(vg, color);
            nvgText(vg, x, y, textByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void color(long vg, int color) {
        NVGColor nvgColor = NVGColor.create();
        nvgRGBA((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF), nvgColor);
        nvgFillColor(vg, nvgColor);
    }
}
