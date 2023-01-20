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

package cc.polyfrost.oneconfig.renderer;

import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.renderer.asset.Image;
import cc.polyfrost.oneconfig.renderer.asset.SVG;
import cc.polyfrost.oneconfig.renderer.font.Font;
import cc.polyfrost.oneconfig.utils.InputHandler;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.nanovg.NVGColor;

import java.util.function.LongConsumer;

/**
 * Handles NanoVG rendering and wraps it in a more convenient interface.
 */
public interface NanoVGHelper {
    @SuppressWarnings("deprecation")
    NanoVGHelper INSTANCE = LwjglManager.INSTANCE.getNanoVGHelper();

    /**
     * Sets up rendering, calls the consumer with the NanoVG context, and then cleans up.
     *
     * @param consumer The consumer to call.
     * @see NanoVGHelper#setupAndDraw(boolean, LongConsumer)
     */
    void setupAndDraw(LongConsumer consumer);

    /**
     * Sets up rendering, calls the consumer with the NanoVG context, and then cleans up.
     *
     * @param mcScaling Whether to render with Minecraft's scaling.
     * @param consumer  The consumer to call.
     */
    void setupAndDraw(boolean mcScaling, LongConsumer consumer);

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
    void drawRect(long vg, float x, float y, float width, float height, int color);

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
    void drawRoundedRect(long vg, float x, float y, float width, float height, int color, float radius);

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
    void drawRoundedRectVaried(long vg, float x, float y, float width, float height, int color, float radiusTL, float radiusTR, float radiusBR, float radiusBL);

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
    void drawHollowRoundRect(long vg, float x, float y, float width, float height, int color, float radius, float thickness);

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
    void drawGradientRect(long vg, float x, float y, float width, float height, int color, int color2, GradientDirection direction);

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
    void drawGradientRoundedRect(long vg, float x, float y, float width, float height, int color, int color2, float radius, GradientDirection direction);

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
    void drawHSBBox(long vg, float x, float y, float width, float height, int colorTarget);

    /**
     * Draws a circle with the given parameters.
     *
     * @param vg     The NanoVG context.
     * @param x      The x position.
     * @param y      The y position.
     * @param radius The radius.
     * @param color  The color.
     */
    void drawCircle(long vg, float x, float y, float radius, int color);

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
    void drawText(long vg, String text, float x, float y, int color, float size, Font font);

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
    void drawWrappedString(long vg, String text, float x, float y, float width, int color, float size, Font font);

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
     * @param lineHeight The line's height.
     * @param font       The font.
     */
    void drawWrappedString(long vg, String text, float x, float y, float width, int color, float size, float lineHeight, Font font);

    float[] getWrappedStringBounds(long vg, String text, float width, float fontSize, Font font);

    float[] getWrappedStringBounds(long vg, String text, float width, float fontSize, float lineHeight, Font font);

    float getWrappedStringHeight(long vg, String text, float width, float fontSize, float lineHeight, Font font);

    float getWrappedStringWidth(long vg, String text, float width, float fontSize, Font font);

    float getWrappedStringWidth(long vg, String text, float width, float fontSize, float lineHeight, Font font);

    /**
     * Draw a formatted URL (a string in blue with an underline) that when clicked, opens the given text.
     *
     * <p><b>This does NOT scale to Minecraft's GUI scale!</b></p>
     *
     * @see NanoVGHelper#drawText(long, String, float, float, int, float, Font)
     * @see InputHandler#isAreaClicked(float, float, float, float)
     */
    void drawURL(long vg, String url, float x, float y, float size, Font font, InputHandler inputHandler);

    /**
     * Draws an image with the provided file path.
     *
     * @param vg       The NanoVG context.
     * @param filePath The file path.
     * @param x        The x position.
     * @param y        The y position.
     * @param width    The width.
     * @param height   The height.
     * @see NanoVGHelper#drawImage(long, String, float, float, float, float, int)
     */
    void drawImage(long vg, String filePath, float x, float y, float width, float height);

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
    void drawImage(long vg, String filePath, float x, float y, float width, float height, int color);

    /**
     * Draws an image with the provided file path and parameters.
     *
     * @see NanoVGHelper#drawImage(long, String, float, float, float, float)
     */
    void drawImage(long vg, Image image, float x, float y, float width, float height);

    /**
     * Draws an image with the provided file path and parameters.
     *
     * @see NanoVGHelper#drawImage(long, String, float, float, float, float, int)
     */
    void drawImage(long vg, Image image, float x, float y, float width, float height, int color);

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
    void drawRoundImage(long vg, String filePath, float x, float y, float width, float height, float radius);

