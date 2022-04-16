package io.polyfrost.oneconfig.lwjgl.font;

import io.polyfrost.oneconfig.lwjgl.IOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.lwjgl.nanovg.NanoVG.nvgCreateFontMem;

public class FontManager {
    public static FontManager INSTANCE = new FontManager();
    private final ArrayList<Font> fonts = new ArrayList<>();

    public void initialize(long vg) {
        fonts.add(new Font("inter-bold", "/assets/oneconfig/font/Inter-Bold.ttf"));
        fonts.add(new Font("mc-regular", "/assets/oneconfig/font/Minecraft-Regular.otf"));
        for (Font font : fonts) {
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
