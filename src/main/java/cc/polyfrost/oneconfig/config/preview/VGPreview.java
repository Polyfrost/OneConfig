package cc.polyfrost.oneconfig.config.preview;

import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.utils.InputHandler;

/**
 * NanoVG-specific rendering preview class.
 */
public abstract class VGPreview extends BasicPreview {
    @Override
    public final void setupCallDraw(long vg, float x, float y, float width, float height, InputHandler inputHandler) {
        RenderManager.translate(vg, x, y);
        draw(vg, width, getHeight(), inputHandler);
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
    protected abstract void draw(long vg, float width, float height, InputHandler inputHandler);
}
