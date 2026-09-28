package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_DisplayMode;
import pl.tomgirl.lenis.window.DisplaySdl;

// We need this shim because Window#getWidth/getHeight return GUI-scaled dimensions in legacy but framebuffer dimensions in modern.
public final class Window {
    private final net.minecraft.client.render.Window scaled;
    private final DisplaySdl display = DisplaySdl.instance();

    public Window(Minecraft minecraft) {
        this.scaled = new net.minecraft.client.render.Window(minecraft);
    }

    public long getWindow() {
        return display.getHandle();
    }

    public int getWidth() {
        return display.getWidth();
    }

    public int getHeight() {
        return display.getHeight();
    }

    public int getScreenWidth() {
        return display.getWindowWidth();
    }

    public int getScreenHeight() {
        return display.getWindowHeight();
    }

    public int getRefreshRate() {
        long handle = display.getHandle();
        SDL_DisplayMode mode = SDLVideo.SDL_GetWindowFullscreenMode(handle);
        if (mode == null) {
            int displayId = SDLVideo.SDL_GetDisplayForWindow(handle);
            mode = displayId == 0 ? null : SDLVideo.SDL_GetCurrentDisplayMode(displayId);
        }
        return mode == null ? 0 : (int) mode.refresh_rate();
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
