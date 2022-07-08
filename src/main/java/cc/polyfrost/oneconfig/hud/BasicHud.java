package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.renderer.RenderManager;

public abstract class BasicHud extends Hud {
    public boolean rounded;
    public boolean border;
    public OneColor bgColor;
    public OneColor borderColor;
    public float cornerRadius;
    public float borderSize;

    /**
     * @param enabled      If the hud is enabled
     * @param x            X-coordinate of hud on a 1080p display
     * @param y            Y-coordinate of hud on a 1080p display
     * @param scale        Scale of the hud
     * @param rounded      If the corner is rounded or not
     * @param cornerRadius Radius of the corner
     * @param paddingX     Horizontal background padding
     * @param paddingY     Vertical background padding
     * @param bgColor      Background color
     * @param border       If the hud has a border or not
     * @param borderSize   Thickness of the border
     * @param borderColor  The color of the border
     */
    public BasicHud(boolean enabled, float x, float y, float scale, boolean rounded, int cornerRadius, int paddingX, int paddingY, OneColor bgColor, boolean border, float borderSize, OneColor borderColor) {
        super(enabled, x, y, scale);
        this.rounded = rounded;
        this.cornerRadius = cornerRadius;
        this.paddingX = paddingX;
        this.paddingY = paddingY;
        this.bgColor = bgColor;
        this.border = border;
        this.borderSize = borderSize;
        this.borderColor = borderColor;
    }

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     * @param scale   Scale of the hud
     */
    public BasicHud(boolean enabled, float x, float y, float scale) {
        this(enabled, x, y, scale, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     */
    public BasicHud(boolean enabled, float x, float y) {
        this(enabled, x, y, 1, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    /**
     * @param enabled If the hud is enabled
     */
    public BasicHud(boolean enabled) {
        this(enabled, 0, 0, 1, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    public BasicHud() {
        this(false, 0, 0, 1, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    @Override
    public void drawAll(UMatrixStack matrices, float x, float y, float scale) {
        if (shouldShow()) {
            if (shouldDrawBackground()) drawBackground(x, y, getWidth(scale), getHeight(scale), scale);
            draw(matrices, x, y, scale);
        }
    }

    @Override
    public void drawExampleAll(UMatrixStack matrices, float x, float y, float scale) {
        if (shouldDrawBackground()) drawBackground(x, y, getWidth(scale), getHeight(scale), scale);
        drawExample(matrices, x, y, scale);
    }

    /**
     * @return If the background should be drawn
     */
    public boolean shouldDrawBackground() {
        return true;
    }

    private void drawBackground(float x, float y, float width, float height, float scale) {
        RenderManager.setupAndDraw(true, (vg) -> {
            if (rounded) {
                RenderManager.drawRoundedRect(vg, x, y, (width + paddingX * scale), (height + paddingY * scale), bgColor.getRGB(), cornerRadius * scale);
                if (border)
                    RenderManager.drawHollowRoundRect(vg, x - borderSize * scale, y - borderSize * scale, (width + paddingX * scale) + borderSize * scale, (height + paddingY * scale) + borderSize * scale, borderColor.getRGB(), cornerRadius * scale, borderSize * scale);
            } else {
                RenderManager.drawRect(vg, x, y, (width + paddingX * scale), (height + paddingY * scale), bgColor.getRGB());
                if (border)
                    RenderManager.drawHollowRoundRect(vg, x - borderSize * scale, y - borderSize * scale, (width + paddingX * scale) + borderSize * scale, (height + paddingY * scale) + borderSize * scale, borderColor.getRGB(), 0, borderSize * scale);
            }
        });
    }

}
