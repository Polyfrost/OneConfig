package cc.polyfrost.oneconfig.lwjgl.font;

import cc.polyfrost.oneconfig.lwjgl.IOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.nanovg.NanoVG.nvgCreateFontMem;

public class FontManager {
    public static FontManager INSTANCE = new FontManager();

    public void initialize(long vg) {
        for (Fonts fonts : Fonts.values()) {
            Font font = fonts.font;
            int loaded = -1;
            try {
                ByteBuffer buffer = IOUtil.resourceToByteBuffer(font.getFileName());
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
}
