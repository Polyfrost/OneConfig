package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.hud.TextHud;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class TestHud_Test extends TextHud {
    public TestHud_Test(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }

    @Override
    public List<String> getLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("FPS: " + Minecraft.getDebugFPS());
        if (hasSecondLine) lines.add(secondLine);
        return lines;
    }

    @Switch(
            name = "Has Second Line"
    )
    public boolean hasSecondLine = false;

    @Text(
            name = "Second Line Text"
    )
    public String secondLine = "Epic text";
}
