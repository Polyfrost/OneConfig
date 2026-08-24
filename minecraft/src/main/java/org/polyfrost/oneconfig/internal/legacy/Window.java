package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

// We need this shim because Window#getWidth/getHeight return GUI-scaled dimensions in legacy but framebuffer dimensions in modern.
public final class Window {
    private final net.minecraft.client.render.Window scaled;

    public Window(Minecraft minecraft) {
        this.scaled = new net.minecraft.client.render.Window(minecraft);
    }

    public long getWindow() {
        return Display.getHandle();
    }

    public int getWidth() {
        return Display.getWidth();
    }

    public int getHeight() {
        return Display.getHeight();
    }

    public int getScreenWidth() {
        return Display.getScreenWidth();
    }

    public int getScreenHeight() {
        return Display.getScreenHeight();
    }

    public int getRefreshRate() {
        return Display.getDisplayMode().getFrequency();
    }

    public int getGuiScaledWidth() {
        return this.scaled.getWidth();
    }

    public int getGuiScaledHeight() {
        return this.scaled.getHeight();
    }

    public int getGuiScale() {
        return this.scaled.getScale();
    }
}
*///?}
