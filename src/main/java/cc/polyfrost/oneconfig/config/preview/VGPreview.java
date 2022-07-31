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
    public final void setupCallDraw(UMatrixStack matrices, long vg, float x, float y, float width, float height) {
        RenderManager.translate(vg, x, y);
        draw(vg, width, height);
        RenderManager.resetTransform(vg);
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
     * @param width The width of the preview.
     * @param height The height of the preview.
     */
    protected abstract void draw(long vg, float width, float height);

    @Override
    public final float getHeight(UMatrixStack matrices, long vg, float x, float y, float width) {
        return getHeight(vg, x, y, width);
    }

    protected abstract float getHeight(long vg, float x, float y, float width);
}
