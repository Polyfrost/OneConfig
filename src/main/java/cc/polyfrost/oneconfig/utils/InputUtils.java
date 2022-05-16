package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import gg.essential.universal.UResolution;
import org.lwjgl.input.Mouse;

public class InputUtils {
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

    public static boolean isAreaClicked(int x, int y, int width, int height, boolean ignoreBlock) {
        return isAreaHovered(x, y, width, height) && isClicked(ignoreBlock);
    }

    public static boolean isAreaClicked(int x, int y, int width, int height) {
        return isAreaClicked(x, y, width, height, false);
    }

    public static boolean isClicked(boolean ignoreBlock) {
        return OneConfigGui.INSTANCE != null && OneConfigGui.INSTANCE.mouseDown && !Mouse.isButtonDown(0) && (!blockClicks || ignoreBlock);
    }

    public static boolean isClicked() {
        return isClicked(false);
    }

    public static int mouseX() {
        if (OneConfigGui.INSTANCE == null) return Mouse.getX();
        return (int) (Mouse.getX() / OneConfigGui.INSTANCE.getScaleFactor());
    }

    public static int mouseY() {
        if (OneConfigGui.INSTANCE == null) return UResolution.getWindowHeight() - Math.abs(Mouse.getY());
        return (int) ((UResolution.getWindowHeight() - Math.abs(Mouse.getY())) / OneConfigGui.INSTANCE.getScaleFactor());
    }

    /**
     * Should be used if there is something above other components and you don't want it clicking trough
     */
    public static void blockClicks(boolean value) {
        blockClicks = value;
    }

    public static boolean isBlockingClicks() {
        return blockClicks;
    }
}
