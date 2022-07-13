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
    protected boolean enabled;
    transient private Config config;
    public final Position position;
    protected float scale;

    /**
     * @param enabled If the hud is enabled
     * @param x       X-coordinate of hud on a 1080p display
     * @param y       Y-coordinate of hud on a 1080p display
     * @param scale   Scale of the hud
     */
    public Hud(boolean enabled, float x, float y, float scale) {
        this.enabled = enabled;
        this.scale = scale;
        position = new Position(x, y, getWidth(scale), getHeight(scale));
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
    protected abstract void draw(UMatrixStack matrices, float x, float y, float scale);

    /**
     * Function called when drawing the example version of the hud.
     * This is used in for example, the hud editor gui.
     *
     * @param x     Top left x-coordinate of the hud
     * @param y     Top left y-coordinate of the hud
     * @param scale Scale of the hud
     */
    protected void drawExample(UMatrixStack matrices, float x, float y, float scale) {
        draw(matrices, x, y, scale);
    }

    /**
     * @param scale Scale of the hud
     * @return The width of the hud
     */
    protected abstract float getWidth(float scale);

    /**
     * @param scale Scale of the hud
     * @return The height of the hud
     */
    protected abstract float getHeight(float scale);

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
     */
    public void drawAll(UMatrixStack matrices) {
        if (!shouldShow()) return;
        position.setSize(getWidth(scale), getHeight(scale));
        draw(matrices, position.getX(), position.getY(), scale);
    }

    /**
     * Draw example version of the background, the hud and all childed huds, used by HudGui
     */
    public void drawExampleAll(UMatrixStack matrices) {
        position.setSize(getExampleWidth(scale), getExampleHeight(scale));
        draw(matrices, position.getX(), position.getY(), scale);
    }

    protected boolean shouldShow() {
        if (!showInGuis && Platform.getGuiPlatform().getCurrentScreen() != null && !(Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui))
            return false;
        if (!showInChat && Platform.getGuiPlatform().isInChat()) return false;
        return showInDebug || !Platform.getGuiPlatform().isInDebug();
    }

    /**
     * @return If the hud is enabled
     */
    public boolean isEnabled() {
        return enabled && (config == null || config.enabled);
    }

    /**
     * Set the config to disable accordingly, intended for internal use
     *
     * @param config The config instance
     */
    public void setConfig(Config config) {
        this.config = config;
    }

    /**
     * @return The scale of the Hud
     */
    public float getScale() {
        return scale;
    }

    /**
     * Set a new scale value
     *
     * @param scale The new scale
     */
    public void setScale(float scale) {
        this.scale = scale;
        position.updateSizePosition(getWidth(scale), getHeight(scale));
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