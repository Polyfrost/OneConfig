package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import gg.essential.universal.UResolution;
import org.lwjgl.input.Mouse;

/**
 * Various utility methods for input.
 * <p>
 * All values returned from this class are not scaled to Minecraft's GUI scale.
 * For scaled values, see {@link gg.essential.universal.UMouse}.
 * </p>
 */
public final class InputUtils {
    private static boolean blockClicks = false;

    /**
     * function to determine weather the mouse is currently over a specific region. Uses the current nvgScale to fix to any scale.
     *
     * @return true if mouse is over region, false if not.
     */
    public static boolean isAreaHovered(int x, int y, int width, int height) {
        int mouseX = mouseX();
        int mouseY = mouseY();
        return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;
    }

    /**
     * Checks whether the mouse is currently over a specific region and clicked.
     *
     * @param x           the x position of the region
     * @param y           the y position of the region
     * @param width       the width of the region
     * @param height      the height of the region
     * @param ignoreBlock if true, will ignore {@link InputUtils#blockClicks(boolean)}
     * @return true if the mouse is clicked and is over the region, false if not
     * @see InputUtils#isAreaHovered(int, int, int, int)
     */
    public static boolean isAreaClicked(int x, int y, int width, int height, boolean ignoreBlock) {
        return isAreaHovered(x, y, width, height) && isClicked(ignoreBlock);
    }

    /**
     * Checks whether the mouse is currently over a specific region and clicked.
     *
     * @param x      the x position of the region
     * @param y      the y position of the region
     * @param width  the width of the region
     * @param height the height of the region
     * @return true if the mouse is clicked and is over the region, false if not
     * @see InputUtils#isAreaClicked(int, int, int, int, boolean)
     */
    public static boolean isAreaClicked(int x, int y, int width, int height) {
        return isAreaClicked(x, y, width, height, false);
    }

    /**
     * Checks whether the mouse is clicked or not.
     *
     * @param ignoreBlock if true, will ignore {@link InputUtils#blockClicks(boolean)}
     * @return true if the mouse is clicked, false if not
     */
    public static boolean isClicked(boolean ignoreBlock) {
        return OneConfigGui.INSTANCE != null && OneConfigGui.INSTANCE.mouseDown && !Mouse.isButtonDown(0) && (!blockClicks || ignoreBlock);
    }

    /**
     * Checks whether the mouse is clicked or not.
     *
     * @return true if the mouse is clicked, false if not
     * @see InputUtils#isClicked(boolean)
     */
    public static boolean isClicked() {
        return isClicked(false);
    }

    /**
     * Gets the current mouse X position.
     * <p>
     * All values returned from this class are not scaled to Minecraft's GUI scale.
     * For scaled values, see {@link gg.essential.universal.UMouse}.
     * </p>
     *
     * @return the current mouse X position
     */
    public static int mouseX() {
        if (OneConfigGui.INSTANCE == null) return Mouse.getX();
        return (int) (Mouse.getX() / OneConfigGui.INSTANCE.getScaleFactor());
    }

    /**
     * Gets the current mouse Y position.
     * <p>
     * All values returned from this class are not scaled to Minecraft's GUI scale.
     * For scaled values, see {@link gg.essential.universal.UMouse}.
     * </p>
     *
     * @return the current mouse Y position
     */
    public static int mouseY() {
        if (OneConfigGui.INSTANCE == null) return UResolution.getWindowHeight() - Math.abs(Mouse.getY());
        return (int) ((UResolution.getWindowHeight() - Math.abs(Mouse.getY())) / OneConfigGui.INSTANCE.getScaleFactor());
    }

    /**
     * Should be used if there is something above other components, and you don't want it clicking trough.
     */
    public static void blockClicks(boolean value) {
        blockClicks = value;
    }

    /**
     * Whether clicks are blocked
     *
     * @return true if clicks are blocked, false if not
     */
    public static boolean isBlockingClicks() {
        return blockClicks;
    }
}