    /**
     * Draws a rounded image with the provided file path and parameters.
     *
     * @see NanoVGHelper#drawRoundImage(long, String, float, float, float, float, float)
     */
    void drawRoundImage(long vg, Image image, float x, float y, float width, float height, float radius);

    /**
     * Get the width of the provided String.
     *
     * @param vg       The NanoVG context.
     * @param text     The text.
     * @param fontSize The font size.
     * @param font     The font.
     * @return The width of the text.
     */
    float getTextWidth(long vg, String text, float fontSize, Font font);

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
    void drawLine(long vg, float x, float y, float endX, float endY, float width, int color);

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
    void drawDropShadow(long vg, float x, float y, float w, float h, float blur, float spread, float cornerRadius);

    void fillNVGColorWithRGBA(float r, float g, float b, float a, NVGColor color);

    NVGColor color(long vg, int color);

    /**
     * Scales all rendering by the provided scale.
     *
     * @param vg The NanoVG context.
     * @param x  The x scale.
     * @param y  The y scale.
     */
    void scale(long vg, float x, float y);

    void resetTransform(long vg);

    /**
     * Sets the global alpha value to render with.
     *
     * @param vg    The NanoVG context.
     * @param alpha The alpha value.
     */
    void setAlpha(long vg, float alpha);

    void drawSvg(long vg, String filePath, float x, float y, float width, float height, float scale);

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
    void drawSvg(long vg, String filePath, float x, float y, float width, float height);

    void drawSvg(long vg, String filePath, float x, float y, float width, float height, int color, float scale);

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
     */
    void drawSvg(long vg, String filePath, float x, float y, float width, float height, int color);

    void drawSvg(long vg, SVG svg, float x, float y, float width, float height, float scale);

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see NanoVGHelper#drawSvg(long, String, float, float, float, float)
     */
    void drawSvg(long vg, SVG svg, float x, float y, float width, float height);

    void drawSvg(long vg, SVG svg, float x, float y, float width, float height, int color, float scale);

    /**
     * Draws an SVG with the provided file path and parameters.
     *
     * @see NanoVGHelper#drawSvg(long, String, float, float, float, float, int)
     */
    void drawSvg(long vg, SVG svg, float x, float y, float width, float height, int color);

    /**
     * Draw a circle with an info icon inside of it
     *
     * @param vg   The NanoVG context.
     * @param type The icon type.
     * @param x    The x position.
     * @param y    The y position.
     * @param size The diameter.
     */
    void drawInfo(long vg, InfoType type, float x, float y, float size);

    /**
     * Reads pixel colors from the screen. <br>
     * Due to the nature of how this works, this will <b>return the previous frame's data</b>, because the read operation has to be executed OUTSIDE the vg frame.
     *
     * @return the previous frame's data. For the first call, this method will return 0 (transparent).
     */
    int[] readPixels(int x, int y, int width, int height);

    boolean isDrawing();

    /**
     * @deprecated Use {@link cc.polyfrost.oneconfig.renderer.TextRenderer} instead.
     */
    @Deprecated
    int drawBorderedText(String text, float x, float y, int color, int opacity);

    /**
     * @deprecated Use {@link cc.polyfrost.oneconfig.renderer.TextRenderer} instead.
     */
    @Deprecated
    void drawScaledString(String text, float x, float y, int color, TextType type, float scale);

    void translate(long vg, float x, float y);

    void rotate(long vg, double angle);

    enum TextType {
        NONE, SHADOW, FULL;

        public static TextType toType(int type) {
            return values()[type];
        }
    }

    enum GradientDirection {
        /**
         * Top to bottom
         */
        DOWN,
        /**
         * Bottom to top
         */
        UP,
        /**
         * Left to right
         */
        LEFT,
        /**
         * Right to left
         */
        RIGHT,
        /**
         * Top left to bottom right
         */
        DIAGONAL_DOWN,
        /**
         * Bottom right to top left
         */
        DIAGONAL_UP;

        /**
         * return the positions needed for the gradient to work, based on the given rectangle.
         *
         * @return float array of positions, in the order of sx, sy, ex, ey
         */
        public static float[] getValues(float x, float y, float width, float height, @NotNull GradientDirection direction) {
            switch (direction) {
                case DOWN:
                default:
                    return new float[]{x, y, x, y + height};
                case UP:
                    return new float[]{x, y + height, x, y};
                case LEFT:
                    return new float[]{x + width, y, x, y};
                case RIGHT:
                    return new float[]{x, y, x + width, y};
                case DIAGONAL_DOWN:
                    return new float[]{x, y, x + width, y + height};
                case DIAGONAL_UP:
                    return new float[]{x, y + height, x + width, y};
            }
        }
    }
}
