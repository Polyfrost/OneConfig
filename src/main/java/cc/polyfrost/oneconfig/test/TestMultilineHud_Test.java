package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.hud.TextHud;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;

import java.util.List;

public class TestMultilineHud_Test extends TextHud {
    public TestMultilineHud_Test() {
        super(true);
    }

    @Override
    public List<String> update() {
        return Lists.newArrayList(String.valueOf(System.currentTimeMillis()), String.valueOf(Minecraft.getSystemTime()));
    }
}
