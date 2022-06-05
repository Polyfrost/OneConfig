package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Font;
import cc.polyfrost.oneconfig.renderer.font.Fonts;

import java.util.ArrayList;

/**
 * Simple text utility class for NanoVG text rendering.
 */
public final class TextUtils {

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
    public static ArrayList<String> wrapText(long vg, String text, float maxWidth, float fontSize, Fonts font) {
        return wrapText(vg, text, maxWidth, fontSize, font.font);
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
    public static ArrayList<String> wrapText(long vg, String text, float maxWidth, float fontSize, Font font) {
        ArrayList<String> wrappedText = new ArrayList<>();
        text += " ";
        int prevIndex = 0;
        for (int i = text.indexOf(" "); i >= 0; i = text.indexOf(" ", i + 1)) {
            String textPart = text.substring(0, i);
            float textWidth = RenderManager.getTextWidth(vg, textPart, fontSize, font);
            if (textWidth < maxWidth) {
                prevIndex = i;
                continue;
            }
            wrappedText.add(text.substring(0, prevIndex) + " ");
            wrappedText.addAll(wrapText(vg, text.substring(prevIndex + 1), maxWidth, fontSize, font));
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
}
