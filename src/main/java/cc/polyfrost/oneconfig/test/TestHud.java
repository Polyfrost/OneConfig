package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.config.annotations.Option;
import cc.polyfrost.oneconfig.config.data.OptionType;
import cc.polyfrost.oneconfig.hud.TextHud;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class TestHud extends TextHud {
    public TestHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }

    @Override
    public List<String> getLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("FPS: " + Minecraft.getDebugFPS());
        if (hasSecondLine) lines.add(secondLine);
        return lines;
    }

    @Option(
            name = "Enable Second Line",
            type = OptionType.SWITCH
    )
    public boolean hasSecondLine = false;

    @Option(
            name = "Second Line Text",
            type = OptionType.TEXT
    )
    public String secondLine = "Epic text";
}
