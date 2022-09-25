package cc.polyfrost.oneconfig.renderer;

import java.nio.IntBuffer;

public interface AssetHelper {

    int DEFAULT_FLAGS = 2 | 4 | 1; // NanoVG.NVG_IMAGE_REPEATX | NanoVG.NVG_IMAGE_REPEATY | NanoVG.NVG_IMAGE_GENERATE_MIPMAPS

    /**
     * Loads an assets from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param flags    The image flags
     * @return Whether the asset was loaded successfully.
     */
    boolean loadImage(long vg, String fileName, int flags);

    /**
     * Loads an assets from resources.
     *
     * @param vg    The NanoVG context.
     * @param image The Image
     * @return Whether the asset was loaded successfully.
     */
    boolean loadImage(long vg, Image image);

    /**
     * Loads an assets from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @return Whether the asset was loaded successfully.
     */
    boolean loadImage(long vg, String fileName);

    /**
     * Loads an SVG from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param width    The width of the SVG.
     * @param height   The height of the SVG.
     * @param flags    The image flags
     * @return Whether the SVG was loaded successfully.
     */
    boolean loadSVG(long vg, String fileName, float width, float height, int flags);

    /**
     * Loads an assets from resources.
     *
     * @param vg     The NanoVG context.
     * @param svg    The SVG
     * @param width  The width of the SVG.
     * @param height The height of the SVG.
     * @return Whether the asset was loaded successfully.
     */
    boolean loadSVG(long vg, SVG svg, float width, float height);

    /**
     * Loads an SVG from resources.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to load.
     * @param width    The width of the SVG.
     * @param height   The height of the SVG.
     * @return Whether the SVG was loaded successfully.
     */
    boolean loadSVG(long vg, String fileName, float width, float height);

    /**
     * Get a loaded assets from the cache.
     * <p><b>Requires the assets to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The assets
     * @see AssetHelper#loadImage(long, String)
     */
    int getImage(String fileName);

    /**
     * Remove an assets from the cache, allowing the assets to be garbage collected.
     * Should be used when the GUI rendering the assets is closed.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to remove.
     * @see AssetHelper#loadImage(long, String)
     */
    void removeImage(long vg, String fileName);

    /**
     * Clears all images from the cache, allowing the images cleared to be garbage collected.
     * Should be used when the GUI rendering loaded images are closed.
     *
     * @param vg The NanoVG context.
     */
    void clearImages(long vg);

    /**
     * Get a loaded SVG from the cache.
     * <p><b>Requires the SVG to have been loaded first.</b></p>
     *
     * @param fileName The name of the file to load.
     * @return The SVG
     * @see AssetHelper#loadSVG(long, String, float, float)
     */
    int getSVG(String fileName, float width, float height);

    /**
     * Remove an SVG from the cache, allowing the SVG to be garbage collected.
     * Should be used when the GUI rendering the SVG is closed.
     *
     * @param vg       The NanoVG context.
     * @param fileName The name of the file to remove.
     * @see AssetHelper#loadSVG(long, String, float, float)
     */
    void removeSVG(long vg, String fileName, float width, float height);

    /**
     * Clears all SVGs from the cache, allowing the SVGs cleared to be garbage collected.
     * Should be used when the GUI rendering loaded SVGs are closed.
     *
     * @param vg The NanoVG context.
     */
    void clearSVGs(long vg);

    /**
     * Convert the given image (as a quantified path) to an IntBuffer, of its pixels, in order, stored as integers in ARGB format.
     * Mostly an internal method; used by LWJGL.
     *
     * @param fileName quantified path to the image
     * @return intBuffer of the image's pixels in ARGB format
     */
    IntBuffer imageToIntBuffer(String fileName);
}
