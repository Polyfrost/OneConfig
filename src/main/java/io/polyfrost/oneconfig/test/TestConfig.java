package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.config.annotations.Option;
import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.config.data.ModType;
import io.polyfrost.oneconfig.config.data.OptionType;
import io.polyfrost.oneconfig.config.interfaces.Config;

public class TestConfig extends Config {

    @Option(
            name = "Test switch",
            description = "Best description",
            subcategory = "Test",
            type = OptionType.SWITCH
    )
    public static boolean switchTest;

    @Option(
            name = "Test Page",
            type = OptionType.PAGE,
            subcategory = "Test"
    )
    public static TestPage testPage = new TestPage();

    public TestConfig() {
        super(new Mod("hacks", ModType.UTIL_QOL, "ShadyDev", "1.0"), "hacksConfig.json");
    }
}

