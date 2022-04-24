package io.polyfrost.oneconfig.utils;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

public class InputUtils {
    /**
     * function to determine weather the mouse is currently over a specific region. Uses the current nvgScale to fix to any scale.
     * @return true if mouse is over region, false if not.
     */
    public static boolean isAreaHovered(int x, int y, int width, int height) {
        int mouseX = Mouse.getX();
        int mouseY = Minecraft.getMinecraft().displayHeight - Math.abs(Mouse.getY());
        return mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height;       // TODO add scaling info
    }
}
