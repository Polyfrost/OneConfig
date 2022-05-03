package cc.polyfrost.oneconfig.lwjgl.image;

import java.nio.ByteBuffer;

public class Image {
    private final int reference;
    private final ByteBuffer buffer;

    public Image(int reference, ByteBuffer buffer) {
        this.reference = reference;
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public int getReference() {
        return reference;
    }
}
