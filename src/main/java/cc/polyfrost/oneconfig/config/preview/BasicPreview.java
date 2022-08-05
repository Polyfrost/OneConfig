package cc.polyfrost.oneconfig.config.preview;

import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;

/**
 * The base class for all config preview classes.
 */
public abstract class BasicPreview {
    public final float WIDTH = 1008;

    /**
     * Prepares the preview for drawing and calls a draw method specified by the preview.
     * The parameters are not indicators of the type of rendering required to draw the preview,
     * but rather what information is available from the GUI itself. For example, if the preview
     * does not require a matrix stack, the matrix stack parameter does not need to be used.
     *
     * @param matrices The matrix stack that could be used to draw the preview.
     * @param vg       The VG instance that could be used to draw the preview.
     * @param x        The x coordinate of the preview.
     * @param y        The y coordinate of the preview.
     */
    public abstract void setupCallDraw(UMatrixStack matrices, long vg, float x, float y);

    /**
     * @return return the entire height of this preview.
     */
    public abstract float getHeight();
}
