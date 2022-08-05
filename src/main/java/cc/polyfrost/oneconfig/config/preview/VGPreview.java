package cc.polyfrost.oneconfig.config.preview;

import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.renderer.RenderManager;

/**
 * NanoVG-specific rendering preview class.
 */
public abstract class VGPreview extends BasicPreview {
    @Override
    public final void setupCallDraw(UMatrixStack matrices, long vg, float x, float y) {
        draw(vg, x, y, WIDTH);
        // australia moment
        if (OneConfigConfig.australia) {
            RenderManager.translate(vg, UResolution.getWindowWidth(), UResolution.getWindowHeight());
            RenderManager.rotate(vg, (float) Math.toRadians(180));
        }
    }

    /**
     * Draws the preview.
     *
     * @param vg The VG instance used to draw the preview.
     * @param x  The x coordinate of the preview.
     * @param y  The y coordinate of the preview.
     * @param width The width. constant.
     */
    protected abstract void draw(long vg, float x, float y, float width);
}
