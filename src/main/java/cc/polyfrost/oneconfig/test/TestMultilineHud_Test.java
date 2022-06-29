package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.hud.TextHud;
import net.minecraft.client.Minecraft;

import java.util.List;

public class TestMultilineHud_Test extends TextHud {
    public TestMultilineHud_Test() {
        super(true);
    }

    @Override
    protected void getLines(List<String> lines) {
        lines.clear();
        lines.add(String.valueOf(System.currentTimeMillis()));
        lines.add(String.valueOf(Minecraft.getSystemTime()));
    }
}
