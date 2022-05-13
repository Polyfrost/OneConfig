package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import net.minecraft.client.Minecraft;

public class TestHud extends BasicHud {
    public TestHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }

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
        RenderManager.drawScaledString("FPS: " + Minecraft.getDebugFPS(), x, y, 0xffffff, false, scale);
    }
}
