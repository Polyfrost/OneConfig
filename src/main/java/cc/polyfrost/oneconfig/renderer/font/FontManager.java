package cc.polyfrost.oneconfig.renderer.font;

import cc.polyfrost.oneconfig.utils.IOUtils;

import java.io.IOException;
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
        for (Fonts fonts : Fonts.values()) {
            loadFont(vg, fonts.font);
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
