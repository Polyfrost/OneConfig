package io.polyfrost.oneconfig.utils;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

public class InputUtils {
    /**
     * function to determine weather the mouse is currently over a specific region. Uses the current nvgScale to fix to any scale.
     *
     * @return true if mouse is over region, false if not.
     */
    public static boolean isAreaHovered(int x, int y, int width, int height) {
        int mouseX = Mouse.getX();
        int mouseY = Minecraft.getMinecraft().displayHeight - Math.abs(Mouse.getY());
        return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;       // TODO add scaling info
    }

    public static boolean isClicked(int x, int y, int width, int height) {
        return isAreaHovered(x, y, width, height) && Mouse.isButtonDown(0);        // TODO make actually do what its meant to do (only 1 event)
    }

    public static int mouseX() {
        return Mouse.getX();
    }

    public static int mouseY() {
        return Minecraft.getMinecraft().displayHeight - Math.abs(Mouse.getY());
    }
}
