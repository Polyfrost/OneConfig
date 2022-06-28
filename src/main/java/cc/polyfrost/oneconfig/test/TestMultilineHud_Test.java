package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.hud.MultiTextHud;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;

import java.util.List;

public class TestMultilineHud_Test extends MultiTextHud {
    public TestMultilineHud_Test() {
        super(true);
    }

    @Override
    protected List<String> getLines() {
        return Lists.newArrayList(String.valueOf(System.currentTimeMillis()), String.valueOf(Minecraft.getSystemTime()));
    }
}
