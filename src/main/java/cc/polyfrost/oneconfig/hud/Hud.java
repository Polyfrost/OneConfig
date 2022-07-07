package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.platform.Platform;

/**
 * Represents a HUD element in OneConfig.
 * A HUD element can be used to display useful information to the user, like FPS or CPS.
 * <p>
 * If you simply want to display text, extend {@link TextHud} or {@link SingleTextHud},
 * whichever applies to the use case. Then, override the required methods.
 * <p>
 * If you want to display something else, extend this class and override {@link Hud#getWidth(float)}, {@link Hud#getHeight(float)}, and {@link Hud#draw(UMatrixStack, float, float, float)} with the width, height, and the drawing code respectively.
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
    public boolean enabled;
    transient private Config config;
    public double xUnscaled;
    public double yUnscaled;
    public float scale;

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     * @param scale   Scale of the hud
     */
    public Hud(boolean enabled, float x, float y, float scale) {
        this.enabled = enabled;
        this.scale = scale;
        if (x / 1920d <= 0.5d) xUnscaled = x / 1920d;
        else xUnscaled = (x + getWidth(scale)) / 1920d;
        if (y / 1080d <= 0.5d) yUnscaled = y / 1080d;
        else yUnscaled = (y + getHeight(scale)) / 1090d;
    }

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     */
    public Hud(boolean enabled, float x, float y) {
        this(enabled, x, y, 1);
    }

    /**
     * @param enabled If the hud is enabled
     */
    public Hud(boolean enabled) {
        this(enabled, 0, 0, 1);
    }

    public Hud() {
        this(false, 0, 0, 1);
    }

    /**
     * Function called when drawing the hud
     *
     * @param x     Top left x-coordinate of the hud
     * @param y     Top left y-coordinate of the hud
     * @param scale Scale of the hud
     */
    public abstract void draw(UMatrixStack matrices, float x, float y, float scale);

    /**
     * Function called when drawing the example version of the hud.
     * This is used in for example, the hud editor gui.
     *
     * @param x     Top left x-coordinate of the hud
     * @param y     Top left y-coordinate of the hud
     * @param scale Scale of the hud
     */
    public void drawExample(UMatrixStack matrices, float x, float y, float scale) {
        draw(matrices, x, y, scale);
    }

    /**
     * @param scale Scale of the hud
     * @return The width of the hud
     */
    public abstract float getWidth(float scale);

    /**
     * @param scale Scale of the hud
     * @return The height of the hud
     */
    public abstract float getHeight(float scale);

    /**
     * @param scale Scale of the hud
     * @return The width of the example version of the hud
     */
    public float getExampleWidth(float scale) {
        return getWidth(scale);
    }

    /**
     * @param scale Scale of the hud
     * @return The height of the example version of the hud
     */
    public float getExampleHeight(float scale) {
        return getHeight(scale);
    }

    /**
     * Draw the background, the hud and all childed huds, used by HudCore
     *
     * @param x          X-coordinate
     * @param y          Y-coordinate
     * @param scale      Scale of the hud
     */
    public void drawAll(UMatrixStack matrices, float x, float y, float scale) {
        if (shouldShow()) draw(matrices, x, y, scale);
    }

    /**
     * Draw example version of the background, the hud and all childed huds, used by HudGui
     *
     * @param x          X-coordinate
     * @param y          Y-coordinate
     * @param scale      Scale of the hud
     */
    public void drawExampleAll(UMatrixStack matrices, float x, float y, float scale) {
        drawExample(matrices, x, y, scale);
    }

    protected boolean shouldShow() {
        if (!showInGuis && Platform.getGuiPlatform().getCurrentScreen() != null && !(Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui)) return false;
        if (!showInChat && Platform.getGuiPlatform().isInChat()) return false;
        return showInDebug || !Platform.getGuiPlatform().isInDebug();
    }

    /**
     * @param screenWidth width of the screen
     * @return X-coordinate of the hud
     */
    public float getXScaled(int screenWidth) {
        if (xUnscaled <= 0.5)
            return (int) (screenWidth * xUnscaled);
        return (float) (screenWidth - (1d - xUnscaled) * screenWidth - (getWidth(scale)));
    }

    /**
     * @param screenHeight height of the screen
     * @return Y-coordinate of the hud
     */
    public float getYScaled(int screenHeight) {
        if (yUnscaled <= 0.5) return (int) (screenHeight * yUnscaled);
        return (float) (screenHeight - (1d - yUnscaled) * screenHeight - (getHeight(scale)));
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