package cc.polyfrost.oneconfig.renderer;

public class NVGAsset {
    private final int image;
    private final int width;
    private final int height;

    protected NVGAsset(int image, int width, int height) {
        this.image = image;
        this.width = width;
        this.height = height;
    }

    public int getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
