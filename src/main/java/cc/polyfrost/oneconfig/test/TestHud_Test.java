package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.hud.SingleTextHud;

public class TestHud_Test extends SingleTextHud {
    int times = 0;
    @Switch(
            name = "Custom Option"
    )
    public boolean yes;

    public TestHud_Test() {
        super("Time", true);
    }

    @Override
    public String getText() {
        times++;
        return String.valueOf(times);
    }
}
