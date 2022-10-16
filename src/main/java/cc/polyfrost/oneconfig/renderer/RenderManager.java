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

package cc.polyfrost.oneconfig.renderer;

import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.libs.universal.UGraphics;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.platform.NanoVGPlatform;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.font.Font;
import cc.polyfrost.oneconfig.renderer.font.FontManager;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Handles NanoVG rendering and wraps it in a more convenient interface.
 */
public final class RenderManager {
    private static long vg = -1;

    //nanovg

    private RenderManager() {

    }

    /**
     * Sets up rendering, calls the consumer with the NanoVG context, and then cleans up.
     *
     * @param consumer The consumer to call.
     * @see RenderManager#setupAndDraw(boolean, LongConsumer)
     */
    public static void setupAndDraw(LongConsumer consumer) {
        setupAndDraw(false, consumer);
    }

    /**
     * Sets up rendering, calls the consumer with the NanoVG context, and then cleans up.
     *
     * @param mcScaling Whether to render with Minecraft's scaling.
     * @param consumer  The consumer to call.
     */
    public static void setupAndDraw(boolean mcScaling, LongConsumer consumer) {
        if (vg == -1) {
            vg = Platform.getNanoVGPlatform().nvgCreate(NanoVGPlatform.NVG_ANTIALIAS);
            if (vg == -1) {
                throw new RuntimeException("Failed to create nvg context");
            }
            FontManager.INSTANCE.initialize(vg);
        }

        Platform.getGLPlatform().enableStencil();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_ALPHA_TEST);

        beginFrame(mcScaling);

        consumer.accept(vg);

        endFrame();

