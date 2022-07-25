package cc.polyfrost.oneconfig.renderer;

/**
 * Data class storing an image.
 * This class is purely a data class, and does not contain any logic. It does not need to be used unless you want to
 * differentiate between a String and an image.
 */
public class Image {
    public final String filePath;

    public Image(String filePath) {
        this.filePath = filePath;
    }
}
