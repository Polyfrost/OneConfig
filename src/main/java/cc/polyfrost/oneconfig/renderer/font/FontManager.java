package cc.polyfrost.oneconfig.renderer.font;

import cc.polyfrost.oneconfig.utils.IOUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import static org.lwjgl.nanovg.NanoVG.nvgCreateFontMem;

public class FontManager {
    public static FontManager INSTANCE = new FontManager();

    /**
     * Load all fonts in the Fonts class
     *
     * @param vg NanoVG context
     */

    public void initialize(long vg) {
        for (Field field : Fonts.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object font = field.get(null);
                if (!(font instanceof Font)) continue;
                loadFont(vg, (Font) font);
            } catch (Exception e) {
                throw new RuntimeException("Could not initialize fonts");
            }
        }
    }

    /**
     * Load a font into NanoVG
     *
     * @param vg   NanoVG context
     * @param font The font to be loaded
     */
    public void loadFont(long vg, Font font) {
        if (font.isLoaded()) return;
        int loaded = -1;
        try {
            ByteBuffer buffer = IOUtils.resourceToByteBuffer(font.getFileName());
            loaded = nvgCreateFontMem(vg, font.getName(), buffer, 0);
            font.setBuffer(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (loaded == -1) {
            throw new RuntimeException("Failed to initialize font " + font.getName());
        } else {
            font.setLoaded(true);
        }
    }
}
