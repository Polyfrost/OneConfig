package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.config.annotations.Option;
import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.config.data.ModType;
import io.polyfrost.oneconfig.config.data.OptionType;
import io.polyfrost.oneconfig.config.interfaces.Config;

public class TestConfig extends Config {

    @Option(
            name = "Test dual thing",
            description = "Best description",
            subcategory = "Test",
            optionLeft = "FUNNY", optionRight = "not funny",
            type = OptionType.DUAL_OPTION
    )
    public static boolean switchTest;

    @Option(
            name = "Test string",
            description = "Best description",
            subcategory = "Test",
            optionLeft = "HI", optionRight = "BYE",
            type = OptionType.DUAL_OPTION
    )
    public static boolean switchTest1;

    @Option(
            name = "Test dual option",
            description = "Best description",
            subcategory = "Test",
            optionRight = "cool", optionLeft = "not cool",
            type = OptionType.DUAL_OPTION,
            size = 2
    )
    public static boolean switchTest2;

    @Option(
            name = "Test option",
            description = "Best description",
            subcategory = "Test",
            options = {"Hello", "World", "Fish", "Cat"},
            type = OptionType.UNI_SELECTOR
    )
    public static int switchTest3;

    @Option(
            name = "Test Page",
            type = OptionType.PAGE,
            subcategory = "Test"
    )
    public static TestPage testPage = new TestPage();

    @Option(
            name = "Test switch",
            description = "Best description",
            subcategory = "Other subcategory",
            type = OptionType.SWITCH
    )
    public static boolean switchTest4;

    @Option(
            name = "Test switch",
            description = "Best description",
            subcategory = "Other subcategory",
            type = OptionType.SWITCH
    )
    public static boolean switchTest5;

    @Option(
            name = "Test check",
            description = "Best description",
            subcategory = "Other subcategory",
            type = OptionType.CHECKBOX
    )
    public static boolean switchTest6;

    public TestConfig() {
        super(new Mod("hacks", ModType.UTIL_QOL, "ShadyDev", "1.0"), "hacksConfig.json");
    }
}

