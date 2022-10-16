package cc.polyfrost.oneconfig.config.preview;

import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.renderer.RenderManager;

/**
 * NanoVG-specific rendering preview class.
 */
public abstract class VGPreview extends BasicPreview {
    @Override
    public final void setupCallDraw(long vg, float x, float y, float width, float height) {
        RenderManager.translate(vg, x, y);
        draw(vg, width, getHeight());
        RenderManager.translate(vg, -x, -y);
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
     */
    protected abstract void draw(long vg, float width, float height);
}
