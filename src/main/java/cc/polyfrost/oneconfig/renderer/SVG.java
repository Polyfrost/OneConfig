package cc.polyfrost.oneconfig.renderer;

/**
 * Data class storing an SVG image.
 * This class is purely a data class, and does not contain any logic. It does not need to be used unless you want to
 * differentiate between a String and an SVG image.
 */
public class SVG {
    public final String filePath;

    public SVG(String filePath) {
        this.filePath = filePath;
    }
}
