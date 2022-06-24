package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.hud.SingleTextHud;
import net.minecraft.client.Minecraft;

public class TestHud_Test extends SingleTextHud {
    @Switch(
            name = "Custom Option"
    )
    public boolean yes;

    public TestHud_Test(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }

    @Override
    public String getDefaultTitle() {
        return "FPS";
    }

    @Override
    public String getText() {
        return Integer.toString(Minecraft.getDebugFPS());
    }
}