        GL11.glPopAttrib();
    }

    public static void beginFrame(boolean mcScaling) {
        if (mcScaling) {
            nvgBeginFrame(vg, (float) UResolution.getScaledWidth(), (float) UResolution.getScaledHeight(), (float) UResolution.getScaleFactor());
        } else {
            nvgBeginFrame(vg, UResolution.getWindowWidth(), UResolution.getWindowHeight(), 1);
        }
    }

    public static void endFrame() {
        nvgEndFrame(vg);
    }

    /**
     * Draws a rectangle with the given parameters.
     *
     * @param vg     The NanoVG context.
     * @param x      The x position.
     * @param y      The y position.
     * @param width  The width.
     * @param height The height.
     * @param color  The color.
     */
    public static void drawRect(long vg, float x, float y, float width, float height, int color) {
        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, height);
        NVGColor nvgColor = color(vg, color);
        nvgFill(vg);
        nvgColor.free();
    }

    /**
     * Draws a rounded rectangle with the given parameters.
     *
     * @param vg     The NanoVG context.
     * @param x      The x position.
     * @param y      The y position.
     * @param width  The width.
     * @param height The height.
     * @param color  The color.
     * @param radius The radius.
     */
    public static void drawRoundedRect(long vg, float x, float y, float width, float height, int color, float radius) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
        color(vg, color);
        NVGColor nvgColor = color(vg, color);
        nvgFill(vg);
        nvgColor.free();
    }

    /**
     * Draw a rounded rectangle where every corner has a different radius
     *
     * @param vg       The NanoVG context
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     * @param color    The color.
     * @param radiusTL Top left corner radius.
     * @param radiusTR Top right corner radius.
     * @param radiusBR Bottom right corner radius.
     * @param radiusBL Bottom left corner radius
     */
    public static void drawRoundedRectVaried(long vg, float x, float y, float width, float height, int color, float radiusTL, float radiusTR, float radiusBR, float radiusBL) {
        nvgBeginPath(vg);
        nvgRoundedRectVarying(vg, x, y, width, height, radiusTL, radiusTR, radiusBR, radiusBL);
        color(vg, color);
        NVGColor nvgColor = color(vg, color);
        nvgFill(vg);
        nvgColor.free();
    }

    /**
     * Draws a hollow rounded rectangle with the given parameters.
     *
     * @param vg        The NanoVG context.
     * @param x         The x position.
     * @param y         The y position.
     * @param width     The width.
     * @param height    The height.
     * @param color     The color.
     * @param radius    The radius.
     * @param thickness The thickness.
     */
    public static void drawHollowRoundRect(long vg, float x, float y, float width, float height, int color, float radius, float thickness) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + thickness, y + thickness, width - thickness, height - thickness, radius);
        nvgStrokeWidth(vg, thickness + 0.5f);
        nvgPathWinding(vg, NVG_HOLE);
        color(vg, color);
        NVGColor nvgColor = color(vg, color);
        nvgStrokeColor(vg, nvgColor);
        nvgStroke(vg);
        nvgColor.free();
    }

    /**
     * Draws a gradient rectangle with the given parameters.
     *
     * @param vg     The NanoVG context.
     * @param x      The x position.
     * @param y      The y position.
     * @param width  The width.
     * @param height The height.
     * @param color  The first color of the gradient.
     * @param color2 The second color of the gradient.
     */
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

    /**
     * Draws a rounded gradient rectangle with the given parameters.
     *
     * @param vg     The NanoVG context.
     * @param x      The x position.
     * @param y      The y position.
     * @param width  The width.
     * @param height The height.
     * @param color  The first color of the gradient.
     * @param color2 The second color of the gradient.
     * @param radius The corner radius.
     */
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

    /**
     * Draw a HSB box
     *
     * @param vg          The NanoVG context.
     * @param x           The x coordinate.
     * @param y           The y coordinate
     * @param width       The width.
     * @param height      The height.
     * @param colorTarget Hue color
     */
    public static void drawHSBBox(long vg, float x, float y, float width, float height, int colorTarget) {
        drawRoundedRect(vg, x, y, width, height, colorTarget, 8f);

        NVGPaint bg = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, 8f);
        NVGColor nvgColor = color(vg, -1);
        NVGColor nvgColor2 = color(vg, Colors.TRANSPARENT);
        nvgFillPaint(vg, nvgLinearGradient(vg, x, y, x + width, y, nvgColor, nvgColor2, bg));
        nvgFill(vg);
        nvgColor.free();
        nvgColor2.free();

        NVGPaint bg2 = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, 8f);
        NVGColor nvgColor3 = color(vg, Colors.TRANSPARENT);
        NVGColor nvgColor4 = color(vg, Colors.BLACK);
        nvgFillPaint(vg, nvgLinearGradient(vg, x, y, x, y + height, nvgColor3, nvgColor4, bg2));
        nvgFill(vg);
        nvgColor3.free();
        nvgColor4.free();
    }

    /**
     * Draws a circle with the given parameters.
     *
     * @param vg     The NanoVG context.
     * @param x      The x position.
     * @param y      The y position.
     * @param radius The radius.
     * @param color  The color.
     */
    public static void drawCircle(long vg, float x, float y, float radius, int color) {
        nvgBeginPath(vg);
        nvgCircle(vg, x, y, radius);
        NVGColor nvgColor = color(vg, color);
        nvgFill(vg);
        nvgColor.free();
    }

    /**
     * Draws a String with the given parameters.
     *
     * @param vg    The NanoVG context.
     * @param text  The text.
     * @param x     The x position.
     * @param y     The y position.
     * @param color The color.
     * @param size  The size.
     * @param font  The font.
     * @see cc.polyfrost.oneconfig.renderer.font.Font
     */
    public static void drawText(long vg, String text, float x, float y, int color, float size, Font font) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, font.getName());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        NVGColor nvgColor = color(vg, color);
        nvgText(vg, x, y, text);
        nvgFill(vg);
        nvgColor.free();
    }

    /**
     * Draws a String wrapped at the given width, with the given parameters.
     *
     * @param vg    The NanoVG context.
     * @param text  The text.
     * @param x     The x position.
     * @param y     The y position.
     * @param width The width.
     * @param color The color.
     * @param size  The size.
     * @param font  The font.
     */
    public static void drawWrappedString(long vg, String text, float x, float y, float width, int color, float size, float lineHeight, Font font) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, font.getName());
        nvgTextLineHeight(vg, lineHeight);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP); // Align top because center is weird with wrapping
        NVGColor nvgColor = color(vg, color);
        nvgTextBox(vg, x, y, width, text);
        nvgFill(vg);
        nvgColor.free();
    }

    public static float getWrappedStringHeight(long vg, String text, float width, float fontSize, float lineHeight, Font font) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, font.getName());
        nvgTextLineHeight(vg, lineHeight);
        nvgTextBoxBounds(vg, 0, 0, width, text, bounds);
        return bounds[3] - bounds[1];
    }

    /**
     * Draw a formatted URL (a string in blue with an underline) that when clicked, opens the given text.
     *
     * <p><b>This does NOT scale to Minecraft's GUI scale!</b></p>
     *
     * @see RenderManager#drawText(long, String, float, float, int, float, Font)
     * @see InputHandler#isAreaClicked(float, float, float, float)
     */
    public static void drawURL(long vg, String url, float x, float y, float size, Font font, InputHandler inputHandler) {
        drawText(vg, url, x, y, Colors.PRIMARY_500, size, font);
        float length = getTextWidth(vg, url, size, font);
        drawRect(vg, x, y + size / 2, length, 1, Colors.PRIMARY_500);
        if (inputHandler.isAreaClicked((int) (x - 2), (int) (y - 1), (int) (length + 4), (int) (size / 2 + 3))) {
            NetworkUtils.browseLink(url);
        }
    }

    /**
     * Draws an image with the provided file path.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     * @see RenderManager#drawImage(long, String, float, float, float, float, int)
     */
    public static void drawImage(long vg, String filePath, float x, float y, float width, float height) {
        if (AssetLoader.INSTANCE.loadImage(vg, filePath)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = AssetLoader.INSTANCE.getImage(filePath);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    /**
     * Draws an image with the provided file path.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     * @param color    The color.
     */
    public static void drawImage(long vg, String filePath, float x, float y, float width, float height, int color) {
        if (AssetLoader.INSTANCE.loadImage(vg, filePath)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = AssetLoader.INSTANCE.getImage(filePath);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRGBA((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF), imagePaint.innerColor());
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    /**
     * Draws an image with the provided file path and parameters.
     *
     * @see RenderManager#drawImage(long, String, float, float, float, float)
     */
    public static void drawImage(long vg, Image image, float x, float y, float width, float height) {
        if (AssetLoader.INSTANCE.loadImage(vg, image)) {
            drawImage(vg, image.filePath, x, y, width, height);
        }
    }

    /**
     * Draws an image with the provided file path and parameters.
     *
     * @see RenderManager#drawImage(long, String, float, float, float, float, int)
     */
    public static void drawImage(long vg, Image image, float x, float y, float width, float height, int color) {
        if (AssetLoader.INSTANCE.loadImage(vg, image)) {
            drawImage(vg, image.filePath, x, y, width, height, color);
        }
    }

    /**
     * Draws a rounded image with the provided file path and parameters.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     * @param radius   The radius.
     */
    public static void drawRoundImage(long vg, String filePath, float x, float y, float width, float height, float radius) {
        if (AssetLoader.INSTANCE.loadImage(vg, filePath)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = AssetLoader.INSTANCE.getImage(filePath);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRoundedRect(vg, x, y, width, height, radius);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    /**
     * Draws a rounded image with the provided file path and parameters.
     *
     * @see RenderManager#drawRoundImage(long, String, float, float, float, float, float)
     */
    public static void drawRoundImage(long vg, Image image, float x, float y, float width, float height, float radius) {
        if (AssetLoader.INSTANCE.loadImage(vg, image)) {
            drawRoundImage(vg, image.filePath, x, y, width, height, radius);
        }
    }

    /**
     * Get the width of the provided String.
     *
     * @param vg       The NanoVG context.
     * @param text     The text.
     * @param fontSize The font size.
     * @param font     The font.
     * @return The width of the text.
     */
    public static float getTextWidth(long vg, String text, float fontSize, Font font) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, font.getName());
        return nvgTextBounds(vg, 0, 0, text, bounds);
    }

    /**
     * Draws a line with the provided parameters.
     *
     * @param vg    The NanoVG context.
     * @param x     The x position.
     * @param y     The y position.
     * @param endX  The end x position.
     * @param endY  The end y position.
     * @param width The width.
     * @param color The color.
     */
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

    /**
     * Draw a drop shadow.
     * <a href="https://github.com/SpinyOwl/legui/blob/develop/LICENSE">Adapted from legui under MIT license</a>
     *
     * @param vg           The NanoVG context.
     * @param x            The x coordinate.
     * @param y            The y coordinate.
     * @param w            The width.
     * @param h            The height.
     * @param blur         The blur (feather).
     * @param spread       The spread.
     * @param cornerRadius The radius of the corner
     */
    public static void drawDropShadow(long vg, float x, float y, float w, float h, float blur, float spread, float cornerRadius) {
        try (NVGPaint shadowPaint = NVGPaint.calloc();  // allocating memory to pass color to nanovg wrapper
             NVGColor firstColor = NVGColor.calloc();  // allocating memory to pass color to nanovg wrapper
             NVGColor secondColor = NVGColor.calloc()  // allocating memory to pass color to nanovg wrapper
        ) {
            fillNVGColorWithRGBA(0, 0, 0, 0.5f, firstColor); // filling allocated memory
            fillNVGColorWithRGBA(0, 0, 0, 0, secondColor); // filling allocated memory

            // creating gradient and put it to shadowPaint
            nvgBoxGradient(vg, x - spread, y - spread, w + 2 * spread, h + 2 * spread, cornerRadius + spread, blur, firstColor, secondColor, shadowPaint);
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x - spread - blur, y - spread - blur, w + 2 * spread + 2 * blur, h + 2 * spread + 2 * blur, cornerRadius + spread);
            nvgRoundedRect(vg, x, y, w, h, cornerRadius);
            nvgPathWinding(vg, NVG_HOLE);
            nvgFillPaint(vg, shadowPaint);
            nvgFill(vg);
        }
    }

    /**
     * Fills the provided {@link NVGColor} with the provided RGBA values.
     *
     * @param r     The red value.
     * @param g     The green value.
     * @param b     The blue value.
     * @param a     The alpha value.
     * @param color The {@link NVGColor} to fill.
     */
    public static void fillNVGColorWithRGBA(float r, float g, float b, float a, NVGColor color) {
        color.r(r);
        color.g(g);
        color.b(b);
        color.a(a);
    }

    /**
     * Create a {@link NVGColor} from the provided RGBA values.
     *
     * @param vg    The NanoVG context.
     * @param color The color.
     * @return The {@link NVGColor} created.
     */
    public static NVGColor color(long vg, int color) {
        NVGColor nvgColor = NVGColor.calloc();
        nvgRGBA((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF), nvgColor);
        nvgFillColor(vg, nvgColor);
        return nvgColor;
    }

    /**
     * Scales all rendering by the provided scale.
     *
     * @param vg The NanoVG context.
     * @param x  The x scale.
     * @param y  The y scale.
     */
    public static void scale(long vg, float x, float y) {
        nvgScale(vg, x, y);
    }

    /**
     * Translates all rendering by the provided x and y values.
     *
     * @param vg The NanoVG context.
     * @param x  The x value.
     * @param y  The y value.
     */
    public static void translate(long vg, float x, float y) {
        nvgTranslate(vg, x, y);
    }

    /**
     * Rotates all rendering by the provided angle.
     *
     * @param vg The NanoVG context.
     * @param a  The angle.
     */
    public static void rotate(long vg, float a) {
        nvgRotate(vg, a);
    }

    /**
     * Resets all prior transformations. <br>
     * <h1><b>NOTE: If you are using this inside the OneConfig GUI, DON'T!<br> It will break things!</b></h1>
     */
    public static void resetTransform(long vg) {
        nvgResetTransform(vg);
    }

    /**
     * Sets the global alpha value to render with.
     *
     * @param vg    The NanoVG context.
     * @param alpha The alpha value.
     */
    public static void setAlpha(long vg, float alpha) {
        nvgGlobalAlpha(vg, alpha);
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     * @param scale    The scale
     */
    public static void drawSvg(long vg, String filePath, float x, float y, float width, float height, float scale) {
        float w = width * scale;
        float h = height * scale;
        if (AssetLoader.INSTANCE.loadSVG(vg, filePath, w, h)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = AssetLoader.INSTANCE.getSVG(filePath, w, h);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     */
    public static void drawSvg(long vg, String filePath, float x, float y, float width, float height) {
        if (OneConfigGui.isOpen()) drawSvg(vg, filePath, x, y, width, height, OneConfigGui.getScaleFactor());
        else drawSvg(vg, filePath, x, y, width, height, 1f);
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     * @param color    The color.
     * @param scale    The scale
     */
    public static void drawSvg(long vg, String filePath, float x, float y, float width, float height, int color, float scale) {
        float w = width * scale;
        float h = height * scale;
        if (AssetLoader.INSTANCE.loadSVG(vg, filePath, w, h)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = AssetLoader.INSTANCE.getSVG(filePath, w, h);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRGBA((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF), imagePaint.innerColor());
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     * @param color    The color.
     */
    public static void drawSvg(long vg, String filePath, float x, float y, float width, float height, int color) {
        if (OneConfigGui.isOpen()) drawSvg(vg, filePath, x, y, width, height, color, OneConfigGui.getScaleFactor());
        else drawSvg(vg, filePath, x, y, width, height, color, 1f);
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see RenderManager#drawSvg(long, String, float, float, float, float)
     */
    public static void drawSvg(long vg, SVG svg, float x, float y, float width, float height, float scale) {
        drawSvg(vg, svg.filePath, x, y, width, height, scale);
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see RenderManager#drawSvg(long, String, float, float, float, float)
     */
    public static void drawSvg(long vg, SVG svg, float x, float y, float width, float height) {
        drawSvg(vg, svg.filePath, x, y, width, height);
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see RenderManager#drawSvg(long, String, float, float, float, float, int)
     */
    public static void drawSvg(long vg, SVG svg, float x, float y, float width, float height, int color, float scale) {
        drawSvg(vg, svg.filePath, x, y, width, height, color, scale);
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see RenderManager#drawSvg(long, String, float, float, float, float)
     */
    public static void drawSvg(long vg, SVG svg, float x, float y, float width, float height, int color) {
        if (OneConfigGui.isOpen()) drawSvg(vg, svg, x, y, width, height, color, OneConfigGui.getScaleFactor());
        else drawSvg(vg, svg, x, y, width, height, color, 1f);
    }

    /**
     * Draw a circle with an info icon inside it
     *
     * @param vg   The NanoVG context.
     * @param type The icon type.
     * @param x    The x position.
     * @param y    The y position.
     * @param size The diameter.
     */
    public static void drawInfo(long vg, InfoType type, float x, float y, float size) {
        SVG icon = null;
        int colorOuter = 0;
        int colorInner = 0;
        switch (type) {
            case INFO:
                icon = SVGs.INFO_CIRCLE;
                colorOuter = Colors.GRAY_400;
                colorInner = Colors.GRAY_300;
                break;
            case SUCCESS:
                icon = SVGs.CHECK_CIRCLE;
                colorOuter = Colors.SUCCESS_700;
                colorInner = Colors.SUCCESS_600;
                break;
            case WARNING:
                icon = SVGs.WARNING;
                colorOuter = Colors.WARNING_600;
                colorInner = Colors.WARNING_500;
                break;
            case ERROR:
                icon = SVGs.ERROR;
                colorOuter = Colors.ERROR_700;
                colorInner = Colors.ERROR_600;
                break;
        }
        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        drawCircle(vg, centerX, centerY, size / 2, colorOuter);
        drawCircle(vg, centerX, centerY, size / 2 - size / 12, colorInner);
        float iconSize = size / 1.75f;
        drawSvg(vg, icon, centerX - iconSize / 2f, centerY - iconSize / 2f, iconSize, iconSize);
    }

    // gl

    private static final Pattern regex = Pattern.compile("(?i)\\\\u00A7[0-9a-f]");

    public static int drawBorderedText(String text, float x, float y, int color, int opacity) {
        String noColors = regex.matcher(text).replaceAll("\u00A7r");
        int yes = 0;
        if (opacity > 3) {
            int xOff = -3;
            while (xOff < 3) {
                xOff++;
                int yOff = -3;
                while (yOff < 3) {
                    yOff++;
                    if (xOff * xOff != yOff * yOff) {
                        yes +=
                                Platform.getGLPlatform().drawText(
                                        noColors, (xOff / 2f) + x, (yOff / 2f) + y, (opacity) << 24, false
                                );
                    }
                }
            }
        }
        yes +=
                Platform.getGLPlatform().drawText(text, x, y, color, false);
        return yes;
    }

    public static void drawScaledString(String text, float x, float y, int color, TextType type, float scale) {
        UGraphics.GL.pushMatrix();
        UGraphics.GL.scale(scale, scale, 1);
        switch (type) {
            case NONE:
                Platform.getGLPlatform().drawText(text, x * (1 / scale), y * (1 / scale), color, false);
                break;
            case SHADOW:
                Platform.getGLPlatform().drawText(text, x * (1 / scale), y * (1 / scale), color, true);
                break;
            case FULL:
                drawBorderedText(text, x * (1 / scale), y * (1 / scale), color, 100);
                break;
        }
        UGraphics.GL.popMatrix();
    }

    public static void drawGlRect(int x, int y, int width, int height, int color) {
        Platform.getGLPlatform().drawRect(x, y, x + width, y + height, color);
    }

    public enum TextType {
        NONE, SHADOW, FULL;

        public static TextType toType(int type) {
            return values()[type];
        }
    }
}
