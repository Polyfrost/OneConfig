/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.internal.renderer;

import org.polyfrost.oneconfig.events.EventManager;
import org.polyfrost.oneconfig.events.event.FramebufferRenderEvent;
import org.polyfrost.oneconfig.events.event.Stage;
import org.polyfrost.oneconfig.libs.eventbus.Subscribe;
import org.polyfrost.oneconfig.libs.universal.UGraphics;
import org.polyfrost.oneconfig.libs.universal.UResolution;
import org.polyfrost.oneconfig.platform.Platform;
import org.polyfrost.oneconfig.renderer.NanoVGHelper;
import org.polyfrost.oneconfig.renderer.TextRenderer;
import org.polyfrost.oneconfig.renderer.asset.AssetHelper;
import org.polyfrost.oneconfig.renderer.asset.Image;
import org.polyfrost.oneconfig.renderer.asset.SVG;
import org.polyfrost.oneconfig.renderer.font.Font;
import org.polyfrost.oneconfig.renderer.font.FontHelper;
import org.polyfrost.oneconfig.utils.IOUtils;
import org.polyfrost.oneconfig.utils.InputHandler;
import org.polyfrost.oneconfig.utils.NetworkUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.opengl.GL11;
import org.polyfrost.oneconfig.utils.color.ColorUtils;

import java.util.function.LongConsumer;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Handles NanoVG rendering and wraps it in a more convenient interface.
 */
public final class NanoVGHelperImpl implements NanoVGHelper {
    private long vg = -1;
    private boolean drawing = false;
    private boolean goingToCancel = false;

    //nanovg

    public NanoVGHelperImpl() {
        EventManager.INSTANCE.register(new Object() {
            @Subscribe
            private void onFramebufferRender(FramebufferRenderEvent event) {
                if (event.stage == Stage.END) {
                    if (drawing) {
                        if (goingToCancel) {
                            drawing = false;
                            goingToCancel = false;
                        } else {
                            goingToCancel = true;
                        }
                    }
                }
            }
        });
    }

    /**
     * Sets up rendering, calls the consumer with the NanoVG context, and then cleans up.
     *
     * @param consumer The consumer to call.
     * @see NanoVGHelperImpl#setupAndDraw(boolean, LongConsumer)
     */
    @Override
    public void setupAndDraw(LongConsumer consumer) {
        setupAndDraw(false, consumer);
    }

    /**
     * Sets up rendering, calls the consumer with the NanoVG context, and then cleans up.
     *
     * @param mcScaling Whether to render with Minecraft's scaling.
     * @param consumer  The consumer to call.
     */
    @Override
    public void setupAndDraw(boolean mcScaling, LongConsumer consumer) {
        drawing = true;
        if (vg == -1) {
            vg = NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS);
            if (vg == -1) {
                throw new RuntimeException("Failed to create nvg context");
            }
            FontHelper.INSTANCE.initialize(vg);
        }

        Platform.getGLPlatform().enableStencil();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        UGraphics.disableAlpha();

        if (mcScaling) {
            nvgBeginFrame(vg, (float) UResolution.getScaledWidth(), (float) UResolution.getScaledHeight(), (float) UResolution.getScaleFactor());
        } else {
            nvgBeginFrame(vg, UResolution.getWindowWidth(), UResolution.getWindowHeight(), 1);
        }

        consumer.accept(vg);

