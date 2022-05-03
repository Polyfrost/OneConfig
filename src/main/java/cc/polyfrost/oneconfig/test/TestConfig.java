package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.config.annotations.ConfigPage;
import cc.polyfrost.oneconfig.config.annotations.Option;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionType;
import cc.polyfrost.oneconfig.config.data.PageLocation;
import cc.polyfrost.oneconfig.config.interfaces.Config;

public class TestConfig extends Config {

    @Option(
            name = "Test dual thing",
            subcategory = "Test",
            min = 3f, max = 127f,
            type = OptionType.SLIDER
    )
    public static float sliderText;

    @Option(
            name = "Test string",
            subcategory = "Test",
            options = {"NO", "YES"},
            type = OptionType.DUAL_OPTION
    )
    public static boolean switchTest1;

    @Option(
            name = "Test dual option",
            subcategory = "Test",
            options = {"HI", "BYE"},
            type = OptionType.DUAL_OPTION
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
            name = "Test checkbox",
            subcategory = "Other subcategory",
            type = OptionType.CHECKBOX
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

    @Option(
            name = "Slider",
            subcategory = "Sliders",
            type = OptionType.SLIDER,
            category = "Sliders",
            size = 2,
            min = 0,
            max = 25
    )
    public static int slider1;
    @Option(
            name = "Stepped Slider",
            subcategory = "Sliders",
            type = OptionType.SLIDER,
            category = "Sliders",
            size = 2,
            min = 0,
            max = 30,
            step = 2
    )
    public static float slider2;


    public TestConfig() {
        super(new Mod("hacks", ModType.UTIL_QOL, "ShadyDev", "1.0"), "hacksConfig.json");
    }
}

