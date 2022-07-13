package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.renderer.RenderManager;

public abstract class BasicHud extends Hud {
    protected boolean rounded;
    protected boolean border;
    protected OneColor bgColor;
    protected OneColor borderColor;
    protected float cornerRadius;
    protected float borderSize;
    protected float paddingX;
    protected float paddingY;

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
    public BasicHud(boolean enabled, float x, float y, float scale, boolean rounded, float cornerRadius, float paddingX, float paddingY, OneColor bgColor, boolean border, float borderSize, OneColor borderColor) {
        super(enabled, x, y, scale);
        this.rounded = rounded;
        this.cornerRadius = cornerRadius;
        this.paddingX = paddingX;
        this.paddingY = paddingY;
        this.bgColor = bgColor;
        this.border = border;
        this.borderSize = borderSize;
        this.borderColor = borderColor;
        position.setSize(getWidth(scale) + paddingX * 2f, getHeight(scale) + paddingY * 2f);
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
    public void drawAll(UMatrixStack matrices) {
        if (!shouldShow()) return;
        position.setSize(getWidth(scale) + paddingX * 2f, getHeight(scale) + paddingY * 2f);
        if (shouldDrawBackground())
            drawBackground(position.getX(), position.getY(), position.getWidth(), position.getHeight(), scale);
        draw(matrices, position.getX() + paddingX, position.getY() + paddingY, scale);
    }

    @Override
    public void drawExampleAll(UMatrixStack matrices) {
        position.setSize(getExampleWidth(scale) + paddingX * 2f, getExampleHeight(scale) + paddingY * 2f);
        if (shouldDrawBackground())
            drawBackground(position.getX(), position.getY(), position.getWidth(), position.getHeight(), scale);
        drawExample(matrices, position.getX() + paddingX, position.getY() + paddingY, scale);
    }

    /**
     * Set a new scale value
     *
     * @param scale The new scale
     */
    @Override
    public void setScale(float scale) {
        this.scale = scale;
        position.updateSizePosition(getWidth(scale) + paddingX * 2f, getHeight(scale) + paddingY * 2f);
    }

    /**
     * @return If the background should be drawn
     */
    protected boolean shouldDrawBackground() {
        return true;
    }

    protected void drawBackground(float x, float y, float width, float height, float scale) {
        // todo: make border rendering perfect
        RenderManager.setupAndDraw(true, (vg) -> {
            if (rounded) {
                RenderManager.drawRoundedRect(vg, x, y, width, height, bgColor.getRGB(), cornerRadius * scale);
                if (border)
                    RenderManager.drawHollowRoundRect(vg, x - borderSize * scale, y - borderSize * scale, width + borderSize * scale, height + borderSize * scale, borderColor.getRGB(), cornerRadius * scale, borderSize * scale);
            } else {
                RenderManager.drawRect(vg, x, y, width, height, bgColor.getRGB());
                if (border)
                    RenderManager.drawHollowRoundRect(vg, x - borderSize * scale, y - borderSize * scale, width + borderSize * scale, height + borderSize * scale, borderColor.getRGB(), 0, borderSize * scale);
            }
        });
    }
}
