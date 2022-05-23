package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.config.annotations.ConfigPage;
import cc.polyfrost.oneconfig.config.annotations.Option;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.data.*;
import cc.polyfrost.oneconfig.config.interfaces.Config;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TestConfig extends Config {

    @Option(
            name = "Very cool HUD",
            subcategory = "Test",
            type = OptionType.HUD,
            size = 2
    )
    public static TestHud TestHud = new TestHud(true, 500, 500);

    @Option(
            name = "This is all still in beta",
            subcategory = "Test",
            type = OptionType.INFO,
            infoType = InfoType.INFO,
            size = 2
    )
    public static boolean ignored;

    @Option(
            name = "Keybind (1x)",
            subcategory = "Test",
            type = OptionType.KEYBIND
    )
    public static OneKeyBind oneKeyBind = new OneKeyBind( 18, 80);

    @Option(
            name = "Keybind (2x)",
            subcategory = "Test",
            type = OptionType.KEYBIND,
            size = 2
    )
    public static OneKeyBind oneKeyBind2 = new OneKeyBind(27, 80);

    @Option(
            name = "Crash game",
            subcategory = "Test",
            type = OptionType.BUTTON,
            buttonText = "Crash!"
    )
    public static Runnable runnable = () -> FMLCommonHandler.instance().exitJava(69, false);

    @Option(
            name = "Crash game",
            subcategory = "Test",
            type = OptionType.BUTTON,
            size = 2,
            buttonText = "Crash!"
    )
    public static Runnable runnable2 = () -> FMLCommonHandler.instance().exitJava(69, false);

    @Option(
            name = "Test color selector",
            subcategory = "Test",
            type = OptionType.COLOR,
            size = 2
    )
    public static OneColor colorTest = new OneColor(126, 137, 42);

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

    @Option(
            name = "Test option",
            subcategory = "Test",
            options = {"Hello", "World", "Fish", "Cat"},
            type = OptionType.UNI_SELECTOR,
            size = 2
    )
    public static int uniSelector1;

    @Option(
            name = "Test option",
            subcategory = "Test",
            options = {"Hello", "World", "Fish", "Cat"},
            type = OptionType.UNI_SELECTOR,
            size = 2
    )
    public static int uniSelector2;

    @Option(
            name = "Test option",
            subcategory = "Test",
            options = {"Hello", "World", "Fish", "Cat"},
            type = OptionType.UNI_SELECTOR,
            size = 2
    )
    public static int uniSelector3;

    @Option(
            name = "Test option",
            subcategory = "Test",
            options = {"Hello", "World", "Fish", "Cat"},
            type = OptionType.UNI_SELECTOR,
            size = 2
    )
    public static int uniSelector4;

    @Option(
            name = "Test option",
            subcategory = "Test",
            options = {"Hello", "World", "Fish", "Cat"},
            type = OptionType.UNI_SELECTOR,
            size = 2
    )
    public static int uniSelector5;

    @Option(
            name = "Test option",
            subcategory = "Test",
            options = {"Hello", "World", "Fish", "Cat"},
            type = OptionType.UNI_SELECTOR,
            size = 2
    )
    public static int uniSelector6;

    @Option(
            name = "Test option",
            subcategory = "Test",
            options = {"Hello", "World", "Fish", "Cat"},
            type = OptionType.UNI_SELECTOR,
            size = 2
    )
    public static int uniSelector7;

    @Option(
            name = "Arrow Uniselect (1x)",
            subcategory = "Test",
            options = {"Option", "World", "Fish"},
            type = OptionType.UNI_SELECTOR,
            size = 2
    )
    public static int uniSelector8;

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

    @Option(
            name = "Slider",
            type = OptionType.SLIDER,
            category = "subcategory test",
            min = 5,
            max = 3287
    )
    public static float slider = 26;

    @Option(
            name = "Slider",
            type = OptionType.SLIDER,
            category = "subcategory test",
            min = 5,
            max = 3287
    )
    public static float slider10 = 26;

    @Option(
            name = "Slider",
            type = OptionType.SLIDER,
            category = "subcategory test",
            min = 5,
            max = 3287,
            subcategory = "Second subcategory"
    )
    public static float slider11 = 26;

    @Option(
            name = "Header Test",
            type = OptionType.HEADER,
            category = "subcategory test",
            subcategory = "Second subcategory",
            size = 2
    )
    public static boolean somethingHere;


    @Option(
            name = "Slider",
            type = OptionType.SLIDER,
            category = "subcategory test",
            min = 5,
            max = 3287,
            subcategory = "Second subcategory"
    )
    public static float slider12 = 26;

    @ConfigPage(
            name = "Test page",
            location = PageLocation.TOP,
            category = "subcategory test",
            subcategory = "Second subcategory"
    )
    public static TestPage testPage23 = new TestPage();

    @ConfigPage(
            name = "Test page",
            location = PageLocation.BOTTOM,
            category = "subcategory test",
            subcategory = "Second subcategory"
    )
    public static TestPage testPage24 = new TestPage();

    @Option(
            name = "Slider",
            type = OptionType.SLIDER,
            category = "subcategory test",
            min = 5,
            max = 3287
    )
    public static float slider13 = 26;


    public TestConfig() {
        super(new Mod("hacks", ModType.UTIL_QOL, SVGs.CASH_DOLLAR.filePath), "hacksConfig.json");
        addDependency("switchTest5", () -> switchTest4);
        addDependency("Test page.testDescription", () -> false);
    }
}