        nvgEndFrame(vg);
        UGraphics.enableAlpha();
        GL11.glPopAttrib();
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
    @Override
    public void drawRect(long vg, float x, float y, float width, float height, int color) {
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
    @Override
    public void drawRoundedRect(long vg, float x, float y, float width, float height, int color, float radius) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, radius);
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
    public void drawRoundedRectVaried(long vg, float x, float y, float width, float height, int color, float radiusTL, float radiusTR, float radiusBR, float radiusBL) {
        nvgBeginPath(vg);
        nvgRoundedRectVarying(vg, x, y, width, height, radiusTL, radiusTR, radiusBR, radiusBL);
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
    @Override
    public void drawHollowRoundRect(long vg, float x, float y, float width, float height, int color, float radius, float thickness) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x + thickness, y + thickness, width - thickness, height - thickness, radius);
        nvgStrokeWidth(vg, thickness + 0.5f);
        nvgPathWinding(vg, NVG_HOLE);
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
    @Override
    public void drawGradientRect(long vg, float x, float y, float width, float height, int color, int color2) {
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
    @Override
    public void drawGradientRoundedRect(long vg, float x, float y, float width, float height, int color, int color2, float radius) {
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
    @Override
    public void drawHSBBox(long vg, float x, float y, float width, float height, int colorTarget) {
        drawRoundedRect(vg, x, y, width, height, colorTarget, 8f);

        NVGPaint bg = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, 8f);
        NVGColor nvgColor = color(vg, -1);
        NVGColor nvgColor2 = color(vg, ColorUtils.getColor(0, 0, 0, 0));
        nvgFillPaint(vg, nvgLinearGradient(vg, x, y, x + width, y, nvgColor, nvgColor2, bg));
        nvgFill(vg);
        nvgColor.free();
        nvgColor2.free();

        NVGPaint bg2 = NVGPaint.create();
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, 8f);
        NVGColor nvgColor3 = color(vg, ColorUtils.getColor(0, 0, 0, 0));
        NVGColor nvgColor4 = color(vg, ColorUtils.getColor(0, 0, 0, 255));
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
    @Override
    public void drawCircle(long vg, float x, float y, float radius, int color) {
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
     * @see Font
     */
    @Override
    public void drawText(long vg, String text, float x, float y, int color, float size, Font font) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, font.getName());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        NVGColor nvgColor = color(vg, color);
        nvgText(vg, x, y, text);
        nvgFill(vg);
        nvgColor.free();
    }


