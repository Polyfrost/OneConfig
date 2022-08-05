package cc.polyfrost.oneconfig.config.preview;

import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;

/**
 * Minecraft-specific rendering preview class.
 */
public abstract class MCPreview extends BasicPreview {

    @Override
    public final void setupCallDraw(UMatrixStack matrices, long vg, float x, float y, float width, float height) {
        matrices.push();
        matrices.translate(x, y, 0);
        draw(matrices, width, height);
        matrices.pop();
    }

    /**
     * Draws the preview.
     *
     * @param matrices The matrix stack used to draw the preview. The X and Y coordinates have already been translated on this stack.
     */
    protected abstract void draw(UMatrixStack matrices, float width, float height);
}
