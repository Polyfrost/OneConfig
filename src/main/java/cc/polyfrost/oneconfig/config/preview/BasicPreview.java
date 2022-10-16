package cc.polyfrost.oneconfig.config.preview;

/**
 * The base class for all config preview classes.
 */
public abstract class BasicPreview {

    /**
     * Prepares the preview for drawing and calls a draw method specified by the preview.
     *
     * @param vg       The VG instance that could be used to draw the preview.
     * @param x        The x coordinate of the preview.
     * @param y        The y coordinate of the preview.
     */
    public abstract void setupCallDraw(long vg, float x, float y, float width, float height);

    /**
     * @return return the entire height of this preview.
     */
    public abstract float getHeight();
}