    @Override
    public void drawWrappedString(long vg, String text, float x, float y, float width, int color, float size, Font font) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, font.getName());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE); // Align top because center is weird with wrapping
        NVGColor nvgColor = color(vg, color);
        nvgTextBox(vg, x, y, width, text);
        nvgFill(vg);
        nvgColor.free();
    }

    /**
     * Draws a String wrapped at the given width, with the given parameters.
     *
     * @param vg         The NanoVG context.
     * @param text       The text.
     * @param x          The x position.
     * @param y          The y position.
     * @param width      The width.
     * @param color      The color.
     * @param size       The size.
     * @param lineHeight The line height.
     * @param font       The font.
     */
    @Override
    public void drawWrappedString(long vg, String text, float x, float y, float width, int color, float size, float lineHeight, Font font) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, font.getName());
        nvgTextLineHeight(vg, lineHeight);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE); // Align top because center is weird with wrapping
        NVGColor nvgColor = color(vg, color);
        nvgTextBox(vg, x, y, width, text);
        nvgFill(vg);
        nvgColor.free();
    }

    @Override
    public float[] getWrappedStringBounds(long vg, String text, float width, float fontSize, Font font) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, font.getName());
        nvgTextBoxBounds(vg, 0, 0, width, text, bounds);
        return bounds;
    }

    @Override
    public float[] getWrappedStringBounds(long vg, String text, float width, float fontSize, float lineHeight, Font font) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, font.getName());
        nvgTextLineHeight(vg, lineHeight);
        nvgTextBoxBounds(vg, 0, 0, width, text, bounds);
        return bounds;
    }

    @Override
    public float getWrappedStringHeight(long vg, String text, float width, float fontSize, float lineHeight, Font font) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, font.getName());
        nvgTextLineHeight(vg, lineHeight);
        nvgTextBoxBounds(vg, 0, 0, width, text, bounds);
        return bounds[3] - bounds[1];
    }

    @Override
    public float getWrappedStringWidth(long vg, String text, float width, float fontSize, Font font) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, font.getName());
        nvgTextBoxBounds(vg, 0, 0, width, text, bounds);
        return bounds[2] - bounds[0];
    }

    @Override
    public float getWrappedStringWidth(long vg, String text, float width, float fontSize, float lineHeight, Font font) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, font.getName());
        nvgTextLineHeight(vg, lineHeight);
        nvgTextBoxBounds(vg, 0, 0, width, text, bounds);
        return bounds[2] - bounds[0];
    }

    /**
     * Draw a formatted URL (a string in blue with an underline) that when clicked, opens the given text.
     *
     * <p><b>This does NOT scale to Minecraft's GUI scale!</b></p>
     *
     * @see NanoVGHelperImpl#drawText(long, String, float, float, int, float, Font)
     * @see InputHandler#isAreaClicked(float, float, float, float)
     */
    @Override
    public void drawURL(long vg, String url, float x, float y, int color, float size, Font font, InputHandler inputHandler) {
        drawText(vg, url, x, y, color, size, font);
        float length = getTextWidth(vg, url, size, font);
        drawRect(vg, x, y + size / 2, length, 1, color);
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
     * @see NanoVGHelperImpl#drawImage(long, String, float, float, float, float, int)
     */
    @Override
    @Deprecated
    public void drawImage(long vg, String filePath, float x, float y, float width, float height) {
        drawImage(vg, filePath, x, y, width, height, IOUtils.class);
    }

    @Override
    public void drawImage(long vg, String filePath, float x, float y, float width, float height, Class<?> clazz) {
        AssetHelper assetHelper = AssetHelper.INSTANCE;
        if (assetHelper.loadImage(vg, filePath, clazz)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = assetHelper.getImage(filePath);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    @Override
    @Deprecated
    public void drawImage(long vg, String filePath, float x, float y, float width, float height, int color) {
        drawImage(vg, filePath, x, y, width, height, color, IOUtils.class);
    }

    @Override
    public void drawImage(long vg, String filePath, float x, float y, float width, float height, int color, Class<?> clazz) {
        AssetHelper assetHelper = AssetHelper.INSTANCE;
        if (assetHelper.loadImage(vg, filePath, clazz)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = assetHelper.getImage(filePath);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            drawImageCommon(vg, x, y, width, height, color, imagePaint);
            imagePaint.free();
        }
    }

    /**
     * <b>Important:</b> You must free {@code imagePaint} yourself!
     */
    private void drawImageCommon(long vg, float x, float y, float width, float height, int color, NVGPaint imagePaint) {
        nvgRGBA((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF), imagePaint.innerColor());
        nvgRect(vg, x, y, width, height);
        nvgFillPaint(vg, imagePaint);
        nvgFill(vg);
    }

    /**
     * Draws an image with the provided file path and parameters.
     *
     * @see NanoVGHelperImpl#drawImage(long, String, float, float, float, float)
     */
    public void drawImage(long vg, Image image, float x, float y, float width, float height) {
        AssetHelper assetHelper = AssetHelper.INSTANCE;
        if (assetHelper.loadImage(vg, image)) {
            drawImage(vg, image.filePath, x, y, width, height, image.getClass());
        }
    }

    /**
     * Draws an image with the provided file path and parameters.
     *
     * @see NanoVGHelperImpl#drawImage(long, String, float, float, float, float, int)
     */
    @Override
    public void drawImage(long vg, Image image, float x, float y, float width, float height, int color) {
        AssetHelper assetHelper = AssetHelper.INSTANCE;
        if (assetHelper.loadImage(vg, image)) {
            drawImage(vg, image.filePath, x, y, width, height, color, image.getClass());
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
    @Override
    @Deprecated
    public void drawRoundImage(long vg, String filePath, float x, float y, float width, float height, float radius) {
        drawRoundImage(vg, filePath, x, y, width, height, radius, IOUtils.class);
    }

    @Override
    public void drawRoundImage(long vg, String filePath, float x, float y, float width, float height, float radius, Class<?> clazz) {
        AssetHelper assetHelper = AssetHelper.INSTANCE;
        if (assetHelper.loadImage(vg, filePath, clazz)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = assetHelper.getImage(filePath);
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
     * @see NanoVGHelperImpl#drawRoundImage(long, String, float, float, float, float, float)
     */
    @Override
    @Deprecated
    public void drawRoundImage(long vg, Image image, float x, float y, float width, float height, float radius) {
        if (AssetHelper.INSTANCE.loadImage(vg, image)) {
            drawRoundImage(vg, image.filePath, x, y, width, height, radius, image.getClass());
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
    @Override
    public float getTextWidth(long vg, String text, float fontSize, Font font) {
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
    @Override
    public void drawLine(long vg, float x, float y, float endX, float endY, float width, int color) {
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
    @Override
    public void drawDropShadow(long vg, float x, float y, float w, float h, float blur, float spread, float cornerRadius) {
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
    @Override
    public void fillNVGColorWithRGBA(float r, float g, float b, float a, NVGColor color) {
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
    @Override
    public NVGColor color(long vg, int color) {
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
    @Override
    public void scale(long vg, float x, float y) {
        nvgScale(vg, x, y);
    }

    /**
     * Translate to a location
     *
     * @param vg The NanoVG context
     * @param x  The x scale
     * @param y  The y scale
     */
    @Override
    public void translate(long vg, float x, float y) {
        nvgTranslate(vg, x, y);
    }

    @Override
    public void rotate(long vg, float angle) {
        nvgRotate(vg, angle);
    }

    /**
     * Reset all transforms
     *
     * @param vg The NanoVG context
     */
    @Override
    public void resetTransform(long vg) {
        nvgResetTransform(vg);
    }

    /**
     * Sets the global alpha value to render with.
     *
     * @param vg    The NanoVG context.
     * @param alpha The alpha value.
     */
    @Override
    public void setAlpha(long vg, float alpha) {
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
    @Override
    @Deprecated
    public void drawSvg(long vg, String filePath, float x, float y, float width, float height, float scale) {
        drawSvg(vg, filePath, x, y, width, height, scale, IOUtils.class);
    }

    @Override
    public void drawSvg(long vg, String filePath, float x, float y, float width, float height, float scale, Class<?> clazz) {
        float w = width * scale;
        float h = height * scale;
        AssetHelper assetHelper = AssetHelper.INSTANCE;
        if (assetHelper.loadSVG(vg, filePath, w, h, clazz)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = assetHelper.getSVG(filePath, w, h);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            nvgRect(vg, x, y, width, height);
            nvgFillPaint(vg, imagePaint);
            nvgFill(vg);
            imagePaint.free();
        }
    }

    /**
     * Draws a SVG with the provided file path and parameters.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     */
    @Deprecated
    @Override
    public void drawSvg(long vg, String filePath, float x, float y, float width, float height) {
        drawSvg(vg, filePath, x, y, width, height, IOUtils.class);
    }

    /**
     * Draws a SVG with the provided file path and parameters.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     */
    @Override
    public void drawSvg(long vg, String filePath, float x, float y, float width, float height, Class<?> clazz) {
        drawSvg(vg, filePath, x, y, width, height, 1f, clazz);
    }

    /**
     * Draws a SVG with the provided file path and parameters.
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
    @Override
    public void drawSvg(long vg, String filePath, float x, float y, float width, float height, int color, float scale) {
        drawSvg(vg, filePath, x, y, width, height, color, scale, IOUtils.class);
    }

    @Override
    public void drawSvg(long vg, String filePath, float x, float y, float width, float height, int color, float scale, Class<?> clazz) {
        float w = width * scale;
        float h = height * scale;
        AssetHelper assetHelper = AssetHelper.INSTANCE;
        if (assetHelper.loadSVG(vg, filePath, w, h, clazz)) {
            NVGPaint imagePaint = NVGPaint.calloc();
            int image = assetHelper.getSVG(filePath, w, h);
            nvgBeginPath(vg);
            nvgImagePattern(vg, x, y, width, height, 0, image, 1, imagePaint);
            drawImageCommon(vg, x, y, width, height, color, imagePaint);
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
    @Override
    public void drawSvg(long vg, String filePath, float x, float y, float width, float height, int color) {
        drawSvg(vg, filePath, x, y, width, height, color, IOUtils.class);
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
    @Override
    public void drawSvg(long vg, String filePath, float x, float y, float width, float height, int color, Class<?> clazz) {
        drawSvg(vg, filePath, x, y, width, height, color, 1f, clazz);
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see NanoVGHelperImpl#drawSvg(long, String, float, float, float, float)
     */
    @Override
    public void drawSvg(long vg, SVG svg, float x, float y, float width, float height, float scale) {
        drawSvg(vg, svg.filePath, x, y, width, height, scale, svg.getClass());
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see NanoVGHelperImpl#drawSvg(long, String, float, float, float, float)
     */
    @Override
    public void drawSvg(long vg, SVG svg, float x, float y, float width, float height) {
        drawSvg(vg, svg.filePath, x, y, width, height, svg.getClass());
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see NanoVGHelperImpl#drawSvg(long, String, float, float, float, float, int)
     */
    @Override
    public void drawSvg(long vg, SVG svg, float x, float y, float width, float height, int color, float scale) {
        drawSvg(vg, svg.filePath, x, y, width, height, color, scale, svg.getClass());
    }

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see NanoVGHelperImpl#drawSvg(long, String, float, float, float, float)
     */
    @Override
    public void drawSvg(long vg, SVG svg, float x, float y, float width, float height, int color) {
        drawSvg(vg, svg, x, y, width, height, color, 1f);
    }

    @Override
    public boolean isDrawing() {
        return drawing;
    }

    // gl

    /**
     * @deprecated Use {@link TextRenderer} instead.
     */
    @Deprecated
    public int drawBorderedText(String text, float x, float y, int color, int opacity) {
        return TextRenderer.drawBorderedText(text, x, y, color, opacity);
    }

    /**
     * @deprecated Use {@link TextRenderer} instead.
     */
    @Deprecated
    public void drawScaledString(String text, float x, float y, int color, TextType type, float scale) {
        TextRenderer.drawScaledString(text, x, y, color, TextRenderer.TextType.toType(type.ordinal()), scale);
    }
}
