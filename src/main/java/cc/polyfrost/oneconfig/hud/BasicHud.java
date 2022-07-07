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

    public float paddingTop;
    public float paddingBottom;

    public float paddingLeft;
    public float paddingRight;

    public float minWidth;
    public float minHeight;

    /**
     * @param enabled      If the hud is enabled
     * @param x            X-coordinate of hud on a 1080p display
     * @param y            Y-coordinate of hud on a 1080p display
     * @param scale        Scale of the hud
     * @param rounded      If the corner is rounded or not
     * @param cornerRadius Radius of the corner
     * @param paddingTop     Horizontal background padding
     * @param paddingBottom  Horizontal background padding
     * @param paddingLeft    Vertical background padding
     * @param paddingRight   Vertical background padding
     * @param minWidth      Minimum width of the hud
     * @param minHeight     Minimum height of the hud
     * @param bgColor      Background color
     * @param border       If the hud has a border or not
     * @param borderSize   Thickness of the border
     * @param borderColor  The color of the border
     */
    public BasicHud(boolean enabled, float x, float y, float scale, boolean rounded, float cornerRadius, float paddingTop, float paddingBottom, float paddingLeft, float paddingRight, float minWidth, float minHeight, OneColor bgColor, boolean border, float borderSize, OneColor borderColor) {
        super(enabled, x, y, scale);
        this.rounded = rounded;
        this.cornerRadius = cornerRadius;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
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
        this(enabled, x, y, scale, false, 2, 5, 5, 5, 5, 0, 0, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     */
    public BasicHud(boolean enabled, float x, float y) {
        this(enabled, x, y, 1);
    }

    /**
     * @param enabled If the hud is enabled
     */
    public BasicHud(boolean enabled) {
        this(enabled, 0, 0);
    }

    public BasicHud() {
        this(false);
    }

    @Override
    public void drawAll(UMatrixStack matrices, float x, float y, float scale) {
        if (shouldShow()) {
            if (shouldDrawBackground()) drawBackground(x, y, scale);
            draw(matrices, x, y, scale);
        }
    }

    @Override
    public void drawExampleAll(UMatrixStack matrices, float x, float y, float scale) {
        drawExample(matrices, x, y, scale);
    }

    /**
     * @return If the background should be drawn
     */
    public boolean shouldDrawBackground() {
        return true;
    }

    private void drawBackground(float x, float y, float scale) {
        RenderManager.setupAndDraw(true, (vg) -> {
            RenderManager.scale(vg, scale, scale);
            final Hitbox hitbox = calculateHitBox(x, y, scale);
            if (rounded) {
                RenderManager.drawRoundedRect(vg, hitbox.x, hitbox.y, hitbox.width, hitbox.height, bgColor.getRGB(), cornerRadius);
                if (border)
                    RenderManager.drawHollowRoundRect(vg, hitbox.x - borderSize, hitbox.y - borderSize, hitbox.width + borderSize, hitbox.height + borderSize, borderColor.getRGB(), cornerRadius, borderSize);
            } else {
                RenderManager.drawRect(vg, hitbox.x, hitbox.y, hitbox.width, hitbox.height, bgColor.getRGB());
                if (border)
                    RenderManager.drawHollowRoundRect(vg, hitbox.x - borderSize, hitbox.y - borderSize, hitbox.width + borderSize, hitbox.height + borderSize, borderColor.getRGB(), 0, borderSize);
            }
        });
    }

    /**
     * Taken from EvergreenHUD under GPL 3.0
     * <a href="https://github.com/isXander/EvergreenHUD/blob/1.18/LICENSE.md">...</a>
     */
    @Override
    public Hitbox calculateHitBox(float x, float y, float scale) {
        float providedWidth = getWidth(scale);
        float providedHeight = getHeight(scale);
        float width = providedWidth * scale;
        float height = providedHeight * scale;

        float top = paddingTop * scale;
        float bottom = paddingBottom * scale;
        float left = paddingLeft * scale;
        float right = paddingRight * scale;

        float extraWidth = Math.max((minWidth - providedWidth) * scale, 0f) / 2f;
        float extraHeight = Math.max(((minHeight - providedHeight) * scale), 0f) / 2f;

        return new Hitbox(x - left - extraWidth, y - top - extraHeight, width + left + right + extraWidth, height + top + bottom + extraHeight);
    }

}
