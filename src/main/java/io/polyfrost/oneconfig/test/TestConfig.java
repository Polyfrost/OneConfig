package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.config.annotations.Category;
import io.polyfrost.oneconfig.config.annotations.HudComponent;
import io.polyfrost.oneconfig.config.annotations.Switch;
import io.polyfrost.oneconfig.config.annotations.TextField;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.config.data.ModType;
import io.polyfrost.oneconfig.config.interfaces.Config;

public class TestConfig extends Config {

    @Switch(name = "Cool Switch")
    public static boolean toggle = false;

    @Category(name = "Cool Category")
    public static class category {
        @TextField(name = "Cool text field")
        public static String text = "Very cool text";
    }

    @HudComponent(name = "text hud")
    public static TestHud testTextHud = new TestHud();
    @HudComponent(name = "text hud v2")
    public static TestHud testTextHud2 = new TestHud();

    public TestConfig() {
        super(new ModData("hacks", ModType.QOL, "ShadyDev", "1.0"), "hacksConfig.json");
    }
}
