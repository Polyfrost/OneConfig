package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.config.annotations.ConfigPage;
import io.polyfrost.oneconfig.config.annotations.Option;
import io.polyfrost.oneconfig.config.data.Mod;
import io.polyfrost.oneconfig.config.data.ModType;
import io.polyfrost.oneconfig.config.data.OptionType;
import io.polyfrost.oneconfig.config.data.PageLocation;
import io.polyfrost.oneconfig.config.interfaces.Config;

public class TestConfig extends Config {

    @Option(
            name = "Test dual thing",
            subcategory = "Test",
            optionLeft = "FUNNY", optionRight = "not funny",
            type = OptionType.DUAL_OPTION
    )
    public static boolean switchTest;

    @Option(
            name = "Test string",
            subcategory = "Test",
            optionLeft = "HI", optionRight = "BYE",
            type = OptionType.DUAL_OPTION
    )
    public static boolean switchTest1;

    @Option(
            name = "Test dual option",
            subcategory = "Test",
            optionRight = "cool", optionLeft = "not cool",
            type = OptionType.DUAL_OPTION,
            size = 2
    )
    public static boolean switchTest2;

    @Option(
            name = "Test option",
            subcategory = "Test",
            options = {"Hello", "World", "Fish", "Cat"},
            type = OptionType.UNI_SELECTOR
    )
    public static int switchTest3;

    @ConfigPage(
            name = "Test Page",
            location = PageLocation.TOP
    )
    public static TestPage testPage = new TestPage();

    @ConfigPage(
            name = "Test Page width description",
            description = "Wow, an epic description",
            location = PageLocation.BOTTOM
    )
    public static TestPage testPage2 = new TestPage();

    @Option(
            name = "Test switch",
            subcategory = "Other subcategory",
            type = OptionType.SWITCH
    )
    public static boolean switchTest4;

    @Option(
            name = "Test switch",
            subcategory = "Other subcategory",
            type = OptionType.SWITCH
    )
    public static boolean switchTest5;

    @Option(
            name = "Favorite food",
            subcategory = "Dropdowns",
            type = OptionType.DROPDOWN,
            category = "Dropdowns",
            options = {"Taco", "Pizza", "Hamburger", "Paimon"}
    )
    public static int dropdown1;

    @Option(
            name = "Favorite food",
            subcategory = "Dropdowns",
            type = OptionType.DROPDOWN,
            category = "Dropdowns",
            options = {"Taco", "Pizza", "Hamburger", "Paimon"}
    )
    public static int dropdown2;

    @Option(
            name = "Favorite food",
            subcategory = "Dropdowns",
            type = OptionType.DROPDOWN,
            category = "Dropdowns",
            size = 2,
            options = {"Taco", "Pizza", "Hamburger", "Paimon"}
    )
    public static int dropdown3;

    public TestConfig() {
        super(new Mod("hacks", ModType.UTIL_QOL, "ShadyDev", "1.0"), "hacksConfig.json");
    }
}

