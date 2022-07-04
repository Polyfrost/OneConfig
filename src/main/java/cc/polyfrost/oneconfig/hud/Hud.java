package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;

/**
 * Represents a HUD element in OneConfig.
 * A HUD element can be used to display useful information to the user, like FPS or CPS.
 * <p>
 * If you simply want to display text, extend {@link TextHud} or {@link SingleTextHud},
 * whichever applies to the use case. Then, override the required methods.
 * <p>
 * If you want to display something else, extend this class and override {@link Hud#getWidth(float)}, {@link Hud#getHeight(float)}, and {@link Hud#draw(UMatrixStack, int, int, float)} with the width, height, and the drawing code respectively.
 * </p>
 * <p>
 * It should also be noted that additional options to the HUD can be added simply by declaring them.
 * <pre>{@code
 *     public class TestHud extends SingleTextHud {
 *         @literal @Switch(
 *             name = "Additional Option"
 *         )
 *         public boolean additionalOption = true;
 *     }
 *     }</pre>
 * </p>
 * To register an element, add it to your OneConfig {@link Config}.
 * <pre>{@code
 *  *     public class YourConfig extends Config {
 *  *         @literal @HUD(
 *  *             name = "HUD Element"
 *  *         )
 *  *         public YourHudElement hudElement = new YourHudElement("Title");
 *  *     }
 *  *     }</pre>
 */
public abstract class Hud {
    private boolean enabled;
    transient private Config config;
    public boolean rounded;
    public boolean border;
    public OneColor bgColor;
    public OneColor borderColor;
    public float cornerRadius;
    public float borderSize;
    public double xUnscaled;
    public double yUnscaled;
    public float scale;
    public float paddingX;
    public float paddingY;

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
    public Hud(boolean enabled, int x, int y, float scale, boolean rounded, int cornerRadius, int paddingX, int paddingY, OneColor bgColor, boolean border, float borderSize, OneColor borderColor) {
        this.enabled = enabled;
        this.scale = scale;
        this.rounded = rounded;
        this.cornerRadius = cornerRadius;
        this.paddingX = paddingX;
        this.paddingY = paddingY;
        this.bgColor = bgColor;
        this.border = border;
        this.borderSize = borderSize;
        this.borderColor = borderColor;
        if (x / 1920d <= 0.5d) xUnscaled = x / 1920d;
        else xUnscaled = (x + getWidth(scale)) / 1920d;
        if (y / 1080d <= 0.5d) yUnscaled = y / 1080d;
        else yUnscaled = (y + getHeight(scale)) / 1090d;
    }

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     * @param scale   Scale of the hud
     */
    public Hud(boolean enabled, int x, int y, int scale) {
        this(enabled, x, y, scale, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     */
    public Hud(boolean enabled, int x, int y) {
        this(enabled, x, y, 1, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    /**
     * @param enabled If the hud is enabled
     */
    public Hud(boolean enabled) {
        this(enabled, 0, 0, 1, false, 2, 5, 5, new OneColor(0, 0, 0, 120), false, 2, new OneColor(0, 0, 0));
    }

    /**
     * Function called when drawing the hud
     *
     * @param x     Top left x-coordinate of the hud
     * @param y     Top left y-coordinate of the hud
     * @param scale Scale of the hud
     */
    public abstract void draw(UMatrixStack matrices, int x, int y, float scale);

    /**
     * Function called when drawing the example version of the hud.
     * This is used in for example, the hud editor gui.
     *
     * @param x     Top left x-coordinate of the hud
     * @param y     Top left y-coordinate of the hud
     * @param scale Scale of the hud
     */
    public void drawExample(UMatrixStack matrices, int x, int y, float scale) {
        draw(matrices, x, y, scale);
    }

    /**
     * @param scale Scale of the hud
     * @return The width of the hud
     */
    public abstract int getWidth(float scale);

    /**
     * @param scale Scale of the hud
     * @return The height of the hud
     */
    public abstract int getHeight(float scale);

    /**
     * @param scale Scale of the hud
     * @return The width of the example version of the hud
     */
    public int getExampleWidth(float scale) {
        return getWidth(scale);
    }

    /**
     * @param scale Scale of the hud
     * @return The height of the example version of the hud
     */
    public int getExampleHeight(float scale) {
        return getHeight(scale);
    }

    /**
     * @return If the background should be drawn
     */
    public boolean drawBackground() {
        return true;
    }

    /**
     * Draw the background, the hud and all childed huds, used by HudCore
     *
     * @param x          X-coordinate
     * @param y          Y-coordinate
     * @param scale      Scale of the hud
     * @param background If background should be drawn or not
     */
    public void drawAll(UMatrixStack matrices, float x, float y, float scale, boolean background) {
        if (!showInGuis && Platform.getGuiPlatform().getCurrentScreen() != null && !(Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui)) return;
        if (!showInChat && Platform.getGuiPlatform().isInChat()) return;
        if (!showInDebug && Platform.getGuiPlatform().isInDebug()) return;
        if (background && drawBackground()) drawBackground(x, y, getWidth(scale), getHeight(scale), scale);
        draw(matrices, (int) (x + paddingX * scale / 2f), (int) (y + paddingY * scale / 2f), scale);
    }

    /**
     * Draw example version of the background, the hud and all childed huds, used by HudGui
     *
     * @param x          X-coordinate
     * @param y          Y-coordinate
     * @param scale      Scale of the hud
     * @param background If background should be drawn or not
     */
    public void drawExampleAll(UMatrixStack matrices, float x, float y, float scale, boolean background) {
        if (background) drawBackground(x, y, getWidth(scale), getHeight(scale), scale);
        drawExample(matrices, (int) (x + paddingX * scale / 2f), (int) (y + paddingY * scale / 2f), scale);
    }

    /**
     * Draw example version of the background, the hud and all childed huds, used by HudGui
     *
     * @param x      X-coordinate
     * @param y      Y-coordinate
     * @param width  Width of the hud
     * @param height Height of the hud
     * @param scale  Scale of the hud
     */
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

    /**
     * @param screenWidth width of the screen
     * @return X-coordinate of the hud
     */
    public float getXScaled(int screenWidth) {
        if (xUnscaled <= 0.5)
            return (int) (screenWidth * xUnscaled);
        return (float) (screenWidth - (1d - xUnscaled) * screenWidth - (getWidth(scale) + paddingX * scale));
    }

    /**
     * @param screenHeight height of the screen
     * @return Y-coordinate of the hud
     */
    public float getYScaled(int screenHeight) {
        if (yUnscaled <= 0.5) return (int) (screenHeight * yUnscaled);
        return (float) (screenHeight - (1d - yUnscaled) * screenHeight - (getHeight(scale) + paddingY * scale));
    }

    /**
     * @return If the hud is enabled
     */
    public boolean isEnabled() {
        return enabled && (config == null || config.enabled);
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @Switch(
            name = "Show in Chat"
    )
    public boolean showInChat = true;

    @Switch(
            name = "Show in F3 (Debug)"
    )
    public boolean showInDebug = false;

    @Switch(
            name = "Show in GUIs"
    )
    public boolean showInGuis = true;
}