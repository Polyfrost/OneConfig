package cc.polyfrost.oneconfig.renderer;

import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.renderer.font.Font;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import com.google.common.annotations.Beta;
import org.lwjgl.nanovg.NVGColor;

import java.util.ArrayList;

import static cc.polyfrost.oneconfig.renderer.RenderManager.color;
import static org.lwjgl.nanovg.NanoVG.*;

public class TextRenderer {
    /**
     * Draws a String with the given parameters.
     *
     * @param vg            The NanoVG context.
     * @param text          The text.
     * @param x             The x position.
     * @param y             The y position.
     * @param color         The color.
     * @param size          The size.
     * @param font          The font.
     * @param letterSpacing The letter spacing
     * @see cc.polyfrost.oneconfig.renderer.font.Font
     */
    public static void drawText(long vg, String text, float x, float y, int color, float size, Font font, float letterSpacing) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgTextLetterSpacing(vg, letterSpacing);
        nvgFontFace(vg, font.getName());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        NVGColor nvgColor = color(vg, color);
        nvgText(vg, x, y, text);
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
        drawText(vg, text, x, y, color, size, font, 0);
    }

    /**
     * Draws a String with the given parameters.
     *
     * @param vg            The NanoVG context.
     * @param text          The text.
     * @param x             The x position.
     * @param y             The y position.
     * @param color         The color.
     * @param size          The size.
     * @param font          The font.
     * @param lineHeight    The line height
     * @param letterSpacing The letter spacing
     * @see cc.polyfrost.oneconfig.renderer.font.Font
     */
    public static void drawText(long vg, ArrayList<String> text, float x, float y, int color, float size, Font font, float lineHeight, float letterSpacing) {
        float textY = y;
        for (String line : text) {
            drawText(vg, line, x, textY, color, size, font, letterSpacing);
            textY += lineHeight;
        }
    }

    /**
     * Draws a String with the given parameters.
     *
     * @param vg         The NanoVG context.
     * @param text       The text.
     * @param x          The x position.
     * @param y          The y position.
     * @param color      The color.
     * @param size       The size.
     * @param font       The font.
     * @param lineHeight The line height
     * @see cc.polyfrost.oneconfig.renderer.font.Font
     */
    public static void drawText(long vg, ArrayList<String> text, float x, float y, int color, float size, Font font, float lineHeight) {
        drawText(vg, text, x, y, color, size, font, lineHeight, 0);
    }

    /**
     * Draws a String wrapped at the given width, with the given parameters.
     *
     * @param vg            The NanoVG context.
     * @param text          The text.
     * @param x             The x position.
     * @param y             The y position.
     * @param width         The width.
     * @param color         The color.
     * @param size          The size.
     * @param font          The font.
     */
    public static void drawWrappedString(long vg, String text, float x, float y, float width, int color, float size, Font font) {
        nvgBeginPath(vg);
        nvgFontSize(vg, size);
        nvgFontFace(vg, font.getName());
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        NVGColor nvgColor = color(vg, color);
        nvgTextBox(vg, x, y, width, text);
        nvgFill(vg);
        nvgColor.free();
    }

    /**
     * Draw a formatted URL (a string in blue with an underline) that when clicked, opens the given text.
     *
     * <p><b>This does NOT scale to Minecraft's GUI scale!</b></p>
     *
     * @see RenderManager#drawText(long, String, float, float, int, float, Font)
     * @see InputUtils#isAreaClicked(float, float, float, float)
     */
    public static void drawURL(long vg, String url, float x, float y, float size, Font font) {
        drawText(vg, url, x, y, Colors.PRIMARY_500, size, font);
        float length = getTextWidth(vg, url, size, font);
        RenderManager.drawRect(vg, x, y + size / 2, length, 1, Colors.PRIMARY_500);
        if (InputUtils.isAreaClicked((int) (x - 2), (int) (y - 1), (int) (length + 4), (int) (size / 2 + 3))) {
            NetworkUtils.browseLink(url);
        }
    }

    /**
     * Get the width of the provided String.
     *
     * @param vg            The NanoVG context.
     * @param text          The text.
     * @param fontSize      The font size.
     * @param font          The font.
     * @param letterSpacing The letter spacing
     * @return The width of the text.
     */
    public static float getTextWidth(long vg, String text, float fontSize, Font font, float letterSpacing) {
        float[] bounds = new float[4];
        nvgFontSize(vg, fontSize);
        nvgTextLetterSpacing(vg, letterSpacing);
        nvgFontFace(vg, font.getName());
        return nvgTextBounds(vg, 0, 0, text, bounds);
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
        return getTextWidth(vg, text, fontSize, font, 0);
    }

    /**
     * Wraps a string into an array of lines.
     *
     * @param vg            The NanoVG context.
     * @param text          The text to wrap.
     * @param maxWidth      The maximum width of each line.
     * @param fontSize      The font size.
     * @param font          The font to use.
     * @param letterSpacing The letter spacing
     * @return The array of lines.
     */
    @Beta
    public static ArrayList<String> wrapText(long vg, String text, float maxWidth, float fontSize, Font font, float letterSpacing) {
        ArrayList<String> wrappedText = new ArrayList<>();
        text += " ";
        int prevIndex = 0;
        for (int i = text.indexOf(" "); i >= 0; i = text.indexOf(" ", i + 1)) {
            String textPart = text.substring(0, i);
            float textWidth = getTextWidth(vg, textPart, fontSize, font, letterSpacing);
            if (textWidth < maxWidth) {
                prevIndex = i;
                continue;
            }
            wrappedText.add(text.substring(0, prevIndex) + " ");
            wrappedText.addAll(wrapText(vg, text.substring(prevIndex + 1), maxWidth, fontSize, font, letterSpacing));
            break;
        }
        if (wrappedText.size() == 0) wrappedText.add(text);
        String temp = wrappedText.get(wrappedText.size() - 1);
        if (temp.length() != 0) {
            wrappedText.remove(wrappedText.size() - 1);
            wrappedText.add(temp.substring(0, temp.length() - 1));
        }
        return wrappedText;
    }

    /**
     * Wraps a string into an array of lines.
     *
     * @param vg       The NanoVG context.
     * @param text     The text to wrap.
     * @param maxWidth The maximum width of each line.
     * @param fontSize The font size.
     * @param font     The font to use.
     * @return The array of lines.
     */
    @Beta
    public static ArrayList<String> wrapText(long vg, String text, float maxWidth, float fontSize, Font font) {
        return wrapText(vg, text, maxWidth, fontSize, font, 0);
    }
}
