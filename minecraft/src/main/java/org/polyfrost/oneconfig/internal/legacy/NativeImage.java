package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;
import java.nio.file.Path;

import javax.imageio.ImageIO;

public class NativeImage implements AutoCloseable {
    public final int width;
    public final int height;

    private final BufferedImage image;

    public NativeImage(int width, int height, boolean useCalloc) {
        this.width = width;
        this.height = height;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public NativeImage(BufferedImage image) {
        this(image.getWidth(), image.getHeight(), false);

        Graphics2D graphics = this.image.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
    }

    public int getPixelRGBA(int x, int y) {
        return swapRedBlue(image.getRGB(x, y));
    }

    public void setPixelRGBA(int x, int y, int color) {
        image.setRGB(x, y, swapRedBlue(color));
    }

    private static int swapRedBlue(int color) {
        return (color & 0xFF00FF00) | ((color & 0xFF) << 16) | ((color >>> 16) & 0xFF);
    }

    public void writeToFile(Path path) throws java.io.IOException {
        ImageIO.write(image, "png", path.toFile());
    }

    @Override
    public void close() {
        image.flush();
    }

    public void downloadTexture(int level, boolean forceOpaque) {
        int count = width * height;
        IntBuffer buffer = BufferUtils.createIntBuffer(count);
        int previousAlignment = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);

        try {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glGetTexImage(
                    GL11.GL_TEXTURE_2D,
                    level,
                    GL12.GL_BGRA,
                    GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                    buffer
            );

            int[] pixels = new int[count];
            buffer.get(pixels);

            if (forceOpaque) {
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] |= 0xFF000000;
                }
            }

            image.setRGB(0, 0, width, height, pixels, 0, width);
        } finally {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, previousAlignment);
        }
    }

    public void flipY() {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int[] row = new int[width];

        for (int y = 0; y < height / 2; y++) {
            int opposite = height - 1 - y;
            System.arraycopy(pixels, y * width, row, 0, width);
            System.arraycopy(pixels, opposite * width, pixels, y * width, width);
            System.arraycopy(row, 0, pixels, opposite * width, width);
        }
    }
}
*///?}
