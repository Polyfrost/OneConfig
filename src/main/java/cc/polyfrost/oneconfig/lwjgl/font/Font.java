package cc.polyfrost.oneconfig.lwjgl.font;

import java.nio.ByteBuffer;

public class Font {
    private final String fileName;
    private final String name;
    private boolean loaded = false;
    private ByteBuffer buffer = null;
    private final int unitsPerEm;
    private final int ascender;
    private final int descender;

    public Font(String name, String fileName, int unitsPerEm, int ascender, int descender) {
        this.name = name;
        this.fileName = fileName;
        this.unitsPerEm = unitsPerEm;
        this.ascender = ascender;
        this.descender = descender;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isLoaded() {
        return loaded;
    }

    void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public int getUnitsPerEm() {
        return unitsPerEm;
    }

    public int getAscender() {
        return ascender;
    }

    public int getDescender() {
        return descender;
    }
}
