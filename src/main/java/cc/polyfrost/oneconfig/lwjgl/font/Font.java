package cc.polyfrost.oneconfig.lwjgl.font;

import java.nio.ByteBuffer;

public class Font {
    private final String fileName;
    private final String name;
    private boolean loaded = false;
    private ByteBuffer buffer = null;

    public Font(String name, String fileName) {
        this.name = name;
        this.fileName = fileName;
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

}

