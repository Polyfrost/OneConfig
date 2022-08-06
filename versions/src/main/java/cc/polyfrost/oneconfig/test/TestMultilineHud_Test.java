package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.hud.TextHud;

import java.util.List;

public class TestMultilineHud_Test extends TextHud {
    public TestMultilineHud_Test() {
        super(true);
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        lines.add(String.valueOf(System.currentTimeMillis()));
        lines.add("HEY!");
    }
}
