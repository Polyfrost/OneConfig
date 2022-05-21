package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextUtils {

    public static ArrayList<String> wrapText(long vg, String text, float maxWidth, float fontSize, Fonts font) {
        ArrayList<String> wrappedText = new ArrayList<>();
        List<String> split = Arrays.asList(text.split(" "));
        for (int i = split.size(); i >= 0; i--) {
            String textPart = String.join(" ", split.subList(0, i));
            float textWidth = RenderManager.getTextWidth(vg, textPart, fontSize, font);
            if (textWidth > maxWidth) continue;
            wrappedText.add(textPart);
            if (i != split.size())
                wrappedText.addAll(wrapText(vg, String.join(" ", split.subList(i, split.size())), maxWidth, fontSize, font));
            break;
        }
        if (text.endsWith(" ")) {
            String lastLine = wrappedText.get(wrappedText.size() - 1);
            lastLine += " ";
            wrappedText.remove(wrappedText.size() - 1);
            wrappedText.add(lastLine);
        }
        return wrappedText;
    }
}
