package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.hud.interfaces.BasicHud;
import io.polyfrost.oneconfig.renderer.Renderer;
import net.minecraft.client.Minecraft;

public class TestHud extends BasicHud {

    @Override
    public int getWidth(float scale) {
        return (int) (Minecraft.getMinecraft().fontRendererObj.getStringWidth("FPS: " + Minecraft.getDebugFPS()) * scale);
    }

    @Override
    public int getHeight(float scale) {
        return (int) (9 * scale);
    }

    @Override
    public void draw(int x, int y, float scale) {
        Renderer.drawTextScale("FPS: " + Minecraft.getDebugFPS(), x, y, 0xffffff, false, scale);
    }
}
