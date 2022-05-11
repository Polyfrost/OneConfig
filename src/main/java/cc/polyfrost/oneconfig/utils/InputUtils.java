package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.OneConfig;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import org.lwjgl.input.Mouse;

public class InputUtils {
    /**
     * function to determine weather the mouse is currently over a specific region. Uses the current nvgScale to fix to any scale.
     *
     * @return true if mouse is over region, false if not.
     */
    public static boolean isAreaHovered(int x, int y, int width, int height) {
        int mouseX = mouseX();
        int mouseY = mouseY();
        return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;       // TODO add scaling info
    }

    public static boolean isAreaClicked(int x, int y, int width, int height) {
        return isAreaHovered(x, y, width, height) && isClicked();
    }

    public static boolean isClicked() {
        return OneConfigGui.INSTANCE != null && OneConfigGui.INSTANCE.mouseDown && !Mouse.isButtonDown(0);
    }

    public static int mouseX() {
        if (OneConfigGui.INSTANCE == null) return Mouse.getX();
        return (int) (Mouse.getX() / OneConfigGui.INSTANCE.getScaleFactor());
    }

    public static int mouseY() {
        if (OneConfigGui.INSTANCE == null) return OneConfig.getDisplayHeight() - Math.abs(Mouse.getY());
        return (int) ((OneConfig.getDisplayHeight() - Math.abs(Mouse.getY())) / OneConfigGui.INSTANCE.getScaleFactor());
    }
}
