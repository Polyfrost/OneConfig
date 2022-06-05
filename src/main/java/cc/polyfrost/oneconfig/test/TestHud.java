package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.hud.elements.TextHud;
import net.minecraft.client.Minecraft;

public class TestHud extends TextHud {
    public TestHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }

    @Override
    public String getText() {
        return "FPS: " + Minecraft.getDebugFPS();
    }
}
